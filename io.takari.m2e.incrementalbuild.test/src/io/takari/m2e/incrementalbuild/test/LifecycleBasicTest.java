package io.takari.m2e.incrementalbuild.test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

@SuppressWarnings("restriction")
public class LifecycleBasicTest extends AbstractMavenProjectTestCase {

  public void testTakariJar() throws Exception {
    IProject project = importProject("projects/takari-jar/pom.xml");
    waitForJobsToComplete();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    assertNoErrors(project);

    // XXX assert lifecycle mapping

    assertFile(project, "target/classes/main/MainClass.class");
    assertFile(project, "target/classes/main.properties");
    assertFile(project, "target/test-classes/test/TestClass.class");
    assertFile(project, "target/test-classes/test.properties");
  }

  private void assertFile(IProject project, String path) {
    IFile file = project.getFile(path);
    assertTrue(file + " synchronized", file.isSynchronized(IResource.DEPTH_ZERO));
    assertTrue(file + " exists", file.exists());
  }
}
