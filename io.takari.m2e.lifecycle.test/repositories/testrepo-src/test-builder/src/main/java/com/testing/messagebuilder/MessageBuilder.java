package com.testing.messagebuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.takari.builder.Builder;
import io.takari.builder.BuilderContext;
import io.takari.builder.InputFile;
import io.takari.builder.LifecyclePhase;
import io.takari.builder.OutputFile;
import io.takari.builder.Parameter;

public class MessageBuilder {
  
  @InputFile
  private Path inputFile;
  
  @Parameter("${project.basedir}/pom.xml")
  private String pom;
  
  @OutputFile("${project.build.directory}/out.txt")
  private Path outputFile;
  
  /**
   * 
   * @throws Exception
   */
  @Builder(name="build-message", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
  public void execute () throws Exception {
    if (Files.exists(inputFile)) {
      String message = new String(Files.readAllBytes(inputFile));
      if (!message.isEmpty()) {
        if (message.equals("pom-error")) {
          throw new Exception(message);
//          BuilderContext.getMessages().error(Paths.get(pom), 0, 0, message, null);
        } else if (message.equals("out")) {
          Files.write(outputFile, message.getBytes());
        } else {
          BuilderContext.getMessages().error(inputFile, 0, 0, message, null);
        }
        
      }
    }
  }
}
