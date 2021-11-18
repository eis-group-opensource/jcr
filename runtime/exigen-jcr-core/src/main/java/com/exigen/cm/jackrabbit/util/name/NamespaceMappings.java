/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.util.name;

import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;



/**
 * The class <code>NamespaceMappings</code> holds a namespace mapping that is
 * used internally in the search index. Storing paths with the full uri of a
 * namespace would require too much space in the search index.
 */
public interface NamespaceMappings extends NamespaceResolver {

    /**
     * Translates a property name from a session local namespace mapping into a
     * search index private namespace mapping.
     *
     * @param name     the property name to translate
     * @param resolver the <code>NamespaceResolver</code> of the local session.
     * @return the translated property name
     */
    public String translatePropertyName(String name,
                                        NamespaceResolver resolver)
            throws IllegalNameException, UnknownPrefixException;
}
