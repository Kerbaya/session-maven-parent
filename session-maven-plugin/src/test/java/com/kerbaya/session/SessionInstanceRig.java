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
import java.util.Arrays;
import java.util.Collections;

import com.kerbaya.session.command.resolve_artifacts.ResolveArtifactQuery;
import com.kerbaya.session.command.resolve_artifacts.ResolveArtifactsCommand;
import com.kerbaya.session.command.resolve_artifacts.ResolveArtifactsResult;

public class SessionInstanceRig
{
	public static void main(String[] args)
	{
		int rc;
		try
		{
			try (SessionInstance si = new SessionInstance(
					Arrays.asList(
							"C:/dev/apache-maven-3.8.4/bin/mvn.cmd",
							"--quiet",
							"com.kerbaya:session-maven-plugin:1.0.0-SNAPSHOT:session"), 
					Paths.get("C:/temp"), 
					null))
			{
				for (int i = 0; i < 10; i++)
				{
					ResolveArtifactsCommand rac = new ResolveArtifactsCommand();
					ResolveArtifactQuery raq = new ResolveArtifactQuery();
					raq.setGroupId("com.kerbaya");
					raq.setArtifactId("stamp-mavfen-plugin");
					raq.setExtension("jar");
					raq.setClassifier("");
					raq.setVersion("1.0.3");
					rac.setQueries(Collections.singleton(raq));
					ResolveArtifactsResult rep = si.execute(rac);
					System.out.println(rep.getResults());
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
