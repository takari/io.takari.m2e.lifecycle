/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.m2e.incrementalbuild.test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import io.takari.incrementalbuild.BuildContext;
import io.takari.incrementalbuild.MessageSeverity;
import io.takari.incrementalbuild.Resource;

@Mojo(name = "message")
public class MessageMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.basedir}/src/resources")
  private File directory;

  @Parameter
  private List<String> includes = Arrays.asList("*.txt");

  @Parameter
  private List<String> excludes;

  @Component
  private BuildContext context;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      for (Resource<File> input : context.registerAndProcessInputs(directory, includes, excludes)) {
        String message = Files.readFirstLine(input.getResource(), Charsets.UTF_8);
        if (message != null && !message.isEmpty()) {
          input.addMessage(0, 0, message, MessageSeverity.ERROR, null);
        }
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Could not read message file", e);
    }
  }

}
