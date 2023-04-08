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
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

public class ByteSequenceFinderTest
{
	private static byte[] a(int... b)
	{
		byte[] r = new byte[b.length];
		for (int i = 0; i < b.length; i++)
		{
			Assert.assertTrue(b[i] >= Byte.MIN_VALUE);
			Assert.assertTrue(b[i] <= Byte.MAX_VALUE);
			r[i] = (byte) b[i];
		}
		return r;
	}
	
	private static void test(byte[] find, byte[] seq, int offset)
	{
		try (InputStream is = new ByteArrayInputStream(seq))
		{
			boolean r = new ByteSequenceFinder(is).find(find);
			if (offset == -1)
			{
				Assert.assertFalse(r);
				Assert.assertEquals(-1, is.read());
			}
			else
			{
				Assert.assertTrue(r);
				for (int i = offset + find.length; i < seq.length; i++)
				{
					Assert.assertNotEquals(-1, is.read());
				}
				Assert.assertEquals(-1, is.read());
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	@Test
	public void test()
	{
		test(a(0, 1, 2), a(0, 1, 2), 0);
		test(a(), a(), 0);
		test(a(3, 4), a(0, 1, 2, 3, 4, 5), 3);
		test(a(3, 4), a(0, 1, 2, 3), -1);
		test(a(0), a(0, 1, 2, 0, 1, 2), 0);
	}
}
