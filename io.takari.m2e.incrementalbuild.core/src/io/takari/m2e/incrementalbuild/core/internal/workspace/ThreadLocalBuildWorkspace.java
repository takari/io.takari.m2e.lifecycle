package io.takari.m2e.incrementalbuild.core.internal.workspace;

import io.takari.incrementalbuild.workspace.Workspace;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class ThreadLocalBuildWorkspace implements Workspace {

  private static final ThreadLocal<Workspace> delegate = new ThreadLocal<>();

  @Override
  public Mode getMode() {
    return delegate.get().getMode();
  }

  @Override
  public Workspace escalate() {
    return delegate.get().escalate();
  }

  @Override
  public boolean isPresent(File file) {
    return delegate.get().isPresent(file);
  }

  @Override
  public void deleteFile(File file) throws IOException {
    delegate.get().deleteFile(file);
  }

  @Override
  public void processOutput(File file) {
    delegate.get().processOutput(file);
  }

  @Override
  public OutputStream newOutputStream(File file) throws IOException {
    return delegate.get().newOutputStream(file);
  }

  @Override
  public ResourceStatus getResourceStatus(File resource, long lastModified, long length) {
    return delegate.get().getResourceStatus(resource, lastModified, length);
  }

  @Override
  public void walk(File basedir, FileVisitor visitor) throws IOException {
    delegate.get().walk(basedir, visitor);
  }

  public static void setDelegate(Workspace delegate) {
    ThreadLocalBuildWorkspace.delegate.set(delegate);
  }
}
