package io.takari.m2e.apt.test;

import io.takari.m2e.apt.test.processor.Annotation;

@Annotation
public class TestComponent {

  public static TestComponentBuilder builder() {
    return new TestComponentBuilder();
  }
}
