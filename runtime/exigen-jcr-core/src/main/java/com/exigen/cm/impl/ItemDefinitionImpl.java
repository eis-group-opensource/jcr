/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;

import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.ItemDef;

public class ItemDefinitionImpl implements ItemDefinition {

    protected NodeTypeImpl declaringNodeType;
    private boolean _protected;
    private QName name;
    private final boolean autocreated;
    private final boolean mandatory;
    private final int onParentVersion;
    protected final NamespaceResolver nsResolver;
    private String jcrName;


    public ItemDefinitionImpl(NodeTypeImpl nodeType, ItemDef def, NamespaceResolver nsResolver) throws RepositoryException {
        this(def.getName(), def.isProtected(), def.isMandatory(), def.isAutoCreated(), def.getOnParentVersion(), nsResolver);
        this.declaringNodeType = nodeType;

    }

    public ItemDefinitionImpl(QName name, boolean _protected, boolean mandatory, boolean autocreated, int onParentVersion, NamespaceResolver nsResolver) throws RepositoryException {
        this._protected = _protected;
        this.name = name;
        this.mandatory = mandatory;
        this.autocreated = autocreated;
        this.onParentVersion =onParentVersion;
        this.nsResolver = nsResolver;

        try {
            this.jcrName = name.toJCRName(nsResolver);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e);
        }
    }

    public NodeType getDeclaringNodeType() {
        return declaringNodeType;
    }

    public String getName() {
        return jcrName;
    }

    public boolean isAutoCreated() {
        return autocreated;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public int getOnParentVersion() {
        return onParentVersion;
    }

    public boolean isProtected() {
        return _protected;
    }
    
    public QName getQName() {
        return name;
    }    

}


/*
 * $Log: ItemDefinitionImpl.java,v $
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/03/27 14:57:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.2  2006/02/27 15:02:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/16 13:53:05  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */