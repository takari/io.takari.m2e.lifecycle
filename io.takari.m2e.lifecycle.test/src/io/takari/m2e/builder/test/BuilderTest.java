/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.m2e.builder.test;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;

import io.takari.m2e.test.WorkspaceChangeRecorder;

@SuppressWarnings("restriction")
public class BuilderTest extends AbstractMavenProjectTestCase {

  private final WorkspaceChangeRecorder recorder = new WorkspaceChangeRecorder("target/generated-sources");

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

  private void assertSynchronized(IProject project, String path) {
    IFile file = project.getFile(path);
    assertTrue(file + " synchronized", file.isSynchronized(IResource.DEPTH_ZERO));
  }

  //
  // the tests
  //

  public void testBasic() throws Exception {
    IProject project = importProject("projects/basic-builder/pom.xml");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.FULL_BUILD);
    assertNoErrors(project);

    // full build, assert the output is regenerated even if the input didn't change
    
    recorder.clear();
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.FULL_BUILD);
    assertNoErrors(project);
    recorder.assertPaths("target/generated-sources/enum/com/testing/Enum1.java");
    assertSynchronized(project, "target/generated-sources/enum/com/testing/Enum1.java");

    // no-change incremental build, assert no outputs
    recorder.clear();
    project.getFile("pom.xml").touch(monitor);
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    assertNoErrors(project);
    recorder.assertPaths(new String[0]);
    assertSynchronized(project, "target/generated-sources/enum/com/testing/Enum1.java");

    // create new file
    recorder.clear();
    project.getFile("src/main/resources/Enum2.enum-values").create(new ByteArrayInputStream("Something 1\nElse 2".getBytes()), true,
        monitor);
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    assertNoErrors(project);
    recorder.assertPaths(
      "target/generated-sources/enum/com/testing/Enum1.java",
      "target/generated-sources/enum/com/testing/Enum2.java");
    assertSynchronized(project, "target/generated-sources/enum/com/testing/Enum1.java");
    assertSynchronized(project, "target/generated-sources/enum/com/testing/Enum2.java");

    // change existing file
    recorder.clear();
    project.getFile("src/main/resources/Enum2.enum-values").setContents(new ByteArrayInputStream("New 1\nContent 2".getBytes()),
        0, monitor);
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    assertNoErrors(project);
    recorder.assertPaths(
      "target/generated-sources/enum/com/testing/Enum1.java",
      "target/generated-sources/enum/com/testing/Enum2.java");
    assertSynchronized(project, "target/generated-sources/enum/com/testing/Enum1.java");
    assertSynchronized(project, "target/generated-sources/enum/com/testing/Enum2.java");

    // delete existing file
    recorder.clear();
    project.getFile("src/main/resources/Enum2.enum-values").delete(true, monitor);
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    assertNoErrors(project);
    recorder.assertPaths(
      "target/generated-sources/enum/com/testing/Enum1.java",
      "target/generated-sources/enum/com/testing/Enum2.java");
    assertSynchronized(project, "target/generated-sources/enum/com/testing/Enum1.java");
    assertSynchronized(project, "target/generated-sources/enum/com/testing/Enum2.java");

    // create a file that does not match expected input pattern
    recorder.clear();
    project.getFile("src/main/resources/Enum3.txt").create(new ByteArrayInputStream(new byte[0]), true,
        monitor);
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    assertNoErrors(project);
    recorder.assertPaths(new String[0]);
    assertSynchronized(project, "target/generated-sources/enum/com/testing/Enum3.java");
  }

  public void testBasic_synchronized() throws Exception {
    IProject project = importProject("projects/basic-builder/pom.xml");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.FULL_BUILD);
    assertNoErrors(project);

    project.getFile("src/main/resources/file2.txt").create(new ByteArrayInputStream(new byte[0]), true,
        monitor);
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    recorder.clear();

    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    waitForJobsToComplete();
    
    recorder.assertPaths(new String[0]);
  }

  public void testBasic_pom_configuration_changes() throws Exception {
    IProject project = importProject("projects/basic-builder/pom.xml");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.FULL_BUILD);
    assertNoErrors(project);
    assertTrue(project.getFile("target/generated-sources/enum/com/testing/Enum1.java").isAccessible());

    // change includes/exclude to look for different file extension
    recorder.clear();
    copyContent(project, "pom.xml-changed", "pom.xml");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    project.deleteMarkers(IMavenConstants.MARKER_CONFIGURATION_ID, true, IResource.DEPTH_INFINITE);
    assertNoErrors(project);
    recorder.assertPaths("target/generated-sources/enum/com/testing/Enum1.java");
    assertFalse(project.getFile("target/generated-sources/enum/com/testing/Enum1.java").isAccessible());

    // reset back to original pom.xml
    copyContent(project, "pom.xml-orig", "pom.xml");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.FULL_BUILD);
    assertNoErrors(project);
    assertTrue(project.getFile("target/generated-sources/enum/com/testing/Enum1.java").isAccessible());
  }

  public void testBasic_cleanGeneratedSources() throws Exception {
    IProject project = importProject("projects/basic-builder/pom.xml");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.FULL_BUILD);
    assertNoErrors(project);
    assertTrue(project.getFile("target/generated-sources/enum/com/testing/Enum1.java").isAccessible());

    // resource is deleted from sources folder and generated source from its output location
    // can happen when source change is picked by scm update followed by clean build
    project.getFile("src/main/resources/Enum1.enum-values").delete(true, monitor);
    project.getFile("target/generated-sources/enum/com/testing/Enum1.java").delete(true, monitor);

    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.FULL_BUILD);
    assertNoErrors(project);
    assertFalse(project.getFile("target/generated-sources/enum/com/testing/Enum1.java").isAccessible());
  }

//  public void testBasic_incremental_deleteSourceDirectory() throws Exception {
//    IProject project = importProject("projects/basic-builder/pom.xml");
//    buildWithSecurityManager(project, IncrementalProjectBuilder.FULL_BUILD);
//    assertNoErrors(project);
//    assertTrue(project.getFile("target/generated-sources/enum/com/testing/Enum1.java").isAccessible());
//
//    project.getFile("src/main/resources/Enum1.enum-values").delete(true, monitor);
//    project.getFolder("src/main/resources").delete(true, monitor);
//
//    buildWithSecurityManager(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
//    assertNoErrors(project);
//    assertFalse(project.getFile("target/generated-sources/enum/com/testing/Enum1.java").isAccessible());
//  }

  public void testBasic_rename() throws Exception {
    IProject project = importProject("projects/basic-builder/pom.xml");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.FULL_BUILD);
    assertNoErrors(project);
    assertTrue(project.getFile("target/generated-sources/enum/com/testing/Enum1.java").isAccessible());

    project.getFile("src/main/resources/Enum1.enum-values")
        .move(project.getFile("src/main/resources/Enum2.enum-values").getFullPath(), false, monitor);

    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    assertNoErrors(project);
    assertFalse(project.getFile("target/generated-sources/enum/com/testing/Enum1.java").isAccessible());
    assertTrue(project.getFile("target/generated-sources/enum/com/testing/Enum2.java").isAccessible());
  }

  public void testCrossModule() throws Exception {
    IProject[] projects = importProjects("projects/cross-module-builder", new String[] {"basic-builder/pom.xml"},
        new ResolverConfiguration());
    IProject project = projects[0];
    
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.FULL_BUILD);
    assertNoErrors(project);

    // behaviour is generally undefined, but at very least there should not be a failure
    recorder.clear();
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.FULL_BUILD);
    assertNoErrors(project);
    recorder.assertPaths("target/generated-sources/enum/com/testing/Enum1.java");
  }

  public void testMessages() throws Exception {
    IProject project = importProject("projects/messages-builder/pom.xml");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.FULL_BUILD);
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID, "test", 0,
        "src/main/resources/message.txt", project);

    // change config to remove resource from relevant resource set
    copyContent(project, "pom.xml-changed", "pom.xml");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    // TODO why is project configuration not up to date here, but not after next pom change?
    project.deleteMarkers(IMavenConstants.MARKER_CONFIGURATION_ID, true, IResource.DEPTH_INFINITE);
    assertNoErrors(project);

    // reintroduce resource with a different message
    copyContent(project, "pom.xml-orig", "pom.xml");
    copyContent(project, "src/main/resources/message.txt-changed", "src/main/resources/message.txt");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID, "not-a-test", 0,
        "src/main/resources/message.txt", project);

    // remove the message
    copyContent(project, "src/main/resources/message.txt-empty", "src/main/resources/message.txt");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    assertNoErrors(project);

    // message on pom
    copyContent(project, "src/main/resources/message.txt-pom-error", "src/main/resources/message.txt");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID, "pom-error", 1, project);
    
    // message on output file
    copyContent(project, "src/main/resources/message.txt-out", "src/main/resources/message.txt");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    assertNoErrors(project);
    
    // reintroduce the original message
    copyContent(project, "src/main/resources/message.txt-orig", "src/main/resources/message.txt");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID, "test", 0,
        "src/main/resources/message.txt", project);
  }
  
  public void testConfigurationOnlyBuildClasspath() throws Exception {
    IProject project = importProject("projects/basic-builder/pom.xml");
    waitForJobsToComplete();
    assertNoErrors(project);

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(6, cp.length);

    assertEquals(project.getFolder("src/main/java").getFullPath(), cp[0].getPath());
    assertEquals(project.getFolder("target/generated-sources/enum").getFullPath(), cp[1].getPath());
    assertEquals(project.getFolder("src/main/resources").getFullPath(), cp[2].getPath());
    assertEquals(project.getFolder("src/test/java").getFullPath(), cp[3].getPath());
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", cp[4].getPath().segment(0));
    assertEquals("org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", cp[5].getPath().toOSString());
    
  }
  
  public void testConfigurationUpdate() throws Exception {
    IProject project = importProject("projects/basic-builder/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    copyContent(project, "pom.xml-changed", "pom.xml");
    recorder.clear();
    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
    recorder.assertPaths(new String[0]);
  }

  public void testCrossModuleDependencies() throws Exception {
    IProject dependency = importProject("projects/cross-module-dependencies-builder/dependency/pom.xml");
    buildAndWaitForJobsToComplete(dependency, IncrementalProjectBuilder.FULL_BUILD);
    assertNoErrors(dependency);
    
    IProject project = importProject("projects/cross-module-dependencies-builder/project/pom.xml");
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.FULL_BUILD);
    assertNoErrors(project);
    recorder.assertPaths("target/generated-sources/output.txt");

    // edit a file in a dependency project
    copyContent(dependency, "resources/1.txt-changed", "resources/1.txt");
    recorder.clear();
    buildAndWaitForJobsToComplete(dependency, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    assertNoErrors(dependency);
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    assertNoErrors(project);
    recorder.assertPaths("target/generated-sources/output.txt");
    
    // revert the change
    copyContent(dependency, "resources/1.txt-orig", "resources/1.txt");
    recorder.clear();
    buildAndWaitForJobsToComplete(dependency, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    assertNoErrors(dependency);
    buildAndWaitForJobsToComplete(project, IncrementalProjectBuilder.INCREMENTAL_BUILD);
    assertNoErrors(project);
    recorder.assertPaths("target/generated-sources/output.txt");
  }

  private void buildAndWaitForJobsToComplete(IProject project, int incrementalBuilderType)
      throws CoreException, InterruptedException {
      project.build(incrementalBuilderType, monitor);
      waitForJobsToComplete();
  }
}
