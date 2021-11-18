/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.value;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

import com.exigen.cm.jackrabbit.util.Base64;
import com.exigen.cm.jackrabbit.util.Text;
import com.exigen.cm.jackrabbit.util.TransientFileFactory;

/**
 * The <code>ValueHelper</code> class provides several <code>Value</code>
 * related utility methods.
 */
public class _ValueHelper {

    /**
     * empty private constructor
     */
    private _ValueHelper() {
    }

    /**
     * Same as {@link #convert(String, int, ValueFactory)} using
     * <code>ValueFactoryImpl</code>.
     *
     * @param srcValue
     * @param targetType
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     * @deprecated Use {@link #convert(String, int, ValueFactory)} instead.
     * @see #convert(Value, int, ValueFactory)
     */
    public static Value convert(String srcValue, int targetType)
            throws ValueFormatException, IllegalArgumentException {
        return convert(srcValue, targetType, ValueFactoryImpl.getInstance());
    }

    /**
     * @param srcValue
     * @param targetType
     * @param factory
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     * @see #convert(Value, int, ValueFactory)
     */
    public static Value convert(String srcValue, int targetType, ValueFactory factory)
        throws ValueFormatException, IllegalArgumentException {
        if (srcValue == null) {
            return null;
        } else {
            return factory.createValue(srcValue, targetType);
        }
    }

    /**
     * Same as {@link #convert(InputStream, int, ValueFactory)} using
     * <code>ValueFactoryImpl</code>.
     *
     * @param srcValue
     * @param targetType
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     * @deprecated Use {@link #convert(InputStream, int, ValueFactory)} instead.
     */
    public static Value convert(InputStream srcValue, int targetType)
            throws ValueFormatException, IllegalArgumentException {
        return convert(srcValue, targetType, ValueFactoryImpl.getInstance());
    }

    /**
     * @param srcValue
     * @param targetType
     * @param factory
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     */
    public static Value convert(InputStream srcValue, int targetType, ValueFactory factory)
        throws ValueFormatException, IllegalArgumentException {
        if (srcValue == null) {
            return null;
        } else {
            return convert(factory.createValue(srcValue), targetType, factory);
        }
    }

    /**
     * Same as {@link #convert(String[], int, ValueFactory)} using
     * <code>ValueFactoryImpl</code>.
     *
     * @param srcValues
     * @param targetType
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     * @deprecated Use {@link #convert(String[], int, ValueFactory)} instead.
     * @see #convert(Value, int, ValueFactory)
     */
    public static Value[] convert(String[] srcValues, int targetType)
            throws ValueFormatException, IllegalArgumentException {
        return convert(srcValues, targetType, ValueFactoryImpl.getInstance());
    }

    /**
     * Same as {@link #convert(String[], int, ValueFactory)} using
     * <code>ValueFactoryImpl</code>.
     *
     * @param srcValues
     * @param targetType
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     * @see #convert(Value, int, ValueFactory)
     */
    public static Value[] convert(String[] srcValues, int targetType, ValueFactory factory)
            throws ValueFormatException, IllegalArgumentException {
        if (srcValues == null) {
            return null;
        }
        Value[] newValues = new Value[srcValues.length];
        for (int i = 0; i < srcValues.length; i++) {
            newValues[i] = convert(srcValues[i], targetType, factory);
        }
        return newValues;
    }

    /**
     * @param srcValues
     * @param targetType
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     * @see #convert(Value, int, ValueFactory)
     */
    public static Value[] convert(InputStream[] srcValues, int targetType,
                                  ValueFactory factory)
            throws ValueFormatException, IllegalArgumentException {
        if (srcValues == null) {
            return null;
        }
        Value[] newValues = new Value[srcValues.length];
        for (int i = 0; i < srcValues.length; i++) {
            newValues[i] = convert(srcValues[i], targetType, factory);
        }
        return newValues;
    }

    /**
     * Same as {@link #convert(Value[], int, ValueFactory)} using
     * <code>ValueFactoryImpl</code>.
     *
     * @param srcValues
     * @param targetType
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     * @deprecated Use {@link #convert(Value[], int, ValueFactory)} instead.
     * @see #convert(Value, int, ValueFactory)
     */
    public static Value[] convert(Value[] srcValues, int targetType)
        throws ValueFormatException, IllegalArgumentException {
        return convert(srcValues, targetType, ValueFactoryImpl.getInstance());
    }

    /**
     * @param srcValues
     * @param targetType
     * @param factory
     * @throws ValueFormatException
     * @throws IllegalArgumentException
     * @see #convert(Value, int, ValueFactory)
     */
    public static Value[] convert(Value[] srcValues, int targetType,
                                  ValueFactory factory)
            throws ValueFormatException, IllegalArgumentException {
        if (srcValues == null) {
            return null;
        }

        Value[] newValues = new Value[srcValues.length];
        int srcValueType = PropertyType.UNDEFINED;
        for (int i = 0; i < srcValues.length; i++) {
            if (srcValues[i] == null) {
                newValues[i] = null;
                continue;
            }
            // check type of values
            if (srcValueType == PropertyType.UNDEFINED) {
                srcValueType = srcValues[i].getType();
            } else if (srcValueType != srcValues[i].getType()) {
                // inhomogeneous types
                String msg = "inhomogeneous type of values";
                throw new ValueFormatException(msg);
            }

            newValues[i] = convert(srcValues[i], targetType, factory);
        }
        return newValues;
    }

    /**
     * Same as {@link #convert(Value, int, ValueFactory)} using
     * <code>ValueFactoryImpl</code>.

     * @param srcValue
     * @param targetType
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @deprecated Use {@link #convert(Value, int, ValueFactory)} instead.
     * @see #convert(Value, int, ValueFactory)
     */
    public static Value convert(Value srcValue, int targetType)
        throws ValueFormatException, IllegalStateException,
        IllegalArgumentException {
        return convert(srcValue, targetType, ValueFactoryImpl.getInstance());
    }

    /**
     * Converts the given value to a value of the specified target type.
     * The conversion is performed according to the rules described in
     * "6.2.6 Property Type Conversion" in the JSR 170 specification.
     *
     * @param srcValue
     * @param targetType
     * @param factory
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public static Value convert(Value srcValue, int targetType, ValueFactory factory)
            throws ValueFormatException, IllegalStateException,
            IllegalArgumentException {
        if (srcValue == null) {
            return null;
        }

        Value val;
        int srcType = srcValue.getType();

        if (srcType == targetType) {
            // no conversion needed, return original value
            return srcValue;
        }

        switch (targetType) {
            case PropertyType.STRING:
                // convert to STRING
                try {
                    val = factory.createValue(srcValue.getString());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.BINARY:
                // convert to BINARY
                try {
                    val = factory.createValue(srcValue.getStream());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.BOOLEAN:
                // convert to BOOLEAN
                try {
                    val = factory.createValue(srcValue.getBoolean());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.DATE:
                // convert to DATE
                try {
                    val = factory.createValue(srcValue.getDate());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.DOUBLE:
                // convert to DOUBLE
                try {
                    val = factory.createValue(srcValue.getDouble());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.LONG:
                // convert to LONG
                try {
                    val = factory.createValue(srcValue.getLong());
                } catch (RepositoryException re) {
                    throw new ValueFormatException("conversion failed: "
                            + PropertyType.nameFromValue(srcType) + " to "
                            + PropertyType.nameFromValue(targetType), re);
                }
                break;

            case PropertyType.PATH:
                // convert to PATH
                switch (srcType) {
                    case PropertyType.PATH:
                        // no conversion needed, return original value
                        // (redundant code, just here for the sake of clarity)
                        return srcValue;

                    case PropertyType.BINARY:
                    case PropertyType.STRING:
                    case PropertyType.NAME: // a name is always also a relative path
                        // try conversion via string
                        String path;
                        try {
                            // get string value
                            path = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to PATH value",
                                    re);
                        }
                        val = factory.createValue(path, targetType);
                        break;

                    case PropertyType.BOOLEAN:
                    case PropertyType.DATE:
                    case PropertyType.DOUBLE:
                    case PropertyType.LONG:
                    case PropertyType.REFERENCE:
                        throw new ValueFormatException("conversion failed: "
                                + PropertyType.nameFromValue(srcType) + " to "
                                + PropertyType.nameFromValue(targetType));
                    case PropertyType283.WEAKREFERENCE:
                        throw new ValueFormatException("conversion failed: "
                                + PropertyType.nameFromValue(srcType) + " to "
                                + PropertyType.nameFromValue(targetType));

                    default:
                        throw new IllegalArgumentException("not a valid type constant: " + srcType);
                }
                break;

            case PropertyType.NAME:
                // convert to NAME
                switch (srcType) {
                    case PropertyType.NAME:
                        // no conversion needed, return original value
                        // (redundant code, just here for the sake of clarity)
                        return srcValue;

                    case PropertyType.BINARY:
                    case PropertyType.STRING:
                    case PropertyType.PATH: // path might be a name (relative path of length 1)
                        // try conversion via string
                        String name;
                        try {
                            // get string value
                            name = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to NAME value",
                                    re);
                        }
                        val = factory.createValue(name, targetType);
                        break;

                    case PropertyType.BOOLEAN:
                    case PropertyType.DATE:
                    case PropertyType.DOUBLE:
                    case PropertyType.LONG:
                    case PropertyType.REFERENCE:
                        throw new ValueFormatException("conversion failed: "
                                + PropertyType.nameFromValue(srcType) + " to "
                                + PropertyType.nameFromValue(targetType));
                    case PropertyType283.WEAKREFERENCE:
                        throw new ValueFormatException("conversion failed: "
                                + PropertyType.nameFromValue(srcType) + " to "
                                + PropertyType.nameFromValue(targetType));

                    default:
                        throw new IllegalArgumentException("not a valid type constant: " + srcType);
                }
                break;

            case PropertyType.REFERENCE:
                // convert to REFERENCE
                switch (srcType) {
	                case PropertyType.REFERENCE:
	                    // no conversion needed, return original value
	                    // (redundant code, just here for the sake of clarity)
	                    return srcValue;
	                case PropertyType283.WEAKREFERENCE:
	                    // no conversion needed, return original value
	                    // (redundant code, just here for the sake of clarity)
	                    return srcValue;

                    case PropertyType.BINARY:
                    case PropertyType.STRING:
                        // try conversion via string
                        String uuid;
                        try {
                            // get string value
                            uuid = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to REFERENCE value", re);
                        }
                        val = factory.createValue(uuid, targetType);
                        break;

                    case PropertyType.BOOLEAN:
                    case PropertyType.DATE:
                    case PropertyType.DOUBLE:
                    case PropertyType.LONG:
                    case PropertyType.PATH:
                    case PropertyType.NAME:
                        throw new ValueFormatException("conversion failed: "
                                + PropertyType.nameFromValue(srcType) + " to "
                                + PropertyType.nameFromValue(targetType));

                    default:
                        throw new IllegalArgumentException("not a valid type constant: " + srcType);
                }
                break;

            case PropertyType283.WEAKREFERENCE:
                // convert to REFERENCE
                switch (srcType) {
	                case PropertyType.REFERENCE:
	                    // no conversion needed, return original value
	                    // (redundant code, just here for the sake of clarity)
	                    return srcValue;
	                case PropertyType283.WEAKREFERENCE:
	                    // no conversion needed, return original value
	                    // (redundant code, just here for the sake of clarity)
	                    return srcValue;

                    case PropertyType.BINARY:
                    case PropertyType.STRING:
                        // try conversion via string
                        String uuid;
                        try {
                            // get string value
                            uuid = srcValue.getString();
                        } catch (RepositoryException re) {
                            // should never happen
                            throw new ValueFormatException("failed to convert source value to REFERENCE value", re);
                        }
                        val = factory.createValue(uuid, targetType);
                        break;

                    case PropertyType.BOOLEAN:
                    case PropertyType.DATE:
                    case PropertyType.DOUBLE:
                    case PropertyType.LONG:
                    case PropertyType.PATH:
                    case PropertyType.NAME:
                        throw new ValueFormatException("conversion failed: "
                                + PropertyType.nameFromValue(srcType) + " to "
                                + PropertyType.nameFromValue(targetType));

                    default:
                        throw new IllegalArgumentException("not a valid type constant: " + srcType);
                }
                break;
      
            default:
                throw new IllegalArgumentException("not a valid type constant: " + targetType);
        }

        return val;
    }

    /**
     * Same as {@link #copy(Value, ValueFactory)} using <code>ValueFactoryImpl</code>.
     *
     * @param srcValue
     * @throws IllegalStateException
     * @deprecated Use {@link #copy(Value, ValueFactory)} instead.
     */
    public static Value copy(Value srcValue) throws IllegalStateException {
        return copy(srcValue, ValueFactoryImpl.getInstance());
    }

    /**
     *
     * @param srcValue
     * @param factory
     * @throws IllegalStateException
     */
    public static Value copy(Value srcValue, ValueFactory factory)
        throws IllegalStateException {
        if (srcValue == null) {
            return null;
        }

        Value newVal = null;
        try {
            switch (srcValue.getType()) {
                case PropertyType.BINARY:
                    newVal = factory.createValue(srcValue.getStream());
                    break;

                case PropertyType.BOOLEAN:
                    newVal = factory.createValue(srcValue.getBoolean());
                    break;

                case PropertyType.DATE:
                    newVal = factory.createValue(srcValue.getDate());
                    break;

                case PropertyType.DOUBLE:
                    newVal = factory.createValue(srcValue.getDouble());
                    break;

                case PropertyType.LONG:
                    newVal = factory.createValue(srcValue.getLong());
                    break;

                case PropertyType.PATH:
                case PropertyType.NAME:
                case PropertyType.REFERENCE:
                    newVal = factory.createValue(srcValue.getString(), srcValue.getType());
                    break;
                case PropertyType283.WEAKREFERENCE:
                    newVal = factory.createValue(srcValue.getString(), srcValue.getType());
                    break;

                case PropertyType.STRING:
                    newVal = factory.createValue(srcValue.getString());
                    break;
            }
        } catch (RepositoryException re) {
            // should never get here
        }
        return newVal;
    }

    /**
     * Same as {@link #copy(Value[], ValueFactory)} using <code>ValueFactoryImpl</code>.
     *
     * @param srcValues
     * @throws IllegalStateException
     * @deprecated Use {@link #copy(Value[], ValueFactory)} instead.
     */
    public static Value[] copy(Value[] srcValues) throws IllegalStateException {
        return copy(srcValues, ValueFactoryImpl.getInstance());
    }

    /**
     * @param srcValues
     * @param factory
     * @throws IllegalStateException
     */
    public static Value[] copy(Value[] srcValues, ValueFactory factory)
        throws IllegalStateException {
        if (srcValues == null) {
            return null;
        }

        Value[] newValues = new Value[srcValues.length];
        for (int i = 0; i < srcValues.length; i++) {
            newValues[i] = copy(srcValues[i], factory);
        }
        return newValues;
    }

    /**
     * Serializes the given value to a <code>String</code>. The serialization
     * format is the same as used by Document & System View XML, i.e.
     * binary values will be Base64-encoded whereas for all others
     * <code>{@link Value#getString()}</code> will be used.
     *
     * @param value        the value to be serialized
     * @param encodeBlanks if <code>true</code> space characters will be encoded
     *                     as <code>"_x0020_"</code> within he output string.
     * @return a string representation of the given value.
     * @throws IllegalStateException if the given value is in an illegal state
     * @throws RepositoryException   if an error occured during the serialization.
     */
    public static String serialize(Value value, boolean encodeBlanks)
            throws IllegalStateException, RepositoryException {
        StringWriter writer = new StringWriter();
        try {
            serialize(value, encodeBlanks, writer);
        } catch (IOException ioe) {
            throw new RepositoryException("failed to serialize value",
                    ioe);
        }
        return writer.toString();
    }

    /**
     * Outputs the serialized value to a <code>Writer</code>. The serialization
     * format is the same as used by Document & System View XML, i.e.
     * binary values will be Base64-encoded whereas for all others
     * <code>{@link Value#getString()}</code> will be used for serialization.
     *
     * @param value        the value to be serialized
     * @param encodeBlanks if <code>true</code> space characters will be encoded
     *                     as <code>"_x0020_"</code> within he output string.
     * @param writer       writer to output the encoded data
     * @throws IllegalStateException if the given value is in an illegal state
     * @throws IOException           if an i/o error occured during the
     *                               serialization
     * @throws RepositoryException   if an error occured during the serialization.
     */
    public static void serialize(Value value, boolean encodeBlanks,
                                 Writer writer)
            throws IllegalStateException, IOException, RepositoryException {
        if (value.getType() == PropertyType.BINARY) {
            // binary data, base64 encoding required;
            // the encodeBlanks flag can be ignored since base64-encoded
            // data cannot contain space characters
            InputStream in = value.getStream();
            try {
                Base64.encode(in, writer);
                // no need to close StringWriter
                //writer.close();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        } else {
            String textVal = value.getString();
            if (encodeBlanks) {
                // enocde blanks in string
                textVal = Text.replace(textVal, " ", "_x0020_");
            }
            writer.write(textVal);
        }
    }

    /**
     * Deserializes the given string to a <code>Value</code> of the given type.
     * Same as {@link #deserialize(String, int, boolean, ValueFactory)} using
     * <code>ValueFactoryImpl</code>.
     *
     * @param value        string to be deserialized
     * @param type         type of value
     * @param decodeBlanks if <code>true</code> <code>"_x0020_"</code>
     *                     character sequences will be decoded to single space
     *                     characters each.
     * @return the deserialized <code>Value</code>
     * @throws ValueFormatException if the string data is not of the required
     *                              format
     * @throws RepositoryException  if an error occured during the
     *                              deserialization.
     * @deprecated Use {@link #deserialize(String, int, boolean, ValueFactory)}
     * instead.
     */
    public static Value deserialize(String value, int type,
                                    boolean decodeBlanks)
            throws ValueFormatException, RepositoryException {
        return deserialize(value, type, decodeBlanks, ValueFactoryImpl.getInstance());
    }

    /**
     * Deserializes the given string to a <code>Value</code> of the given type.
     *
     * @param value        string to be deserialized
     * @param type         type of value
     * @param decodeBlanks if <code>true</code> <code>"_x0020_"</code>
     *                     character sequences will be decoded to single space
     *                     characters each.
     * @param factory      ValueFactory used to build the <code>Value</code> object.
     * @return the deserialized <code>Value</code>
     * @throws ValueFormatException if the string data is not of the required
     *                              format
     * @throws RepositoryException  if an error occured during the
     *                              deserialization.
     */
    public static Value deserialize(String value, int type, boolean decodeBlanks,
                                    ValueFactory factory)
            throws ValueFormatException, RepositoryException {
        if (type == PropertyType.BINARY) {
            // base64 encoded binary value;
            // the encodeBlanks flag can be ignored since base64-encoded
            // data cannot contain encoded space characters
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                Base64.decode(value, baos);
                // no need to close ByteArrayOutputStream
                //baos.close();
            } catch (IOException ioe) {
                throw new RepositoryException("failed to decode binary value",
                        ioe);
            }
            // NOTE: for performance reasons the BinaryValue is created directly
            // from the byte-array. This is inconsistent with the other calls,
            // that delegate the value creation to the ValueFactory.
            return new BinaryValue(baos.toByteArray());
        } else {
            if (decodeBlanks) {
                // decode encoded blanks in value
                value = Text.replace(value, "_x0020_", " ");
            }
            return convert(value, type, factory);
        }
    }

    /**
     * Deserializes the string data read from the given reader to a
     * <code>Value</code> of the given type. Same as
     * {@link #deserialize(Reader, int, boolean, ValueFactory)} using
     * <code>ValueFactoryImpl</code>.
     *
     * @param reader       reader for the string data to be deserialized
     * @param type         type of value
     * @param decodeBlanks if <code>true</code> <code>"_x0020_"</code>
     *                     character sequences will be decoded to single space
     *                     characters each.
     * @return the deserialized <code>Value</code>
     * @throws IOException          if an i/o error occured during the
     *                              serialization
     * @throws ValueFormatException if the string data is not of the required
     *                              format
     * @throws RepositoryException  if an error occured during the
     *                              deserialization.
     * @deprecated Use {@link #deserialize(Reader, int, boolean, ValueFactory)}
     * instead.
     */
    public static Value deserialize(Reader reader, int type,
                                    boolean decodeBlanks)
            throws IOException, ValueFormatException, RepositoryException {
        return deserialize(reader, type, decodeBlanks, ValueFactoryImpl.getInstance());
    }

    /**
     * Deserializes the string data read from the given reader to a
     * <code>Value</code> of the given type.
     *
     * @param reader       reader for the string data to be deserialized
     * @param type         type of value
     * @param decodeBlanks if <code>true</code> <code>"_x0020_"</code>
     *                     character sequences will be decoded to single space
     *                     characters each.
     * @param factory      ValueFactory used to build the <code>Value</code> object.
     * @return the deserialized <code>Value</code>
     * @throws IOException          if an i/o error occured during the
     *                              serialization
     * @throws ValueFormatException if the string data is not of the required
     *                              format
     * @throws RepositoryException  if an error occured during the
     *                              deserialization.
     */
    public static Value deserialize(Reader reader, int type,
                                    boolean decodeBlanks, ValueFactory factory)
            throws IOException, ValueFormatException, RepositoryException {
        if (type == PropertyType.BINARY) {
            // base64 encoded binary value;
            // the encodeBlanks flag can be ignored since base64-encoded
            // data cannot contain encoded space characters

            // decode to temp file
            TransientFileFactory fileFactory = TransientFileFactory.getInstance();
            final File tmpFile = fileFactory.createTransientFile("bin", null, null);
            FileOutputStream out = new FileOutputStream(tmpFile);
            try {
                Base64.decode(reader, out);
            } finally {
                out.close();
            }

            // create an InputStream that keeps a hard reference to the temp file
            // in order to prevent its automatic deletion once the associated
            // File object is reclaimed by the garbage collector;
            // pass InputStream wrapper to ValueFactory, that creates a BinaryValue.
            return factory.createValue(new FilterInputStream(new FileInputStream(tmpFile)) {

                public void close() throws IOException {
                    in.close();
                    // temp file can now safely be removed
                    tmpFile.delete();
                }
            });
/*
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Base64.decode(reader, baos);
            // no need to close ByteArrayOutputStream
            //baos.close();
            return new BinaryValue(baos.toByteArray());
*/
        } else {
            char[] chunk = new char[8192];
            int read;
            StringBuffer buf = new StringBuffer();
            while ((read = reader.read(chunk)) > -1) {
                buf.append(chunk, 0, read);
            }
            String value = buf.toString();
            if (decodeBlanks) {
                // decode encoded blanks in value
                value = Text.replace(value, "_x0020_", " ");
            }
            return convert(value, type, factory);
        }
    }
}
