/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.util.name;

import com.exigen.cm.jackrabbit.BaseException;



/**
 * Base class for exceptions about malformed or otherwise
 * invalid JCR names and paths.
 */
public class NameException extends BaseException {

    /**
     * Creates a NameException with the given error message.
     *
     * @param message error message
     */
    public NameException(String message) {
        super(message);
    }

    /**
     * Creates a NameException with the given error message and
     * root cause exception.
     *
     * @param message   error message
     * @param rootCause root cause exception
     */
    public NameException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

}
