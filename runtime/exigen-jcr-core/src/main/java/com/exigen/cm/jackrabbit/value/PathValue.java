/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.value;


import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.Path;

/**
 * A <code>PathValue</code> provides an implementation
 * of the <code>Value</code> interface representing a <code>PATH</code> value
 * (an absolute or relative workspace path).
 */
public class PathValue extends BaseValue {

    public static final int TYPE = PropertyType.PATH;

    private final String path;

    /**
     * Returns a new <code>PathValue</code> initialized to the value
     * represented by the specified <code>String</code>.
     * <p/>
     * The specified <code>String</code> must be a valid absolute or relative
     * path.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>PathValue</code> representing the
     *         the specified value.
     * @throws javax.jcr.ValueFormatException If the <code>String</code> is not a valid
     *                              absolute or relative path.
     */
    public static PathValue valueOf(String s) throws ValueFormatException {
        if (s != null) {
            try {
                Path.checkFormat(s);
            } catch (MalformedPathException mpe) {
                throw new ValueFormatException(mpe.getMessage());
            }
            return new PathValue(s);
        } else {
            throw new ValueFormatException("not a valid path format");
        }
    }

    /**
     * Protected constructor creating a <code>PathValue</code> object
     * without validating the path.
     *
     * @param path the path this <code>PathValue</code> should represent
     * @see #valueOf
     */
    protected PathValue(String path) {
        super(TYPE);
        this.path = path;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p/>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>PathValue</code> object that
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
        if (obj instanceof PathValue) {
            PathValue other = (PathValue) obj;
            if (path == other.path) {
                return true;
            } else if (path != null && other.path != null) {
                return path.equals(other.path);
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
        if (path != null) {
            return path;
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

        throw new ValueFormatException("conversion to date failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public long getLong()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        setValueConsumed();

        throw new ValueFormatException("conversion to long failed: inconvertible types");
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

        throw new ValueFormatException("conversion to double failed: inconvertible types");
    }
}
