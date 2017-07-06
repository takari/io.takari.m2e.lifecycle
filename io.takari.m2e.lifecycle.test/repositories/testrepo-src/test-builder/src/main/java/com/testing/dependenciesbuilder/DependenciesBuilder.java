package com.testing.dependenciesbuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.takari.builder.Builder;
import io.takari.builder.DependencyResources;
import io.takari.builder.LifecyclePhase;
import io.takari.builder.OutputFile;
import io.takari.builder.ResolutionScope;

public class DependenciesBuilder {
  
  @DependencyResources(scope=ResolutionScope.COMPILE, defaultIncludes="**/*.txt")
  private Collection<URL> resources;
  

  @OutputFile(defaultValue="${project.build.directory}/generated-sources/output.txt")
  private Path outputFile;
  
  /**
   * 
   * @throws Exception
   */
  @Builder(name="concat-text-files", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
  public void execute () throws Exception {
    
    Files.write(outputFile, String.join("\n", resources.stream()
        .map(r -> readResourceContent(r))
        .collect(Collectors.toCollection(TreeSet::new))).getBytes());
  }
  
  private String readResourceContent(URL url) {
    StringBuilder sb = new StringBuilder();
    try(BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))){
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        sb.append(inputLine);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return sb.toString();
  }
}
