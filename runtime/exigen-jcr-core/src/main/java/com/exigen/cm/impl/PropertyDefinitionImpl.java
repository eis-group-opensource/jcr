/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.JCRHelper;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.nodetype.ValueConstraint;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class PropertyDefinitionImpl extends ItemDefinitionImpl implements PropertyDefinition{

    private PropDef def;

    public PropertyDefinitionImpl(QName name, boolean _protected, boolean mandatory, boolean autocreated, int onParentVersion, NamespaceResolver nsResolver) throws RepositoryException {
        super(name, _protected, mandatory, autocreated, onParentVersion, nsResolver);
    }

    public PropertyDefinitionImpl(NodeTypeImpl nodeType, PropDef def, NamespaceResolver nsResolver) throws RepositoryException {
        super(nodeType, def, nsResolver);
        this.def = def;
    }

    public int getRequiredType() {
        return def.getRequiredType();
    }

    public String[] getValueConstraints() {
        ValueConstraint[] constraints = def.getValueConstraints();
        if (constraints == null || constraints.length == 0) {
            return new String[0];
        }
        String[] vca = new String[constraints.length];
        for (int i = 0; i < constraints.length; i++) {
            vca[i] = constraints[i].getDefinition(nsResolver);
        }
        return vca;
    }

    public Value[] getDefaultValues() {
    	InternalValue[] vals = def.getDefaultValues();
    	if (vals.length == 0){
    		return new Value[]{};
    	} else {
    		try {
				return JCRHelper.getValues(def.getDefaultValues(), nsResolver);
			} catch (RepositoryException e) {
				throw new RuntimeException(e);
			}
    	}
    }

    public boolean isMultiple() {
        return def.isMultiple();
    }

    public Long getSQLId() {
        return def.getSQLId();
    }

    public boolean isUnstructured() {
        return def.isUnstructured();
    }

    public String getColumnName() {
        return def.getColumnName();
    }

    /**
     * Returns the wrapped property definition.
     *
     * @return the wrapped property definition.
     */
    public PropDef unwrap() {
        return def;
    }

    
    public String toString(){
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", getQName());
        builder.append("mulitple", isMultiple());
        builder.append("type", getRequiredType());
        return builder.toString();
    }
}


/*
 * $Log: PropertyDefinitionImpl.java,v $
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2007/03/02 09:31:58  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.3  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.2  2006/04/20 12:15:08  dparhomenko
 * PTR#0144983 stored procedure for Hypersonic check security
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/03/27 14:57:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.3  2006/03/03 10:33:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.2  2006/02/27 15:02:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/16 13:53:04  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */