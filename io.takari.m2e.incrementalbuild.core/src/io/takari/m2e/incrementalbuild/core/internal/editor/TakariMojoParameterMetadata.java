package io.takari.m2e.incrementalbuild.core.internal.editor;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.editor.xml.internal.mojo.DefaultMojoParameterMetadata;
import org.eclipse.m2e.editor.xml.mojo.MojoParameter;
import org.eclipse.m2e.editor.xml.mojo.PlexusConfigHelper;

@SuppressWarnings("restriction")
public class TakariMojoParameterMetadata extends DefaultMojoParameterMetadata {

  @Override
  public List<MojoParameter> loadMojoParameters(PluginDescriptor desc, MojoDescriptor mojo,
      PlexusConfigHelper helper, IProgressMonitor monitor) throws CoreException {

    List<MojoParameter> parameters = super.loadMojoParameters(desc, mojo, helper, monitor);

    MojoParameter goalConfig =
        new MojoParameter(mojo.getGoal(), mojo.getImplementation(), new ArrayList<>(parameters));
    parameters.add(goalConfig);

    return parameters;
  }

}
