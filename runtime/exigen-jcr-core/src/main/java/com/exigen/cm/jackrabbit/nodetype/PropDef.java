/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype;

import javax.jcr.RepositoryException;

import com.exigen.cm.jackrabbit.value.InternalValue;

//import org.apache.jackrabbit.core.value.InternalValue;

/**
 * <code>PropDef</code> is the internal representation of
 * a property definition. It refers to <code>QName</code>s only
 * and is thus isolated from session-specific namespace mappings.
 *
 * @see javax.jcr.nodetype.PropertyDefinition
 */
public interface PropDef extends ItemDef {

    public static final PropDef[] EMPTY_ARRAY = new PropDef[0];

    /**
     * Returns an identifier for this property definition.
     *
     * @return an identifier for this property definition.
     */
    //Long getId();

    /**
     * Returns the required type.
     *
     * @return the required type.
     */
    int getRequiredType();

    /**
     * Returns the array of value constraints.
     *
     * @return the array of value constraints.
     */
    ValueConstraint[] getValueConstraints();

    /**
     * Returns the array of default values.
     *
     * @return the array of default values.
     * @throws RepositoryException 
     */
    InternalValue[] getDefaultValues();// throws RepositoryException;

    /**
     * Reports whether this property can have multiple values.
     *
     * @return the 'multiple' flag.
     */
    boolean isMultiple();

    public Long getSQLId();

    String getColumnName();

    boolean isUnstructured();
    
    boolean isIndexable();

    boolean isFullTextSearch();

	//Serializable getTempId();

	PropDefId getPropDefId();
}
