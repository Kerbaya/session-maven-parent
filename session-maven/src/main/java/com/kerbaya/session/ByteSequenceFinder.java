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

import lombok.AllArgsConstructor;

@AllArgsConstructor
class ByteSequenceFinder
{
	private final InputStream is;
	
	public boolean find(byte[] find)
	{
		int length = find.length;
		byte[] buffer = new byte[length];
		
		if (!ByteUtils.fill(is, buffer))
		{
			return false;
		}
		
		int off = 0;
		while (!ByteUtils.circularFind(find, 0, buffer, off))
		{
			int b;
			try
			{
				b = is.read();
			}
			catch (IOException e)
			{
				throw new IllegalStateException(e);
			}
			if (b == -1)
			{
				return false;
			}
			
			buffer[off] = (byte) b;
			off++;
			if (off == length)
			{
				off = 0;
			}
		}
		
		return true;
	}
}
