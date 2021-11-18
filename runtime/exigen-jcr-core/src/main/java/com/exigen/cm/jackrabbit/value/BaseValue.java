/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.value;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import com.exigen.cm.jackrabbit.util.ISO8601;

/**
 * This class is the superclass of the type-specific
 * classes implementing the <code>Value</code> interfaces.
 *
 * @see javax.jcr.Value
 * @see StringValue
 * @see LongValue
 * @see DoubleValue
 * @see BooleanValue
 * @see DateValue
 * @see BinaryValue
 * @see NameValue
 * @see PathValue
 * @see ReferenceValue
 */
public abstract class BaseValue implements Value {

    protected static final String DEFAULT_ENCODING = "UTF-8";

    private static final short STATE_UNDEFINED = 0;
    private static final short STATE_VALUE_CONSUMED = 1;
    private static final short STATE_STREAM_CONSUMED = 2;

    private short state = STATE_UNDEFINED;

    protected final int type;

    protected InputStream stream = null;

    /**
     * Package-private default constructor.
     *
     * @param type The type of this value.
     */
    BaseValue(int type) {
        this.type = type;
    }

    /**
     * Checks if the non-stream value of this instance has already been
     * consumed (if any getter methods except <code>{@link #getStream()}</code> and
     * <code>{@link #getType()}</code> have been previously called at least once) and
     * sets the state to <code>STATE_STREAM_CONSUMED</code>.
     *
     * @throws IllegalStateException if any getter methods other than
     *                               <code>getStream()</code> and
     *                               <code>getType()</code> have been
     *                               previously called at least once.
     */
    protected void setStreamConsumed() throws IllegalStateException {
        if (state == STATE_VALUE_CONSUMED) {
            throw new IllegalStateException("non-stream value has already been consumed");
        }
        state = STATE_STREAM_CONSUMED;
    }

    /**
     * Checks if the stream value of this instance has already been
     * consumed (if {@link #getStream()} has been previously called
     * at least once) and sets the state to <code>STATE_VALUE_CONSUMED</code>.
     *
     * @throws IllegalStateException if <code>getStream()</code> has been
     *                               previously called at least once.
     */
    protected void setValueConsumed() throws IllegalStateException {
        if (state == STATE_STREAM_CONSUMED) {
            throw new IllegalStateException("stream value has already been consumed");
        }
        state = STATE_VALUE_CONSUMED;
    }

    /**
     * Returns the internal string representation of this value without modifying
     * the value state.
     *
     * @return the internal string representation
     * @throws javax.jcr.ValueFormatException if the value can not be represented as a
     *                              <code>String</code> or if the value is
     *                              <code>null</code>.
     * @throws javax.jcr.RepositoryException  if another error occurs.
     */
    protected abstract String getInternalString()
            throws ValueFormatException, RepositoryException;

    //----------------------------------------------------------------< Value >
    /**
     * {@inheritDoc}
     */
    public int getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public Calendar getDate()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        setValueConsumed();

        Calendar cal = ISO8601.parse(getInternalString());
        if (cal == null) {
            throw new ValueFormatException("not a valid date format");
        } else {
            return cal;
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLong()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        setValueConsumed();

        try {
            return Long.parseLong(getInternalString());
        } catch (NumberFormatException e) {
            throw new ValueFormatException("conversion to long failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        setValueConsumed();

        return Boolean.valueOf(getInternalString()).booleanValue();
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        setValueConsumed();

        try {
            return Double.parseDouble(getInternalString());
        } catch (NumberFormatException e) {
            throw new ValueFormatException("conversion to double failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getStream()
            throws IllegalStateException, RepositoryException {
        setStreamConsumed();

        if (stream != null) {
            return stream;
        }

        try {
            // convert via string
            stream = new ByteArrayInputStream(getInternalString().getBytes(DEFAULT_ENCODING));
            return stream;
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException(DEFAULT_ENCODING
                    + " not supported on this platform", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getString()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        setValueConsumed();

        return getInternalString();
    }
}
