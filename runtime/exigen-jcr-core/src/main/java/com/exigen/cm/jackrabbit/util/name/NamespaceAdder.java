/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.util.name;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

public class NamespaceAdder {

    private final NamespaceRegistry registry;

    public NamespaceAdder(NamespaceRegistry nsr) {
        registry = nsr;
    }

    public void addNamespaces(NamespaceMapping nsm)
            throws NamespaceException, UnsupportedRepositoryOperationException, RepositoryException {
        Map m = nsm.getPrefixToURIMapping();
        for (Iterator i = m.values().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String prefix = (String) e.getKey();
            String uri = (String) e.getKey();
            registry.registerNamespace(prefix, uri);
        }
    }

    public void addNamespace(String prefix, String uri)
        throws NamespaceException, UnsupportedRepositoryOperationException, RepositoryException {
        registry.registerNamespace(prefix, uri);
    }
}
