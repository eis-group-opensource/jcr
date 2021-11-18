/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.name;

import com.exigen.cm.jackrabbit.BaseException;


/**
 * Thrown when a malformed JCR path string is encountered. This exception is
 * thrown when attempting to parse a JCR path string that does not match the
 * JCR patch syntax, contains an invalid path elements, or is otherwise not
 * well formed.
 * <p>
 * See the section 4.6 of the JCR 1.0 specification for details of the
 * JCR path syntax.
 */
public class MalformedPathException extends BaseException {

    /**
     * Creates a MalformedPathException with the given error message.
     *
     * @param message error message
     */
    public MalformedPathException(String message) {
        super(message);
    }

    /**
     * Creates a MalformedPathException with the given error message
     * and root cause exception.
     *
     * @param message error message
     * @param rootCause root cause exception
     */
    public MalformedPathException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

}
