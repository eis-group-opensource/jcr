/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype.v283;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.InvalidConstraintException;
import com.exigen.cm.jackrabbit.nodetype.NodeDefImpl;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.PropDefImpl;

public class NodeTypeDefinitionImpl implements NodeTypeTemplate{

	private ArrayList<NodeDefinitionTemplate> childNodes = new ArrayList<NodeDefinitionTemplate>();
	private ArrayList<String> supertypes = new ArrayList<String>();
	private ArrayList<PropertyDefinitionTemplate> properties = new ArrayList<PropertyDefinitionTemplate>();
	private String name;
	private String primaryItemName;
	private boolean orderable = false;
	private boolean mixin = false;
	private boolean indexed = false;
	private boolean fts = false;
	
	public NodeDefinition[] getDeclaredChildNodeDefinitions() {
		return childNodes.toArray(new NodeDefinition[childNodes.size()]);
	}

	public PropertyDefinition[] getDeclaredPropertyDefinitions() {
		return properties.toArray(new PropertyDefinition[properties.size()]);
	}

	public String[] getDeclaredSupertypeNames() {
		return supertypes.toArray(new String[supertypes.size()]);
	}

	public String getName() {
		return name;
	}

	public String getPrimaryItemName() {
		return primaryItemName;
	}

	public boolean hasOrderableChildNodes() {
		return orderable;	
	}

	public boolean isMixin() {
		return mixin;
	}

	public boolean isIndexed() {
		return indexed;
	}

	public boolean isFTS() {
		return fts ;
	}

	public List getNodeDefintionTemplates() {
		return childNodes;
	}

	public List getPropertyDefintionTemplates() {
		return properties;
	}

	public void setDeclaredSuperTypeNames(String[] names) {
		supertypes.clear();
		supertypes.addAll(Arrays.asList(names));
	}

	public void setMixin(boolean mixin) {
		this.mixin = mixin;
	}

	public void setName(String name) {
		this.name = name;
		
	}

	public void setOrderableChildNodes(boolean orderable) {
		this.orderable = orderable;
		
	}

	public void setPrimaryItemName(String name) {
		this.name = name;
	}

	public void setFTS(boolean fts){
		this.fts = fts;
	}
	
	public void setIndexed(boolean indexed){
		this.indexed = indexed;
	}
	
	
	public NodeTypeDef toNodeTypeDef(NamespaceResolver resolver) throws RepositoryException{
		NodeTypeDef def = new NodeTypeDef();
		try {
			def.setName(QName.fromJCRName(getName(), resolver));
			ArrayList<QName> names = new ArrayList<QName>();
			for(String n:supertypes){
				names.add(QName.fromJCRName(n, resolver));
			}
			def.setSupertypes(names.toArray(new QName[names.size()]));
			def.setMixin(isMixin());
			def.setOrderableChildNodes(hasOrderableChildNodes());
			if (getPrimaryItemName() != null){
				def.setPrimaryItemName(QName.fromJCRName(getPrimaryItemName(), resolver));
			}
			
			//convert child names
			ArrayList<NodeDefImpl> childDefs = new ArrayList<NodeDefImpl>();
			for(NodeDefinitionTemplate nd: this.childNodes){
				NodeDefinitionTemplateImpl nti = (NodeDefinitionTemplateImpl) nd;
				childDefs.add(nti.toNodeDef(resolver, def.getName()));
			}
			//convert properties
			ArrayList<PropDefImpl> propDefs = new ArrayList<PropDefImpl>();
			for(PropertyDefinitionTemplate nd: this.properties){
				PropertyDefinitionTemplateImpl nti = (PropertyDefinitionTemplateImpl) nd;
				propDefs.add(nti.toPropDef(resolver, def.getName()));
			}
			def.setPropertyDefs((PropDefImpl[]) propDefs.toArray(new PropDefImpl[propDefs.size()]));
			def.setChildNodeDefs((NodeDefImpl[]) childDefs.toArray(new NodeDefImpl[childDefs.size()]));
			
		} catch (IllegalNameException e) {
			throw new RepositoryException("Error building NodeTypeDef object :"+e.getMessage());
		} catch (UnknownPrefixException e) {
			throw new RepositoryException("Error building NodeTypeDef object :"+e.getMessage());
		} catch (InvalidConstraintException e) {
			throw new RepositoryException("Error building NodeTypeDef object :"+e.getMessage());
		}
		return def;
	}
}
