/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

/**
 * This interface defines the base for all internal versioning items. Internal
 * versioning items are decoupled from their external form as exposed to the
 * repository or in form of the node extensions {@link javax.jcr.version.Version}
 * or {@link javax.jcr.version.VersionHistory}.
 */
public interface InternalVersionItem {

    /**
     * Returns the external id of this item
     *
     * @return
     */
    Long getId();

    /**
     * returns the parent version item or null
     *
     * @return
     */
    InternalVersionItem getParent();

}
