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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.kerbaya.session.command.Command;
import com.kerbaya.session.command.CommandException;
import com.kerbaya.session.command.Result;

public class SessionInstance implements AutoCloseable
{
	private final Object oosLock = new Object();
	private final AtomicLong nextRequestId = new AtomicLong();
	
	private final Process process;
	private final OutputStream os;
	private final ObjectOutputStream oos;
	private final ResponseReader responseReader;
	
	public SessionInstance(
			List<String> command, 
			Path dir, 
			Map<String, String> environment) 
					throws IOException, InterruptedException
	{
		ProcessBuilder pb = new ProcessBuilder(command)
				.redirectError(Redirect.INHERIT);
		
		if (dir != null)
		{
			pb = pb.directory(dir.toFile());
		}
		
		if (environment != null)
		{
			Map<String, String> pbEnv = pb.environment();
			pbEnv.clear();
			pbEnv.putAll(environment);
		}
		
		boolean ok = false;
		
		process = pb.start();
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
						throw new IllegalStateException("Maven stopped");
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
		finally
		{
			if (!ok)
			{
				process.destroyForcibly();
			}
		}
	}
	
	private Result execute0(Command<?> command) throws IOException, InterruptedException
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
			return res.getResult();
		}
		
		throw new CommandException(ei);
	}
	
	public <R extends Result> R execute(Command<R> command)
	{
		try
		{
			@SuppressWarnings("unchecked")
			R result = (R) execute0(command);
			return result;
		}
		catch (IOException | InterruptedException e)
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
}
