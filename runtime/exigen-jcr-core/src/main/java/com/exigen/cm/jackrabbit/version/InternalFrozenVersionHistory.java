/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import javax.jcr.ItemNotFoundException;
import javax.jcr.version.VersionException;

/**
 * This interface defines a frozen versionable child node, that was created
 * during a {@link javax.jcr.Node#checkin()} with a OPV==Version node.
 */
public interface InternalFrozenVersionHistory extends InternalFreeze {

    /**
     * Returns the id of the version history that was assigned to the node at
     * the time it was versioned.
     *
     * @return the id of the version history
     * @throws ItemNotFoundException 
     */
    String getVersionHistoryId() throws ItemNotFoundException;

    /**
     * Returns the version history that was assigned to the node at
     * the time it was versioned.
     *
     * @return the internal version history.
     * @throws VersionException if the history cannot be retrieved.
     */
    InternalVersionHistory getVersionHistory()
            throws VersionException;

    /**
     * Returns the id of the base version that was assigned to the node at
     * the time it was versioned.
     *
     * @return the id of the base version
     * @throws ItemNotFoundException 
     */
    String getBaseVersionId() throws ItemNotFoundException;

    /**
     * Returns the base version that was assigned to the node at
     * the time it was versioned.
     *
     * @return the inernal base version
     * @throws VersionException if the version could not be retrieved
     */
    InternalVersion getBaseVesion() throws VersionException;
}
