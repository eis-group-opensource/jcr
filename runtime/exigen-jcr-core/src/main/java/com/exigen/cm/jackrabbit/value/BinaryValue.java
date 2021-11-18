/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.value;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * A <code>BinaryValue</code> provides an implementation
 * of the <code>Value</code> interface representing a binary value.
 */
public class BinaryValue extends BaseValue {

    public static final int TYPE = PropertyType.BINARY;

    // those fields are mutually exclusive, i.e. only one can be non-null
    private byte[] streamData = null;
    private String text = null;

	private BLOBFileValue blobValue;

    /**
     * Constructs a <code>BinaryValue</code> object based on a string.
     *
     * @param text the string this <code>BinaryValue</code> should represent
     */
    public BinaryValue(String text) {
        super(TYPE);
        this.text = text;
    }

    /**
     * Constructs a <code>BinaryValue</code> object based on a stream.
     *
     * @param stream the stream this <code>BinaryValue</code> should represent
     */
    public BinaryValue(InputStream stream) {
        super(TYPE);
        this.stream = stream;
    }

    /**
     * Constructs a <code>BinaryValue</code> object based on a stream.
     *
     * @param stream the stream this <code>BinaryValue</code> should represent
     */
    public BinaryValue(BLOBFileValue blobValue) {
        super(TYPE);
        this.blobValue = blobValue;
    }

    /**
     * Constructs a <code>BinaryValue</code> object based on a stream.
     *
     * @param data the stream this <code>BinaryValue</code> should represent
     */
    public BinaryValue(byte[] data) {
        super(TYPE);
        streamData = data;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p/>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>BinaryValue</code> object that
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
        if (obj instanceof BinaryValue) {
            BinaryValue other = (BinaryValue) obj;
            if (text == other.text && stream == other.stream
                    && streamData == other.streamData) {
                return true;
            }
            // stream, streamData and text are mutually exclusive,
            // i.e. only one of them can be non-null
            if (stream != null) {
                return stream.equals(other.stream);
            } else if (streamData != null) {
                return streamData.equals(other.streamData);
            } else {
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
     * Gets the string representation of this binary value.
     *
     * @return string representation of this binary value.
     *
     * @throws javax.jcr.ValueFormatException
     * @throws javax.jcr.RepositoryException  if another error occurs
     */
    public String getInternalString()
            throws ValueFormatException, RepositoryException {
        // build text value if necessary
        if (streamData != null) {
            try {
                text = new String(streamData, DEFAULT_ENCODING);
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException(DEFAULT_ENCODING
                        + " not supported on this platform", e);
            }
            streamData = null;
        } else if (stream != null || blobValue != null) {
        	if (stream == null){
        		stream = blobValue.getStream();
        	}
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = stream.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
                byte[] data = out.toByteArray();
                text = new String(data, DEFAULT_ENCODING);
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException(DEFAULT_ENCODING
                        + " not supported on this platform", e);
            } catch (IOException e) {
                throw new RepositoryException("conversion from stream to string failed", e);
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
            stream = null;
        }

        if (text != null) {
            return text;
        } else {
            throw new ValueFormatException("empty value");
        }
    }

    //----------------------------------------------------------------< Value >
    /**
     * {@inheritDoc}
     */
    public InputStream getStream()
            throws IllegalStateException, RepositoryException {
        setStreamConsumed();

        // build stream value if necessary
        if (streamData != null) {
            stream = new ByteArrayInputStream(streamData);
            streamData = null;
        } else if (blobValue != null && stream == null){
        	stream = blobValue.getStream();
        } else if (text != null) {
            try {
                stream = new ByteArrayInputStream(text.getBytes(DEFAULT_ENCODING));
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException(DEFAULT_ENCODING
                        + " not supported on this platform", e);
            }
            text = null;
        }

        return super.getStream();
    }

	public BLOBFileValue getBlobValue() {
		return blobValue;
	}

	public void setBlobValue(BLOBFileValue blobValue) {
		this.blobValue = blobValue;
	}
}
