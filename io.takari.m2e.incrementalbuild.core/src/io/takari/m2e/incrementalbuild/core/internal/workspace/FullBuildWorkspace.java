/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.m2e.incrementalbuild.core.internal.workspace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;

import io.takari.incrementalbuild.workspace.Workspace;

@SuppressWarnings("restriction")
public class FullBuildWorkspace extends AbstractBuildWorkspace implements Workspace {

  public FullBuildWorkspace(IProject project,
      IIncrementalBuildFramework.BuildResultCollector results) {
    super(project, results);
  }

  public FullBuildWorkspace(AbstractBuildWorkspace parent) {
    super(parent);
  }

  @Override
  public Mode getMode() {
    return Mode.ESCALATED;
  }

  @Override
  public Workspace escalate() {
    return this;
  }

  @Override
  public ResourceStatus getResourceStatus(File resource, long lastModified, long length) {
    IFile file = getFile(resource);

    if (!file.exists()) {
      return ResourceStatus.REMOVED;
    }

    if (file.getLocalTimeStamp() != lastModified || resource.length() != length) {
      return ResourceStatus.MODIFIED;
    }

    return ResourceStatus.UNMODIFIED;
  }

  @Override
  public void walk(File basedir, final FileVisitor visitor) throws IOException {
    if (!basedir.isDirectory()) {
      return;
    }
    final IContainer folder = getFolder(basedir);
    try {
      folder.accept(new IResourceVisitor() {
        @Override
        public boolean visit(IResource resource) throws CoreException {
          if (resource instanceof IFile) {
            File file = resource.getLocation().toFile();
            long lastModified = file.lastModified();
            long length = file.length();
            visitor.visit(file, lastModified, length, ResourceStatus.NEW);
          }
          return true;
        }
      });
    } catch (CoreException e) {
      // TODO likely not good enough
      throw new IOException(e);
    }
  }

  @Override
  public OutputStream newOutputStream(File file) throws IOException {
    File parent = file.getParentFile();
    if (!parent.isDirectory() && !parent.mkdirs()) {
      throw new IOException("Could not create directory " + parent);
    }
    processOutput(file);
    return new FileOutputStream(file);
  }
}
