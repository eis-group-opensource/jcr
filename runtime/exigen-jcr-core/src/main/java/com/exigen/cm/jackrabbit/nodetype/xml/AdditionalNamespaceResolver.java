/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype.xml;

//import org.apache.jackrabbit.name.AbstractNamespaceResolver;

import java.util.Enumeration;
import java.util.Properties;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import com.exigen.cm.jackrabbit.name.AbstractNamespaceResolver;

/**
 * A simple namespace resolver implementation, that uses the additional
 * namespaces declared in an XML element.
 */
public class AdditionalNamespaceResolver extends AbstractNamespaceResolver {

    /** Map from namespace prefixes to namespace URIs. */
    private final Properties prefixToURI = new Properties();

    /** Map from namespace URIs to namespace prefixes. */
    private final Properties uriToPrefix = new Properties();

    /**
     * Creates a namespace resolver using the namespaces defined in
     * the given prefix-to-URI property set.
     *
     * @param namespaces namespace properties
     */
    public AdditionalNamespaceResolver(Properties namespaces) {
        Enumeration prefixes = namespaces.propertyNames();
        while (prefixes.hasMoreElements()) {
            String prefix = (String) prefixes.nextElement();
            addNamespace(prefix, namespaces.getProperty(prefix));
        }
        addNamespace("", "");
    }

    /**
     * Creates a namespace resolver using the namespaces declared
     * in the given namespace registry.
     *
     * @param registry namespace registry
     * @throws RepositoryException on repository errors
     */
    public AdditionalNamespaceResolver(NamespaceRegistry registry)
            throws RepositoryException {
        String[] prefixes = registry.getPrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            addNamespace(prefixes[i], registry.getURI(prefixes[i]));
        }
    }

    /**
     * Adds the given namespace declaration to this resolver.
     *
     * @param prefix namespace prefix
     * @param uri namespace URI
     */
    private void addNamespace(String prefix, String uri) {
        prefixToURI.put(prefix, uri);
        uriToPrefix.put(uri, prefix);
    }

    /** {@inheritDoc} */
    public String getURI(String prefix) throws NamespaceException {
        String uri = prefixToURI.getProperty(prefix);
        if (uri != null) {
            return uri;
        } else {
            throw new NamespaceException(
                    "Unknown namespace prefix " + prefix + ".");
        }
    }

    /** {@inheritDoc} */
    public String getPrefix(String uri) throws NamespaceException {
        String prefix = uriToPrefix.getProperty(uri);
        if (prefix != null) {
            return prefix;
        } else {
            throw new NamespaceException(
                    "Unknown namespace URI " + uri + ".");
        }
    }

    public String getURIById(String prefix) {
        throw new UnsupportedOperationException();
    }

}
