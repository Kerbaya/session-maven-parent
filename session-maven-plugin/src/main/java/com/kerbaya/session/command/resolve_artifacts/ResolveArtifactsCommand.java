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
package com.kerbaya.session.command.resolve_artifacts;

import java.util.Set;

import com.kerbaya.session.command.Command;
import com.kerbaya.session.command.CommandVisitor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveArtifactsCommand implements Command<ResolveArtifactsResult>
{
	private static final long serialVersionUID = 2021579810227788748L;

	private Set<ResolveArtifactQuery> queries;
	
	@Override
	public <T> T accept(CommandVisitor<T> visitor)
	{
		return visitor.visit(this);
	}
}
