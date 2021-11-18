/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

/**
 * This Class implements a generic version selector that either selects a
 * version by name, label or creation date. If no version is found and the
 * 'returnLatest' flag is set to <code>true</code>, the latest version is
 * returned.
 */
public class GenericVersionSelector implements VersionSelector {

    /**
     * a versionname hint
     */
    private String name = null;

    /**
     * a versionlabel hint
     */
    private String label = null;

    /**
     * a version date hint
     */
    private Calendar date = null;

    /**
     * flag indicating that it should return the latest version, if no other found
     */
    private boolean returnLatest = true;

    /**
     * Creates a default <code>GenericVersionSelector</code> that always selects
     * the latest version.
     */
    public GenericVersionSelector() {
    }

    /**
     * Creates a <code>GenericVersionSelector</code> that will try to select a
     * version with the given label.
     *
     * @param label
     */
    public GenericVersionSelector(String label) {
        this.label = label;
    }

    /**
     * Creates a <code>GenericVersionSelector</code> that will select the oldest
     * version of all those that are more recent than the given date.
     *
     * @param date
     */
    public GenericVersionSelector(Calendar date) {
        this.date = date;
    }

    /**
     * Returns the name hint.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name hint
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the label hint
     *
     * @return
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label hint
     *
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the date hint
     *
     * @return
     */
    public Calendar getDate() {
        return date;
    }

    /**
     * Sets the date hint
     *
     * @param date
     */
    public void setDate(Calendar date) {
        this.date = date;
    }

    /**
     * Returns the flag, if the latest version should be selected, if no
     * version can be found using the given hint.
     *
     * @return
     */
    public boolean isReturnLatest() {
        return returnLatest;
    }

    /**
     * Sets the flag, if the latest version should be selected, if no
     * version can be found using the given hint.
     *
     * @param returnLatest
     */
    public void setReturnLatest(boolean returnLatest) {
        this.returnLatest = returnLatest;
    }

    /**
     * Selects a version from the given version history using the previously
     * assigned hint in the following order: name, label, date, latest.
     *
     * @param versionHistory
     * @return
     * @throws RepositoryException
     */
    public Version select(VersionHistory versionHistory) throws RepositoryException {
        Version selected = null;
        if (name != null) {
            selected = selectByName(versionHistory, name);
        }
        if (selected == null && label != null) {
            selected = selectByLabel(versionHistory, label);
        }
        if (selected == null && date != null) {
            selected = selectByDate(versionHistory, date);
        }
        if (selected == null && returnLatest) {
            selected = selectByDate(versionHistory, null);
        }
        return selected;
    }

    /**
     * Selects a version by version name.
     *
     * @param history
     * @param name
     * @return the version with the given name or <code>null</code>
     * @throws RepositoryException
     */
    public static Version selectByName(VersionHistory history, String name)
            throws RepositoryException {
        if (history.hasNode(name)) {
            return history.getVersion(name);
        } else {
            return null;
        }
    }

    /**
     * Selects a version by label
     *
     * @param history
     * @param label
     * @return the version with the given label or <code>null</code>
     * @throws RepositoryException
     */
    public static Version selectByLabel(VersionHistory history, String label)
            throws RepositoryException {
        return history.getVersionByLabel(label);
    }

    /**
     * Selects a version by date.
     *
     * @param history
     * @param date
     * @return the latest version newer than the given date date or <code>null</code>
     * @throws RepositoryException
     */
    public static Version selectByDate(VersionHistory history, Calendar date)
            throws RepositoryException {
        long time = (date != null) ? date.getTimeInMillis() : Long.MAX_VALUE;
        long latestDate = Long.MIN_VALUE;
        Version latestVersion = null;
        VersionIterator iter = history.getAllVersions();
        while (iter.hasNext()) {
            Version v = iter.nextVersion();
            long c = v.getCreated().getTimeInMillis();
            if (c > latestDate && c <= time) {
                latestDate = c;
                latestVersion = v;
            }
        }
        return latestVersion;
    }

}
