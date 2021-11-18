/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.xml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipOutputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.exigen.cm.jackrabbit.BaseException;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.util.ISO9075;
import com.exigen.cm.jackrabbit.value.ValueHelper;

/**
 * A <code>DocViewSAXEventGenerator</code> instance can be used to generate
 * SAX events representing the serialized form of an item in Document View XML.
 */
public class DocViewSAXEventGenerator extends AbstractSAXEventGenerator {

    private static Log log = LogFactory.getLog(DocViewSAXEventGenerator.class);

    public static final String CDATA_TYPE = "CDATA";

    // used to temporarily store properties of a node
    private final List props;

    /**
     * Constructor
     *
     * @param node           the node state which should be serialized
     * @param noRecurse      if true, only <code>node</code> and its properties
     *                       will be serialized; otherwise the entire hierarchy
     *                       starting with <code>node</code> will be serialized.
     * @param skipBinary     flag governing whether binary properties are to be
     *                       serialized.
     * @param zout           if binary properties should be serialized into zip,
     *                       should provide ZipOuputStream for it, null otherwise 
     * @param contentHandler the content handler to feed the SAX events to
     * @throws RepositoryException if an error occurs
     */
    public DocViewSAXEventGenerator(Node node, boolean noRecurse,
                                    boolean skipBinary,
                                    ZipOutputStream zout,
                                    ContentHandler contentHandler, boolean skipSystem, NodeExportAcceptor exportAcceptor)
            throws RepositoryException {
        super(node, noRecurse, skipBinary, zout, contentHandler, skipSystem, exportAcceptor);

        props = new ArrayList();
    }

    private QName getQName(String rawName) throws RepositoryException {
        try {
            return QName.fromJCRName(rawName, nsResolver);
        } catch (BaseException e) {
            // should never get here...
            String msg = "internal error: failed to resolve namespace mappings";
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(Node node, int level)
            throws RepositoryException, SAXException {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    protected void enteringProperties(Node node, int level)
            throws RepositoryException, SAXException {
        // reset list of properties
        props.clear();
    }

    /**
     * {@inheritDoc}
     */
    protected void leavingProperties(Node node, int level)
            throws RepositoryException, SAXException {
        String name = node.getName();
        if (name.equals(jcrXMLText)) {
            // the node represents xml character data
            Iterator iter = props.iterator();
            while (iter.hasNext()) {
                Property prop = (Property) iter.next();
                String propName = prop.getName();
                if (propName.equals(jcrXMLCharacters)) {
                    // assume jcr:xmlcharacters is single-valued
                    char[] chars = prop.getValue().getString().toCharArray();
                    contentHandler.characters(chars, 0, chars.length);
                }
            }
        } else {
            // regular node

            // element name
            String elemName;
            if (node.getDepth() == 0) {
                // root node needs a name
                elemName = jcrRoot;
            } else {
                // encode node name to make sure it's a valid xml name
                elemName = ISO9075.encode(name);
            }

            // attributes (properties)
            AttributesImpl attrs = new AttributesImpl();
            Iterator iter = props.iterator();
            while (iter.hasNext()) {
                Property prop = (Property) iter.next();
                String propName = prop.getName();
                // attribute name (encode property name to make sure it's a valid xml name)
                String attrName = ISO9075.encode(propName);
                QName qName = getQName(attrName);

                // attribute value
                if (prop.getType() == PropertyType.BINARY && skipBinary) {
                    // add empty attribute
                    attrs.addAttribute(qName.getNamespaceURI(),
                            qName.getLocalName(), attrName, CDATA_TYPE, "");
                } else {
                        StringBuffer attrValue = new StringBuffer();
                        // process property value(s)
                        boolean multiValued = prop.getDefinition().isMultiple();
                        Value[] vals;
                        String path = prop.getPath();
                        if (multiValued) {
                            vals = prop.getValues();
                        } else {
                            vals = new Value[]{prop.getValue()};
                        }
                        for (int i = 0; i < vals.length; i++) {
                            if (i > 0) {
                                // use space as delimiter for multi-valued properties
                                attrValue.append(" ");
                            }
                            attrValue.append(ValueHelper.serialize(vals[i], true, zout, path+"_"+i));
                        }
                        attrs.addAttribute(qName.getNamespaceURI(),
                                qName.getLocalName(), attrName, CDATA_TYPE,
                                attrValue.toString());
                    }
                
            }
            // start element (node)
            QName qName = getQName(elemName);
            contentHandler.startElement(qName.getNamespaceURI(),
                    qName.getLocalName(), elemName, attrs);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void leaving(Node node, int level)
            throws RepositoryException, SAXException {
        String name = node.getName();
        if (name.equals(jcrXMLText)) {
            // the node represents xml character data
            // (already processed in leavingProperties(NodeImpl, int)
            return;
        }
        // encode node name to make sure it's a valid xml name
        name = ISO9075.encode(name);
        // element name
        String elemName;
        if (node.getDepth() == 0) {
            // root node needs a name
            elemName = jcrRoot;
        } else {
            // encode node name to make sure it's a valid xml name
            elemName = ISO9075.encode(name);
        }

        // end element (node)
        QName qName = getQName(elemName);
        contentHandler.endElement(qName.getNamespaceURI(), qName.getLocalName(),
                elemName);
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(Property prop, int level)
            throws RepositoryException, SAXException {
        props.add(prop);
    }

    /**
     * {@inheritDoc}
     */
    protected void leaving(Property prop, int level)
            throws RepositoryException, SAXException {
        // nop
    }
}
