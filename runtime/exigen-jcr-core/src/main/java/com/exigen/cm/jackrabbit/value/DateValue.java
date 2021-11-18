/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.value;


import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import com.exigen.cm.jackrabbit.util.ISO8601;

/**
 * A <code>DateValue</code> provides an implementation
 * of the <code>Value</code> interface representing a date value.
 */
public class DateValue extends BaseValue {

    public static final int TYPE = PropertyType.DATE;

    private final Calendar date;

    /**
     * Constructs a <code>DateValue</code> object representing a date.
     *
     * @param date the date this <code>DateValue</code> should represent
     */
    public DateValue(Calendar date) {
        super(TYPE);
        this.date = date;
    }

    /**
     * Returns a new <code>DateValue</code> initialized to the value
     * represented by the specified <code>String</code>.
     * <p/>
     * The specified <code>String</code> must be a ISO8601-compliant date/time
     * string.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>DateValue</code> representing the
     *         the specified value.
     * @throws javax.jcr.ValueFormatException If the <code>String</code> is not a valid
     *                              ISO8601-compliant date/time string.
     * @see ISO8601
     */
    public static DateValue valueOf(String s) throws ValueFormatException {
        Calendar cal = ISO8601.parse(s);
        if (cal != null) {
            return new DateValue(cal);
        } else {
            throw new ValueFormatException("not a valid date format");
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p/>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>DateValue</code> object that
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
        if (obj instanceof DateValue) {
            DateValue other = (DateValue) obj;
            if (date == other.date) {
                return true;
            } else if (date != null && other.date != null) {
                return date.equals(other.date);
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
        if (date != null) {
            return ISO8601.format(date);
        } else {
            throw new ValueFormatException("empty value");
        }
    }

    //----------------------------------------------------------------< Value >
    /**
     * {@inheritDoc}
     */
    public Calendar getDate()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        setValueConsumed();

        if (date != null) {
            return date;
        } else {
            throw new ValueFormatException("empty value");
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLong()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        setValueConsumed();

        if (date != null) {
            return date.getTimeInMillis();
        } else {
            throw new ValueFormatException("empty value");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        setValueConsumed();

        if (date != null) {
            throw new ValueFormatException("cannot convert date to boolean");
        } else {
            throw new ValueFormatException("empty value");
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        setValueConsumed();

        if (date != null) {
            long ms = date.getTimeInMillis();
            if (ms <= Double.MAX_VALUE) {
                return ms;
            }
            throw new ValueFormatException("conversion from date to double failed: inconvertible types");
        } else {
            throw new ValueFormatException("empty value");
        }
    }
}
