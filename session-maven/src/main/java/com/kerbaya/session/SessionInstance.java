/*
 * Copyright 2023 Kerbaya Software
 * 
 * This file is part of session-maven-plugin. 
 * 
 * session-maven-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * session-maven-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with session-maven-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kerbaya.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.kerbaya.session.internal.Command;
import com.kerbaya.session.internal.Request;
import com.kerbaya.session.internal.Response;
import com.kerbaya.session.internal.Result;
import com.kerbaya.session.internal.SessionPluginInfo;
import com.kerbaya.session.internal.resolve_artifacts.ResolveArtifactsCommand;
import com.kerbaya.session.internal.resolve_artifacts.ResolveArtifactsResult;

public class SessionInstance implements AutoCloseable
{
	private final Object writeLock = new Object();
	private final AtomicLong nextRequestId = new AtomicLong();
	
	private final Object readLock = new Object();
	private final Map<Long, Response> responseMap = new HashMap<>();
	
	private final Process process;
	private final InputStream is;
	private final OutputStream os;
	
	private volatile boolean reading;
	
	private static List<String> getMvnCommand(Path mvnHome, List<String> mvnOpts)
	{
		List<String> r = new ArrayList<>(mvnOpts.size() + 2);
		r.add(mvnHome.toAbsolutePath()
				.resolve("bin")
				.resolve(System.getProperty("os.name").startsWith("Windows") ?
						"mvn.cmd"
						: "mvn")
				.toString());
		r.add("-q");
		r.addAll(mvnOpts);
		return r;
	}
	
	public SessionInstance(
			Path mvnHome,
			List<String> mvnOpts,
			Path projectDir,
			Map<String, String> environment)
	{
		this(
				getMvnCommand(mvnHome, mvnOpts),
				projectDir,
				environment);
	}
	
	public SessionInstance(
			List<String> mvnCommand, 
			Path projectDir, 
			Map<String, String> environment) 
	{
		List<String> command = new ArrayList<>(mvnCommand.size() + 1);
		command.addAll(mvnCommand);
		command.add(SessionPluginInfo.GOAL);
		
		ProcessBuilder pb = new ProcessBuilder(command)
				.redirectError(Redirect.INHERIT);
		
		if (projectDir != null)
		{
			pb = pb.directory(projectDir.toFile());
		}
		
		if (environment != null)
		{
			Map<String, String> pbEnv = pb.environment();
			pbEnv.clear();
			pbEnv.putAll(environment);
		}
		
		boolean ok = false;
		
		try
		{
			process = pb.start();
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
		
		try
		{
			is = process.getInputStream();
			try
			{
				if (!new ByteSequenceFinder(is).find(
						SessionPluginInfo.READY_PROMPT.getBytes(SessionPluginInfo.READY_PROMPT_ENCODING)))
				{
					throw new IllegalStateException("mavenStopped");
				}
				
				os = process.getOutputStream();
				ok = true;
			}
			finally
			{
				if (!ok)
				{
					is.close();
				}
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
		finally
		{
			if (!ok)
			{
				if (process.isAlive())
				{
					process.destroyForcibly();
				}
			}
		}
	}
	
	private static Response readNext(InputStream is) throws ClassNotFoundException, IOException
	{
		byte[] size = new byte[Integer.BYTES];
		
		if (!ByteUtils.fill(is, size))
		{
			return null;
		}
		
		byte[] resBuffer = new byte[ByteBuffer.wrap(size).getInt(0)];
		
		if (!ByteUtils.fill(is, resBuffer))
		{
			throw new IllegalStateException("incompleteResponse");
		}
		
		try (ByteArrayInputStream bais = new ByteArrayInputStream(resBuffer);
				ObjectInputStream ois = new ObjectInputStream(bais))
		{
			return (Response) ois.readObject();
		}
	}

	private Response readResponse(long myReqId) throws ClassNotFoundException, IOException, InterruptedException
	{
		Long myReqIdKey = myReqId;
		Response res;
		
		synchronized(readLock)
		{
			res = responseMap.remove(myReqIdKey);
			if (res != null)
			{
				return res;
			}
			
			while (reading)
			{
				readLock.wait();
				res = responseMap.remove(myReqIdKey);
				if (res != null)
				{
					return res;
				}
			}
			
			reading = true;
		}
		
		try
		{
			while ((res = readNext(is)) != null)
			{
				long resReqId = res.getRequestId();
				if (resReqId == myReqId)
				{
					return res;
				}
				
				synchronized(readLock)
				{
					responseMap.put(resReqId, res);
					readLock.notifyAll();
				}
			}
		}
		finally
		{
			synchronized(readLock)
			{
				reading = false;
				readLock.notifyAll();
			}
		}
		
		return null;
	}
	
	private <R extends Result> R execute(Command<R> command) 
			throws IOException, ClassNotFoundException, InterruptedException
	{
		long requestId = nextRequestId.getAndIncrement();
		Request req = new Request();
		req.setRequestId(requestId);
		req.setCommand(command);
		
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
		{
			try (ObjectOutputStream oos = new ObjectOutputStream(baos))
			{
				oos.writeObject(req);
			}
			
			byte[] size = ByteBuffer.allocate(Integer.BYTES).putInt(baos.size()).array();
			
			synchronized(writeLock)
			{
				os.write(size);
				os.flush();
				baos.writeTo(os);
				os.flush();
			}
		}
		
		Response res = readResponse(requestId);
		if (res == null)
		{
			throw new IllegalStateException("mavenStopped");
		}
		
		ExceptionInfo ei = res.getExceptionInfo();
		if (ei == null)
		{
			@SuppressWarnings("unchecked")
			R r = (R) res.getResult();
			return r;
		}
		
		throw new CommandException(ei);
	}
	
	public List<ArtifactResult> resolveArtifacts(List<? extends ArtifactCoords> artifactList)
	{
		ResolveArtifactsCommand rac = new ResolveArtifactsCommand();
		rac.setQueries(artifactList.stream()
				.map(SerializableArtifactCoordsFactory.INSTANCE)
				.collect(Collectors.toCollection(ArrayList::new)));
		ResolveArtifactsResult r;

		try
		{
			r = execute(rac);
		}
		catch (IOException | ClassNotFoundException | InterruptedException e)
		{
			throw new IllegalStateException(e);
		}
		
		return r.getResults();
	}
	
	@Override
	public void close()
	{
		try
		{
			close0();
		}
		catch (IOException | InterruptedException e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	private void close0() throws IOException, InterruptedException
	{
		try
		{
			synchronized(writeLock)
			{
				os.close();
			}
		}
		finally
		{
			try
			{
				synchronized(readLock)
				{
					while (reading)
					{
						readLock.wait();
					}
					is.close();
				}
			}
			finally
			{
				process.waitFor();
			}
		}
	}
}
