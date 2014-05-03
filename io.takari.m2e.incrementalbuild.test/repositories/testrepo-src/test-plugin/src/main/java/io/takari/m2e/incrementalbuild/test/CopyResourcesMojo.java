package io.takari.m2e.incrementalbuild.test;

import io.takari.incrementalbuild.BuildContext;
import io.takari.incrementalbuild.BuildContext.Input;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.common.io.Files;

@Mojo(name = "copy-resources")
public class CopyResourcesMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.basedir}/src/resources")
  private File directory;

  @Parameter(defaultValue = "${project.build.directory}/resources")
  private File outputDirectory;

  @Component
  private BuildContext context;

  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      for (Input<File> resource : context.registerAndProcessInputs(directory, null, null)) {
        File src = resource.getResource();
        Path relpath = directory.toPath().relativize(src.toPath());
        File dst = outputDirectory.toPath().resolve(relpath).toFile();
        try (OutputStream os = resource.associateOutput(dst).newOutputStream()) {
          Files.copy(src, os);
        }
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Could not copy resources", e);
    }
  }

}
