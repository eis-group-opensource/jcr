/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.util.name;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Extracts namespace mapping information from an XML file.
 * XML file is parsed and all startPrefixMapping events
 * are intercepted. Scoping of prefix mapping within the XML file
 * may result in multiple namespace using the same prefix. This
 * is handled by mangling the prefix when required.
 *
 * The resulting NamespaceMapping implements NamespaceResolver
 * and can be used by tools (such as o.a.j.tools.nodetype.CompactNodeTypeDefWriter)
 * to resolve namespaces.
 */
public class NamespaceExtractor {
	private static Log log = LogFactory.getLog(NamespaceExtractor.class);
    private final NamespaceMapping mapping = new NamespaceMapping();
    private final Map basePrefixes = new HashMap();
    private String defaultBasePrefix;

    /**
     * Constructor
     * @param fileName
     * @param dpb
     * @throws NamespaceException
     */
    public NamespaceExtractor(String fileName, String dpb) throws NamespaceException {
        defaultBasePrefix = dpb;
        try{
            ContentHandler handler = new NamespaceHandler();
            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(handler);
            parser.parse(new InputSource(new FileInputStream(fileName)));
        } catch(Exception e){
            throw new NamespaceException();
        }
    }

    /**
     * getNamespaceMapping
     * @return a NamespaceMapping
     */
    public NamespaceMapping getNamespaceMapping(){
        return mapping;
    }

    /**
     * SAX ContentHandler that reacts to namespace mappings in incoming XML.
     */
    private class NamespaceHandler extends DefaultHandler{
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (uri == null) uri = "";

            //Replace the empty prefix with the defaultBasePrefix
            if (prefix == null || prefix.equals("")){
                prefix = defaultBasePrefix;
            }

            try{
                // if prefix already used
                if (mapping.hasPrefix(prefix)){
                    int c;
                    Integer co = (Integer) basePrefixes.get(prefix);
                    if (co == null) {
                        basePrefixes.put(prefix, new Integer(1));
                        c = 1;
                    } else {
                        c = co.intValue() + 1;
                        basePrefixes.put(prefix, new Integer(c));
                    }
                    prefix = prefix + "_" + c;
                }
                mapping.setMapping(prefix, uri);
            } catch(NamespaceException e){
                String msg = e.getMessage();
                log.debug(msg);
            }
        }
    }
}
