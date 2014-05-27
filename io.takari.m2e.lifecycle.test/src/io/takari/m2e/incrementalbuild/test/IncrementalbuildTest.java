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
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;

@SuppressWarnings("restriction")
public class IncrementalbuildTest extends AbstractMavenProjectTestCase {

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

    private boolean isRelevant(IResourceDelta delta) {
      if (!(delta.getResource() instanceof IFile)) {
        return false;
      }
      if (!delta.getProjectRelativePath().toString().startsWith("target/resources")) {
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
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
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

  public void testBasic_includes_excludes_changed() throws Exception {
    IProject project = importProject("projects/basic/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertTrue(project.getFile("target/resources/file1.txt").isAccessible());

    // change includes/exclude to ignore all input resources in incremental mode
    recorder.clear();
    copyContent(project, "pom.xml-changed", "pom.xml");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertPaths(recorder.getPaths(), "target/resources/file1.txt");
    assertFalse(project.getFile("target/resources/file1.txt").isAccessible());

    // reset back to original pom.xml
    copyContent(project, "pom.xml-orig", "pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertTrue(project.getFile("target/resources/file1.txt").isAccessible());

    // change includes/exclude to ignore all input resources
    recorder.clear();
    copyContent(project, "pom.xml-changed", "pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertPaths(recorder.getPaths(), "target/resources/file1.txt");
    assertFalse(project.getFile("target/resources/file1.txt").isAccessible());
  }

  public void testDeltaBuildConfigurationChange() throws Exception {
    IProject project = importProject("projects/config-change/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    recorder.clear();
    copyContent(project, "pom.xml-changed", "pom.xml");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertPaths(recorder.getPaths(), "target/resources/file1.txt");
    assertSynchronized(project, "target/resources/file1.txt");
  }

  public void testConfigurationUpdate() throws Exception {
    IProject project = importProject("projects/config-change/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    copyContent(project, "pom.xml-changed", "pom.xml");
    recorder.clear();
    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
    assertPaths(recorder.getPaths(), new String[0]);
  }

  public void testCrossModule() throws Exception {
    IProject[] projects =
        importProjects("projects/cross-module", new String[] {"modulea/pom.xml"},
            new ResolverConfiguration());
    IProject project = projects[0];
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    // behaviour is generally undefined, but at very least there should not be a failure
    recorder.clear();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertPaths(recorder.getPaths(), "target/resources/file1.txt");
  }

  public void testMessages() throws Exception {
    IProject project = importProject("projects/messages/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID, "test123", 0,
        "src/resources/resource.txt", project);

    // change config to remove resource from relevant resource set
    copyContent(project, "pom.xml-changed", "pom.xml");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    // reintroduce resource with a different message
    copyContent(project, "pom.xml-orig", "pom.xml");
    copyContent(project, "src/resources/resource.txt-changed", "src/resources/resource.txt");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID, "123test", 0,
        "src/resources/resource.txt", project);

    // remove the message
    copyContent(project, "src/resources/resource.txt-empty", "src/resources/resource.txt");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    // reintroduce the message
    copyContent(project, "src/resources/resource.txt-orig", "src/resources/resource.txt");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID, "test123", 0,
        "src/resources/resource.txt", project);
  }

  public void testInputsBasedir() throws Exception {
    IProject project = importProject("projects/inputs-basedir/pom.xml");

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertPaths(recorder.getPaths(), "target/resources/resource.txt");
  }

}
