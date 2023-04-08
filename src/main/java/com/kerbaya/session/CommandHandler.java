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

import java.util.List;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.kerbaya.session.command.CommandVisitor;
import com.kerbaya.session.command.Result;
import com.kerbaya.session.command.resolve_artifacts.ResolveArtifactsCommand;
import com.kerbaya.session.command.resolve_artifacts.ResolveArtifactsResult;

class CommandHandler implements CommandVisitor<Result>
{
	private final ResolveArtifactsHandler resolveArtifacts;
	
	public CommandHandler(
			RepositorySystem rs,
			RepositorySystemSession rss,
			List<RemoteRepository> projectRepos)
	{
		resolveArtifacts = new ResolveArtifactsHandler(rs, rss, projectRepos);
	}
	
	@Override
	public ResolveArtifactsResult visit(ResolveArtifactsCommand c)
	{
		return resolveArtifacts.apply(c);
	}
}
