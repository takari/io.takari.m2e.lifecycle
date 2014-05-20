package io.takari.m2e.incrementalbuild.core.internal.workspace;

import io.takari.incrementalbuild.workspace.Workspace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;

@SuppressWarnings("restriction")
abstract class AbstractBuildWorkspace implements Workspace, IIncrementalBuildFramework.BuildContext {

  private final IProject project;

  private final IIncrementalBuildFramework.BuildResultCollector results;

  private final Set<File> processedOutputs = new HashSet<>();

  private final Set<File> deletedOutputs = new HashSet<>();

  protected AbstractBuildWorkspace(IProject project,
      IIncrementalBuildFramework.BuildResultCollector results) {
    this.project = project;
    this.results = results;
  }

  private IPath getRelativePath(File file) {
    IPath basedir = project.getLocation();
    IPath path = Path.fromOSString(file.getAbsolutePath());
    if (basedir.isPrefixOf(path)) {
      return path.makeRelativeTo(basedir);
    }
    throw new IllegalArgumentException();
  }

  protected IFile getFile(File file) {
    return project.getFile(getRelativePath(file));
  }

  protected IFolder getFolder(File file) {
    return project.getFolder(getRelativePath(file));
  }

  @Override
  public boolean isPresent(File file) {
    if (deletedOutputs.contains(file)) {
      return false;
    }
    if (processedOutputs.contains(file)) {
      return true;
    }
    // TODO does this trigger refreshFromLocal?
    return getFile(file).exists();
  }


  @Override
  public void deleteFile(File file) throws IOException {
    if (!file.delete()) {
      throw new IOException("Could not delete " + file);
    }
    deletedOutputs.add(file);
    processedOutputs.remove(file);
    results.refresh(file);
  }

  @Override
  public void processOutput(File file) {
    deletedOutputs.remove(file);
    processedOutputs.add(file);
    results.refresh(file);
  }

  @Override
  public OutputStream newOutputStream(final File file) throws IOException {
    File parent = file.getParentFile();
    if (!parent.isDirectory() && !parent.mkdirs()) {
      throw new IOException("Could not create directory " + parent);
    }
    processOutput(file);
    return new FileOutputStream(file);
  }

  @Override
  public void release() {
    ThreadLocalBuildWorkspace.setDelegate(null);
  }
}
