/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.value;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * A <code>DoubleValue</code> provides an implementation
 * of the <code>Value</code> interface representing a double value.
 */
public class DoubleValue extends BaseValue {

    public static final int TYPE = PropertyType.DOUBLE;

    private final Double dblNumber;

    /**
     * Constructs a <code>DoubleValue</code> object representing a double.
     *
     * @param dblNumber the double this <code>DoubleValue</code> should represent
     */
    public DoubleValue(Double dblNumber) {
        super(TYPE);
        this.dblNumber = dblNumber;
    }

    /**
     * Constructs a <code>DoubleValue</code> object representing a double.
     *
     * @param dbl the double this <code>DoubleValue</code> should represent
     */
    public DoubleValue(double dbl) {
        super(TYPE);
        this.dblNumber = new Double(dbl);
    }

    /**
     * Returns a new <code>DoubleValue</code> initialized to the value
     * represented by the specified <code>String</code>.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>DoubleValue</code> representing the
     *         the specified value.
     * @throws javax.jcr.ValueFormatException If the <code>String</code> does not
     *                                        contain a parsable <code>double</code>.
     */
    public static DoubleValue valueOf(String s) throws ValueFormatException {
        try {
            return new DoubleValue(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            throw new ValueFormatException("invalid format", e);
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p/>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>DoubleValue</code> object that
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
        if (obj instanceof DoubleValue) {
            DoubleValue other = (DoubleValue) obj;
            if (dblNumber == other.dblNumber) {
                return true;
            } else if (dblNumber != null && other.dblNumber != null) {
                return dblNumber.equals(other.dblNumber);
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
        if (dblNumber != null) {
            return dblNumber.toString();
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

        if (dblNumber != null) {
            // loosing timezone information...
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(dblNumber.longValue()));
            return cal;
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

        if (dblNumber != null) {
            return dblNumber.longValue();
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

        throw new ValueFormatException("conversion to boolean failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        setValueConsumed();

        if (dblNumber != null) {
            return dblNumber.doubleValue();
        } else {
            throw new ValueFormatException("empty value");
        }
    }
}
