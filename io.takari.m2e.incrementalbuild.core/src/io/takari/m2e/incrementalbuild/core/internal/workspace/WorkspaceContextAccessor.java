package io.takari.m2e.incrementalbuild.core.internal.workspace;

import io.takari.builder.enforcer.PolicyContextPreserver.CurrentContextAccessor;

public class WorkspaceContextAccessor implements CurrentContextAccessor {

  @Override
  public Object getCurrentContext() {
    return ThreadLocalBuildWorkspace.getDelegate();
  }

  @Override
  public void setCurrentContext(Object value) {
    ThreadLocalBuildWorkspace.setDelegate((AbstractBuildWorkspace) value);
  }

}
