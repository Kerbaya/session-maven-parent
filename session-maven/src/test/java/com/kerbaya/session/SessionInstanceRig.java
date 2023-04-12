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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SessionInstanceRig
{
	public static void main(String[] args)
	{
		try
		{
			DefaultArtifactCoords dac = new DefaultArtifactCoords();
			dac.setGroupId("junit");
			dac.setArtifactId("junit");
			dac.setExtension("jar");
			dac.setVersion("4.13.2");

			Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
				e.printStackTrace();
			});
			Path dir = Files.createTempDirectory(null);
			try (SessionInstance si = new SessionInstance(
					Arrays.asList(
							Paths.get("C:", "dev", "apache-maven-3.8.4", "bin", "mvn.cmd").toAbsolutePath().toString()),
					dir, 
					null))
			{
				int tc = 100;
				ExecutorService es = Executors.newFixedThreadPool(tc);
				for (int i = 0; i < tc; i++)
				{
					es.submit(() -> {
						try
						{
							List<ArtifactResult> rep = si.resolveArtifacts(Collections.singletonList(dac));
							synchronized(System.out)
							{
								System.out.println(rep);
							}
						}
						catch (Throwable t)
						{
							synchronized(System.err)
							{
								t.printStackTrace(System.err);
							}
						}
					});
				}
				es.shutdown();
				es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			}
			finally
			{
				Files.delete(dir);
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}

}
