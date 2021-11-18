/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.HashMap;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.state2.ItemStatus;

public class ParentNode {

    //private Long id;

    private Long childId;

    private Long parentId;

    private Long position;
    
    private ItemStatus _state = ItemStatus.New;

    public ParentNode(HashMap row) {
        //this.id = (Long) row.get(Constants.FIELD_ID);
        this.childId =  (Long) row.get(Constants.FIELD_TYPE_ID);
        this.parentId = (Long) row.get(Constants.TABLE_NODE_PARENT__PARENT_ID);
        this.position = (Long) row.get(Constants.TABLE_NODE_PARENT__LEVEL);
        _state = ItemStatus.Normal;
    }

    public ParentNode(Long nodeId, Long parentId2, long index) {
        this.childId = nodeId;
        this.parentId = parentId2;
        this.position = new Long(index);
        _state = ItemStatus.New;
    }

    public Long getChildId() {
        return childId;
    }

    public void setChildId(Long childId) {
        this.childId = childId;
    }

    /*public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }*/

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long position) {
        this.position = position;
    }

    public ItemStatus getState() {
        return _state;
    }

    public void resetStateToNormal() {
        _state = ItemStatus.Normal;
        
    }

    public void setRemoved() {
    	if (_state.equals(ItemStatus.New)){
    		_state = ItemStatus.Destroyed;
    	} else {
    		_state = ItemStatus.Invalidated;
    	}
        
    }

	public ParentNode copy() {
		ParentNode other = new ParentNode(childId, parentId, position);
		other._state = ItemStatus.Normal;
		//other.id = id;
		return other;
	}

	@Override
	public String toString() {
		ToStringBuilder b = new ToStringBuilder(this);
		b.append("Parnet",getParentId());
		b.append("Chald",getChildId());
		b.append("Positoion",getPosition());
		b.append("Status",getState());
		return b.toString();
	}

}

/*
 * $Log: ParentNode.java,v $
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2007/02/22 09:24:16  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.5  2007/01/24 08:46:24  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.4  2006/09/27 12:32:56  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.3  2006/09/07 10:36:54  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.2  2006/04/20 11:42:46  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.5  2006/03/27 14:57:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.4  2006/03/03 10:33:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.3  2006/02/17 13:03:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.2  2006/02/16 15:47:59  dparhomenko
 * PTR#0144983 restructurize
 *
 * Revision 1.1  2006/02/16 13:53:04  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */