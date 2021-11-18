/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.xml;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.exigen.cm.jackrabbit.BaseException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.util.ISO9075;
import com.exigen.cm.jackrabbit.xml.Importer.TextValue;

/**
 * <code>DocViewImportHandler</code> processes Document View XML SAX events
 * and 'translates' them into <code>{@link Importer}</code> method calls.
 */
class DocViewImportHandler extends TargetImportHandler {

    private static Log log = LogFactory.getLog(DocViewImportHandler.class);

    /**
     * stack of NodeInfo instances; an instance is pushed onto the stack
     * in the startElement method and is popped from the stack in the
     * endElement method.
     */
    private final Stack stack = new Stack();
    // buffer used to merge adjacent character data
    private BufferedStringValue textHandler = new BufferedStringValue();

    /**
     * Constructs a new <code>DocViewImportHandler</code>.
     *
     * @param importer
     * @param nsContext
     */
    DocViewImportHandler(Importer importer, NamespaceResolver nsContext) {
        super(importer, nsContext);
    }

    /**
     * Appends the given character data to the internal buffer.
     *
     * @param ch     the characters to be appended
     * @param start  the index of the first character to append
     * @param length the number of characters to append
     * @throws SAXException if an error occurs
     * @see #characters(char[], int, int)
     * @see #ignorableWhitespace(char[], int, int)
     * @see #processCharacters()
     */
    private void appendCharacters(char[] ch, int start, int length)
            throws SAXException {
        if (textHandler == null) {
            textHandler = new BufferedStringValue();
        }
        try {
            textHandler.append(ch, start, length);
        } catch (IOException ioe) {
            String msg = "internal error while processing internal buffer data";
            log.error(msg, ioe);
            throw new SAXException(msg, ioe);
        }
    }

    /**
     * Translates character data reported by the
     * <code>{@link #characters(char[], int, int)}</code> &
     * <code>{@link #ignorableWhitespace(char[], int, int)}</code> SAX events
     * into a  <code>jcr:xmltext</code> child node with one
     * <code>jcr:xmlcharacters</code> property.
     *
     * @throws SAXException if an error occurs
     * @see #appendCharacters(char[], int, int)
     */
    private void processCharacters()
            throws SAXException {
        try {
            if (textHandler != null && textHandler.length() > 0) {
                // there is character data that needs to be added to
                // the current node

                // check for pure whitespace character data
                Reader reader = textHandler.reader();
                try {
                    int ch;
                    while ((ch = reader.read()) != -1) {
                        if (ch > 0x20) {
                            break;
                        }
                    }
                    if (ch == -1) {
                        // the character data consists of pure whitespace, ignore
                        log.debug("ignoring pure whitespace character data...");
                        // reset handler
                        textHandler.dispose();
                        textHandler = null;
                        return;
                    }
                } finally {
                    reader.close();
                }

                Importer.NodeInfo node =
                        new Importer.NodeInfo(QName.JCR_XMLTEXT, null, null, null);
                Importer.TextValue[] values =
                        new Importer.TextValue[]{textHandler};
                ArrayList props = new ArrayList();

                TextValue[] values2 = values;
                if (values.length == 0){
                	values2 = new TextValue[]{new StringValue("")};
                }
                Importer.PropInfo prop =
                        new Importer.PropInfo(QName.JCR_XMLCHARACTERS,
                                PropertyType.STRING, values, values2);
                

                props.add(prop);
                // call Importer
                importer.startNode(node, props, nsContext);
                importer.endNode(node);

                // reset handler
                textHandler.dispose();
                textHandler = null;
            }
        } catch (IOException ioe) {
            String msg = "internal error while processing internal buffer data";
            log.error(msg, ioe);
            throw new SAXException(msg, ioe);
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    //-------------------------------------------------------< ContentHandler >
    /**
     * {@inheritDoc}
     */
    public void startDocument() throws SAXException {
        try {
            importer.start();
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts)
            throws SAXException {
        // process buffered character data
        processCharacters();

        try {
            QName nodeName = new QName(namespaceURI, localName);
            // decode node name
            nodeName = ISO9075.decode(nodeName);

            // properties

            String uuid = null;
            QName nodeTypeName = null;
            QName[] mixinTypes = null;

            ArrayList props = new ArrayList(atts.getLength());
            for (int i = 0; i < atts.getLength(); i++) {
                QName propName = new QName(atts.getURI(i), atts.getLocalName(i));
                // decode property name
                propName = ISO9075.decode(propName);

                // value(s)
                String attrValue = atts.getValue(i);
                Importer.TextValue[] propValues;
/*
                // @todo should attribute value be interpreted as LIST type (i.e. multi-valued property)?
                String[] strings = Text.explode(attrValue, ' ', true);
                propValues = new Value[strings.length];
                for (int j = 0; j < strings.length; j++) {
                    // decode encoded blanks in value
                    strings[j] = Text.replace(strings[j], "_x0020_", " ");
                    propValues[j] = InternalValue.create(strings[j]);
                }
*/
                if (propName.equals(QName.JCR_PRIMARYTYPE)) {
                    // jcr:primaryType
                    if (attrValue.length() > 0) {
                        try {
                            nodeTypeName = QName.fromJCRName(attrValue, nsContext);
                        } catch (BaseException be) {
                            throw new SAXException("illegal jcr:primaryType value: "
                                    + attrValue, be);
                        }
                    }
                } else if (propName.equals(QName.JCR_MIXINTYPES)) {
                    // jcr:mixinTypes
                    if (attrValue.length() > 0) {
                        try {
                            mixinTypes =
                                    new QName[]{QName.fromJCRName(attrValue, nsContext)};
                        } catch (BaseException be) {
                            throw new SAXException("illegal jcr:mixinTypes value: "
                                    + attrValue, be);
                        }
                    }
                } else if (propName.equals(QName.JCR_UUID)) {
                    // jcr:uuid
                    if (attrValue.length() > 0) {
                        uuid = attrValue;
                    }
                } else {
                	StringTokenizer st = new StringTokenizer(atts.getValue(i)," ");
                	ArrayList<String> vv = new ArrayList<String>();
                	while (st.hasMoreElements()){
                		vv.add(st.nextToken());
                	}
                    propValues = new Importer.TextValue[vv.size()];
                    int iPos = 0 ;
                    for(String v:vv){
                    	propValues[iPos++] = new StringValue(v);
                    }
                    TextValue[] values2 = propValues;
                    if (propValues.length == 0){
                    	values2 = new TextValue[]{new StringValue("")};
                    }

                    props.add(new Importer.PropInfo(propName,
                            PropertyType.UNDEFINED, propValues, values2));
                }
            }

            Importer.NodeInfo node =
                    new Importer.NodeInfo(nodeName, nodeTypeName, mixinTypes, uuid);
            // all information has been collected, now delegate to importer
            importer.startNode(node, props, nsContext);
            // push current node data onto stack
            stack.push(node);
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        /**
         * buffer data reported by the characters event;
         * will be processed on the next endElement or startElement event.
         */
        appendCharacters(ch, start, length);
    }

    /**
     * {@inheritDoc}
     */
    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
        /**
         * buffer data reported by the ignorableWhitespace event;
         * will be processed on the next endElement or startElement event.
         */
        appendCharacters(ch, start, length);
    }

    /**
     * {@inheritDoc}
     */
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        // process buffered character data
        processCharacters();

        Importer.NodeInfo node = (Importer.NodeInfo) stack.peek();
        try {
            // call Importer
            importer.endNode(node);
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
        // we're done with this node, pop it from stack
        stack.pop();
    }

    /**
     * {@inheritDoc}
     */
    public void endDocument() throws SAXException {
        try {
            importer.end();
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }
}
