/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinitionTemplate;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.QName;

/**
 * This class implements the <code>NodeDef</code> interface and additionally
 * provides setter methods for the various node definition attributes.
 */
public class NodeDefImpl extends ItemDefImpl implements NodeDef {

    /**
     * The name of the default primary type.
     */
    private QName defaultPrimaryType = null;

    /**
     * The names of the required primary types.
     */
    private QName[] requiredPrimaryTypes = new QName[] { QName.NT_BASE };

    /**
     * The 'allowsSameNameSiblings' flag.
     */
    private boolean allowsSameNameSiblings = false;

    /**
     * The identifier of this node definition. The identifier is lazily computed
     * based on the characteristics of this node definition and reset on every
     * attribute change.
     */

    private Long sqlId;
    
   // private boolean modified = false;

	private NodeDefId nodeDefId;

    
    /**
     * Default constructor.
     */
    public NodeDefImpl() {
    }

    /**
     * Sets the name of default primary type.
     *
     * @param defaultNodeType
     */
    public void setDefaultPrimaryType(QName defaultNodeType) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        this.defaultPrimaryType = defaultNodeType;
        nodeDefId = null;

    }

    /**
     * Sets the names of the required primary types.
     *
     * @param requiredPrimaryTypes
     */
    public void setRequiredPrimaryTypes(QName[] requiredPrimaryTypes) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        if (requiredPrimaryTypes == null) {
            throw new IllegalArgumentException("requiredPrimaryTypes can not be null");
        }
        this.requiredPrimaryTypes = requiredPrimaryTypes;
        nodeDefId = null;

    }

    /**
     * Sets the 'allowsSameNameSiblings' flag.
     *
     * @param allowsSameNameSiblings
     */
    public void setAllowsSameNameSiblings(boolean allowsSameNameSiblings) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        this.allowsSameNameSiblings = allowsSameNameSiblings;
        nodeDefId = null;
    }

    //------------------------------------------------< ItemDefImpl overrides >
    /**
     * {@inheritDoc}
     */
    public void setDeclaringNodeType(QName declaringNodeType) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        super.setDeclaringNodeType(declaringNodeType);
        nodeDefId = null;
    }

    /**
     * {@inheritDoc}
     */
    public void setName(QName name) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        super.setName(name);
        nodeDefId = null;

    }

    /**
     * {@inheritDoc}
     */
    public void setAutoCreated(boolean autoCreated) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        super.setAutoCreated(autoCreated);
        nodeDefId = null;
    }

    /**
     * {@inheritDoc}
     */
    public void setOnParentVersion(int onParentVersion) {
        // reset id field in order to force lazy recomputation of identifier
       // modified = true;
        super.setOnParentVersion(onParentVersion);
        nodeDefId = null;

    }

    /**
     * {@inheritDoc}
     */
    public void setProtected(boolean writeProtected) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        super.setProtected(writeProtected);
        nodeDefId = null;

    }

    /**
     * {@inheritDoc}
     */
    public void setMandatory(boolean mandatory) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        super.setMandatory(mandatory);
        nodeDefId = null;

    }

    //--------------------------------------------------------------< NodeDef >
    /**
     * {@inheritDoc}
     * <p/>
     * The identifier is computed based on the characteristics of this property
     * definition, i.e. modifying attributes of this property definition will
     * have impact on the identifier returned by this method.
     */
    /*public Long getId() {
        return getSQLId();
    }*/
    
    public Serializable getTempId(){
        return new NodeDefId(this);
    }

    /**
     * {@inheritDoc}
     */
    public QName getDefaultPrimaryType() {
        return defaultPrimaryType;
    }

    /**
     * {@inheritDoc}
     */
    public QName[] getRequiredPrimaryTypes() {
        return requiredPrimaryTypes;
    }

    /**
     * {@inheritDoc}
     */
    public boolean allowsSameNameSiblings() {
        return allowsSameNameSiblings;
    }

    /**
     * {@inheritDoc}
     *
     * @return always <code>true</code>
     */
    public boolean definesNode() {
        return true;
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Compares two node definitions for equality. Returns <code>true</code>
     * if the given object is a node defintion and has the same attributes
     * as this node definition.
     *
     * @param obj the object to compare this node definition with
     * @return <code>true</code> if the object is equal to this node definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeDefImpl) {
            NodeDefImpl other = (NodeDefImpl) obj;
            return super.equals(obj)
                    && Arrays.equals(requiredPrimaryTypes, other.requiredPrimaryTypes)
                    && (defaultPrimaryType == null
                            ? other.defaultPrimaryType == null
                            : defaultPrimaryType.equals(other.defaultPrimaryType))
                    && allowsSameNameSiblings == other.allowsSameNameSiblings;
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }
    public Long getSQLId() {
        return sqlId;
    }

    public void setSQLId(Long id) {
        this.sqlId = id;
        
    }
    
    public String toStirng(){
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", getName());
        return builder.toString();
    }

    public void configure(QName name, boolean _protected, boolean mandatory, boolean autocreated, int onParentVersion, boolean sns) {
        super.configure(name, _protected, mandatory, autocreated, onParentVersion);
        this.allowsSameNameSiblings = sns;
        //QName name, boolean _protected, boolean mandatory, boolean autocreated, int onParentVersion
        
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The identifier is computed based on the characteristics of this property
     * definition, i.e. modifying attributes of this property definition will
     * have impact on the identifier returned by this method.
     */
    public NodeDefId getNodeDefId() {
        if (nodeDefId == null) {
            // generate new identifier based on this node definition
        	nodeDefId = new NodeDefId(this);
        }
        return nodeDefId;
    }

	public NodeDefinitionTemplate createNodeTemplate(
			NodeDefinitionTemplate template,
			NamespaceRegistryImpl nsRegistry) throws RepositoryException {
		try {
			template.setAutoCreated(isAutoCreated());
			template.setDefaultPrimaryType(getDefaultPrimaryType().toJCRName(nsRegistry));
			template.setMandatory(isMandatory());
			template.setName(getName().toJCRName(nsRegistry));
			template.setOnParentVersion(getOnParentVersion());
			template.setProtected(isProtected());
			template.setSameNameSiblings(this.allowsSameNameSiblings);
			
			template.setRequiredPrimaryTypes(QName.convertArrayToArray(getRequiredPrimaryTypes(), nsRegistry));
		} catch (NoPrefixDeclaredException exc){
			throw new RepositoryException(exc); 
		}
		
		return template;
	}
	
	public int generateHashCode(){
		HashCodeBuilder builder = getBuilder();
		builder.append(allowsSameNameSiblings);
		builder.append(defaultPrimaryType);
		ArrayList<QName> rt = new ArrayList<QName>(Arrays.asList(requiredPrimaryTypes));
		Collections.sort(rt);
		builder.append(rt);
		return builder.toHashCode();
	}


}
