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

public class ByteUtilsTest
{
	@Test
	public void circularFindLengthMismatch()
	{
		Assert.assertFalse(ByteUtils.circularFind(new byte[0], 0, new byte[1], 0));
	}
	
	@Test
	public void circularFindEmpty()
	{
		Assert.assertTrue(ByteUtils.circularFind(new byte[0], 0, new byte[0], 100));
	}
	
	@Test
	public void circularFindSameArrayDifferentOffset()
	{
		byte[] a = new byte[] {0, 1, 2, 3};
		Assert.assertFalse(ByteUtils.circularFind(a, 0, a, 1));
	}
	
	@Test
	public void circularFindSameArrayWrappedOffset()
	{
		byte[] a = new byte[] {0, 1, 2, 3};
		Assert.assertTrue(ByteUtils.circularFind(a, 0, a, 4));
	}
	
	@Test
	public void circularFind()
	{
		byte[] a = new byte[] {1, 2, 3, 0};
		byte[] b = new byte[] {2, 3, 0, 1};
		Assert.assertTrue(ByteUtils.circularFind(a, 3, b, 2));
	}
	
	@Test
	public void fillNone() throws IOException
	{
		try (InputStream is = new ByteArrayInputStream(new byte[] {0, 1, 2}))
		{
			byte[] a = new byte[0];
			Assert.assertTrue(ByteUtils.fill(is, a));
			Assert.assertEquals(0, is.read());
			Assert.assertEquals(1, is.read());
			Assert.assertEquals(2, is.read());
			Assert.assertEquals(-1, is.read());
		}
	}
	
	@Test
	public void fillJustEnough() throws IOException
	{
		try (InputStream is = new ByteArrayInputStream(new byte[] {0, 1, 2}))
		{
			byte[] a = new byte[3];
			Assert.assertTrue(ByteUtils.fill(is, a));
			Assert.assertArrayEquals(new byte[] {0, 1, 2}, a);
			Assert.assertEquals(-1, is.read());
		}
	}
	
	@Test
	public void fillNotEnough() throws IOException
	{
		try (InputStream is = new ByteArrayInputStream(new byte[] {0, 1, 2}))
		{
			byte[] a = new byte[4];
			Assert.assertFalse(ByteUtils.fill(is, a));
		}
	}
}
