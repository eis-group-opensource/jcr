/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.core.util;

import java.io.PrintStream;

/**
 * Utility interface for internal use.
 * <p/>
 * A <code>Dumpable</code> object supports dumping its state in a human readable
 * format for diagnostic/debug purposes.
 */
public interface Dumpable {
    /**
     * Dumps the state of this instance in a human readable format for
     * diagnostic purposes.
     *
     * @param ps stream to dump state to
     */
    void dump(PrintStream ps);
}
