/*
 * Copyright 2023 Kerbaya Software
 * 
 * This file is part of session-maven. 
 * 
 * session-maven is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * session-maven is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with session-maven.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kerbaya.session;

class Utils
{
	public static String assertNotEmpty(String str, String key)
	{
		if (str.isEmpty())
		{
			throw new SessionException("emptyInvalid", key);
		}
		
		return str;
	}
	
	public static String assertNotNullAndNotEmpty(String str, String key)
	{
		if (str == null)
		{
			throw new SessionException("nullInvalid", key);
		}
		
		return assertNotEmpty(str, key);
	}
	
	public static String assertNullOrNotEmpty(String str, String key)
	{
		return str == null ?
				null
				: assertNotEmpty(str, key);
	}
}
