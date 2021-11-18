/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state;

import javax.jcr.PropertyType;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class PropertyState {

    //private _NodeImpl node;
    private PropDef def;
    private int propertyType;
    private boolean multiValued;
    protected InternalValue[] values;    
    protected InternalValue[] originalValues;
    private QName name;
    private int requiredType;
    
    //TODO add to constructor
    public Long unstructuredPropertyId;
    private Long nodeId;
    private boolean processed = false;    
    

    /*public PropertyState(_NodeImpl node,InternalValue[] values) {
        this.node = node;
        this.originalValues = values;
    }*/
    //public PropertyState() {
        //this.node = node;
//        this.propertyType = PropertyType.UNDEFINED;
        //this(node, null);
    //}

    public PropertyState(QName name, int requiredType, PropDef def, Long nodeId, Long unstructuredPropertyId) {
        this.def = def;
        this.name = name;
        this.requiredType = requiredType;
        this.propertyType = PropertyType.UNDEFINED;
        this.nodeId = nodeId;
        this.unstructuredPropertyId = unstructuredPropertyId;
    }

    public InternalValue[] getValues() {
        return values;
    }

    public int getType() {
        return this.propertyType;
    }

    public void setValues(InternalValue[] values) {
        if (originalValues == null){
            originalValues = values;
        }
        this.values = values;
    }

    public void setType(int type) {
        this.propertyType = type;
    }

    public void setMultiValued(boolean b) {
        this.multiValued = b;
    }


    public void resetToNormal() {
        originalValues = values;
        processed = false;
        
    }
    public InternalValue[] getInitialValues() {
        return originalValues;
        
    }

    public QName getName() {
        return name;
    }


    public PropDef getPropDef(){
        return def;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public String safeGetJCRPath() {
        //TODO implement me
        return "[nodeid:"+nodeId+"]/"+name;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public void setNormal() {
        processed = false;
        originalValues = values;
        
    }

    @Override
    public String toString() {
        ToStringBuilder b = new ToStringBuilder(this);
        b.append("name", name);
        return b.toString();
    }

	public int getRequiredType() {
		return requiredType;
	}
    
}


/*
 * $Log: PropertyState.java,v $
 * Revision 1.1  2007/04/26 08:58:59  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/10/17 10:46:57  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.5  2006/08/07 14:26:05  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.4  2006/06/09 08:55:48  dparhomenko
 * PTR#1802035 fix version history
 *
 * Revision 1.3  2006/06/05 15:21:39  dparhomenko
 * PTR#1802035 fix version history
 *
 * Revision 1.2  2006/06/05 15:03:39  dparhomenko
 * PTR#1802035 fix version history
 *
 * Revision 1.1  2006/04/17 06:47:04  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.8  2006/03/29 12:56:30  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.7  2006/03/16 13:13:05  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.6  2006/03/14 11:55:31  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.5  2006/03/03 10:33:15  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.4  2006/02/21 16:02:59  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.3  2006/02/17 13:03:42  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.2  2006/02/16 15:47:56  dparhomenko
 * PTR#0144983 restructurize
 *
 * Revision 1.1  2006/02/16 13:53:02  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */