package io.takari.m2e.incrementalbuild.test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

@SuppressWarnings("restriction")
public class BasicTest extends AbstractMavenProjectTestCase {

  private static class WorkspaceChangeRecorder
      implements
        IResourceChangeListener,
        IResourceDeltaVisitor {

    private List<String> paths = new ArrayList<>();

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
      if (delta.getResource() instanceof IFile
          && delta.getProjectRelativePath().toString().startsWith("target/resources")) {
        if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED)) != 0) {
          paths.add(delta.getProjectRelativePath().toString());
        }
      }
      return true;
    }

    public void clear() {
      paths.clear();
    }

    public List<String> getPaths() {
      return paths;
    }
  }

  private final WorkspaceChangeRecorder recorder = new WorkspaceChangeRecorder();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(recorder);
  }

  @Override
  protected void tearDown() throws Exception {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(recorder);
    super.tearDown();
  }

  private void assertPaths(List<String> actual, String... expected) {
    assertEquals(toString(Arrays.asList(expected)), toString(actual));
  }

  private static String toString(Collection<String> strings) {
    StringBuilder sb = new StringBuilder();
    for (String string : strings) {
      sb.append(string).append('\n');
    }
    return sb.toString();
  }

  private void assertSynchronized(IProject project, String path) {
    IFile file = project.getFile(path);
    assertTrue(file + " synchronized", file.isSynchronized(IResource.DEPTH_ZERO));
  }

  //
  // the tests
  //

  public void testBasic() throws Exception {
    IProject project = importProject("projects/basic/pom.xml");
    waitForJobsToComplete();
    assertNoErrors(project);

    // full build, assert the output is regenerated even if the input didn't change
    recorder.clear();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertPaths(recorder.getPaths(), "target/resources/file1.txt");

    // no-change incremental build, assert no outputs
    recorder.clear();
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertPaths(recorder.getPaths(), new String[0]);

    // create new file
    recorder.clear();
    project.getFile("src/resources/file2.txt").create(new ByteArrayInputStream(new byte[0]), true,
        monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertPaths(recorder.getPaths(), "target/resources/file2.txt");
    assertSynchronized(project, "target/resources/file1.txt");

    // change existing file
    recorder.clear();
    project.getFile("src/resources/file2.txt").setContents(
        new ByteArrayInputStream(new byte[] {1}), 0, monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertPaths(recorder.getPaths(), "target/resources/file2.txt");
    assertSynchronized(project, "target/resources/file1.txt");

    // delete existing file
    recorder.clear();
    project.getFile("src/resources/file2.txt").delete(true, monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertPaths(recorder.getPaths(), "target/resources/file2.txt");
    assertSynchronized(project, "target/resources/file1.txt");

    // create a file that does not match expected input pattern
    recorder.clear();
    project.getFile("src/resources/file2.xtx").create(new ByteArrayInputStream(new byte[0]), true,
        monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertPaths(recorder.getPaths(), new String[0]);
  }

}
