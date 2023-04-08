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
import java.util.function.Function;

import com.kerbaya.session.command.CommandException;
import com.kerbaya.session.command.Result;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class RequestHandler implements Function<Request, Runnable>
{
	private final Object outputLock = new Object();
	
	private final ObjectOutputStream oos;
	private final CommandHandler commandHandler;
	
	@Override
	public Runnable apply(Request t)
	{
		return new RequestTask(t);
	}
	
	private void processRequest(Request req) throws IOException
	{
		Response res = new Response();
		res.setRequestId(req.getRequestId());
		ExceptionInfo ei = null;
		Result result;
		try
		{
			result = req.getCommand().accept(commandHandler);
			ei = null;
		}
		catch (CommandException e)
		{
			result = null;
			ei = e.getExceptionInfo();
		}
		catch (RuntimeException | Error e)
		{
			result = null;
			ei = ExceptionInfoFactory.INSTANCE.apply(e);
		}
		
		res.setResult(result);
		res.setExceptionInfo(ei);
		
		synchronized(outputLock)
		{
			oos.writeObject(res);
			oos.flush();
		}
	}
	
	@AllArgsConstructor
	private final class RequestTask implements Runnable
	{
		private final Request request;
		
		@Override
		public void run()
		{
			try
			{
				processRequest(request);
			}
			catch (IOException e)
			{
				throw new IllegalStateException(e);
			}
		}
	}
}
