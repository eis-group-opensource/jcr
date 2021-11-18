/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype;

import java.util.ArrayList;
import java.util.Collections;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.value.InternalValue;

/**
 * This class implements the <code>PropDef</code> interface and additionally
 * provides setter methods for the various property definition attributes.
 */
public class PropDefImpl extends ItemDefImpl implements PropDef {

    /**
     * The required type.
     */
    private int requiredType = PropertyType.UNDEFINED;

    /**
     * The value constraints.
     */
    private ValueConstraint[] valueConstraints = ValueConstraint.EMPTY_ARRAY;

    /**
     * The default values.
     */
    private InternalValue[] defaultValues = InternalValue.EMPTY_ARRAY;

    /**
     * The 'multiple' flag
     */
    private boolean multiple = false;

    /**
     * The identifier of this property definition. The identifier is lazily
     * computed based on the characteristics of this property definition and
     * reset on every attribute change.
     */
    //private boolean modified = false;

    private Long sqlId;
    //private Long id;

    private String columnName;
    
    private boolean indexable = false;
    private boolean fullTextSearch = false;
    
    /**
     * Default constructor.
     */
    public PropDefImpl() {
    }

    /**
     * Sets the required type
     *
     * @param requiredType
     */
    public void setRequiredType(int requiredType) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        this.requiredType = requiredType;
        propDefId = null;

    }

    /**
     * Sets the value constraints.
     *
     * @param valueConstraints
     */
    public void setValueConstraints(ValueConstraint[] valueConstraints) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        if (valueConstraints != null) {
            this.valueConstraints = valueConstraints;
        } else {
            this.valueConstraints = ValueConstraint.EMPTY_ARRAY;
        }
        propDefId = null;

    }

    /**
     * Sets the default values.
     *
     * @param defaultValues
     */
    public void setDefaultValues(InternalValue[] defaultValues) {
        // reset id field in order to force lazy recomputation of identifier
        //id = null;
        if (defaultValues != null) {
            this.defaultValues = defaultValues;
        } else {
            this.defaultValues = InternalValue.EMPTY_ARRAY;
        }
        propDefId = null;
    }

    /**
     * Sets the 'multiple' flag.
     *
     * @param multiple
     */
    public void setMultiple(boolean multiple) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        this.multiple = multiple;
        propDefId = null;

    }

    //------------------------------------------------< ItemDefImpl overrides >
    /**
     * {@inheritDoc}
     */
    public void setDeclaringNodeType(QName declaringNodeType) {
        // reset id field in order to force lazy recomputation of identifier
       // modified = true;
        super.setDeclaringNodeType(declaringNodeType);
        propDefId = null;
    }

    /**
     * {@inheritDoc}
     */
    public void setName(QName name) {
        // reset id field in order to force lazy recomputation of identifier
       // modified = true;
        super.setName(name);
        propDefId = null;

    }

    /**
     * {@inheritDoc}
     */
    public void setAutoCreated(boolean autoCreated) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        super.setAutoCreated(autoCreated);
        propDefId = null;
    }

    /**
     * {@inheritDoc}
     */
    public void setOnParentVersion(int onParentVersion) {
        // reset id field in order to force lazy recomputation of identifier
        //modified = true;
        super.setOnParentVersion(onParentVersion);
        propDefId = null;

    }

    /**
     * {@inheritDoc}
     */
    public void setProtected(boolean writeProtected) {
        // reset id field in order to force lazy recomputation of identifier
       // modified = true;
        super.setProtected(writeProtected);
        propDefId = null;

    }

    /**
     * {@inheritDoc}
     */
    public void setMandatory(boolean mandatory) {
        // reset id field in order to force lazy recomputation of identifier
       // modified = true;
        super.setMandatory(mandatory);
        propDefId = null;

    }

    //--------------------------------------------------------------< PropDef >
    /**
     * {@inheritDoc}
     * <p/>
     * The identifier is computed based on the characteristics of this property
     * definition, i.e. modifying attributes of this property definition will
     * have impact on the identifier returned by this method.
     */
    /*public Long getId() {
        if (id == null) {
            // generate new identifier based on this property definition
            StringBuffer sb = new StringBuffer();

            sb.append(getDeclaringNodeType().toString());
            sb.append('/');
            if (definesResidual()) {
                sb.append('*');
            } else {
                sb.append(getName().toString());
            }
            sb.append('/');
            sb.append(getRequiredType());
            sb.append('/');
            sb.append(isMultiple() ? 1 : 0);

            id = new Long(sb.toString().hashCode());
        }
        return id;
        
    }*/
    
    /*public Serializable getTempId(){
    	return new PropDefId(this);
    } */   

    /**
     * {@inheritDoc}
     */
    public int getRequiredType() {
        return requiredType;
    }

    /**
     * {@inheritDoc}
     */
    public ValueConstraint[] getValueConstraints() {
        return valueConstraints;
    }

    /**
     * {@inheritDoc}
     * @throws RepositoryException 
     */
    public InternalValue[] getDefaultValues(){
    	InternalValue[] r = new InternalValue[defaultValues.length];
    	int pos = 0;
    	for(InternalValue v:defaultValues){
    		try {
				r[pos++] = v.createCopy();
			} catch (RepositoryException e) {
				throw new RuntimeException(e);
			}
    	}
        return r;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * {@inheritDoc}
     *
     * @return always <code>false</code>
     */
    public boolean definesNode() {
        return false;
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Compares two property definitions for equality. Returns <code>true</code>
     * if the given object is a property defintion and has the same attributes
     * as this property definition.
     *
     * @param obj the object to compare this property definition with
     * @return <code>true</code> if the object is equal to this property definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PropDefImpl) {
            PropDefImpl other = (PropDefImpl) obj;
            
            EqualsBuilder builder = new EqualsBuilder();
            builder.append(requiredType, other.requiredType);
            builder.append(valueConstraints, other.valueConstraints);
            builder.append(defaultValues, other.defaultValues);
            builder.append(multiple, other.multiple);
            
            //builder.append(getColumnName(), other.getColumnName());
            
            return super.equals(obj) && builder.isEquals();
            //&& Arrays.equals(defaultValues, other.defaultValues)
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

    public void setSQLId(Long id) {
        this.sqlId = id;
        
    }

    public Long getSQLId() {
        return sqlId;
    }

    
    public String getColumnName() {
        //if (columnName == null){
            /*if (!isMultiple() && getName().getLocalName().indexOf("*")<0 && getRequiredType() != PropertyType.UNDEFINED){
                String result = "X_"+getName().getLocalName().toUpperCase();
                columnName = result;
                //return columnName;
            }
            //return null;
        }*/
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
        this.isUnstructure = null;
        propDefId = null;
    }
    
    private Boolean isUnstructure = null;

	private PropDefId propDefId;
    
    public boolean isUnstructured(){
    	if (isUnstructure == null){
    		isUnstructure = false;
	    	if (getRequiredType() == PropertyType.UNDEFINED || isMultiple()){
	    		isUnstructure = true;
	    	}  
	        if (getColumnName() == null || getColumnName().length()==0){
	        	isUnstructure = true;
	        }
	        if (getName().getLocalName().indexOf("*")>=0){
	        	isUnstructure = true;
	        }
    	}
        return isUnstructure;
    }

    
    public String toString(){
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", getName());
        builder.append("fromType", getDeclaringNodeType());
        builder.append("columnName", getColumnName());
        return builder.toString();
    }

    public boolean isIndexable() {
        return indexable;
    }

    public void setIndexable(boolean indexable) {
        this.indexable = indexable;
    }

    public boolean isFullTextSearch() {
        return fullTextSearch;
    }

    public void setFullTextSearch(boolean fullTestSearch) {
        this.fullTextSearch = fullTestSearch;
        propDefId = null;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The identifier is computed based on the characteristics of this property
     * definition, i.e. modifying attributes of this property definition will
     * have impact on the identifier returned by this method.
     */
    public PropDefId getPropDefId() {
        if (propDefId == null) {
            // generate new identifier based on this property definition
        	propDefId = new PropDefId(this);
        }
        return propDefId;
    }

	public PropertyDefinitionTemplate createPropertyTemplate(
			PropertyDefinitionTemplate template,
			NamespaceRegistryImpl nsRegistry) throws RepositoryException {

		try {
			template.setAutoCreated(isAutoCreated());
			template.setMandatory(isMandatory());
			template.setMultiple(isMultiple());
			template.setName(getName().toJCRName(nsRegistry));
			template.setOnParentVersion(this.getOnParentVersion());
			template.setProtected(isProtected());
			template.setRequiredType(getRequiredType());
	
			ArrayList<Value> values = new ArrayList<Value>();
			for(InternalValue v: getDefaultValues()){
				values.add(v.toJCRValue(nsRegistry));
			}
			template.setDefaultValues((Value[]) values.toArray(new Value[values.size()]));
			ArrayList<String> tmp = new ArrayList<String>();
			for(ValueConstraint c:valueConstraints){
				tmp.add(c.getDefinition());
			}
			template.setValueConstarints((String[]) tmp.toArray(new String[tmp.size()]));
			return template;
		} catch (NoPrefixDeclaredException exc){
			throw new RepositoryException(exc);
		}
	}
	
	public int generateHashCode(){
		HashCodeBuilder builder = getBuilder();
		builder.append(multiple);
		builder.append(requiredType);
		//default values
		ArrayList<String> iv = new ArrayList<String>();
		for(InternalValue v:defaultValues){
			iv.add(v.toString());
		}
		Collections.sort(iv);
		builder.append(iv);
		//valueconstraints
		ArrayList<String> vc = new ArrayList<String>();
		for(ValueConstraint v:valueConstraints){
			vc.add(v.getDefinition());
		}
		Collections.sort(vc);
		builder.append(vc);
		return builder.toHashCode();
	}
	
}
