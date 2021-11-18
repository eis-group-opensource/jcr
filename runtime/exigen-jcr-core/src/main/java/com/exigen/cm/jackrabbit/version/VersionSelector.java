/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * This Interface defines the version selector that needs to provide a version,
 * given some hints and a version history
 */
public interface VersionSelector {
    
    /**
     * Selects a version of the given version history. If this VersionSelector
     * is unable to select one, it can return <code>null</code>.
     *
     * @param versionHistory
     * @return A version or <code>null</code>.
     * @throws RepositoryException if an error occurrs.
     */
    Version select(VersionHistory versionHistory) throws RepositoryException;
}
