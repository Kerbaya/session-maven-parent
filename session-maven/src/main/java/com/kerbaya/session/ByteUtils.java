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

class ByteUtils
{
	public static boolean circularFind(byte[] find, int findOffset, byte[] buffer, int bufferOffset)
	{
		int length = find.length;
		if (buffer.length != length)
		{
			return false;
		}
		
		for (int i = 0; i < length; i++)
		{
			if (find[(findOffset + i) % length] != buffer[(bufferOffset + i) % length])
			{
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean fill(InputStream is, byte[] bytes)
	{
		int off = 0;
		while (off < bytes.length)
		{
			int nextLen = bytes.length - off;
			int len;
			try
			{
				len = is.read(bytes, off, nextLen);				
			}
			catch (IOException e)
			{
				throw new IllegalStateException(e);
			}
			if (len == -1)
			{
				return false;
			}
			off += len;
		}
		
		return true;
	}
}
