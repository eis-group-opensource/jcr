/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.HashMap;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.state2.ItemStatus;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.jackrabbit.name.QName;
public class NodeReference {

    //TODO ovveride equls and hoshcode
    
    private final Long id;
    private final Long fromId;
    private final String toUUID;
    private Long toId;
    
    private final String propertyName;
    private final Long propertyNamespaceId;
    
    private final NamespaceRegistryImpl namespaceRegistry;
    
    //private final SessionImpl session;
    
    private ItemStatus state;
    
    public NodeReference(NamespaceRegistryImpl namespaceRegistry,Long id,_NodeState refFrom, _NodeState refTo, QName propName, String uuid) {
        this.namespaceRegistry = namespaceRegistry;
        this.id = id;
        this.fromId = refFrom.getNodeId();
        this.toId = refTo.getNodeId();
        this.propertyName = propName.getLocalName();
        String uri = propName.getNamespaceURI();
        this.propertyNamespaceId = namespaceRegistry._getByURI(uri).getId();
        this.state = ItemStatus.New;
        this.toUUID = uuid;
    }

    public NodeReference(NamespaceRegistryImpl namespaceRegistry,Long id,Long fromNodeId, Long toNodeId, QName propName, String uuid) {
        this.namespaceRegistry = namespaceRegistry;
        this.id = id;
        this.fromId = fromNodeId;
        this.toId = toNodeId;
        this.propertyName = propName.getLocalName();
        String uri = propName.getNamespaceURI();
        this.propertyNamespaceId = namespaceRegistry._getByURI(uri).getId();
        this.state = ItemStatus.New;
        this.toUUID = uuid;
    }

    public NodeReference(NamespaceRegistryImpl namespaceRegistry,HashMap row) {
        this.namespaceRegistry = namespaceRegistry;
        this.id = (Long) row.get(Constants.FIELD_ID);
        this.fromId =  (Long) row.get(Constants.TABLE_NODE_REFERENCE__FROM);
        this.toId =  (Long) row.get(Constants.TABLE_NODE_REFERENCE__TO);
        this.propertyName =  (String) row.get(Constants.TABLE_NODE_REFERENCE__PROPERTY_NAME);
        this.propertyNamespaceId =  (Long) row.get(Constants.TABLE_NODE_REFERENCE__PROPERTY_NAMESPACE);
        this.toUUID =  (String) row.get(Constants.TABLE_NODE_REFERENCE__UUID);
        this.state = ItemStatus.Normal;
    }

    public Long getFromId() {
        return fromId;
    }

    public Long getId() {
        return id;
    }

    public Long getPropertyNamespaceId() {
        return propertyNamespaceId;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public ItemStatus getState() {
        return state;
    }

    public Long getToId() {
        return toId;
    }

    public QName getPropertyQName() {
        String uri = "";
        if (propertyNamespaceId != null){
            uri = namespaceRegistry._getById(propertyNamespaceId).getUri();
        }
        return new QName(uri, propertyName);
    }

    public void resetStateToNormal() {
        state = ItemStatus.Normal;
        
    }

    public void setRemoved() {
        state= ItemStatus.Invalidated;
        
    }

    public boolean equals(Object obj) {
        if (obj instanceof NodeReference){
            if (id.longValue() == ((NodeReference)obj).getId().longValue()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public int hashCode() {
        return id.hashCode();
    }

    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("id", id);
        builder.append("from", fromId);
        builder.append("to", toId);
        builder.append("property", getPropertyQName());
        return builder.toString();
    }

    public void setDeleted() {
        state = ItemStatus.Destroyed;
    }

	public void setToId(Object object) {
		this.toId = null;
	}

	public void setChanged() {
		state = ItemStatus.Modified;
		
	}

	public String getUUID(){
		return toUUID;
	}
	
}


/*
 * $Log: NodeReference.java,v $
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2007/01/24 08:46:24  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.5  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.4  2006/09/07 10:36:54  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.3  2006/08/16 10:08:59  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.2  2006/04/20 11:42:46  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.4  2006/03/27 14:57:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.3  2006/02/27 15:02:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.2  2006/02/22 12:04:03  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/20 15:32:25  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */