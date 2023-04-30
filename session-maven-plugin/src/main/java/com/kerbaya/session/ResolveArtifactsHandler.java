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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.kerbaya.session.internal.resolve_artifacts.SerializableArtifactResult;
import com.kerbaya.session.internal.resolve_artifacts.ResolveArtifactsCommand;
import com.kerbaya.session.internal.resolve_artifacts.ResolveArtifactsResult;
import com.kerbaya.session.internal.resolve_artifacts.SerializableArtifact;
import com.kerbaya.session.internal.resolve_artifacts.SerializableArtifactCoords;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class ResolveArtifactsHandler implements Function<ResolveArtifactsCommand, ResolveArtifactsResult>
{
	private final RepositorySystem rs;
	private final RepositorySystemSession rss;
	private final List<RemoteRepository> projectRepos;

	private static String assertNotEmpty(String str, String key)
	{
		if (str.isEmpty())
		{
			throw new IllegalArgumentException("emptyStr [" + key + "]");
		}
		return str;
	}
	
	private static String assertNotNullAndNotEmpty(String str, String key)
	{
		if (str == null)
		{
			throw new IllegalArgumentException("nullStr [" + key + "]");
		}
		
		return assertNotEmpty(str, key);
	}
	
	private static String assertNullOrNotEmpty(String str, String key)
	{
		return str == null ?
				null
				: assertNotEmpty(str, key);
	}
	
	private ArtifactRequest serializableArtifactCoordsToArtifactRequest(SerializableArtifactCoords aq)
	{
		String groupId = assertNotNullAndNotEmpty(aq.getGroupId(), "groupId");
		String artifactId = assertNotNullAndNotEmpty(aq.getArtifactId(), "artifactId");
		String extension = assertNotNullAndNotEmpty(aq.getExtension(), "extension");
		String classifier = assertNullOrNotEmpty(aq.getClassifier(), "classifier");
		String version = assertNotNullAndNotEmpty(aq.getVersion(), "version");
		ArtifactRequest ar = new ArtifactRequest(
				classifier == null ?
						new DefaultArtifact(groupId, artifactId, extension, version)
						: new DefaultArtifact(groupId, artifactId, classifier, extension, version),
				projectRepos, 
				null);
		ar.setTrace(new RequestTrace(aq));
		return ar;
	}
	
	private static SerializableArtifactResult artifactResultToSerializableArtifactResult(ArtifactResult ar)
	{
		SerializableArtifactResult rar = new SerializableArtifactResult();
		rar.setArtifactCoords((SerializableArtifactCoords) ar.getRequest().getTrace().getData());
		
		SerializableArtifact sa;
		Artifact a = ar.getArtifact();
		if (a == null)
		{
			sa = null;
		}
		else
		{
			File file = a.getFile();
			if (file == null)
			{
				sa = null;
			}
			else
			{
				sa = new SerializableArtifact();
				sa.setArtifactId(a.getArtifactId());
				sa.setBaseVersion(a.getBaseVersion());
				String classifier = a.getClassifier();
				sa.setClassifier(classifier.isEmpty() ? null : classifier);
				sa.setExtension(a.getExtension());
				sa.setFile(file.getAbsoluteFile());
				sa.setGroupId(a.getGroupId());
				sa.setVersion(a.getVersion());
			}
		}
		
		rar.setArtifact(sa);
		rar.setExceptions(ar.getExceptions().stream()
				.map(new SerializableExceptionInfoFactory())
				.collect(Collectors.toCollection(ArrayList::new)));
		return rar;
	}

	@Override
	public ResolveArtifactsResult apply(ResolveArtifactsCommand c)
	{
		List<ArtifactRequest> requests = c.getQueries().stream()
				.map(this::serializableArtifactCoordsToArtifactRequest)
				.collect(Collectors.toList());
		
		if (requests.isEmpty())
		{
			ResolveArtifactsResult r = new ResolveArtifactsResult();
			r.setResults(Collections.emptyList());
			return r;
		}
		
		DefaultRepositorySystemSession rssCopy = new DefaultRepositorySystemSession(rss);
		rssCopy.setCache(new DefaultRepositoryCache());
		
		List<ArtifactResult> arResList;
		try
		{
			arResList = rs.resolveArtifacts(rssCopy, requests);
		}
		catch (ArtifactResolutionException e)
		{
			arResList = e.getResults();
			if (arResList == null || arResList.isEmpty())
			{
				throw new CommandException(e);
			}
		}
		
		ResolveArtifactsResult rar = new ResolveArtifactsResult();
		rar.setResults(arResList.stream()
				.map(ResolveArtifactsHandler::artifactResultToSerializableArtifactResult)
				.collect(Collectors.toCollection(ArrayList::new)));
		return rar;
	}

}
