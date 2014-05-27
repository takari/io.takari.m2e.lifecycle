package io.takari.m2e.incrementalbuild.core.internal.workspace;

import io.takari.incrementalbuild.workspace.MessageSink;
import io.takari.incrementalbuild.workspace.Workspace;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class ThreadLocalBuildWorkspace implements Workspace, MessageSink {

  private static final MessageSink NULL_MESSAGESINK = new MessageSink() {
    @Override
    public MessageSink replayMessageSink() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void message(Object resource, int line, int column, String message, Severity severity,
        Throwable cause) {}

    @Override
    public int getErrorCount() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clearMessages(Object resource) {
      throw new UnsupportedOperationException();
    }
  };

  private static final ThreadLocal<AbstractBuildWorkspace> delegate = new ThreadLocal<>();



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

  public static void setDelegate(AbstractBuildWorkspace delegate) {
    ThreadLocalBuildWorkspace.delegate.set(delegate);
  }

  @Override
  public void message(Object resource, int line, int column, String message, Severity severity,
      Throwable cause) {
    delegate.get().message(resource, line, column, message, severity, cause);
  }

  @Override
  public MessageSink replayMessageSink() {
    // no need to recreate resource markers in workspace
    return NULL_MESSAGESINK;
  }

  @Override
  public int getErrorCount() {
    return 0; // never fail the build because of error messages
  }

  @Override
  public void clearMessages(Object resource) {
    delegate.get().clearMessages(resource);
  }
}
