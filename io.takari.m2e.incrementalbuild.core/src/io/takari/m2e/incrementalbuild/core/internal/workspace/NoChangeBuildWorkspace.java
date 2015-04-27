/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.m2e.incrementalbuild.core.internal.workspace;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;

@SuppressWarnings("restriction")
public class NoChangeBuildWorkspace extends AbstractBuildWorkspace {

  public NoChangeBuildWorkspace(IProject project,
      IIncrementalBuildFramework.BuildResultCollector results) {
    super(project, results);
  }

  @Override
  public Mode getMode() {
    return Mode.SUPPRESSED;
  }

  @Override
  public ResourceStatus getResourceStatus(File resource, long lastModified, long length) {
    return ResourceStatus.UNMODIFIED;
  }

  @Override
  public void walk(File basedir, FileVisitor visitor) throws IOException {
    // nothing to report in no-change mode
  }

}
