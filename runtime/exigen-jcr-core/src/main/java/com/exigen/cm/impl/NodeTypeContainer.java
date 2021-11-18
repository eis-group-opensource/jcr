/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.HashMap;

import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.state2.ItemStatus;
import com.exigen.cm.jackrabbit.name.QName;

public class NodeTypeContainer {

    //Long id;
    private Long nodeId;
    private Long typeId;
    private Long fromTypeId;
    
    private ItemStatus state;
    
    //TODO remove this property
    private NodeTypeImpl nt;
    
    public NodeTypeContainer( Long nodeId, Long typeId, Long fromTypeId) {
		super();
		//this.id = id;
		this.nodeId = nodeId;
		this.typeId = typeId;
		this.fromTypeId = fromTypeId;
	}

	public NodeTypeContainer(HashMap row, NodeTypeManagerImpl ntManager) throws NoSuchNodeTypeException {
        //this.id = (Long) row.get(Constants.FIELD_ID);
        this.nodeId = (Long) row.get(Constants.FIELD_TYPE_ID);
        this.typeId = (Long) row.get(Constants.TABLE_TYPE__NODE_TYPE);
        this.fromTypeId = (Long) row.get(Constants.TABLE_TYPE__FROM_NODE_TYPE);
        
        state = ItemStatus.Normal;
        
        this.nt = ntManager.getNodeTypeBySQLId(typeId);
    }

    public NodeTypeContainer(Long nodeId, NodeTypeImpl nt2, NodeTypeImpl fromType) {
        this.nodeId = nodeId;
        this.typeId = nt2.getSQLId();
        this.fromTypeId = fromType.getSQLId();
        this.nt = nt2;
        this.state = ItemStatus.New;
    }

    public NodeTypeContainer(Long nodeId, NodeTypeImpl nt2, Long fromType) {
        this.nodeId = nodeId;
        this.typeId = nt2.getSQLId();
        this.fromTypeId = fromType;
        this.nt = nt2;
        this.state = ItemStatus.New;
    }

    public boolean isEffective() {
    	if (state.equals(ItemStatus.Normal) || state.equals(ItemStatus.New)){
    		return true;
    	} else {
    		return false;
    	}
    }

    public QName getName() {
        return nt.getQName();
    }

    public boolean isMixin() {
        return nt.isMixin();
    }

    public ItemStatus getState() {
        return state;
    }

    public Long getNodeTypeId() {
        return typeId;
    }

    public NodeTypeImpl getNodeType() {
        return nt;
    }

    public void resetStateToNormal() {
        state = ItemStatus.Normal;
        
    }

    public void setRemoved() {
        state = ItemStatus.Invalidated;
        
    }

    /*public Long getId() {
        return id;
    }*/
    
    public String toString(){
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", getName());
        builder.append("state", state);
        return builder.toString();
    }

    public Long getFromTypeId() {
        return fromTypeId;
    }

    /*public void setId(Long typeId2) {
        this.id = typeId2;
        
    }*/

    public Long getNodeId() {
        return nodeId;
    }

	public NodeTypeContainer copy() {
		NodeTypeContainer other = new NodeTypeContainer(nodeId, typeId, fromTypeId);
		
		other.state = ItemStatus.Normal;
	    other.nt = nt;
	    
	    return other;
	}

}


/*
 * $Log: NodeTypeContainer.java,v $
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
 * Revision 1.3  2006/08/07 14:25:54  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.2  2006/04/20 11:42:46  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.7  2006/03/27 14:57:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.6  2006/03/21 13:19:27  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.5  2006/03/03 10:33:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.4  2006/02/27 15:02:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.3  2006/02/17 13:03:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.2  2006/02/16 15:47:59  dparhomenko
 * PTR#0144983 restructurize
 *
 * Revision 1.1  2006/02/16 13:53:05  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */