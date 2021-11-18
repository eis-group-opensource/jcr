/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.zip.ZipOutputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.value.ValueHelper;

/**
 * A <code>SysViewSAXEventGenerator</code> instance can be used to generate SAX events
 * representing the serialized form of an item in System View XML.
 */
public class SysViewSAXEventGenerator extends AbstractSAXEventGenerator {

    /**
     * The XML elements and attributes used in serialization
     */
    public static final String NODE_ELEMENT = "node";
    public static final String PREFIXED_NODE_ELEMENT =
        QName.NS_SV_PREFIX + ":" + NODE_ELEMENT;

    public static final String PROPERTY_ELEMENT = "property";
    public static final String PREFIXED_PROPERTY_ELEMENT =
        QName.NS_SV_PREFIX + ":" + PROPERTY_ELEMENT;;

    public static final String VALUE_ELEMENT = "value";
    public static final String PREFIXED_VALUE_ELEMENT =
        QName.NS_SV_PREFIX + ":" + VALUE_ELEMENT;;

    public static final String NAME_ATTRIBUTE = "name";
    public static final String PREFIXED_NAME_ATTRIBUTE =
        QName.NS_SV_PREFIX + ":" + NAME_ATTRIBUTE;

    public static final String TYPE_ATTRIBUTE = "type";
    public static final String PREFIXED_TYPE_ATTRIBUTE =
        QName.NS_SV_PREFIX + ":" + TYPE_ATTRIBUTE;

    public static final String CDATA_TYPE = "CDATA";
    public static final String ENUMERATION_TYPE = "ENUMERATION";

    /**
     * Constructor
     *
     * @param node           the node state which should be serialized
     * @param noRecurse      if true, only <code>node</code> and its properties will
     *                       be serialized; otherwise the entire hierarchy starting with
     *                       <code>node</code> will be serialized.
     * @param skipBinary     flag governing whether binary properties are to be serialized.
     * @param zout           out stream for zip binaries, null - if binaries are stored in main XML 
     * @param contentHandler the content handler to feed the SAX events to
     * @param skipSystem 
     * @throws RepositoryException if an error occurs
     */
    public SysViewSAXEventGenerator(Node node, boolean noRecurse,
                                    boolean skipBinary,
                                    ZipOutputStream zout,
                                    ContentHandler contentHandler, boolean skipSystem
                                    , NodeExportAcceptor exportAcceptor)
            throws RepositoryException {
        super(node, noRecurse, skipBinary, zout, contentHandler, skipSystem, exportAcceptor);
    }

    private int counter = 0;
    private long total;
    
    /**
     * {@inheritDoc}
     */
    protected void entering(Node node, int level)
            throws RepositoryException, SAXException {
    	counter++;
    	if (counter > 100){
    		System.out.print(".");
    		counter = 0;
    		total++;
    	}
        AttributesImpl attrs = new AttributesImpl();
        // name attribute
        String nodeName;
        if (node.getDepth() == 0) {
            // root node needs a name
            nodeName = jcrRoot;
        } else {
            // encode node name to make sure it's a valid xml name
            nodeName = node.getName();
        }

        attrs.addAttribute(QName.NS_SV_URI, NAME_ATTRIBUTE, PREFIXED_NAME_ATTRIBUTE,
                CDATA_TYPE, nodeName);
        // start node element
        contentHandler.startElement(QName.NS_SV_URI, NODE_ELEMENT,
                PREFIXED_NODE_ELEMENT, attrs);
    }

    /**
     * {@inheritDoc}
     */
    protected void enteringProperties(Node node, int level)
            throws RepositoryException, SAXException {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    protected void leavingProperties(Node node, int level)
            throws RepositoryException, SAXException {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    protected void leaving(Node node, int level)
            throws RepositoryException, SAXException {
        // end node element
        contentHandler.endElement(QName.NS_SV_URI, NODE_ELEMENT, PREFIXED_NODE_ELEMENT);
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(Property prop, int level)
            throws RepositoryException, SAXException {
        String propName = prop.getName();
        AttributesImpl attrs = new AttributesImpl();
        // name attribute
        attrs.addAttribute(QName.NS_SV_URI, NAME_ATTRIBUTE, PREFIXED_NAME_ATTRIBUTE,
                CDATA_TYPE, propName);
        // type attribute
        int type = prop.getType();
        String typeName;
        try {
            typeName = PropertyType283.nameFromValue(type);
        } catch (IllegalArgumentException iae) {
            // should never be getting here
            throw new RepositoryException("unexpected property-type ordinal: "
                    + type, iae);
        }
        attrs.addAttribute(QName.NS_SV_URI, TYPE_ATTRIBUTE, PREFIXED_TYPE_ATTRIBUTE,
                ENUMERATION_TYPE, typeName);

        // start property element
        contentHandler.startElement(QName.NS_SV_URI, PROPERTY_ELEMENT,
                PREFIXED_PROPERTY_ELEMENT, attrs);

        // values
        if (prop.getType() == PropertyType.BINARY && skipBinary) {
            // empty value element
            contentHandler.startElement(QName.NS_SV_URI, VALUE_ELEMENT,
                    PREFIXED_VALUE_ELEMENT, new AttributesImpl());
            contentHandler.endElement(QName.NS_SV_URI, VALUE_ELEMENT,
                    PREFIXED_VALUE_ELEMENT);
        } else {
            boolean multiValued = prop.getDefinition().isMultiple();
            Value[] vals;
            String path = prop.getPath(); // used for storing path in zip for binaries
            if (multiValued) {
                vals = prop.getValues();
            } else {
                vals = new Value[]{prop.getValue()};
            }
            for (int i = 0; i < vals.length; i++) {
                Value val = vals[i];

                // start value element
                contentHandler.startElement(QName.NS_SV_URI, VALUE_ELEMENT,
                        PREFIXED_VALUE_ELEMENT, new AttributesImpl());

                // characters
                Writer writer = new Writer() {
                    public void close() /*throws IOException*/ {
                    }

                    public void flush() /*throws IOException*/ {
                    }

                    public void write(char[] cbuf, int off, int len) throws IOException {
                        try {
                            contentHandler.characters(cbuf, off, len);
                        } catch (SAXException se) {
                            throw new IOException(se.toString());
                        }
                    }
                };
                try {
                    ValueHelper.serialize(val, false, writer, zout, path+"_"+i);
                    // no need to close our Writer implementation
                    //writer.close();
                } catch (IOException ioe) {
                    // check if the exception wraps a SAXException
                    // (see Writer.write(char[], int, int) above)
                    Throwable t = ioe.getCause();
                    if (t != null && t instanceof SAXException) {
                        throw (SAXException) t;
                    } else {
                        throw new SAXException(ioe);
                    }
                }

                // end value element
                contentHandler.endElement(QName.NS_SV_URI, VALUE_ELEMENT,
                        PREFIXED_VALUE_ELEMENT);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void leaving(Property prop, int level)
            throws RepositoryException, SAXException {
        contentHandler.endElement(QName.NS_SV_URI, PROPERTY_ELEMENT,
                PREFIXED_PROPERTY_ELEMENT);
    }
}
