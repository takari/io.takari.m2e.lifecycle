package io.takari.m2e.incrementalbuild.core.internal.workspace;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

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
    return Mode.DELTA;
  }

  @Override
  public ResourceStatus getResourceStatus(File resource, long lastModified, long length) {
    return ResourceStatus.UNMODIFIED;
  }

  @Override
  public void walk(File basedir, Collection<String> includes, Collection<String> excludes,
      FileVisitor visitor) throws IOException {
    // nothing to report in delta mode
  }

}
