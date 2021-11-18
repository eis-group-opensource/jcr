/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.fs;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Extends the regular <code>java.io.OutputStream</code> with a random
 * access facility. Multiple <code>write()</code> operations can be
 * positioned off sequence with the {@link #seek} method.
 */
public abstract class RandomAccessOutputStream extends OutputStream {

    /**
     * Sets the current position in the resource where the next write
     * will occur.
     *
     * @param position the new position in the resource.
     * @throws IOException if an error occurs while seeking to the position.
     */
    public abstract void seek(long position) throws IOException;
}
