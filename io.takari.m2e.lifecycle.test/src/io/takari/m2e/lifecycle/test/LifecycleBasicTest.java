/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.m2e.lifecycle.test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

@SuppressWarnings("restriction")
public class LifecycleBasicTest extends AbstractMavenProjectTestCase {

  public void testTakariJar() throws Exception {
    IProject project = importProject("projects/takari-jar/pom.xml");
    waitForJobsToComplete();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    assertNoErrors(project);

    // assert java project configuration
    IJavaProject jproject = JavaCore.create(project);
    assertEquals("1.5", jproject.getOption(JavaCore.COMPILER_SOURCE, false));
    assertEquals("1.5", jproject.getOption(JavaCore.COMPILER_COMPLIANCE, false));
    assertEquals("1.5", jproject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, false));

    // assert JRE classpath container
    IClasspathEntry cpe = getJREContainer(jproject);
    assertNotNull("JRE Container", cpe);
    assertEquals("J2SE-1.5", JavaRuntime.getExecutionEnvironmentId(cpe.getPath()));

    // XXX assert lifecycle mapping

    assertFile(project, "target/classes/main/MainClass.class");
    assertFile(project, "target/classes/main.properties");
    assertFile(project, "target/test-classes/test/TestClass.class");
    assertFile(project, "target/test-classes/test.properties");
  }

  private IClasspathEntry getJREContainer(IJavaProject jproject) throws JavaModelException {
    for (IClasspathEntry cpe : jproject.getRawClasspath()) {
      if (JavaRuntime.JRE_CONTAINER.equals(cpe.getPath().segment(0))) {
        return cpe;
      }
    }
    return null;
  }

  private void assertFile(IProject project, String path) {
    IFile file = project.getFile(path);
    assertTrue(file + " synchronized", file.isSynchronized(IResource.DEPTH_ZERO));
    assertTrue(file + " exists", file.exists());
  }
}
