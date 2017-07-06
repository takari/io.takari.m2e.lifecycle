/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.m2e.incrementalbuild.core.internal.workspace;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import io.takari.incrementalbuild.workspace.MessageSink;
import io.takari.incrementalbuild.workspace.Workspace;

public class ThreadLocalBuildWorkspace implements Workspace, MessageSink {

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
  public boolean isRegularFile(File file) {
    return delegate.get().isRegularFile(file);
  }

  @Override
  public boolean isDirectory(File file) {
    return delegate.get().isDirectory(file);
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

  public static AbstractBuildWorkspace getDelegate() {
    return ThreadLocalBuildWorkspace.delegate.get();
  }

  @Override
  public void message(Object resource, int line, int column, String message, Severity severity,
      Throwable cause) {
    delegate.get().message(resource, line, column, message, severity, cause);
  }

  @Override
  public void clearMessages(Object resource) {
    delegate.get().clearMessages(resource);
  }
}
