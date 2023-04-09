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

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.kerbaya.session.command.CommandException;
import com.kerbaya.session.command.resolve_artifacts.ResolveArtifactQuery;
import com.kerbaya.session.command.resolve_artifacts.ResolveArtifactResult;
import com.kerbaya.session.command.resolve_artifacts.ResolveArtifactsCommand;
import com.kerbaya.session.command.resolve_artifacts.ResolveArtifactsResult;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class ResolveArtifactsHandler implements Function<ResolveArtifactsCommand, ResolveArtifactsResult>
{
	private final RepositorySystem rs;
	private final RepositorySystemSession rss;
	private final List<RemoteRepository> projectRepos;

	private ArtifactRequest resolveArtifactQueryToArtifactRequest(ResolveArtifactQuery aq)
	{
		ArtifactRequest ar = new ArtifactRequest(
				new DefaultArtifact(
						aq.getGroupId(), aq.getArtifactId(), aq.getClassifier(), aq.getExtension(), aq.getVersion()),
				projectRepos, 
				null);
		ar.setTrace(new RequestTrace(aq));
		return ar;
	}
	
	private static ResolveArtifactQuery artifactResultToResolveArtifactQuery(ArtifactResult ar)
	{
		return (ResolveArtifactQuery) ar.getRequest().getTrace().getData();
	}
	
	private static ResolveArtifactResult artifactResultToResolveArtifactResult(ArtifactResult ar)
	{
		ResolveArtifactResult rar = new ResolveArtifactResult();
		rar.setResolved(ar.isResolved());
		Artifact a = ar.getArtifact();
		if (a == null)
		{
			rar.setBaseVersion(null);
			rar.setPath(null);
			rar.setVersion(null);
		}
		else
		{
			rar.setBaseVersion(a.getBaseVersion());
			File file = a.getFile();
			rar.setPath(file == null ? null : file.getAbsolutePath().toString());
			rar.setVersion(a.getVersion());
		}
		
		rar.setExceptions(ar.getExceptions().stream()
				.map(ExceptionInfoFactory.INSTANCE)
				.collect(Collectors.toCollection(ArrayList::new)));
		return rar;
	}

	@Override
	public ResolveArtifactsResult apply(ResolveArtifactsCommand c)
	{
		List<ArtifactRequest> requests = c.getQueries().stream()
				.map(this::resolveArtifactQueryToArtifactRequest)
				.collect(Collectors.toList());
		
		if (requests.isEmpty())
		{
			ResolveArtifactsResult r = new ResolveArtifactsResult();
			r.setResults(Collections.emptyMap());
			return r;
		}
		
		List<ArtifactResult> arResList;
		try
		{
			arResList = rs.resolveArtifacts(rss, requests);
		}
		catch (ArtifactResolutionException e)
		{
			arResList = e.getResults();
			if (arResList == null || arResList.isEmpty())
			{
				throw new CommandException(ExceptionInfoFactory.INSTANCE.apply(e));
			}
		}
		
		ResolveArtifactsResult rar = new ResolveArtifactsResult();
		rar.setResults(arResList.stream()
				.collect(Collectors.toMap(
						ResolveArtifactsHandler::artifactResultToResolveArtifactQuery, 
						ResolveArtifactsHandler::artifactResultToResolveArtifactResult)));
		return rar;
	}

}
