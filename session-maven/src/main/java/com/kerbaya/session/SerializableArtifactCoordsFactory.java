/*
 * Copyright 2023 Kerbaya Software
 * 
 * This file is part of session-maven. 
 * 
 * session-maven is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * session-maven is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with session-maven.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kerbaya.session;

import java.util.function.Function;

import com.kerbaya.session.internal.resolve_artifacts.SerializableArtifactCoords;

class SerializableArtifactCoordsFactory implements Function<ArtifactCoords, SerializableArtifactCoords>
{
	public static final Function<? super ArtifactCoords, ? extends SerializableArtifactCoords> INSTANCE =
			new SerializableArtifactCoordsFactory();
	
	private SerializableArtifactCoordsFactory() {}
	
	@Override
	public SerializableArtifactCoords apply(ArtifactCoords t)
	{
		SerializableArtifactCoords sac = new SerializableArtifactCoords();
		sac.setGroupId(Utils.assertNotNullAndNotEmpty(t.getGroupId(), "groupId"));
		sac.setArtifactId(Utils.assertNotNullAndNotEmpty(t.getArtifactId(), "artifactId"));
		sac.setClassifier(Utils.assertNullOrNotEmpty(t.getClassifier(), "classifier"));
		sac.setExtension(Utils.assertNotNullAndNotEmpty(t.getExtension(), "extension"));
		sac.setVersion(Utils.assertNotNullAndNotEmpty(t.getVersion(), "version"));
		return sac;
	}
}
