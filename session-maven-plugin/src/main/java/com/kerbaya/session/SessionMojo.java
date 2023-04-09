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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import lombok.Getter;
import lombok.Setter;

@org.apache.maven.plugins.annotations.Mojo(
		name=SessionPluginInfo.MOJO_NAME,
		requiresProject=false)
public class SessionMojo implements org.apache.maven.plugin.Mojo
{
	@Getter
	@Setter
	private Log log;
	
	@Inject
	private RepositorySystem rs;
	
	@Parameter(defaultValue="${repositorySystemSession}", required=true, readonly=true)
    private RepositorySystemSession rss;
	
	@Parameter(defaultValue="${project.remoteProjectRepositories}", required=true, readonly=true)
    private List<RemoteRepository> projectRepos;

	private void execute0() throws IOException, ClassNotFoundException, InterruptedException
	{
		ExecutorService es = Executors.newCachedThreadPool();
		CommandHandler commandHandler = new CommandHandler(rs, rss, projectRepos);
		
		try (OutputStream nullOs = new NullOutputStream();
				PrintStream nullPs = new PrintStream(nullOs);
				OutputStream os = new NonClosingOutputStream(System.out))
		{
			System.setOut(nullPs);
			
			try (Writer out = new OutputStreamWriter(os, SessionPluginInfo.READY_PROMPT_ENCODING))
			{
				out.append(SessionPluginInfo.READY_PROMPT);
			}
			
			try (InputStream is = new NonClosingInputStream(System.in);
					ObjectInputStream ois = new ObjectInputStream(is);
					ObjectOutputStream oos = new ObjectOutputStream(os))
			{
				RequestHandler requestHandler = new RequestHandler(oos, commandHandler);
				
				Request req;
				while ((req = (Request) ois.readObject()) != null)
				{
					es.execute(requestHandler.apply(req));
				}
				
				es.shutdown();
				es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			}
		}
	}
	
	@Override
	public void execute() throws MojoExecutionException
	{
		try
		{
			execute0();
		}
		catch (ClassNotFoundException | IOException | InterruptedException e)
		{
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
