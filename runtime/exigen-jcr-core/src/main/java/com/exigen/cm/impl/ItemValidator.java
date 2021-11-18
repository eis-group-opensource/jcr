/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeRegistry;
import com.exigen.cm.jackrabbit.version.NodeStateEx;

/**
 * Utility class for validating an item against constraints
 * specified by its definition.
 */
public class ItemValidator {

    /**
     * Log instance for this class
     */
    private Log log = LogFactory.getLog(ItemValidator.class);
    
    /**
     * node type registry
     */
    protected final NodeTypeRegistry ntReg;

    /**
     * hierarchy manager used for generating error msg's
     * that contain human readable paths
     *
     * @see #safeGetJCRPath(ItemId)
     */
    //protected final HierarchyManager hierMgr;
    /**
     * namespace resolver used for generating error msg's
     * that contain human readable paths
     *
     * @see #safeGetJCRPath(Path)
     */
    protected final NamespaceResolver nsResolver;

    /**
     * Creates a new <code>ItemValidator</code> instance.
     *
     * @param ntReg      node type registry
     * @param hierMgr    hierarchy manager
     * @param nsResolver namespace resolver
     */
    public ItemValidator(NodeTypeRegistry ntReg,
                         
                         NamespaceResolver nsResolver) {
        //HierarchyManager hierMgr,
        this.ntReg = ntReg;
        //this.hierMgr = hierMgr;
        this.nsResolver = nsResolver;
    }

    /**
     * Checks whether the given node state satisfies the constraints specified
     * by its primary and mixin node types. The following validations/checks are
     * performed:
     * <ul>
     * <li>check if its node type satisfies the 'required node types' constraint
     * specified in its definition</li>
     * <li>check if all 'mandatory' child items exist</li>
     * <li>for every property: check if the property value satisfies the
     * value constraints specified in the property's definition</li>
     * </ul>
     *
     * @param nodeState state of node to be validated
     * @throws ConstraintViolationException if any of the validations fail
     * @throws RepositoryException          if another error occurs
     */
    /*public void validate(NodeState nodeState)
            throws ConstraintViolationException, RepositoryException {
        // effective primary node type
        EffectiveNodeType entPrimary =
                ntReg.getEffectiveNodeType(nodeState.getNodeTypeName());
        // effective node type (primary type incl. mixins)
        EffectiveNodeType entPrimaryAndMixins = getEffectiveNodeType(nodeState);
        NodeDef def = ntReg.getNodeDef(nodeState.getDefinitionId());

        // check if primary type satisfies the 'required node types' constraint
        QName[] requiredPrimaryTypes = def.getRequiredPrimaryTypes();
        for (int i = 0; i < requiredPrimaryTypes.length; i++) {
            if (!entPrimary.includesNodeType(requiredPrimaryTypes[i])) {
                String msg = safeGetJCRPath(nodeState.getNodeId())
                        + ": missing required primary type "
                        + requiredPrimaryTypes[i];
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        // mandatory properties
        PropDef[] pda = entPrimaryAndMixins.getMandatoryPropDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            if (!nodeState.hasPropertyName(pd.getName())) {
                String msg = safeGetJCRPath(nodeState.getNodeId())
                        + ": mandatory property " + pd.getName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        // mandatory child nodes
        NodeDef[] cnda = entPrimaryAndMixins.getMandatoryNodeDefs();
        for (int i = 0; i < cnda.length; i++) {
            NodeDef cnd = cnda[i];
            if (!nodeState.hasChildNodeEntry(cnd.getName())) {
                String msg = safeGetJCRPath(nodeState.getNodeId())
                        + ": mandatory child node " + cnd.getName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
    }*/

    /**
     * Checks whether the given property state satisfies the constraints
     * specified by its definition. The following validations/checks are
     * performed:
     * <ul>
     * <li>check if the type of the property values does comply with the
     * requiredType specified in the property's definition</li>
     * <li>check if the property values satisfy the value constraints
     * specified in the property's definition</li>
     * </ul>
     *
     * @param propState state of property to be validated
     * @throws ConstraintViolationException if any of the validations fail
     * @throws RepositoryException          if another error occurs
     */
    //public void validate(PropertyState propState)
    //        throws ConstraintViolationException, RepositoryException {
        /*PropDef def = ntReg.getPropDef(propState.getDefinitionId());
        InternalValue[] values = propState.getValues();
        int type = PropertyType.UNDEFINED;
        for (int i = 0; i < values.length; i++) {
            if (type == PropertyType.UNDEFINED) {
                type = values[i].getType();
            } else if (type != values[i].getType()) {
                throw new ConstraintViolationException(safeGetJCRPath(propState.getPropertyId())
                        + ": inconsistent value types");
            }
            if (def.getRequiredType() != PropertyType.UNDEFINED
                    && def.getRequiredType() != type) {
                throw new ConstraintViolationException(safeGetJCRPath(propState.getPropertyId())
                        + ": requiredType constraint is not satisfied");
            }
        }
        EffectiveNodeType.checkSetPropertyValueConstraints(def, values);*/
       // throw new UnsupportedOperationException();
    //}

    //-------------------------------------------------< misc. helper methods >
    /**
     * Helper method that builds the effective (i.e. merged and resolved)
     * node type representation of the specified node's primary and mixin
     * node types.
     *
     * @param nodeState
     * @return the effective node type
     * @throws RepositoryException
     */
    /*public EffectiveNodeType getEffectiveNodeType(NodeState nodeState)
            throws RepositoryException {
        // mixin types
        Set set = nodeState.getMixinTypeNames();
        QName[] types = new QName[set.size() + 1];
        set.toArray(types);
        // primary type
        types[types.length - 1] = nodeState.getNodeTypeName();
        try {
            return ntReg.getEffectiveNodeType(types);
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node "
                    + safeGetJCRPath(nodeState.getNodeId());
            log.debug(msg);
            throw new RepositoryException(msg, ntce);
        }
    }*/

    /**
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @param path path to convert
     * @return JCR path
     */
    public String safeGetJCRPath(Path path) {
        try {
            return path.toJCRPath(nsResolver);
        } catch (NoPrefixDeclaredException npde) {
            log.error("failed to convert " + path.toString() + " to JCR path.");
            // return string representation of internal path as a fallback
            return path.toString();
        }
    }

    /**
     * Failsafe translation of internal <code>ItemId</code> to JCR path for use
     * in error messages etc.
     *
     * @param id id to translate
     * @return JCR path
     */
    public String safeGetJCRPath(Long id) {
        return "[NodeId:"+id+"]";
    }
    
    public String safeGetJCRPath(NodeStateEx state) throws RepositoryException {
    	return state.safeGetJCRPath();
    }
    
    public String safeGetJCRPath(ItemId id) {
    	return id.toString();
    }
}
