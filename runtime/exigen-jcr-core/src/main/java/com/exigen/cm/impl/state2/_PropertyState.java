/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import java.util.ArrayList;
import java.util.Collection;

import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.NodeReference;
import com.exigen.cm.impl.PropertyDefinitionImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.observation.EventState;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.PropDefImpl;
import com.exigen.cm.jackrabbit.uuid.UUID;
import com.exigen.cm.jackrabbit.value.BLOBFileValue;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class _PropertyState extends _ItemState {

	private _NodeState parent;

	private QName name;

	private int propertyType;

	private int requiredType;

	private boolean multivalued;

	private PropDefImpl def;

	private Long unstructuredPropertyId;

	private InternalValue[] initialValues;

	private InternalValue[] values;

	private _AbstractsStateManager stateManager;

	private PropertyDefinitionImpl definition;
	
    private boolean triggerEvents = false;	

	public _PropertyState(RepositoryImpl repository, _NodeState parent,
			QName name, int propertyType, int requiredType,
			boolean multivalued, PropDefImpl def, Long unstructuredPropertyId) {
		super(repository);
		this.parent = parent;
		this.name = name;
		this.propertyType = propertyType;
		this.requiredType = requiredType;
		this.multivalued = multivalued;
		this.def = def;
		this.unstructuredPropertyId = unstructuredPropertyId;
	}

	@Override
	public QName getName() {
		return name;
	}

	void setInitialValues(InternalValue[] val) throws RepositoryException {
		//todo copy values
		//try {
			if (val != null){
				this.initialValues = new InternalValue[val.length];
				int i = 0;
				for(InternalValue v:val){
					if (v.getType() == PropertyType.BINARY){
						this.initialValues[i++] = v.createCopy();
					} else {
						this.initialValues[i++] = v;
					}
				}
				//this.initialValues = val;
				this.values = initialValues;
			}
		//} catch (RepositoryException exc){
		//	exc.printStackTrace();
		//	throw exc;
		//}
		fixStoreContaianer();
	}

	@Override
	public boolean isNode() {
		return false;
	}

	@Override
	public _NodeState getParent(){
		return parent;
	}

	public _PropertyState copyProperty(_NodeState newParent) throws RepositoryException{
		_PropertyState other = new _PropertyState(getRepository(), newParent,
				name, propertyType, requiredType, multivalued, def,
				unstructuredPropertyId);
		other.setInitialValues(values);
		other.setStatusNormal();
		return other;
	}

	public PropertyDefinitionImpl getDefinition(){
		if (definition == null) {
			try {
				definition = stateManager.getNodeTypeManager().getPropertyDefinition(def);
			} catch (RepositoryException e) {
				throw new RuntimeException("Unable to get property definition");
			}
		}
		return definition;
	}

	public void assignSession(_AbstractsStateManager sessionStatemanager) throws RepositoryException {
		this.stateManager = sessionStatemanager;
		this.nodeTypeManager = sessionStatemanager.getNodeTypeManager();
		fixStoreContaianer();
	}

	private void fixStoreContaianer() {
		if (stateManager != null){
			if (values != null){
				for(InternalValue v: values){
					if (v.internalValue() instanceof BLOBFileValue){
						BLOBFileValue vv = (BLOBFileValue)v.internalValue();
						vv.assignStoreContainer(this.stateManager.getStoreContainer());
					}
				}
			}
			if (initialValues != null){
				for(InternalValue v: initialValues){
					if (v.internalValue() instanceof BLOBFileValue){
						BLOBFileValue vv = (BLOBFileValue)v.internalValue();
						vv.assignStoreContainer(this.stateManager.getStoreContainer());
					}
				}
			}
		}		
	}

	public int getType() {
		return propertyType;
	}

	public InternalValue[] getValues() {
		return values;
	}

	public boolean isTriggerEvents() {
		return triggerEvents;
	}

	public void setTriggerEvents(boolean triggerEvents) {
		this.triggerEvents = triggerEvents;
	}

	public InternalValue[] getInitialValues() {
		return initialValues;
	}

	public InternalValue getValue() {
		if (!multivalued){
			return getValues()[0];
		}
		return null;
	}

	public Long getUnstructuredPropertyId() {
		return unstructuredPropertyId;
	}

	public void setUnstructuredPropertyId(Long unstructuredPropertyId) {
		this.unstructuredPropertyId = unstructuredPropertyId;
	}

	public Long getNodeId() {
		return parent.getNodeId();
	}

	public String getString() {
		return values[0].toString();
	}

	public void resetToNormal() {
		setStatusNormal();
		initialValues = values;
		this.triggerEvents = false;
	}

	public void internalSetValue(InternalValue[] values, int type, boolean setModification, boolean triggerEvents) throws RepositoryException{
        
        if (triggerEvents){
            this.triggerEvents  = true;
        }
        
        // check for null value
        if (values == null) {
            // setting a property to null removes it automatically
            getParent().removeChildProperty(getName(), setModification);
            return;
        }
        ArrayList<InternalValue> list = new ArrayList<InternalValue>();
        // compact array (purge null entries)
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                list.add(values[i]);
            }
        }
        values = (InternalValue[]) list.toArray(new InternalValue[list.size()]);


        // free old values as necessary
        //TODO create for rferences
        InternalValue[] oldValues = getValues();
        if (oldValues != null) {
            for (int i = 0; i < oldValues.length; i++) {
                InternalValue old = oldValues[i];
                if (old != null && old.getType() == PropertyType.BINARY) {
                    // BINARY value
                    BLOBFileValue blob = (BLOBFileValue) old.internalValue();
                    blob.discard();
                    blob = null; // gc hint
                    //throw new UnsupportedOperationException();
                }
                if (old != null && (old.getType() == PropertyType.REFERENCE || old.getType() == PropertyType283.WEAKREFERENCE)) {
                    // Reference value
                    // REFERENCE value
                    //UUID uuid = (UUID) old.internalValue();
                    //_NodeImpl refNode = session.getNodeManager().getItemByUUID(uuid.toString());
                    parent.removeReference(this, old);
                }
            }
        }

        // set new values
        setValues(values);
        
        for (int i = 0; i < values.length; i++) {
            InternalValue v = values[i];
            if (v != null && (v.getType() == PropertyType.REFERENCE || v.getType() == PropertyType283.WEAKREFERENCE)) {
                // REFERENCE value
                UUID uuid = (UUID) v.internalValue();
                //TODO may be true ?
                _NodeState refNode = stateManager.getItemByUUID(uuid.toString(), false);
                NodeReference ref = parent.registerReference(this, v, refNode);
                refNode.registerTmpRefeference(ref);
            } else if (v != null && v.getType() == PropertyType.BINARY) {
                BLOBFileValue b = (BLOBFileValue) v.internalValue();
                if (b.getStoreId() == null){
                	Long confId = parent.getStoreConfigurationId();
                	if (confId != null){
                		_NodeState confNode = stateManager.getNodeState(confId, null);
                		if (confNode.isNodeType(Constants.EWF_STORE_CONFIGURATION)){
                			String storeId = confNode.getProperty(Constants.EWF_STORE_CONFIGURATION__NAME, true).getString();
                			b.setStoreId(storeId);
                			//TODO copy properties
                		} else {
                			throw new RepositoryException("incorrect store configuration for node "+parent.getPrimaryPath());
                		}
                	}
                }
            }
        }
        
        // set type
        if (type == PropertyType.UNDEFINED) {
            // fallback to default type
            type = PropertyType.STRING;
        }
        this.propertyType = type;
        
        
        if (setModification){
            parent.registerModification();
            if (getStatus().equals(ItemStatus.New) || getStatus().equals(ItemStatus.Modified)){
                //do nothing
            } else if (getStatus().equals(ItemStatus.Normal)){
                setStatusModified();
            } else {
                throw new UnsupportedOperationException("Unknown state");
            }
        }
        
	}

	public void setValues(InternalValue[] values) {
		this.values = values;
		fixStoreContaianer();
		
	}

	@Override
	public String toString() {
		ToStringBuilder builder = new ToStringBuilder(this);
		builder.append("nodeId", parent.getNodeId());
		builder.append("name", name);
		return builder.toString();
	}

	public int getRequiredType() {
		return requiredType;
	}

	public void setRequiredType(int requiredType) {
		this.requiredType = requiredType;
	}

	public Collection<EventState> getEvents(){
		return new ArrayList<EventState>();
	}

}
