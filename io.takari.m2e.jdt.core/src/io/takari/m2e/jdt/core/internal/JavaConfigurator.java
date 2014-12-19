/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.m2e.jdt.core.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import org.eclipse.m2e.jdt.internal.AbstractJavaProjectConfigurator;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;

@SuppressWarnings("restriction")
public class JavaConfigurator extends AbstractJavaProjectConfigurator
    implements
      IJavaProjectConfigurator {

  private static final String COMPILER_PLUGIN_GROUP_ID = "io.takari.maven.plugins";

  private static final String COMPILER_PLUGIN_ARTIFACT_ID = "takari-lifecycle-plugin";

  private static final String GOAL_COMPILE = "compile";

  private static final String GOAL_TESTCOMPILE = "testCompile";

  private static final String DEFAULT_COMPILER_LEVEL = "1.7";

  private static final String PATH_EXPORT_PACKAGE = "META-INF/takari/export-package";

  private static final String PATH_MANIFESTMF = "META-INF/MANIFEST.MF";

  @Override
  protected List<MojoExecution> getCompilerMojoExecutions(ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {
    return request.getMavenProjectFacade().getMojoExecutions(COMPILER_PLUGIN_GROUP_ID,
        COMPILER_PLUGIN_ARTIFACT_ID, monitor, GOAL_COMPILE, GOAL_TESTCOMPILE);
  }

  @Override
  protected boolean isTestCompileExecution(MojoExecution execution) {
    return GOAL_TESTCOMPILE.equals(execution.getGoal());
  }

  @Override
  protected boolean isCompileExecution(MojoExecution execution) {
    return GOAL_COMPILE.equals(execution.getGoal());
  }

  @Override
  protected String getDefaultSourceLevel() {
    return DEFAULT_COMPILER_LEVEL;
  }

  @Override
  protected String getDefaultTargetLevel(String source) {
    if (source != null) {
      if ("1.2".equals(source) || "1.3".equals(source)) {
        return "1.4";
      }
      return source;
    }
    return DEFAULT_COMPILER_LEVEL;
  }

  @Override
  protected void addJavaProjectOptions(Map<String, String> options,
      ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    super.addJavaProjectOptions(options, request, monitor);
    if (enforceClasspathAccessRules(request.getMavenProjectFacade(), monitor)) {
      options.put(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, "error"); //$NON-NLS-1$
    }
  }

  private boolean enforceClasspathAccessRules(IMavenProjectFacade facade, IProgressMonitor monitor)
      throws CoreException {
    MavenProject mavenProject = facade.getMavenProject(monitor);
    for (MojoExecution execution : facade.getMojoExecutions(COMPILER_PLUGIN_GROUP_ID,
        COMPILER_PLUGIN_ARTIFACT_ID, monitor, GOAL_COMPILE)) {
      String accessRulesViolation =
          getParameterValue(mavenProject, "accessRulesViolation", String.class, execution, monitor);
      if ("error".equals(accessRulesViolation)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    if (enforceClasspathAccessRules(facade, monitor)) {
      configureClasspathAccessRules(facade, classpath, monitor);
    }
  }

  private void configureClasspathAccessRules(IMavenProjectFacade facade,
      IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = facade.getMavenProject(monitor);

    Map<ArtifactKey, Artifact> dependencies = new HashMap<>();
    Set<ArtifactKey> directDependencies = new HashSet<>();

    for (Artifact artifact : mavenProject.getArtifacts()) {
      dependencies.put(toArtifactKey(artifact), artifact);
    }
    for (Dependency artifact : mavenProject.getDependencies()) {
      directDependencies.add(toArtifactKey(artifact));
    }

    for (IClasspathEntryDescriptor entry : classpath.getEntryDescriptors()) {
      ArtifactKey artifactKey = entry.getArtifactKey();
      if (!dependencies.containsKey(artifactKey)) {
        continue; // ignore custom classpath entries
      }
      if (directDependencies.contains(artifactKey)) {
        // direct dependency, honour exported-package configuration
        Collection<String> exportedPackages = getExportedPackages(dependencies.get(artifactKey));
        if (exportedPackages != null) {
          for (String exportedPackage : exportedPackages) {
            IPath pattern = new Path(exportedPackage).append("/*");
            entry.addAccessRule(JavaCore.newAccessRule(pattern, IAccessRule.K_ACCESSIBLE));
          }
          entry.addAccessRule(JavaCore.newAccessRule(new Path("**"), IAccessRule.K_NON_ACCESSIBLE
              | IAccessRule.IGNORE_IF_BETTER));
        }
      } else {
        // indirect dependency, forbid everything
        entry.addAccessRule(JavaCore.newAccessRule(new Path("**"), IAccessRule.K_NON_ACCESSIBLE
            | IAccessRule.IGNORE_IF_BETTER));
      }
    }
  }

  private Collection<String> getExportedPackages(Artifact artifact) {
    File file = artifact.getFile();
    if (file.isDirectory()) {
      return getExportedPackages(file);
    } else if (file.isFile()) {
      try (ZipFile zip = new ZipFile(file)) {
        return getExportedPackages(zip);
      } catch (IOException e) {
        // silently ignore
      }
    }
    return null;
  }

  private Collection<String> getExportedPackages(ZipFile zip) {
    ZipEntry entry = zip.getEntry(PATH_EXPORT_PACKAGE);
    if (entry != null) {
      try (InputStream is = zip.getInputStream(entry)) {
        return parseExportPackage(is);
      } catch (IOException e) {
        // silently ignore
      }
    }
    entry = zip.getEntry(PATH_MANIFESTMF);
    if (entry != null) {
      try (InputStream is = zip.getInputStream(entry)) {
        return parseBundleManifest(is);
      } catch (IOException | BundleException e) {
        // silently ignore
      }
    }
    return null;
  }

  private Collection<String> getExportedPackages(File directory) {
    try (InputStream is = new FileInputStream(new File(directory, PATH_EXPORT_PACKAGE))) {
      return parseExportPackage(is);
    } catch (IOException e) {
      // silently ignore
    }
    try (InputStream is = new FileInputStream(new File(directory, PATH_MANIFESTMF))) {
      return parseBundleManifest(is);
    } catch (IOException | BundleException e) {
      // silently ignore
    }
    return null;
  }

  protected static Collection<String> parseExportPackage(InputStream is) throws IOException {
    LineProcessor<List<String>> processor = new LineProcessor<List<String>>() {
      final List<String> result = new ArrayList<String>();

      @Override
      public boolean processLine(String line) throws IOException {
        result.add(line.replace('.', '/'));
        return true; // keep reading
      }

      @Override
      public List<String> getResult() {
        return result;
      }
    };
    return CharStreams.readLines(new InputStreamReader(is, Charsets.UTF_8), processor);
  }

  protected static Collection<String> parseBundleManifest(InputStream is) throws IOException,
      BundleException {
    Headers<String, String> headers = Headers.parseManifest(is);
    if (!headers.containsKey(Constants.BUNDLE_SYMBOLICNAME)) {
      return null; // not an OSGi bundle
    }
    String exportPackageHeader = headers.get(Constants.EXPORT_PACKAGE);
    if (exportPackageHeader == null) {
      return ImmutableSet.of(); // nothing is exported
    }
    Set<String> packages = new HashSet<>();
    for (ManifestElement element : ManifestElement.parseHeader(Constants.EXPORT_PACKAGE,
        exportPackageHeader)) {
      packages.add(element.getValue().replace('.', '/'));
    }
    return packages;
  }

  private ArtifactKey toArtifactKey(Artifact artifact) {
    return new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(),
        artifact.getBaseVersion(), artifact.getClassifier());
  }

  private ArtifactKey toArtifactKey(Dependency artifact) {
    return new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
        artifact.getClassifier());
  }

  @Override
  public void configureRawClasspath(ProjectConfigurationRequest request,
      IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException {}
}
