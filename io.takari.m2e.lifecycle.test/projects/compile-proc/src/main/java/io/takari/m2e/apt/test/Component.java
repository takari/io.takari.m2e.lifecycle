package io.takari.m2e.apt.test;

import io.takari.m2e.apt.test.processor.Annotation;

@Annotation
public class Component {

  public static ComponentBuilder builder() {
    return new ComponentBuilder();
  }
}
