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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.ArrayList;
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
	private final Object oosLock = new Object();
	private final AtomicLong nextRequestId = new AtomicLong();
	
	private final Process process;
	private final OutputStream os;
	private final ObjectOutputStream oos;
	private final ResponseReader responseReader;
	
	private static List<String> getMvnCommand(Path mvnHome, List<String> mvnOpts)
	{
		List<String> r = new ArrayList<>(mvnOpts.size() + 1);
		r.add(mvnHome.toAbsolutePath()
				.resolve("bin")
				.resolve(System.getProperty("os.name").startsWith("Windows") ?
						"mvn.cmd"
						: "mvn")
				.toString());
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
			os = process.getOutputStream();
			try
			{
				oos = new ObjectOutputStream(os);
				try
				{
					responseReader = new ResponseReader(process::getInputStream);
					new Thread(responseReader).start();
					if (!responseReader.waitForState())
					{
						throw new SessionException("mavenStopped");
					}
					ok = true;
				}
				finally
				{
					if (!ok)
					{
						oos.close();
					}
				}
			}
			finally
			{
				if (!ok)
				{
					os.close();
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
				process.destroyForcibly();
			}
		}
	}
	
	private <R extends Result> R execute(Command<R> command) throws IOException
	{
		long requestId = nextRequestId.getAndIncrement();
		Request req = new Request();
		req.setRequestId(requestId);
		req.setCommand(command);
		
		synchronized(oosLock)
		{
			oos.writeObject(req);
			oos.flush();
		}
		
		Response res = responseReader.waitForResponse(requestId);
		if (res == null)
		{
			throw new IllegalStateException("Maven stopped");
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
		catch (IOException e)
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
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	private void close0() throws IOException
	{
		try
		{
			oos.close();
		}
		finally
		{
			os.close();
		}
	}
}
