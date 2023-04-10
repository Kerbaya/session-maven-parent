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

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class SessionInstanceRig
{
	public static void main(String[] args)
	{
		int rc;
		try
		{
			DefaultArtifactCoords dac = new DefaultArtifactCoords();
			dac.setGroupId("junit");
			dac.setArtifactId("junit");
			dac.setExtension("jar");
			dac.setVersion("4.13.2");

			try (SessionInstance si = new SessionInstance(
					Paths.get("C:", "dev", "apache-maven-3.8.4"),
					Collections.emptyList(),
					Paths.get("C:/temp"), 
					null))
			{
				for (int i = 0; i < 10; i++)
				{
					List<ArtifactResult> rep = si.resolveArtifacts(Collections.singletonList(dac));
					System.out.println(rep);
				}
			}
			rc=0;
		}
		catch (Throwable t)
		{
			t.printStackTrace();
			rc=1;
		}
		System.exit(rc);
	}

}
