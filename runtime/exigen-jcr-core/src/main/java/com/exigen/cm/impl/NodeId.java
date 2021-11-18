/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import com.exigen.cm.jackrabbit.uuid.UUID;

/**
 * Node identifier. An instance of this class identifies a node using its UUID.
 * Once created a node identifier instance is immutable.
 */
public class NodeId extends ItemId {

    /** Serial version UID of this class. */
    static final long serialVersionUID = 7026219091360041109L;

    /** UUID of the identified node */
    private final Long id;
    private String uuid;

    /**
     * Creates a node identifier instance for the identified node.
     *
     * @param uuid node UUID
     */
    public NodeId(Long id, String uuid) {
        if (id == null) {
            throw new IllegalArgumentException("id can not be null");
        }
        if (uuid == null) {
            //throw new IllegalArgumentException("uuid can not be null");
        	uuid = UUID.randomUUID().toString();
        }
        this.id = id;
        this.uuid = uuid;
    }

    public NodeId(Long id, UUID uuid) {
        if (id == null) {
            throw new IllegalArgumentException("id can not be null");
        }
        if (uuid == null) {
            throw new IllegalArgumentException("uuid can not be null");
        }
        this.id = id;
        this.uuid = uuid.toString();
    }

    /**
     * Returns <code>true</code> as this class represents a node identifier,
     * not a property identifier.
     *
     * @return always <code>true</code>
     * @see ItemId#denotesNode()
     */
    public boolean denotesNode() {
        return true;
    }

    /**
     * Returns the UUID of the identified node.
     *
     * @return node UUID
     */
    public Long getId() {
        return id;
    }
    
    public String getUUID(){
    	return uuid;
    }

    /**
     * Returns a <code>NodeId</code> holding the value of the specified
     * string. The string must be in the format returned by the
     * <code>NodeId.toString()</code> method.
     *
     * @param s a <code>String</code> containing the <code>NodeId</code>
     *          representation to be parsed.
     * @return the <code>NodeId</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     *                                  as a <code>NodeId</code>.
     * @see #toString()
     */
    /*public static NodeId valueOf(Long s) throws IllegalArgumentException {
        if (s == null) {
            throw new IllegalArgumentException("invalid NodeId literal");
        }
        return new NodeId(s, null);
    }*/

    //-------------------------------------------< java.lang.Object overrides >

    /**
     * Compares node identifiers for equality.
     *
     * @param obj other object
     * @return <code>true</code> if the given object is a node identifier
     *         instance that identifies the same node as this identifier,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeId) {
            NodeId other = (NodeId) obj;
            return id.equals(other.id);
        }
        return false;
    }

    /**
     * Returns the node UUID.
     *
     * @return node UUID
     * @see Object#toString()
     */
    public String toString() {
        return id.toString()+":"+uuid;
    }

    /**
     * Returns the hash code of the node UUID. The computed hash code
     * is memorized for better performance.
     *
     * @return hash code
     * @see Object#hashCode()
     */
    public int hashCode() {
        // NodeId is immutable, we can store the computed hash code value
        if (hash == 0) {
            hash = id.hashCode();
        }
        return hash;
    }
}
