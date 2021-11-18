/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import com.exigen.cm.jackrabbit.name.QName;

public class PropertyId extends ItemId{

    //private _NodeImpl node;
    private QName name;
    private Long parentId;

    public PropertyId(Long nodeId, QName propName) {
        //this.node = node;
        this.name = propName;
        parentId = nodeId;
    }

    public QName getName() {
        return name;
    }

    /*public _NodeImpl getNode() {
        return node;
    }*/
    
    /**
     * Compares property identifiers for equality.
     *
     * @param obj other object
     * @return <code>true</code> if the given object is a property identifier
     *         instance that identifies the same property as this identifier,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PropertyId) {
            PropertyId other = (PropertyId) obj;
            return parentId.equals(other.parentId)
                    && name.equals(other.name);
        }
        return false;
    }
    
    /**
     * Returns the hash code of this property identifier. The hash code
     * is computed from the parent node UUID and the property name. The
     * hash code is memorized for performance.
     *
     * @return hash code
     * @see Object#hashCode()
     */
    public int hashCode() {
        // PropertyId is immutable, we can store the computed hash code value
        int h = hash;
        if (h == 0) {
            h = 17;
            h = 37 * h + parentId.hashCode();
            h = 37 * h + name.hashCode();
            hash = h;
        }
        return h;
    }
    
    /**
     * Returns a string representation of this property identifier.
     *
     * @return property identifier string
     * @see Object#toString()
     */
    public String toString() {
        return parentId + "/" + name.toString();
    }

    /**
     * Returns <code>false</code> as this class represents a property
     * identifier, not a node identifier.
     *
     * @return always <code>false</code>
     * @see ItemId#denotesNode()
     */
    public boolean denotesNode() {
        return false;
    }

	public Long getParentId() {
		return parentId;
	}

}


/*
 * $Log: PropertyId.java,v $
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.2  2006/09/07 14:38:03  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/03/03 10:33:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.2  2006/02/20 15:32:25  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/16 13:53:05  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */