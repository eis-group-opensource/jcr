/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.value;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

/**
 * This class implements the  <code>ValueFactory</code> interface.
 *
 * @see javax.jcr.Session#getValueFactory()
 */
public class ValueFactoryImpl implements ValueFactory {

    /**
     * Constructs a <code>ValueFactory</code> object.
     */
    public ValueFactoryImpl() {
    }

    //---------------------------------------------------------< ValueFactory >
    /**
     * {@inheritDoc}
     */
    public Value createValue(boolean value) {
        return new BooleanValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(Calendar value) {
        return new DateValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(double value) {
        return new DoubleValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(InputStream value) {
        return new BinaryValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(long value) {
        return new LongValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(Node value) throws RepositoryException {
        return new ReferenceValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(String value) {
        return new StringValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(String value, int type)
            throws ValueFormatException {
        return ValueHelper.convert(value, type);
    }

	public static ValueFactoryImpl getInstance() {
		// TODO Auto-generated method stub
		return new ValueFactoryImpl();
	}
}
