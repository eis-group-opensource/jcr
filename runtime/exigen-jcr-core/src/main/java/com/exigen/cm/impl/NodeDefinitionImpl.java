/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.NodeDef;

public class NodeDefinitionImpl extends ItemDefinitionImpl implements NodeDefinition{

    /** Log for this class */
    private static final Log log = LogFactory.getLog(NodeDefinitionImpl.class);

    
    private NodeDef def;
    
    private NodeTypeManagerImpl manager;
    
    public NodeDefinitionImpl(NodeTypeManagerImpl manager, NodeTypeImpl nodeType, NodeDef def, NamespaceResolver nsResolver) throws RepositoryException {
        super(nodeType, def, nsResolver);
        this.def = def;
        this.manager = manager;
    }
    
    public NodeDefinitionImpl(QName name, boolean _protected, boolean mandatory, boolean autocreated, int onParentVersion, NamespaceResolver nsResolver) throws RepositoryException{
        super(name, _protected, mandatory, autocreated, onParentVersion, nsResolver);
    }

    public NodeType[] getRequiredPrimaryTypes() {
        QName[] ntNames = ((NodeDef) def).getRequiredPrimaryTypes();
        try {
            if (ntNames == null || ntNames.length == 0) {
                // return "nt:base"
                return new NodeType[] { manager.getNodeType(QName.NT_BASE) };
            } else {
                NodeType[] nodeTypes = new NodeType[ntNames.length];
                for (int i = 0; i < ntNames.length; i++) {
                    nodeTypes[i] = manager.getNodeType(ntNames[i]);
                }
                return nodeTypes;
            }
        } catch (NoSuchNodeTypeException e) {
            // should never get here
            log.error("required node type does not exist", e);
            return new NodeType[0];
        }
    }

    public NodeType getDefaultPrimaryType() {
        QName name = def.getDefaultPrimaryType();
        if (name != null){
            try {
                return manager.getNodeType(def.getDefaultPrimaryType());
            } catch (NoSuchNodeTypeException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
        
    }

    public boolean allowsSameNameSiblings() {
        return def.allowsSameNameSiblings();
    }

    public Long getSQLId() {
        return def.getSQLId();
    }

    /**
     * Returns the wrapped node definition.
     *
     * @return the wrapped node definition.
     */
    public NodeDef unwrap() {
        return (NodeDef) def;
    }


}


/*
 * $Log: NodeDefinitionImpl.java,v $
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.2  2006/04/19 08:06:46  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/03/27 14:57:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.5  2006/03/14 14:54:07  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.4  2006/03/03 10:33:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.3  2006/02/27 15:02:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.2  2006/02/20 15:32:25  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/16 13:53:04  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */