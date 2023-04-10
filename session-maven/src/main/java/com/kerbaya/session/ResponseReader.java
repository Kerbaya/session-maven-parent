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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.kerbaya.session.internal.Response;
import com.kerbaya.session.internal.SessionPluginInfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ResponseReader implements Runnable
{
	private final Object stateLock = new Object();
	private final Map<Long, Response> responseMap = new HashMap<>();

	private final Supplier<? extends InputStream> inputStreamGetter;
	
	private volatile Boolean state;

	private void run0() throws IOException, ClassNotFoundException
	{
		try (InputStream is = inputStreamGetter.get())
		{
			if (!new ByteSequenceFinder(is).find(
					SessionPluginInfo.READY_PROMPT.getBytes(SessionPluginInfo.READY_PROMPT_ENCODING)))
			{
				return;
			}
			
			synchronized(stateLock)
			{
				state = Boolean.TRUE;
				stateLock.notifyAll();
			}
			
			try (ObjectInputStream ois = new ObjectInputStream(is))
			{
				Response response;
				while ((response = (Response) ois.readObject()) != null)
				{
					Long responseId = response.getRequestId();
					synchronized(stateLock)
					{
						responseMap.put(responseId, response);
						stateLock.notifyAll();
					}
				}
			}
		}
	}
	
	public void run()
	{
		try
		{
			run0();
		}
		catch (ClassNotFoundException | IOException e)
		{
			throw new IllegalStateException(e);
		}
		finally
		{
			synchronized(stateLock)
			{
				state = Boolean.FALSE;
				stateLock.notify();
			}
		}
	}
	
	private boolean waitForState0() throws InterruptedException
	{
		Boolean state;
		if ((state = this.state) == null)
		{
			synchronized(stateLock)
			{
				while ((state = this.state) == null)
				{
					stateLock.wait();
				}
			}
		}
		return state.booleanValue();
	}
	
	public boolean waitForState()
	{
		try
		{
			return waitForState0();
		}
		catch (InterruptedException e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	private Response waitForResponse0(long responseId) throws InterruptedException
	{
		Boolean state = this.state;
		if (state == null)
		{
			throw new IllegalStateException();
		}
		
		Long responseIdKey = responseId;
		Response response;
		synchronized(stateLock)
		{
			while ((response = responseMap.remove(responseIdKey)) == null)
			{
				if (Boolean.FALSE.equals(state))
				{
					return null;
				}
				
				stateLock.wait();
			}
		}
		
		return response;
	}
	
	public Response waitForResponse(long responseId)
	{
		try
		{
			return waitForResponse0(responseId);
		}
		catch (InterruptedException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
