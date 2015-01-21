package io.takari.m2e.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;

public class WorkspaceChangeRecorder implements IResourceChangeListener, IResourceDeltaVisitor {

  private String pathPrefix;

  private List<String> paths = new ArrayList<>();

  public WorkspaceChangeRecorder(String pathPrefix) {
    this.pathPrefix = pathPrefix;
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    try {
      event.getDelta().accept(this);
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean visit(IResourceDelta delta) throws CoreException {
    if (isRelevant(delta)) {
      paths.add(delta.getProjectRelativePath().toString());
    }
    return true;
  }

  public void clear() {
    paths.clear();
  }

  public List<String> getPaths() {
    return paths;
  }

  public void setPathPrefix(String pathPrefix) {
    this.pathPrefix = pathPrefix;
  }

  private boolean isRelevant(IResourceDelta delta) {
    if (!(delta.getResource() instanceof IFile)) {
      return false;
    }
    if (!delta.getProjectRelativePath().toString().startsWith(pathPrefix)) {
      return false;
    }
    if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.REMOVED)) != 0) {
      return true;
    }
    if ((delta.getKind() & IResourceDelta.CHANGED) != 0
        && (delta.getFlags() & IResourceDelta.CONTENT) != 0) {
      return true;
    }
    return false;
  }

  public void assertPaths(String... expected) {
    Assert.assertEquals(toString(Arrays.asList(expected)), toString(getPaths()));
  }

  private static String toString(Collection<String> strings) {
    StringBuilder sb = new StringBuilder();
    for (String string : strings) {
      sb.append(string).append('\n');
    }
    return sb.toString();
  }

}
