/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.m2e.incrementalbuild.core.internal;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

/**
 * The purpose of this configurator is to (hopefully) trigger discovery of
 * takari.m2e.incrementalbuild feature during m2e project import and configuration.
 */
public class BuilderMojoExecutionConfigurator extends AbstractProjectConfigurator {

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    // nothing to configure
  }

  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade,
      MojoExecution execution, IPluginExecutionMetadata executionMetadata) {
    return new MojoExecutionBuildParticipant(execution, true, true);
  }
}
