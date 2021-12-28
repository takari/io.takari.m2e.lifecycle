/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.m2e.incrementalbuild.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.takari.m2e.test.WorkspaceChangeRecorder;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

@SuppressWarnings("restriction")
public class IncrementalbuildTest extends AbstractMavenProjectTestCase {

  private final WorkspaceChangeRecorder recorder = new WorkspaceChangeRecorder("target/resources");

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(recorder);
  }

  @After
  @Override
  public void tearDown() throws Exception {
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

  @Test
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
    recorder.assertPaths("target/resources/file1.txt");

    // no-change incremental build, assert no outputs
    recorder.clear();
    project.getFile("pom.xml").touch(monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    recorder.assertPaths(new String[0]);

    // create new file
    recorder.clear();
    project.getFile("src/resources/file2.txt").create(new ByteArrayInputStream(new byte[0]), true,
        monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    recorder.assertPaths("target/resources/file2.txt");
    assertSynchronized(project, "target/resources/file1.txt");

    // change existing file
    recorder.clear();
    project.getFile("src/resources/file2.txt").setContents(new ByteArrayInputStream(new byte[] {1}),
        0, monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    recorder.assertPaths("target/resources/file2.txt");
    assertSynchronized(project, "target/resources/file1.txt");

    // delete existing file
    recorder.clear();
    project.getFile("src/resources/file2.txt").delete(true, monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    recorder.assertPaths("target/resources/file2.txt");
    assertSynchronized(project, "target/resources/file1.txt");

    // create a file that does not match expected input pattern
    recorder.clear();
    project.getFile("src/resources/file2.xtx").create(new ByteArrayInputStream(new byte[0]), true,
        monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    recorder.assertPaths(new String[0]);
  }

  @Test
  public void testBasic_synchronized() throws Exception {
    IProject project = importProject("projects/basic/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    project.getFile("src/resources/file2.txt").create(new ByteArrayInputStream(new byte[0]), true,
        monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();

    recorder.clear();
    recorder.setPathPrefix("target/incremental");
    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    waitForJobsToComplete();
    recorder.assertPaths(new String[0]);
  }

  @Test
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
    project.deleteMarkers(IMavenConstants.MARKER_CONFIGURATION_ID, true, IResource.DEPTH_INFINITE);
    assertNoErrors(project);
    recorder.assertPaths("target/resources/file1.txt");
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
    project.deleteMarkers(IMavenConstants.MARKER_CONFIGURATION_ID, true, IResource.DEPTH_INFINITE);
    assertNoErrors(project);
    recorder.assertPaths("target/resources/file1.txt");
    assertFalse(project.getFile("target/resources/file1.txt").isAccessible());
  }

  @Test
  public void testBasic_cleanGeneratedResources() throws Exception {
    IProject project = importProject("projects/basic/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertTrue(project.getFile("target/resources/file1.txt").isAccessible());

    // resource is deleted from sources folder and from its output location
    // can happen when source change is picked by scm update followed by clean build
    project.getFile("src/resources/file1.txt").delete(true, monitor);
    project.getFile("target/resources/file1.txt").delete(true, monitor);

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertFalse(project.getFile("target/resources/file1.txt").isAccessible());
  }

  @Test
  public void testBasic_incremental_deleteSourceDirectory() throws Exception {
    IProject project = importProject("projects/basic/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertTrue(project.getFile("target/resources/file1.txt").isAccessible());

    project.getFile("src/resources/file1.txt").delete(true, monitor);
    project.getFile("src/resources/file1.xtx").delete(true, monitor);
    project.getFolder("src/resources").delete(true, monitor);

    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertFalse(project.getFile("target/resources/file1.txt").isAccessible());
  }

  @Test
  public void testBasic_rename() throws Exception {
    IProject project = importProject("projects/basic/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertTrue(project.getFile("target/resources/file1.txt").isAccessible());

    project.getFile("src/resources/file1.txt")
        .move(project.getFile("src/resources/file2.txt").getFullPath(), false, monitor);

    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertFalse(project.getFile("target/resources/file1.txt").isAccessible());
    assertTrue(project.getFile("target/resources/file2.txt").isAccessible());
  }

  @Test
  public void testDeltaBuildConfigurationChange() throws Exception {
    IProject project = importProject("projects/config-change/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    recorder.clear();
    copyContent(project, "pom.xml-changed", "pom.xml");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    project.deleteMarkers(IMavenConstants.MARKER_CONFIGURATION_ID, true, IResource.DEPTH_INFINITE);
    assertNoErrors(project);
    recorder.assertPaths("target/resources/file1.txt");
    assertSynchronized(project, "target/resources/file1.txt");
  }

  @Test
  public void testConfigurationUpdate() throws Exception {
    IProject project = importProject("projects/config-change/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    copyContent(project, "pom.xml-changed", "pom.xml");
    recorder.clear();
    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
    recorder.assertPaths(new String[0]);
  }

  @Test
  public void testCrossModule() throws Exception {
    IProject[] projects = importProjects("projects/cross-module", new String[] {"modulea/pom.xml"},
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
    recorder.assertPaths("target/resources/file1.txt");
  }

  @Test
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
    // TODO why "config isn't up to date" here but after not the next pom.xml changes?
    project.deleteMarkers(IMavenConstants.MARKER_CONFIGURATION_ID, true, IResource.DEPTH_INFINITE);
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

  @Test
  public void testInputsBasedir() throws Exception {
    IProject project = importProject("projects/inputs-basedir/pom.xml");

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    recorder.assertPaths("target/resources/resource.txt");
  }

  @Test
  public void testUnmodifiedOutputStream() throws Exception {
    IProject project = importProject("projects/unmodified-output-stream/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    recorder.assertPaths("target/resources/file1.txt");

    // no-change incremental build, assert no outputs
    recorder.clear();
    project.getFile("pom.xml").touch(monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    assertTrue(project.getFile("target/resources/file1.txt").isSynchronized(IResource.DEPTH_ZERO));
    recorder.assertPaths(new String[0]);
  }

  @Test
  public void testBasedirDoesnotexist() throws Exception {
    IProject project = importProject("projects/basedir-doesnotexist/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
  }

  @Test
  public void testChainedMojos() throws Exception {
    // assert mojos can read resources generated by other mojos during the same build
    IProject project = importProject("projects/chained-mojos/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    recorder.assertPaths("target/resources/file.txt", "target/resources2/file.txt");
  }
}
