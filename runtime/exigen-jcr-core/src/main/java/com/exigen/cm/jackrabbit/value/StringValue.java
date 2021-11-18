/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.value;

import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;

/**
 * A <code>StringValue</code> provides an implementation
 * of the <code>Value</code> interface representing a string value.
 */
public class StringValue extends BaseValue {

    public static final int TYPE = PropertyType.STRING;

    private final String text;

    /**
     * Constructs a <code>StringValue</code> object representing a string.
     *
     * @param text the string this <code>StringValue</code> should represent
     */
    public StringValue(String text) {
        super(TYPE);
        this.text = text;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p/>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>StringValue</code> object that
     * represents the same value as this object.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof StringValue) {
            StringValue other = (StringValue) obj;
            if (text == other.text) {
                return true;
            } else if (text != null && other.text != null) {
                return text.equals(other.text);
            }
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

    //------------------------------------------------------------< BaseValue >
    /**
     * {@inheritDoc}
     */
    protected String getInternalString() throws ValueFormatException {
        if (text != null) {
            return text;
        } else {
            throw new ValueFormatException("empty value");
        }
    }
}
