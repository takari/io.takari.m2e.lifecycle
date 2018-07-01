/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.m2e.incrementalbuild.core.internal.workspace;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;


// this class was originally copied from io.takari.incrementalbuild.spi.IncrementalFileOutputStream
class IncrementalFileOutputStream extends OutputStream {

  public static final int BUF_SIZE = 1024 * 16;

  private final AbstractBuildWorkspace workspace;

  private final File file;

  private final RandomAccessFile raf;

  private final byte[] buffer;

  private boolean modified;

  private boolean closed = false;

  public IncrementalFileOutputStream(AbstractBuildWorkspace workspace, File file)
      throws IOException {
    this.workspace = workspace;
    this.file = file;

    if (file == null) {
      throw new IllegalArgumentException("output file not specified");
    }

    File parent = file.getParentFile();

    if (!parent.isDirectory() && !parent.mkdirs()) {
      throw new IOException("Could not create directory " + parent);
    }

    if (!file.exists()) {
      setModified();
    }

    raf = new RandomAccessFile(file, "rw");
    buffer = new byte[BUF_SIZE];
  }

  @Override
  public void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    long pos = raf.getFilePointer();
    if (pos < raf.length()) {
      setModified();
      raf.setLength(pos);
    }
    raf.close();
    if (!modified) {
      workspace.clearProcessedOutput(file);
    }
  }

  private void setModified() {
    modified = true;
    workspace.processOutput(file);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (modified) {
      raf.write(b, off, len);
    } else {
      for (int n = len; n > 0;) {
        int read = raf.read(buffer, 0, Math.min(buffer.length, n));
        if (read < 0 || !arrayEquals(b, off + len - n, buffer, 0, read)) {
          setModified();
          if (read > 0) {
            raf.seek(raf.getFilePointer() - read);
          }
          raf.write(b, off + len - n, n);
          break;
        } else {
          n -= read;
        }
      }
    }
  }

  private boolean arrayEquals(byte[] a1, int off1, byte[] a2, int off2, int len) {
    for (int i = 0; i < len; i++) {
      if (a1[off1 + i] != a2[off2 + i]) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void write(int b) throws IOException {
    if (modified) {
      raf.write(b);
    } else {
      int i = raf.read();
      if (i < 0 || i != (b & 0xFF)) {
        setModified();
        if (i >= 0) {
          raf.seek(raf.getFilePointer() - 1);
        }
        raf.write(b);
      }
    }
  }

}
