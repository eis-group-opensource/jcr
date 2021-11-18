/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.core.util;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * Document builder class. This class provides an intuitive
 * interface for incrementally building DOM documents.
 */
public final class DOMBuilder {

    /** Static factory for creating DOM DocumentBuilder instances. */
    private static final DocumentBuilderFactory BUILDER_FACTORY =
        DocumentBuilderFactory.newInstance();

    /** Static factory for creating document to output stream transformers. */
    //private static final TransformerFactory TRANSFORMER_FACTORY =
    //    TransformerFactory.newInstance();

    /** The DOM document being built by this builder. */
    private final Document document;

    /** The current element. */
    private Element current;

	private boolean indenting;

    /**
     * Creates a builder for a new DOM document. A new DOM document is
     * instantiated and initialized to contain a root element with the given
     * name. The root element is set as the current element of this builder.
     *
     * @param name name of the root element
     * @throws ParserConfigurationException if a document cannot be created
     */
    public DOMBuilder(String name, boolean indenting) throws ParserConfigurationException  {
    	this.indenting = indenting;
        DocumentBuilder builder = BUILDER_FACTORY.newDocumentBuilder();
        document = builder.newDocument();
        current = document.createElement(name);
        document.appendChild(current);
    }


    /**
     * Writes the document built by this builder into the given output stream.
     * This method is normally invoked only when the document is fully built.
     *
     * @param xml XML output stream
     * @throws IOException if the document could not be written
     */
    public void write(OutputStream xml) throws IOException {
        //try {
            OutputFormat format = new OutputFormat("xml", "UTF-8", indenting);
            XMLSerializer serializer = new XMLSerializer(xml, format);
            serializer.serialize(document);
            //Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            //transformer.transform(
            //        new DOMSource(document), new StreamResult(xml));
        //} catch (TransformerConfigurationException e) {
        //    throw new IOException(e.getMessage());
        //} catch (TransformerException e) {
        //    throw new IOException(e.getMessage());
        //}
    }

    /**
     * Creates a new element with the given name as the child of the
     * current element and makes the created element current. The
     * {@link #endElement() endElement} method needs to be called
     * to return back to the original element.
     *
     * @param name name of the new element
     */
    public void startElement(String name) {
        Element element = document.createElement(name);
        current.appendChild(element);
        current = element;
    }

    /**
     * Makes the parent element current. This method should be invoked
     * after a child element created with the
     * {@link #startElement(String) startElement} method has been fully
     * built.
     */
    public void endElement() {
        current = (Element) current.getParentNode();
    }

    /**
     * Sets the named attribute of the current element.
     *
     * @param name attribute name
     * @param value attribute value
     */
    public void setAttribute(String name, String value) {
        current.setAttribute(name, value);
    }

    /**
     * Sets the named boolean attribute of the current element.
     *
     * @param name attribute name
     * @param value boolean attribute value
     */
    public void setAttribute(String name, boolean value) {
        setAttribute(name, String.valueOf(value));
    }

    /**
     * Adds the given string as text content to the current element.
     *
     * @param content text content
     */
    public void addContent(String content) {
        current.appendChild(document.createTextNode(content));
    }

    /**
     * Adds a new child element with the given name and text content.
     * The created element will contain no attributes and no child elements
     * of its own.
     *
     * @param name child element name
     * @param content child element content
     */
    public void addContentElement(String name, String content) {
        startElement(name);
        addContent(content);
        endElement();
    }

}
