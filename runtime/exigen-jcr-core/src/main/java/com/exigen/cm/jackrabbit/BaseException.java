/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * The abstract class <code>BaseException</code> serves as the base class
 * for all exceptions that are specific to this JCR implementation and that
 * are not derived from <code>javax.jcr.RepositoryException</code>.
 */
public abstract class BaseException extends Exception {

    /**
     * Root failure cause
     */
    private final Throwable rootCause;

    /**
     * Constructs a new instance of this class with <code>null</code> as its
     * detail message.
     */
    public BaseException() {
        super();
        rootCause = null;
    }

    /**
     * Constructs a new instance of this class with the specified detail
     * message.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public BaseException(String message) {
        super(message);
        rootCause = null;
    }

    /**
     * Constructs a new instance of this class with the specified detail
     * message and root cause.
     *
     * @param message   the detail message. The detail message is saved for
     *                  later retrieval by the {@link #getMessage()} method.
     * @param rootCause root failure cause
     */
    public BaseException(String message, Throwable rootCause) {
        super(message);
        this.rootCause = rootCause;
    }

    /**
     * Constructs a new instance of this class with the specified root cause.
     *
     * @param rootCause root failure cause
     */
    public BaseException(Throwable rootCause) {
        super();
        this.rootCause = rootCause;
    }

    /**
     * Returns the detail message, including the message from the nested
     * exception if there is one.
     *
     * @return the detail message (which may be <code>null</code>).
     */
    public String getMessage() {
        String s = super.getMessage();
        if (rootCause == null) {
            return s;
        } else if (s == null) {
            return rootCause.getMessage();
        } else {
            return s + ": " + rootCause.getMessage();
        }
    }

    /**
     * Creates a localized description of this exception.
     * Subclasses may override this method in order to produce a
     * locale-specific message. For subclasses that do not override this
     * method, the default implementation returns the same result as
     * <code>getMessage()</code>.
     *
     * @return The localized description of this exception.
     */
    public String getLocalizedMessage() {
        return getMessage();
    }

    /**
     * Returns the cause of this exception or <code>null</code> if the
     * cause is nonexistent or unknown. (The cause is the throwable that
     * caused this exception to get thrown.)
     *
     * @return the cause of this exception or <code>null</code> if the
     *         cause is nonexistent or unknown.
     */
    public Throwable getCause() {
        return rootCause;
    }

    /**
     * Prints this <code>RepositoryException</code> and its backtrace to the
     * standard error stream.
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints this <code>RepositoryException</code> and its backtrace to the
     * specified print stream.
     *
     * @param s <code>PrintStream</code> to use for output
     */
    public void printStackTrace(PrintStream s) {
        synchronized (s) {
            super.printStackTrace(s);
            if (rootCause != null) {
                rootCause.printStackTrace(s);
            }
        }
    }

    /**
     * Prints this <code>RepositoryException</code> and its backtrace to
     * the specified print writer.
     *
     * @param s <code>PrintWriter</code> to use for output
     */
    public void printStackTrace(PrintWriter s) {
        synchronized (s) {
            super.printStackTrace(s);
            if (rootCause != null) {
                rootCause.printStackTrace(s);
            }
        }
    }
}
