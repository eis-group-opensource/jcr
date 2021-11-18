/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype.v283;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;

import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.NodeDefImpl;

public class NodeDefinitionTemplateImpl implements NodeDefinitionTemplate, NodeDefinition {

	private boolean autoCreated;
	private String defaultPrimaryType;
	private boolean mandatory;
	private String name;
	private int opv;
	private boolean protectedStatus;
	private String[] requiredPrimaryTypes;
	private boolean allowSameNameSiblings;

	public void setAutoCreated(boolean autoCreated) {
		this.autoCreated = autoCreated;
	}

	public void setDefaultPrimaryType(String defaultPrimaryType) {
		this.defaultPrimaryType = defaultPrimaryType;
		
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOnParentVersion(int opv) {
		this.opv = opv;
	}

	public void setProtected(boolean protectedStatus) {
		this.protectedStatus = protectedStatus;
	}

	public void setRequiredPrimaryTypes(String[] requiredPrimaryTypes) {
		this.requiredPrimaryTypes = requiredPrimaryTypes;
		
	}

	public void setSameNameSiblings(boolean allowSameNameSiblings) {
		this.allowSameNameSiblings = allowSameNameSiblings;
		
	}

	public boolean isAllowSameNameSiblings() {
		return allowSameNameSiblings;
	}

	public boolean isAutoCreated() {
		return autoCreated;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public String getName() {
		return name;
	}

	public boolean allowsSameNameSiblings() {
		return allowSameNameSiblings;
	}

	public NodeType getDefaultPrimaryType() {
		throw new UnsupportedOperationException();
	}

	public NodeType[] getRequiredPrimaryTypes() {
		throw new UnsupportedOperationException();
	}

	public NodeType getDeclaringNodeType() {
		throw new UnsupportedOperationException();
	}

	public int getOnParentVersion() {
		return opv;
	}

	public boolean isProtected() {
		return protectedStatus;
	}

	public String getDefaultPrimaryTypeName(){
		return defaultPrimaryType;
	}
	
	public String[] getRequiredPrimaryTypeNames(){
		return requiredPrimaryTypes;
	}

	public NodeDefImpl toNodeDef(NamespaceResolver resolver, QName declaringNodeType) throws IllegalNameException, UnknownPrefixException {
		NodeDefImpl def = new NodeDefImpl();
		
		//common
		def.setName(QName.fromJCRName(getName(), resolver));
		def.setDeclaringNodeType(declaringNodeType);
		def.setAutoCreated(isAutoCreated());
		def.setOnParentVersion(getOnParentVersion());
		def.setProtected(isProtected());
		def.setMandatory(isMandatory());
		
		//specific
		if (this.defaultPrimaryType != null){
			def.setDefaultPrimaryType(QName.fromJCRName(this.defaultPrimaryType, resolver));
		}
		if (this.requiredPrimaryTypes != null){
			QName[] types = new QName[requiredPrimaryTypes.length];
			for(int i = 0 ; i < types.length ; i++){
				types[i] = QName.fromJCRName(requiredPrimaryTypes[i], resolver);
			}
			def.setRequiredPrimaryTypes(types);
		}
		def.setAllowsSameNameSiblings(allowsSameNameSiblings());
		
		return def;
	}
}
