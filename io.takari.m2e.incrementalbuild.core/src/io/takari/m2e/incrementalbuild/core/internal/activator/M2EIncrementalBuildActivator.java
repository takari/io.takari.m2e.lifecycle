package io.takari.m2e.incrementalbuild.core.internal.activator;

import java.util.Arrays;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import io.takari.builder.enforcer.ComposableSecurityManagerPolicy;


public class M2EIncrementalBuildActivator implements BundleActivator {

  private static M2EIncrementalBuildActivator _default;

  private BundleContext context;

  public M2EIncrementalBuildActivator() {
    _default = this;
  }

  public static M2EIncrementalBuildActivator getDefault() {
    return _default;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    this.context = context;
    ComposableSecurityManagerPolicy.setSystemSecurityManager();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    this.context = null;
  }

  public List<Bundle> getBundles() {
    return Arrays.asList(context.getBundles());
  }
}
