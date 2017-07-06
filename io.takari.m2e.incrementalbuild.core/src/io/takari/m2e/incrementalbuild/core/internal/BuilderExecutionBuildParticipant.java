package io.takari.m2e.incrementalbuild.core.internal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;

public class BuilderExecutionBuildParticipant extends MojoExecutionBuildParticipant {

  public BuilderExecutionBuildParticipant(MojoExecution execution, boolean runOnIncremental,
      boolean runOnConfiguration) {
    super(execution, runOnIncremental, runOnConfiguration);
  }

  @Override
  public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
    if (appliesToBuildKind(kind)) {
      IMaven maven = MavenPlugin.getMaven();

      maven.execute(getMavenProjectFacade().getMavenProject(), getMojoExecution(), monitor);
    }
    return getProjectDependencies();
  }

  private Set<IProject> getProjectDependencies() {
    List<Dependency> mavenDependencies =
        getMavenProjectFacade().getMavenProject().getDependencies();
    IMavenProjectRegistry registry = MavenPlugin.getMavenProjectRegistry();
    Set<IProject> iProjectDependencies = new LinkedHashSet<>();

    for (Dependency mavenDependency : mavenDependencies) {
      IMavenProjectFacade facade = registry.getMavenProject(mavenDependency.getGroupId(),
          mavenDependency.getArtifactId(), mavenDependency.getVersion());
      if (facade != null && facade.getProject() != null) {
        iProjectDependencies.add(facade.getProject());
      }
    }
    return iProjectDependencies;
  }
}
