package io.takari.m2e.jdt.test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.ClasspathHelpers;

@SuppressWarnings("restriction")
public class AnnotationProcessingTest extends AbstractMavenProjectTestCase {

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
}
