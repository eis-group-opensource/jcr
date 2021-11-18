/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.name;

import com.exigen.cm.jackrabbit.BaseException;

/**
 * Thrown when a JCR name string with an unknown prefix is encountered.
 * This exception is thrown when attempting to parse a JCR name string
 * whose prefix is not bound to any namespace.
 */
public class UnknownPrefixException extends BaseException {

    /**
     * Creates an UnknownPrefixException with the given error message.
     *
     * @param message error message
     */
    public UnknownPrefixException(String message) {
        super(message);
    }

    /**
     * Creates an IllegalNameException with the given error message and
     * root cause exception.
     *
     * @param message error message
     * @param rootCause root cause exception
     */
    public UnknownPrefixException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

}
