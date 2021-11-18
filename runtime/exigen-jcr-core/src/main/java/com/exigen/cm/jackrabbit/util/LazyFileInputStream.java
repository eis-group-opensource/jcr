/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This Class implements an InputStream that provides the same functionality
 * as a <code>FileInputStream</code> but opens the file by the first file access.
 */
public class LazyFileInputStream extends InputStream {

    /**
     * the underlying input stream
     */
    private FileInputStream in;

    /**
     * FileDescriptor to use
     */
    private FileDescriptor fd;

    /**
     * File to use
     */
    private File file;

    /**
     * Creates a new <code>LazyFileInputStream</code> for the given file. If the
     * file is unreadably, a FileNotFoundException is thrown.
     *
     * @param file
     * @throws java.io.FileNotFoundException
     */
    public LazyFileInputStream(File file)
            throws FileNotFoundException {
        // check if we can read from the file
        if (!file.canRead()) {
            throw new FileNotFoundException(file.getPath());
        }
        this.file = file;
    }

    /**
     * Creates a new <code>LazyFileInputStream</code> for the given file
     * desciptor.
     *
     * @param fdObj
     */
    public LazyFileInputStream(FileDescriptor fdObj) {
        this.fd = fdObj;
    }

    /**
     * Creates a new <code>LazyFileInputStream</code> for the given file. If the
     * file is unreadably, a FileNotFoundException is thrown.
     *
     * @param name
     * @throws java.io.FileNotFoundException
     */
    public LazyFileInputStream(String name) throws FileNotFoundException {
        this(new File(name));
    }

    /**
     * Opens the underlying file input stream in neccessairy.
     * @throws java.io.IOException
     */
    public void open() throws IOException {
        if (in == null) {
            if (file != null) {
                in = new FileInputStream(file);
            } else if (fd != null) {
                in = new FileInputStream(fd);
            } else {
                throw new IOException("Stream already closed.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int read() throws IOException {
        open();
        return in.read();
    }

    /**
     * {@inheritDoc}
     */
    public int available() throws IOException {
        open();
        return in.available();
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
        in = null;
        file = null;
        fd = null;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void reset() throws IOException {
        open();
        in.reset();
    }

    /**
     * {@inheritDoc}
     */
    public boolean markSupported() {
        try {
            open();
            return in.markSupported();
        } catch (IOException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void mark(int readlimit) {
        try {
            open();
            in.mark(readlimit);
        } catch (IOException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public long skip(long n) throws IOException {
       open();
       return in.skip(n);
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] b) throws IOException {
        open();
        return in.read(b);
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] b, int off, int len) throws IOException {
        open();
        return in.read(b, off, len);
    }

}
