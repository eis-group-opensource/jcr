/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.name;

import javax.jcr.NamespaceException;

/**
 * Interface for resolving namespace URIs and prefixes. Unlike the JCR
 * {@link javax.jcr.NamespaceRegistry} interface, this interface contains
 * no functionality other than the basic namespace URI and prefix resolution
 * methods. This interface is therefore used internally in many places where
 * the full namespace registry is either not available or some other mechanism
 * is used for resolving namespaces.
 */
public interface NamespaceResolver {

    /**
     * Returns the URI to which the given prefix is mapped.
     *
     * @param prefix namespace prefix
     * @return the namespace URI to which the given prefix is mapped.
     * @throws NamespaceException if the prefix is unknown.
     */
    String getURI(String prefix) throws NamespaceException;

    /**
     * Returns the prefix which is mapped to the given URI.
     *
     * @param uri namespace URI
     * @return the prefix mapped to the given URI.
     * @throws NamespaceException if the URI is unknown.
     */
    String getPrefix(String uri) throws NamespaceException;

    /**
     * Parses the given prefixed JCR name into a qualified name.
     *
     * @param name the raw name, potentially prefixed.
     * @return the QName instance for the raw name.
     * @throws IllegalNameException   if the given name is not a valid JCR name
     * @throws UnknownPrefixException if the JCR name prefix does not resolve
     */
    public QName getQName(String name)
            throws IllegalNameException, UnknownPrefixException;

    /**
     * Returns the qualified name in the prefixed JCR name format.
     *
     * @return name the qualified name
     * @throws NoPrefixDeclaredException if the namespace can not be resolved
     */
    public String getJCRName(QName name) throws NoPrefixDeclaredException;

    public String getURIById(String prefix);
}
