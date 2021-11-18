/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.virtual;



/**
 * This Interface defines a virtual item state provider.
 */
public interface VirtualItemStateProvider {

//extends ItemStateManager, ItemStateListener {

    /**
     * Checks if the id refers to the root of a virtual tree.
     *
     * @param id
     * @return
     */
//    boolean isVirtualRoot(ItemId id);

    /**
     * Returns the id of the root node of the virtual tree.
     *
     * @return
     */
    //NodeId getVirtualRootId();

    /**
     * Creats a new virtual property state
     *
     * @param parent
     * @param name
     * @param type
     * @param multiValued
     * @return
     * @throws RepositoryException
     */
/*    VirtualPropertyState createPropertyState(VirtualNodeState parent,
                                                    QName name, int type,
                                                    boolean multiValued)
            throws RepositoryException;
*/
    /**
     * Creates a new virtual node state
     *
     * @param parent
     * @param name
     * @param uuid
     * @param nodeTypeName
     * @return
     * @throws RepositoryException
     */
/*    VirtualNodeState createNodeState(VirtualNodeState parent, QName name,
                                            String uuid, QName nodeTypeName)
            throws RepositoryException;
*/
    /**
     * Informs this provider that the node references to one of its states has
     * changed.
     *
     * @param refs
     * @return <code>true</code> if the reference target is one of its items.
     */
    //boolean setNodeReferences(NodeReferences refs);

}
