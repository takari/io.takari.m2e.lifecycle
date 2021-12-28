package io.takari.m2e.lifecycle.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.takari.m2e.test.WorkspaceChangeRecorder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

@SuppressWarnings("restriction")
public class PomPropertiesTest extends AbstractMavenProjectTestCase {
  private final WorkspaceChangeRecorder recorder = new WorkspaceChangeRecorder("target/classes");

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

  @Test
  public void testPomProperties() throws Exception {
    IProject project = importProject("projects/pomproperties/pom.xml");
    waitForJobsToComplete();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    assertNoErrors(project);

    String path =
        "target/classes/META-INF/maven/io.takari.m2e.incrementalbuild.test/pomproperties/pom.properties";
    IFile file = project.getFile(path);
    assertTrue(file.isSynchronized(IResource.DEPTH_ONE));

    Properties properties = loadProperties(file);
    assertEquals("io.takari.m2e.incrementalbuild.test", properties.getProperty("groupId"));
    assertEquals("pomproperties", properties.getProperty("artifactId"));
    assertEquals("0.1", properties.getProperty("version"));

    // incremental no-change rebuild
    recorder.clear();
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
    recorder.assertPaths(new String[0]);

    //
    recorder.clear();
    copyContent(project, "pom.xml-changed", "pom.xml");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    String pathChanged =
        "target/classes/META-INF/maven/io.takari.m2e.incrementalbuild.test-changed/pomproperties/pom.properties";
    recorder.assertPaths(path, pathChanged);
    
    IFile fileChanged = project.getFile(pathChanged);
    assertFalse(file.exists());
    assertTrue(fileChanged.isSynchronized(IResource.DEPTH_ONE));

    properties = loadProperties(fileChanged);
    assertEquals("io.takari.m2e.incrementalbuild.test-changed", properties.getProperty("groupId"));
    assertEquals("pomproperties", properties.getProperty("artifactId"));
    assertEquals("0.1", properties.getProperty("version"));
  }

  private Properties loadProperties(IFile file) throws IOException, CoreException {
    Properties properties = new Properties();
    try (InputStream is = file.getContents()) {
      properties.load(is);
    }
    return properties;
  }
}
