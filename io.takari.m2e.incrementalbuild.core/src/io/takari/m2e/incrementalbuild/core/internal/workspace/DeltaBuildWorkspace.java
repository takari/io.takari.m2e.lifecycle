package io.takari.m2e.incrementalbuild.core.internal.workspace;

import io.takari.incrementalbuild.workspace.Workspace;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;

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
    IResourceDelta delta = this.delta.findMember(getFile(resource).getFullPath());

    if (delta != null) {
      return toStatus(delta.getKind());
    }

    return ResourceStatus.UNMODIFIED;
  }

  @Override
  public void walk(final File basedir, final FileVisitor visitor) throws IOException {
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
              long lastModified = resource.getLocalTimeStamp();
              long length = file.lastModified(); // TODO does EFS provide any benefits
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
