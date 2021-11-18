/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype;

import java.io.Serializable;

/**
 * <code>PropDefId</code> serves as identifier for a given <code>PropDef</code>.
 *
 *
 * uniquely identifies a <code>PropDef</code> in the
 * node type registry.
 */
public class PropDefId implements Serializable {

    /**
     * Serialization UID of this class.
     */
    static final long serialVersionUID = 3675238890036653593L;

    /**
     * The internal id is computed based on the characteristics of the
     * <code>PropDef</code> that this <code>PropDefId</code> identifies.
     */
    private final int id;

    /**
     * Creates a new <code>PropDefId</code> that serves as identifier for
     * the given <code>PropDef</code>. An internal id is computed based on
     * the characteristics of the <code>PropDef</code> that it identifies.
     *
     * @param def <code>PropDef</code> to create identifier for
     */
    PropDefId(PropDef def) {
        if (def == null) {
            throw new IllegalArgumentException("PropDef argument can not be null");
        }
        // build key (format: <declaringNodeType>/<name>/<requiredType>/<multiple>)
        StringBuffer sb = new StringBuffer();

        sb.append(def.getDeclaringNodeType().toString());
        sb.append('/');
        if (def.definesResidual()) {
            sb.append('*');
        } else {
            sb.append(def.getName().toString());
        }
        sb.append('/');
        sb.append(def.getRequiredType());
        sb.append('/');
        sb.append(def.isMultiple() ? 1 : 0);

        id = sb.toString().hashCode();
    }

    /**
     * Private constructor that creates a <code>PropDefId</code> using an
     * internal id
     *
     * @param id internal id
     */
    private PropDefId(int id) {
        this.id = id;
    }

    /**
     * Returns a <code>PropDefId</code> holding the value of the specified
     * string. The string must be in the format returned by the
     * <code>PropDefId.toString()</code> method.
     *
     * @param s a <code>String</code> containing the <code>PropDefId</code>
     *          representation to be parsed.
     * @return the <code>PropDefId</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     *                                  as a <code>PropDefId</code>.
     * @see #toString()
     */
    public static PropDefId valueOf(String s) throws IllegalArgumentException {
        if (s == null) {
            throw new IllegalArgumentException("invalid PropDefId literal");
        }
        return new PropDefId(Integer.parseInt(s));
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PropDefId) {
            PropDefId other = (PropDefId) obj;
            return id == other.id;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return Integer.toString(id);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        // the computed 'id' is used as hash code
        return id;
    }
}
