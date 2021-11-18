/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.name;

import com.exigen.cm.jackrabbit.BaseException;

/**
 * Thrown when an illegal JCR name string is encountered. This exception is
 * thrown when attempting to parse a JCR name string that does not match the
 * JCR name syntax, or is otherwise not a legal name. Note that an
 * {@link UnknownPrefixException} is thrown if the prefix of the JCR name
 * string is syntactically valid but not bound to any namespace. 
 * <p>
 * See the section 4.6 of the JCR 1.0 specification for details of the
 * JCR name syntax.
 */
public class IllegalNameException extends BaseException {

    /**
     * Creates an IllegalNameException with the given error message.
     *
     * @param message error message
     */
    public IllegalNameException(String message) {
        super(message);
    }

    /**
     * Creates an IllegalNameException with the given error message and
     * root cause exception.
     *
     * @param message error message
     * @param rootCause root cause exception
     */
    public IllegalNameException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

}
