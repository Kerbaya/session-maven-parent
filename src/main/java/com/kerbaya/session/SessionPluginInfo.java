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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

class SessionPluginInfo
{
	public static final String MOJO_NAME = "session";
	public static final String GOAL;
	public static final String READY_PROMPT;
	public static final Charset READY_PROMPT_ENCODING = StandardCharsets.UTF_8;
	
	static
	{
		ResourceBundle rb = ResourceBundle.getBundle(SessionPluginInfo.class.getName());
		GOAL = new StringBuilder()
				.append(rb.getString("groupId"))
				.append(':')
				.append(rb.getString("artifactId"))
				.append(':')
				.append(rb.getString("version"))
				.append(':')
				.append(MOJO_NAME)
				.toString();
		
		READY_PROMPT = "\r\n" + GOAL + " READY\r\n";
	}
	
	private SessionPluginInfo() {}
}
