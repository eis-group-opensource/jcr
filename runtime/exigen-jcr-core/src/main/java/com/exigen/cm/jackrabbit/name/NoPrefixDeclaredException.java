/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.name;

import com.exigen.cm.jackrabbit.BaseException;


/**
 * Thrown when the namespace prefix of a qualified name is not found. This
 * exception is thrown when trying to convert a qualified name whose namespace
 * prefix is not found into a JCR name string. The JCR name string can not be
 * created without the namespace prefix.
 */
public class NoPrefixDeclaredException extends BaseException {

    /**
     * Creates a NoPrefixDeclaredException with the given error message.
     *
     * @param message error message
     */
    public NoPrefixDeclaredException(String message) {
        super(message);
    }

    /**
     * Creates a NoPrefixDeclaredException with the given error message and
     * root cause exception.
     *
     * @param message error message
     * @param rootCause root cause exception
     */
    public NoPrefixDeclaredException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

}
