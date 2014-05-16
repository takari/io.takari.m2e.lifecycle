package io.takari.m2e.incrementalbuild.core.internal;

import static org.eclipse.core.resources.IncrementalProjectBuilder.AUTO_BUILD;
import static org.eclipse.core.resources.IncrementalProjectBuilder.CLEAN_BUILD;
import static org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;

@SuppressWarnings("restriction")
public class TakariIncrementalBuildFramework implements IIncrementalBuildFramework {

  @Override
  public BuildContext setupProjectBuildContext(IProject project, int kind, IResourceDelta delta,
      BuildResultCollector results) throws CoreException {
    AbstractBuildWorkspace workspace;
    if (kind == CLEAN_BUILD) {
      workspace = new NoChangeBuildWorkspace(project, results);
    } else if ((kind == INCREMENTAL_BUILD || kind == AUTO_BUILD) && delta != null) {
      workspace = new DeltaBuildWorkspace(project, delta, results);
    } else {
      workspace = new FullBuildWorkspace(project, results);
    }
    ThreadLocalBuildWorkspace.setDelegate(workspace);
    return workspace;
  }

}
