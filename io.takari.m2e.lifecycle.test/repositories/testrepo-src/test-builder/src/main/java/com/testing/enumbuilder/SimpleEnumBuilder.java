package com.testing.enumbuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import io.takari.builder.Builder;
import io.takari.builder.GeneratedSourcesDirectory;
import io.takari.builder.InputDirectoryFiles;
import io.takari.builder.LifecyclePhase;
import io.takari.builder.Parameter;

public class SimpleEnumBuilder {
  
  @InputDirectoryFiles(defaultValue="${project.basedir}/src/main/resources", defaultIncludes="*.enum-values")
  private List<Path> inputFiles;
  
  @Parameter(required=true)
  private String packageName;
  
  @GeneratedSourcesDirectory(defaultValue="${project.build.directory}/generated-sources/enum")
  private Path outputDir;
  
  /**
   * 
   * @throws Exception
   */
  @Builder(name="build-enum", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
  public void execute () throws Exception {
    inputFiles.forEach(p -> this.createEnum(p));
  }
  
  private void createEnum(Path valuesFile) {
    try {
      String name = valuesFile.getFileName().toString().replaceFirst("[.][^.]+$", "");
      TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(name)
          .addModifiers(Modifier.PUBLIC);
      
      try (InputStream in = Files.newInputStream(valuesFile); BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
        String line = null;
        while ((line = reader.readLine()) != null) {
          addEnumEntry(enumBuilder, line.split("\\s"));
        }
      }
      
      TypeSpec typeSpec = enumBuilder
        .addField(int.class, "value", Modifier.PRIVATE, Modifier.FINAL)
        .addMethod(MethodSpec.constructorBuilder()
          .addParameter(int.class, "value")
          .addStatement("this.$N = $N", "value", "value")
          .build())
        .addMethod(MethodSpec.methodBuilder("getValue")
            .addStatement("return this.$N", "value")
            .returns(int.class)
            .build())
        .build();
      
      JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();

      javaFile.writeTo(outputDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void addEnumEntry(TypeSpec.Builder enumBuilder, String[] values) {
    enumBuilder.addEnumConstant(values[0], TypeSpec.anonymousClassBuilder("$L", Integer.parseInt(values[1])).build());
  }

}
