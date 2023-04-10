/*
 * Copyright 2023 Kerbaya Software
 * 
 * This file is part of session-maven-api. 
 * 
 * session-maven-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * session-maven-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with session-maven-api.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kerbaya.session;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Locale.Category;
import java.util.function.Supplier;

import lombok.Getter;

public class SessionException extends RuntimeException
{
	private static final long serialVersionUID = 5766580345562247998L;

	@Getter
	private final List<Object> messageArguments;
	
	public SessionException(String msgKey, Object... msgArgs)
	{
		super(msgKey);
		this.messageArguments = Collections.unmodifiableList(Arrays.asList(msgArgs));
	}
	
	public SessionException(Throwable cause, String msgKey, Object... msgArgs)
	{
		super(msgKey, cause);
		this.messageArguments = Collections.unmodifiableList(Arrays.asList(msgArgs));
	}
	
	public final String getMessageKey()
	{
		return super.getMessage();
	}
	
	@Override
	public final String getMessage()
	{
		return getMessageKey() + " " + getMessageArguments();
	}
	
	@SafeVarargs
	private static String getMessagePattern(
			String baseName, 
			Locale locale, 
			String key, 
			Supplier<? extends ClassLoader>... loaderGetters)
	{
		for (Supplier<? extends ClassLoader> loaderGetter: loaderGetters)
		{
			ClassLoader loader;
			try
			{
				loader = loaderGetter.get();
			}
			catch (SecurityException e)
			{
				continue;
			}
			
			try
			{
				return ResourceBundle.getBundle(baseName, locale, loader).getString(key);
			}
			catch (MissingResourceException e)
			{
			}
		}
		
		try
		{
			return ResourceBundle.getBundle(baseName, locale).getString(key);
		}
		catch (MissingResourceException e)
		{
			return null;
		}
	}
	
	public final String getLocalizedMessage(Locale locale)
	{
		String messagePattern = null;
		String key = getMessageKey();
		Class<?> type = getClass();
		do
		{
			messagePattern = getMessagePattern(
					type.getName(), 
					locale, 
					key, 
					Thread.currentThread()::getContextClassLoader, 
					type::getClassLoader);
			
			if (messagePattern != null)
			{
				break;
			}
			
			type = type.getSuperclass();
		} while (!type.equals(SessionException.class));
		
		if (messagePattern == null)
		{
			return getMessage();
		}
		
		if (messageArguments.isEmpty())
		{
			return messagePattern;
		}
		
		return new MessageFormat(messagePattern, locale).format(messageArguments.toArray());
	}
	
	@Override
	public final String getLocalizedMessage()
	{
		return getLocalizedMessage(Locale.getDefault(Category.DISPLAY));
	}
}
