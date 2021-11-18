/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.util.name;

import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.QName;

/**
 * The name cache defines an interface that is used by the {@link NameFormat}
 * and is usually implemented by {@link NamespaceResolver}s.
 * <p/>
 * Please note, that the redundant naming of the methods is intentionally in
 * order to allow a class to implement several caches.
 */
public interface NameCache {

    /**
     * Retrieves a qualified name from the cache for the given jcr name. If the
     * name is not cached <code>null</code> is returned.
     *
     * @param jcrName the jcr name
     * @return the qualified name or <code>null</code>
     */
    public QName retrieveName(String jcrName);

    /**
     * Retrieves a jcr name from the cache for the given qualified name. If the
     * name is not cached <code>null</code> is returned.
     *
     * @param name the qualified name
     * @return the jcr name or <code>null</code>
     */
    public String retrieveName(QName name);

    /**
     * Puts a name into the cache.
     *
     * @param jcrName the jcr name
     * @param name the qualified name
     */
    public void cacheName(String jcrName, QName name);

    /**
     * Evicts all names from the cache, i.e. clears it.
     */
    public void evictAllNames();

}