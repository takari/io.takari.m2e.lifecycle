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
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;

import io.takari.incrementalbuild.workspace.Workspace;

@SuppressWarnings("restriction")
public class DeltaBuildWorkspace extends AbstractBuildWorkspace implements Workspace {

  private final IResourceDelta delta;

  public DeltaBuildWorkspace(IProject project, IResourceDelta delta,
      IIncrementalBuildFramework.BuildResultCollector results) {
    super(project, results);
    this.delta = delta;
  }

  @Override
  public Mode getMode() {
    return Mode.DELTA;
  }

  @Override
  public ResourceStatus getResourceStatus(File resource, long lastModified, long length) {
    IResourceDelta delta = this.delta.findMember(getFile(resource).getProjectRelativePath());

    if (delta != null) {
      return toStatus(delta.getKind());
    }

    return ResourceStatus.UNMODIFIED;
  }

  @Override
  protected void doWalk(final File basedir, final FileVisitor visitor) throws IOException {
    IResourceDelta delta = this.delta.findMember(getFolder(basedir).getProjectRelativePath());
    if (delta != null) {
      try {
        delta.accept(new IResourceDeltaVisitor() {
          @Override
          public boolean visit(IResourceDelta delta) throws CoreException {
            // TODO filter irrelevant delta kind and flags, like phantom or team changes
            if (delta.getResource() instanceof IFile) {
              IFile resource = (IFile) delta.getResource();
              File file = resource.getLocation().toFile();
              long lastModified = file.lastModified();
              long length = file.length(); // TODO does EFS provide any benefits
              visitor.visit(file, lastModified, length, toStatus(delta.getKind()));
            }
            return true; // keep visiting
          }
        });
      } catch (CoreException e) {
        // TODO likely not good enough
        throw new IOException(e);
      }
    }

    // walk files changed/deleted through this build context.
    // this is necessary because eclipse workspace does not report builder's own changes
    doWalk(basedir, processedOutputs, visitor, ResourceStatus.MODIFIED);
    doWalk(basedir, deletedOutputs, visitor, ResourceStatus.REMOVED);
  }

  private void doWalk(File basedir, Collection<File> files, FileVisitor visitor,
      ResourceStatus status) {
    for (File file : files) {
      if (!file.toPath().startsWith(basedir.toPath())) {
        continue;
      }
      if (delta == null || this.delta.findMember(getRelativePath(file)) == null) {
        visitor.visit(file, file.lastModified(), file.length(), status);
      }
    }
  }

  protected ResourceStatus toStatus(int kind) {
    switch (kind) {
      case IResourceDelta.ADDED:
        return ResourceStatus.NEW;
      case IResourceDelta.CHANGED:
        return ResourceStatus.MODIFIED;
      case IResourceDelta.REMOVED:
        return ResourceStatus.REMOVED;
      default:
        throw new IllegalArgumentException();
    }
  }
}
