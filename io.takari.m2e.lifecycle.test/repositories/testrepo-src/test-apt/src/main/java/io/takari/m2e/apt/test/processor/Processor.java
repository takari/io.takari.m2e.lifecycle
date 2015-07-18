package io.takari.m2e.apt.test.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class Processor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(Annotation.class)) {
      try {
        TypeElement cls = (TypeElement) element;
        PackageElement pkg = (PackageElement) cls.getEnclosingElement();
        String clsSimpleName = cls.getSimpleName() + "Builder";
        String pkgName = pkg.getQualifiedName().toString();
        String clsQualifiedName = pkgName + "." + clsSimpleName;
        FileObject sourceFile =
            processingEnv.getFiler().createSourceFile(clsQualifiedName, element);
        BufferedWriter w = new BufferedWriter(sourceFile.openWriter());
        try {
          w.append("package ").append(pkgName).append(";");
          w.newLine();
          w.append("@io.takari.m2e.apt.test.processor.GeneratedFrom(")
              .append(cls.getQualifiedName().toString()).append(".class)");
          w.newLine();
          w.append("public class ").append(clsSimpleName);
          w.append(" { }");
        } finally {
          w.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), element);
      }
    }
    return false; // not "claimed" so multiple processors can be tested
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(Annotation.class.getName());
  }
}
