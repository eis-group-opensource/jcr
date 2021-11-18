/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import java.util.ArrayList;

import javax.jcr.RepositoryException;

import com.exigen.cm.jackrabbit.name.NamespaceResolver;

public interface _StateManager {

	public _NodeState getNodeState(Long id, ArrayList<Long> readAheadIds) throws RepositoryException;

	public NamespaceResolver getNamespaceResolver();



}
