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
package com.kerbaya.session.internal.resolve_artifacts;

import java.io.Serializable;

import com.kerbaya.session.ArtifactCoords;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class SerializableArtifactCoords implements ArtifactCoords, Serializable
{
	private static final long serialVersionUID = 1L;

	private String groupId;
	private String artifactId;
	private String extension;
	private String classifier;
	private String version;
}
