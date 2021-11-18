/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import com.exigen.cm.impl.ItemId;
import com.exigen.cm.impl.NodeId;
import com.exigen.cm.jackrabbit.name.Path;

public interface ItemState {

    NodeId getParentId() throws ItemNotFoundException, AccessDeniedException, RepositoryException;
    Long getParentNodeId() throws ItemNotFoundException, AccessDeniedException, RepositoryException;

    public ItemId getId();

    abstract boolean isNode();
    
    Path getPrimaryPath()throws RepositoryException ;

    String getParentUUID() throws AccessDeniedException, RepositoryException ;


}


/*
 * $Log: ItemState.java,v $
 * Revision 1.1  2007/04/26 08:58:59  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/11/03 13:09:19  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.5  2006/10/17 10:46:57  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.4  2006/05/22 14:48:07  dparhomenko
 * PTR#1801941 add observationsupport
 *
 */