package io.takari.m2e.incrementalbuild.core.internal;

import java.io.File;
import java.io.IOException;

import io.takari.incrementalbuild.workspace.Workspace;

public class IncrementalBuildWorkspace implements Workspace {

  @Override
  public boolean isPresent(File file) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void deleteFile(File file) throws IOException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void processOutput(File outputFile) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ResourceStatus getResourceStatus(File resource, long lastModified, long length) {
    // TODO Auto-generated method stub
    return null;
  }

}
