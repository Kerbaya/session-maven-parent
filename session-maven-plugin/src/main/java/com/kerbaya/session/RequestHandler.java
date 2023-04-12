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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.function.Function;

import com.kerbaya.session.internal.Request;
import com.kerbaya.session.internal.Response;
import com.kerbaya.session.internal.Result;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class RequestHandler implements Function<byte[], Runnable>
{
	private final Object outputLock = new Object();
	
	private final OutputStream os;
	private final CommandHandler commandHandler;
	
	@Override
	public Runnable apply(byte[] reqBytes)
	{
		return new RequestTask(reqBytes);
	}
	
	private void processRequest(byte[] reqBytes) throws IOException, ClassNotFoundException
	{
		Request req;
		try (ByteArrayInputStream bais = new ByteArrayInputStream(reqBytes);
				ObjectInputStream ois = new ObjectInputStream(bais))
		{
			req = (Request) ois.readObject();
		}
		
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
			ei = new SerializableExceptionInfoFactory().apply(e);
		}
		
		res.setResult(result);
		res.setExceptionInfo(ei);
		
		// thread-level buffers
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
		{
			try (ObjectOutputStream oos = new ObjectOutputStream(baos))
			{
				oos.writeObject(res);
			}
			
			byte[] size = ByteBuffer.allocate(Integer.BYTES).putInt(baos.size()).array();
			
			synchronized(outputLock)
			{
				os.write(size);
				os.flush();
				baos.writeTo(os);
				os.flush();
			}
		}
	}
	
	@AllArgsConstructor
	private final class RequestTask implements Runnable
	{
		private final byte[] reqBytes;
		
		@Override
		public void run()
		{
			try
			{
				processRequest(reqBytes);
			}
			catch (IOException | ClassNotFoundException e)
			{
				throw new IllegalStateException(e);
			}
		}
	}
}
