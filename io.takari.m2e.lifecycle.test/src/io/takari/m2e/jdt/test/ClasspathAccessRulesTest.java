package io.takari.m2e.jdt.test;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.jdt.internal.BuildPathManager;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.ClasspathHelpers;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

@SuppressWarnings("restriction")
public class ClasspathAccessRulesTest extends AbstractMavenProjectTestCase {

  public void testBasic() throws Exception {
    IProject project = importProject("projects/classpath-access-rules/basic/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertErrorMarker("org.eclipse.jdt.core.problem", "Access restriction", 9,
        "src/main/java/basic/Basic.java", project);
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] cp =
        BuildPathManager.getMaven2ClasspathContainer(javaProject).getClasspathEntries();
    assertEquals(2, cp.length);

    ClasspathHelpers.assertClasspath(
        new String[] {".*/junit-4.11.jar", ".*/hamcrest-core-1.3.jar"}, cp);

    IAccessRule[] rules = cp[1].getAccessRules();
    assertEquals(1, rules.length);
    assertEquals(new Path("**/*"), rules[0].getPattern());

    File exportsFile =
        project.getFile("target/classes/META-INF/takari/export-package").getLocation().toFile();
    List<String> exports = Files.readLines(exportsFile, Charsets.UTF_8);
    assertEquals(1, exports.size());
    assertEquals("basic", exports.get(0));
  }

}
