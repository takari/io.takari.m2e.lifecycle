/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.m2e.incrementalbuild.core.internal.workspace;

import static org.eclipse.core.resources.IncrementalProjectBuilder.AUTO_BUILD;
import static org.eclipse.core.resources.IncrementalProjectBuilder.CLEAN_BUILD;
import static org.eclipse.core.resources.IncrementalProjectBuilder.FULL_BUILD;
import static org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD;
import static org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant2.PRECONFIGURE_BUILD;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;

import io.takari.builder.enforcer.PolicyContextPreserver;

@SuppressWarnings("restriction")
public class TakariIncrementalBuildFramework implements IIncrementalBuildFramework {

  @Override
  public BuildContext setupProjectBuildContext(IProject project, int kind, IResourceDelta delta,
      BuildResultCollector results) {
    final AbstractBuildWorkspace workspace = newWorkspace(project, kind, delta, results);
    ThreadLocalBuildWorkspace.setDelegate(workspace);
    PolicyContextPreserver.registerAccessor(new WorkspaceContextAccessor());
    return workspace;
  }

  private AbstractBuildWorkspace newWorkspace(IProject project, int kind, IResourceDelta delta,
      BuildResultCollector results) {
    switch (kind) {
      case CLEAN_BUILD:
        return new NoChangeBuildWorkspace(project, results);
      case INCREMENTAL_BUILD:
      case AUTO_BUILD:
        return new DeltaBuildWorkspace(project, delta, results);
      case FULL_BUILD:
        return new FullBuildWorkspace(project, results);
      case PRECONFIGURE_BUILD:
        return new NoChangeBuildWorkspace(project, results);
      default:
        throw new IllegalArgumentException();
    }
  }
}
