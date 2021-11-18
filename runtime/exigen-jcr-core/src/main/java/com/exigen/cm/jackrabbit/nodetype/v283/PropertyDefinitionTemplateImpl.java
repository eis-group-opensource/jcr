/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype.v283;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.InvalidConstraintException;
import com.exigen.cm.jackrabbit.nodetype.PropDefImpl;
import com.exigen.cm.jackrabbit.nodetype.ValueConstraint;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class PropertyDefinitionTemplateImpl implements PropertyDefinitionTemplate{

	private boolean autoCreated;
	private Value[] defaultValues;
	private boolean mandatory;
	private boolean multiple;
	private String name;
	private int opv;
	private boolean protectedStatus;
	private int type;
	private String[] constraints;

	public void setAutoCreated(boolean autoCreated) {
		this.autoCreated = autoCreated;
	}

	public void setDefaultValues(Value[] defaultValues) {
		this.defaultValues = defaultValues;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
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

	public void setRequiredType(int type) {
		this.type = type;
	}

	public void setValueConstarints(String[] constraints) {
		this.constraints = constraints;
	}

	public boolean isAutoCreated() {
		return autoCreated;
	}

	public String[] getConstraints() {
		return constraints;
	}

	public Value[] getDefaultValues() {
		return defaultValues;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public String getName() {
		return name;
	}


	public int getType() {
		return type;
	}

	public PropDefImpl toPropDef(NamespaceResolver resolver, QName declaringNodeType) throws IllegalNameException, UnknownPrefixException, InvalidConstraintException, ValueFormatException, RepositoryException {
		PropDefImpl def = new PropDefImpl();
		
		//common
		def.setName(QName.fromJCRName(getName(), resolver));
		def.setDeclaringNodeType(declaringNodeType);
		def.setAutoCreated(isAutoCreated());
		def.setOnParentVersion(getOnParentVersion());
		def.setProtected(isProtected());
		def.setMandatory(isMandatory());
		
		def.setRequiredType(this.type);
		if (this.constraints != null){
			ValueConstraint[] vc = new ValueConstraint[constraints.length];
			for (int i = 0 ; i < vc.length ; i++){
				vc[i] = ValueConstraint.create(type, constraints[i], resolver);
			}
			def.setValueConstraints(vc);			
		}
		if (defaultValues != null){
			InternalValue[] dv = new InternalValue[defaultValues.length];
			for(int i = 0 ; i < dv.length ; i++){
				dv[i] = InternalValue.create(defaultValues[i], resolver, null); 
			}
			def.setDefaultValues(dv);
		}
		def.setMultiple(isMultiple());
		
		
		return def; 
	}

	private boolean isProtected() {
		return protectedStatus;
	}

	private int getOnParentVersion() {
		return opv;
	}
}
