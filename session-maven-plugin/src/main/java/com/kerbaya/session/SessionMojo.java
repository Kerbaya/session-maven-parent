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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.ByteBuffer;
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

import com.kerbaya.session.internal.SessionPluginInfo;

import lombok.Getter;
import lombok.Setter;

/**
 * Starts a long-running Maven session to service requests from Maven Session (com.kerbaya:session-maven) 
 */
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
	
	public static boolean fill(InputStream is, byte[] bytes) throws IOException
	{
		int off = 0;
		while (off < bytes.length)
		{
			int nextLen = bytes.length - off;
			int len = is.read(bytes, off, nextLen);				

			if (len == -1)
			{
				return false;
			}
			off += len;
		}
		
		return true;
	}

	private void execute0() throws IOException, ClassNotFoundException, InterruptedException
	{
		ExecutorService es = Executors.newCachedThreadPool();
		try
		{
			CommandHandler commandHandler = new CommandHandler(rs, rss, projectRepos);
			
			OutputStream os = System.out;
			try (OutputStream nullOs = new NullOutputStream();
					PrintStream nullPs = new PrintStream(nullOs))
			{
				System.setOut(nullPs);
				
				try (OutputStream gos = new NonClosingOutputStream(os);
						Writer out = new OutputStreamWriter(gos, SessionPluginInfo.READY_PROMPT_ENCODING))
				{
					out.append(SessionPluginInfo.READY_PROMPT);
				}
				
				RequestHandler requestHandler = new RequestHandler(os, commandHandler);
				
				ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
				byte[] size = sizeBuffer.array();
				
				while (fill(System.in, size))
				{
					byte[] reqBuffer = new byte[sizeBuffer.getInt(0)];
					if (!fill(System.in, reqBuffer))
					{
						break;
					}
					
					es.execute(requestHandler.apply(reqBuffer));
				}
			}
		}
		finally
		{
			es.shutdown();
			es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
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
