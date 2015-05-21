package io.takari.m2e.jar.internal;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class JarProjectConfigurator extends AbstractProjectConfigurator {

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {}

  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade,
      MojoExecution execution, IPluginExecutionMetadata executionMetadata) {
    Xpp3Dom configuration = execution.getConfiguration();

    if (Boolean.parseBoolean(configuration.getChild("skip").getValue())) {
      return null;
    }

    PluginDescriptor pluginDescriptor = execution.getMojoDescriptor().getPluginDescriptor();
    MojoDescriptor descriptor = pluginDescriptor.getMojo("pom-properties");

    String _executionId = "m2e-takari-lifecycle_" + execution.getExecutionId() + "_pom-properties";
    MojoExecution _execution =
        new MojoExecution(execution.getPlugin(), "pom-properties", _executionId);
    _execution.setConfiguration(execution.getConfiguration());
    _execution.setMojoDescriptor(descriptor);
    _execution.setLifecyclePhase(execution.getLifecyclePhase());

    return new MojoExecutionBuildParticipant(_execution, true);
  }

}
