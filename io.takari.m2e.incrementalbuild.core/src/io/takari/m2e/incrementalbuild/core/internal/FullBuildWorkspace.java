package io.takari.m2e.incrementalbuild.core.internal;

import io.takari.incrementalbuild.workspace.Workspace;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;

@SuppressWarnings("restriction")
public class FullBuildWorkspace extends AbstractBuildWorkspace implements Workspace {

  public FullBuildWorkspace(IProject project,
      IIncrementalBuildFramework.BuildResultCollector results) {
    super(project, results);
  }

  @Override
  public Mode getMode() {
    return Mode.ESCALATED;
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
  public void walk(File basedir, Collection<String> includes, Collection<String> excludes,
      final FileVisitor visitor) throws IOException {
    final IFolder folder = getFolder(basedir);
    final PathMatcher matcher = PathMatcher.fromStrings(includes, excludes);
    try {
      folder.accept(new IResourceVisitor() {
        @Override
        public boolean visit(IResource resource) throws CoreException {
          if (resource instanceof IFile && matcher.matches(resource.getFullPath())) {
            File file = resource.getLocation().toFile();
            long lastModified = resource.getLocalTimeStamp();
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
}
