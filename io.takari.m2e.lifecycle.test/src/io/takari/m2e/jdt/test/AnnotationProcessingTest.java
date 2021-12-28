package io.takari.m2e.jdt.test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.ClasspathHelpers;
import org.junit.Test;

@SuppressWarnings("restriction")
public class AnnotationProcessingTest extends AbstractMavenProjectTestCase {

  @Test
  public void testAnnotationProcessing() throws Exception {
    IProject project = importProject("projects/compile-proc/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    ClasspathHelpers.assertClasspath(project //
        , "/compile-proc/src/main/java" //
        , "/compile-proc/src/test/java" //
        , "/compile-proc/target/generated-sources/annotations" //
        , "/compile-proc/target/generated-test-sources/test-annotations" //
        , "org.eclipse.jdt.launching.JRE_CONTAINER/.*" //
        , "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER" //
    );
  }

  @Test
  public void testAnnotationProcessingCache() throws Exception {
    IProject parent = importProject("projects/compile-proc-cache/pom.xml");
    parent.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(parent);

    IProject project1 = importProject("projects/compile-proc-cache/one/pom.xml");
    project1.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project1);

    IProject project2 = importProject("projects/compile-proc-cache/two/pom.xml");
    project2.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project2);

    IFile pr1test2 = project1.getFile("src/main/java/io/takari/m2e/apt/test/Test2.java");
    IFile pr2test2 = project2.getFile("src/main/java/io/takari/m2e/apt/test/Test2.java");
    // move file
    pr2test2.move(pr1test2.getFullPath(), true, monitor);

    project1.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project1);

    project2.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project2);
  }
}
