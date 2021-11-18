/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.value;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import com.exigen.cm.impl.state2.StoreContainer;
import com.exigen.cm.jackrabbit.fs.FileSystemResource;
import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.util.ISO8601;
import com.exigen.cm.jackrabbit.uuid.UUID;

/**
 * <code>InternalValue</code> represents the internal format of a property value.
 * <p/>
 * The following table specifies the internal format for every property type:
 * <pre>
 * <table>
 * <tr><b>PropertyType</b><td></td><td><b>Internal Format</b></td></tr>
 * <tr>STRING<td></td><td>String</td></tr>
 * <tr>LONG<td></td><td>Long</td></tr>
 * <tr>DOUBLE<td></td><td>Double</td></tr>
 * <tr>DATE<td></td><td>Calendar</td></tr>
 * <tr>BOOLEAN<td></td><td>Boolean</td></tr>
 * <tr>NAME<td></td><td>QName</td></tr>
 * <tr>PATH<td></td><td>Path</td></tr>
 * <tr>BINARY<td></td><td>BLOBFileValue</td></tr>
 * <tr>REFERENCE<td></td><td>UUID</td></tr>
 * </table>
 * </pre>
 */
public class InternalValue {

    public static final InternalValue[] EMPTY_ARRAY = new InternalValue[0];

    private final Object val;
    private final int type;

    private Long sqlId;

    private String contentId;



    //------------------------------------------------------< factory methods >
    /**
     * @param value
     * @param nsResolver
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public static InternalValue create(Value value, NamespaceResolver nsResolver, StoreContainer sc)
            throws ValueFormatException, RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("null value");
        }

        switch (value.getType()) {
            case PropertyType.BINARY:
                try {
                    if (value instanceof BLOBFileValue) {
                        return new InternalValue((BLOBFileValue) value);
                    } else {
                        InputStream stream = value.getStream();
                        
                        try {
                        	if (sc != null) {
                        		return new InternalValue(new BLOBFileValue(stream, sc));
                        	} else {
                        		return new InternalValue(new BLOBFileValue(stream));
                        	}
                        } finally {
                            try {
                                stream.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }
                } catch (IOException ioe) {
                    throw new ValueFormatException(ioe.getMessage());
                }
            case PropertyType.BOOLEAN:
                return new InternalValue(value.getBoolean());
            case PropertyType.DATE:
                return new InternalValue(value.getDate());
            case PropertyType.DOUBLE:
                return new InternalValue(value.getDouble());
            case PropertyType.LONG:
                return new InternalValue(value.getLong());
            case PropertyType.REFERENCE:
                return new InternalValue(new UUID(value.getString()), false);
            case PropertyType283.WEAKREFERENCE:
                return new InternalValue(new UUID(value.getString()), true);
            case PropertyType.NAME:
                try {
                    return new InternalValue(QName.fromJCRName(value.getString(), nsResolver));
                } catch (IllegalNameException ine) {
                    throw new ValueFormatException(ine.getMessage());
                } catch (UnknownPrefixException upe) {
                    throw new ValueFormatException(upe.getMessage());
                }
            case PropertyType.PATH:
                try {
                    return new InternalValue(Path.create(value.getString(), nsResolver, false));
                } catch (MalformedPathException mpe) {
                    throw new ValueFormatException(mpe.getMessage());
                }
            case PropertyType.STRING:
                return new InternalValue(value.getString());

            default:
                throw new IllegalArgumentException("illegal value");
        }
    }

    /**
     * @param value
     * @param targetType
     * @param nsResolver
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public static InternalValue create(Value value, int targetType,
                                       NamespaceResolver nsResolver, StoreContainer sc)
            throws ValueFormatException, RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("null value");
        }
        return create(ValueHelper.convert(value, targetType), nsResolver, sc);
    }

    /**
     * @param value
     * @param targetType
     * @param nsResolver
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public static InternalValue create(String value, int targetType,
                                       NamespaceResolver nsResolver,
                                       StoreContainer sc)
            throws ValueFormatException, RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("null value");
        }
        return create(ValueHelper.convert(value, targetType), nsResolver, sc);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(String value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(long value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(double value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(Calendar value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(boolean value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(byte[] value) {
        return new InternalValue(new BLOBFileValue(value));
    }

    /**
     * @param value
     * @return
     * @throws IOException
     * @throws RepositoryException 
     */
    public static InternalValue create(InputStream value, String contentId, StoreContainer storeProvider) throws IOException, RepositoryException {
        InternalValue result;
        if (value != null){
        	BLOBFileValue internalValue = new BLOBFileValue(value, storeProvider);
            result = new InternalValue(internalValue);
            result.setContentId(internalValue.getContentId());
        } else {
        	BLOBFileValue internalValue = new BLOBFileValue(contentId, storeProvider);
            result = new InternalValue(internalValue);
            result.setContentId(internalValue.getContentId());
        }
        return result;
    }

    /**
     * @param value
     * @return
     * @throws IOException
     */
    public static InternalValue create(FileSystemResource value)
            throws IOException {
        return new InternalValue(new BLOBFileValue(value));
    }

    /**
     * @param value
     * @return
     * @throws IOException
     * @throws RepositoryException 
     */
    public static InternalValue create(File value) throws IOException, RepositoryException {
       return new InternalValue(new BLOBFileValue(value));
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(QName value) {
        return new InternalValue(value);
    }

    /**
     * @param values
     * @return
     */
    public static InternalValue[] create(QName[] values) {
        InternalValue[] ret = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = new InternalValue(values[i]);
        }
        return ret;
    }

    /**
     * @param values
     * @return
     */
    public static InternalValue[] create(String[] values) {
        InternalValue[] ret = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = new InternalValue(values[i]);
        }
        return ret;
    }

    /**
     * @param values
     * @return
     */
    public static InternalValue[] create(Calendar[] values) {
        InternalValue[] ret = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = new InternalValue(values[i]);
        }
        return ret;
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(Path value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(UUID value, boolean isWeak) {
        return new InternalValue(value, isWeak);
    }

    //----------------------------------------------------< conversions, etc. >
    /**
     * @param nsResolver
     * @return
     * @throws RepositoryException
     */
    public Value toJCRValue(NamespaceResolver nsResolver)
            throws RepositoryException {
        switch (type) {
            case PropertyType.BINARY:
                return new BinaryValue(((BLOBFileValue) val));
            case PropertyType.BOOLEAN:
                return new BooleanValue(((Boolean) val));
            case PropertyType.DATE:
                return new DateValue((Calendar) val);
            case PropertyType.DOUBLE:
                return new DoubleValue((Double) val);
            case PropertyType.LONG:
                return new LongValue((Long) val);
            case PropertyType.REFERENCE:
                return ReferenceValue.valueOf(((UUID) val).toString(), false);
            case PropertyType283.WEAKREFERENCE:
                return ReferenceValue.valueOf(((UUID) val).toString(), true);
            case PropertyType.PATH:
                try {
                    return PathValue.valueOf(((Path) val).toJCRPath(nsResolver));
                } catch (NoPrefixDeclaredException npde) {
                    // should never get here...
                    throw new RepositoryException("internal error: encountered unregistered namespace", npde);
                }
            case PropertyType.NAME:
                try {
                    return NameValue.valueOf(((QName) val).toJCRName(nsResolver));
                } catch (NoPrefixDeclaredException npde) {
                    // should never get here...
                    throw new RepositoryException("internal error: encountered unregistered namespace", npde);
                }
            case PropertyType.STRING:
                return new StringValue((String) val);
            default:
                throw new RepositoryException("illegal internal value type");
        }
    }

    /**
     * @return
     */
    public Object internalValue() {
        return val;
    }

    /**
     * @return
     */
    public int getType() {
        return type;
    }

    public InternalValue createCopyBinary() throws RepositoryException {
    	BLOBFileValue bfv = (BLOBFileValue)val;
    	bfv = bfv.copy();
    	InternalValue result = new InternalValue(new BLOBFileValue(bfv));
    	
    	result.contentId = bfv.getContentId();
    	result.sqlId = bfv.getSQLId();
    	return result;
	}

    
    /**
     * @return
     * @throws RepositoryException
     */
    public InternalValue createCopy() throws RepositoryException {
        switch (type) {
            case PropertyType.BINARY:
                //try {
                    /*InputStream stream = ((BLOBFileValue) val).getStream();
                    try {
                        return new InternalValue(new BLOBFileValue(stream));
                    } finally {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }*/
                	InternalValue result = new InternalValue(new BLOBFileValue((BLOBFileValue)val));
                	result.contentId = contentId;
                	result.sqlId = sqlId;
                	return result;
                //} catch (IOException ioe) {
                //    throw new RepositoryException("failed to copy binary value", ioe);
                //}
            case PropertyType.BOOLEAN:
                return new InternalValue(((Boolean) val).booleanValue());
            case PropertyType.DATE:
                return new InternalValue((Calendar) val);
            case PropertyType.DOUBLE:
                return new InternalValue(((Double) val).doubleValue());
            case PropertyType.LONG:
                return new InternalValue(((Long) val).longValue());
            case PropertyType.REFERENCE:
                return new InternalValue((UUID) val, false);
            case PropertyType283.WEAKREFERENCE:
                return new InternalValue((UUID) val, true);
            case PropertyType.PATH:
                return new InternalValue((Path) val);
            case PropertyType.NAME:
                return new InternalValue((QName) val);
            case PropertyType.STRING:
                return new InternalValue((String) val);
            default:
                throw new RepositoryException("illegal internal value type");
        }
    }

    /**
     * Returns the string representation of this internal value. If this is a
     * <i>binary</i> value then the path of its backing file will be returned.
     *
     * @return string representation of this internal value
     */
    public String toString() {
        if (type == PropertyType.DATE) {
            return ISO8601.format((Calendar) val);
        } else {
            return val.toString();
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof InternalValue) {
            InternalValue other = (InternalValue) obj;
            return val.equals(other.val);
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

    /**
     * @param s
     * @param type
     * @return
     */
    public static InternalValue valueOf(String s, int type) {
        switch (type) {
            case PropertyType.BOOLEAN:
                return new InternalValue(Boolean.valueOf(s).booleanValue());
            case PropertyType.DATE:
                return new InternalValue(ISO8601.parse(s));
            case PropertyType.DOUBLE:
                return new InternalValue(Double.valueOf(s).doubleValue());
            case PropertyType.LONG:
                return new InternalValue(Long.valueOf(s).longValue());
            case PropertyType.REFERENCE:
                return new InternalValue(new UUID(s), false);
            case PropertyType283.WEAKREFERENCE:
                return new InternalValue(new UUID(s), true);
            case PropertyType.PATH:
                return new InternalValue(Path.valueOf(s));
            case PropertyType.NAME:
                return new InternalValue(QName.valueOf(s));
            case PropertyType.STRING:
                return new InternalValue(s);

            case PropertyType.BINARY:
                throw new IllegalArgumentException("this method does not support the type PropertyType.BINARY");
            default:
                throw new IllegalArgumentException("illegal type");
        }
    }

    //-------------------------------------------------------< implementation >
    private InternalValue(String value) {
        val = value;
        type = PropertyType.STRING;
    }

    private InternalValue(QName value) {
        val = value;
        type = PropertyType.NAME;
    }

    private InternalValue(long value) {
        val = new Long(value);
        type = PropertyType.LONG;
    }

    private InternalValue(double value) {
        val = new Double(value);
        type = PropertyType.DOUBLE;
    }

    private InternalValue(Calendar value) {
        val = value;
        type = PropertyType.DATE;
    }

    private InternalValue(boolean value) {
        val = new Boolean(value);
        type = PropertyType.BOOLEAN;
    }

    private InternalValue(BLOBFileValue value) {
        val = value;
        type = PropertyType.BINARY;
        this.contentId = value.getContentId();
    }

    private InternalValue(Path value) {
        val = value;
        type = PropertyType.PATH;
    }

    private InternalValue(UUID value, boolean isWeak) {
        val = value;
        if (isWeak){
        	type = PropertyType283.WEAKREFERENCE;
        } else {
        	type = PropertyType.REFERENCE;
        }
        
    }

    public void setSQLId(Long id) {
        this.sqlId = id;
        
    }
    public Long getSQLId() {
        return this.sqlId;
        
    }

    public String getContentId() {
        return contentId;
    }
    public void setContentId(String id) {
        this.contentId = id;
        
    }

	

}
