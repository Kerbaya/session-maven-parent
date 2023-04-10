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
package com.kerbaya.session.internal.resolve_artifacts;

import java.io.Serializable;
import java.util.List;

import com.kerbaya.session.Artifact;
import com.kerbaya.session.ArtifactCoords;
import com.kerbaya.session.ArtifactResult;
import com.kerbaya.session.ExceptionInfo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class SerializableArtifactResult implements ArtifactResult, Serializable
{
	private static final long serialVersionUID = 1L;

	private ArtifactCoords artifactCoords;
	private Artifact artifact;
	private List<ExceptionInfo> exceptions;
}
