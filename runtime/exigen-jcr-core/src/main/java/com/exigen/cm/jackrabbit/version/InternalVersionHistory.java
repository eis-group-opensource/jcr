/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import java.util.Iterator;

import javax.jcr.version.VersionException;

import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.jackrabbit.name.QName;

/**
 * This interface defines the internal version history.
 */
public interface InternalVersionHistory extends InternalVersionItem {

    /**
     * Equivalalent to {@link javax.jcr.version.VersionHistory#getRootVersion()}.
     *
     * @see javax.jcr.version.VersionHistory#getRootVersion()
     */
    InternalVersion getRootVersion();

    /**
     * Equivalalent to {@link javax.jcr.version.VersionHistory#getVersion(java.lang.String)}.
     *
     * @see javax.jcr.version.VersionHistory#getVersion(java.lang.String)
     */
    InternalVersion getVersion(QName versionName) throws VersionException;

    /**
     * Checks if the version with the given name exists in this version history.
     *
     * @param versionName the name of the version
     * @return <code>true</code> if the version exists;
     *         <code>false</code> otherwise.
     */
    boolean hasVersion(QName versionName);

    /**
     * Checks if the version for the given uuid exists in this history.
     *
     * @param uuid the uuid of the version
     * @return <code>true</code> if the version exists;
     *         <code>false</code> otherwise.
     */
    boolean hasVersion(String uuid);

    /**
     * Returns the version with the given uuid or <code>null</code> if the
     * respective version does not exist.
     *
     * @param uuid the uuid of the version
     * @return the internal version ot <code>null</code>
     */
    InternalVersion getVersion(String uuid);

    /**
     * Equivalalent to {@link javax.jcr.version.VersionHistory#getVersionByLabel(java.lang.String)}
     * but returns <code>null</code> if the version does not exists.
     *
     * @see javax.jcr.version.VersionHistory#getVersionByLabel(java.lang.String)
     */
    InternalVersion getVersionByLabel(QName label);

    /**
     * Returns an iterator over all versions (not ordered yet), including the
     * root version.
     *
     * @return an iterator over {@link InternalVersion} objects.
     */
    Iterator getVersions();

    /**
     * Returns the number of versions in this version history.
     *
     * @return the number of versions, including the root version.
     */
    int getNumVersions();

    /**
     * Returns the UUID of the versionable node that this history belongs to.
     *
     * @return the UUID of the versionable node.
     */
    String getVersionableUUID();

    /**
     * Returns a string  iterator over all version labels that exist in this
     * version history
     *
     * @return
     */
    QName[] getVersionLabels();

    /**
     * Returns the UUID of the version labels node
     *
     * @return
     */
    String getVersionLabelsUUID();

	_NodeState getNodeState();
}
