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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DefaultArtifactCoords implements ArtifactCoords
{
	private String groupId;
	private String artifactId;
	private String extension;
	private String classifier;
	private String version;
	
	public DefaultArtifactCoords() {}
	
	public DefaultArtifactCoords(String artifactCoords)
	{
		String[] parts = artifactCoords.split(":");
		if (parts.length != 4 && parts.length != 5)
		{
			throw new SessionException("invalidArtifactCoords", artifactCoords);
		}
		
		groupId = Utils.assertNotEmpty(parts[0], "groupId");
		artifactId = Utils.assertNotEmpty(parts[1], "artifactId");
		extension = Utils.assertNotEmpty(parts[2], "extension");
		if (parts.length == 4)
		{
			classifier = null;
			version = Utils.assertNotEmpty(parts[3], "version");
		}
		else
		{
			classifier = Utils.assertNotEmpty(parts[3], "classifier");
			version = Utils.assertNotEmpty(parts[4], "version");
		}
	}
}
