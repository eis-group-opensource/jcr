/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.cache;

import java.io.Serializable;

import org.apache.commons.lang.builder.HashCodeBuilder;

public class CacheKey implements Serializable{

    private Long entityId;
    private String entityName;

    public CacheKey(String entityName, Long entityId) {
        this.entityName = entityName;
        this.entityId = entityId;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getEntityName() {
        return entityName;
    }

    public String toString(){
        StringBuffer sb= new StringBuffer(entityName);
        sb.append("[");
        sb.append(entityId);
        sb.append("]");
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (obj instanceof CacheKey ){
            CacheKey other = (CacheKey) obj;
            if (entityId.equals(other.entityId) && entityName.equals(other.entityName)){
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder();
        b.append(entityName);
        b.append(entityId);
        return b.toHashCode();
    }


}


/*
 * $Log: CacheKey.java,v $
 * Revision 1.1  2007/04/26 09:00:15  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/04/17 06:46:41  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/03/27 14:27:23  dparhomenko
 * PTR#0144983 security
 *
 */