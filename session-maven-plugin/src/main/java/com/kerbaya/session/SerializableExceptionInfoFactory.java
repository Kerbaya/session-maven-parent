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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.kerbaya.session.internal.SerializableExceptionInfo;

class SerializableExceptionInfoFactory implements Function<Throwable, SerializableExceptionInfo>
{
	private final Map<Throwable, SerializableExceptionInfo> thrownDeja = new HashMap<>();
	private final Map<StackTraceElement, StackTraceElement> steDeja = new HashMap<>();
	
	private StackTraceElement createSte(StackTraceElement ste)
	{
		return steDeja.computeIfAbsent(ste, Function.identity());
	}
	
	@Override
	public SerializableExceptionInfo apply(Throwable t)
	{
		if (t == null)
		{
			return null;
		}
		
		SerializableExceptionInfo r = thrownDeja.get(t);
		if (r != null)
		{
			return r;
		}
		
		r = new SerializableExceptionInfo();
		thrownDeja.put(t, r);
		
		r.setCause(apply(t.getCause()));
		r.setMessage(t.getMessage());
		r.setStackTrace(Stream.of(t.getStackTrace())
				.map(this::createSte)
				.collect(Collectors.toCollection(ArrayList::new)));
		r.setSuppressed(Stream.of(t.getSuppressed())
				.map(this)
				.collect(Collectors.toCollection(ArrayList::new)));
		r.setType(t.getClass().getName());
		return r;
	}
}
