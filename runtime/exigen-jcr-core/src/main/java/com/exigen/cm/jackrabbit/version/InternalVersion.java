/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.version.Version;

import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.jackrabbit.name.QName;

/**
 * This interface defines the internal version.
 */
public interface InternalVersion extends InternalVersionItem {

    /**
     * Returns the name of this version.
     *
     * @return the name of this version.
     */
    QName getName();

    /**
     * Returns the frozen node of this version or <code>null</code> if this is
     * the root version.
     *
     * @return the frozen node.
     */
    InternalFrozenNode getFrozenNode();

    /**
     * Equivalent to {@link Version#getCreated()}
     *
     * @see javax.jcr.version.Version#getCreated()
     */
    Calendar getCreated();

    /**
     * Equivalent to {@link Version#getSuccessors()}}
     *
     * @see javax.jcr.version.Version#getSuccessors()
     */
    InternalVersion[] getSuccessors();

    /**
     * Equivalent to {@link Version#getPredecessors()}}
     *
     * @see javax.jcr.version.Version#getPredecessors()
     */
    InternalVersion[] getPredecessors();

    /**
     * Checks if this version is more recent than the given version <code>v</code>.
     * A version is more recent if and only if it is a successor (or a successor
     * of a successor, etc., to any degree of separation) of the compared one.
     *
     * @param v the version to check
     * @return <code>true</code> if the version is more recent;
     *         <code>false</code> otherwise.
     */
    boolean isMoreRecent(InternalVersion v);

    /**
     * returns the internal version history in wich this version lifes in.
     *
     * @return the version history for this version.
     */
    InternalVersionHistory getVersionHistory();

    /**
     * checks if this is the root version.
     *
     * @return <code>true</code> if this version is the root version;
     *         <code>false</code> otherwise.
     */
    boolean isRootVersion();

    /**
     * Checks, if this version has the given label assosiated
     *
     * @param label the label to check.
     * @return <code>true</code> if the label is assigned to this version;
     *         <code>false</code> otherwise.
     */
    boolean hasLabel(QName label);

    /**
     * returns the labels that are assigned to this version
     *
     * @return a string array of labels.
     */
    QName[] getLabels();

    String getUUID() throws ValueFormatException, IllegalStateException, RepositoryException;

	_NodeState getNodeState();
}
