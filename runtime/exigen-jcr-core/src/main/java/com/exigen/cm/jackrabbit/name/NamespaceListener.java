/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.name;

/**
 * Receives notifications when a namespace mapping changes.
 */
public interface NamespaceListener {

    /**
     * Notifies the listeners that an existing namespace <code>uri</code> has
     * been re-mapped from <code>oldPrefix</code> to <code>newPrefix</code>.
     *
     * @param oldPrefix the old prefix.
     * @param newPrefix the new prefix.
     * @param uri       the associated namespace uri.
     */
    public void namespaceRemapped(String oldPrefix, String newPrefix, String uri);

    /**
     * Notifies the listeners that a new namespace <code>uri</code> has been
     * added and mapped to <code>prefix</code>.
     *
     * @param prefix the prefix.
     * @param uri    the namespace uri.
     */
    public void namespaceAdded(String prefix, String uri);
}
