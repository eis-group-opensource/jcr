/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import com.exigen.cm.impl.security.SecurityPermission;
import com.exigen.cm.impl.security.SessionSecurityManager;

public class AccessManager {

    /**
     * READ permission constant
     */
    //static int READ = 1;

    /**
     * WRITE permission constant
     */
    //static int WRITE = 2;

    /**
     * REMOVE permission constant
     */
    //static int REMOVE = 4;

	//private WorkspaceImpl workspace;

	private SessionSecurityManager securityManager;

	public AccessManager(WorkspaceImpl workspace) {
		//this.workspace = workspace;
		//this.userId = workspace.getSession().getUserID();
		//this.groups = workspace.principals.getGroupIdList();
		this.securityManager = ((SessionImpl)workspace.getSession()).getSecurityManager(); 
	}

	public boolean isGranted(Long nodeId, SecurityPermission permission) {
		try {
			securityManager.checkPermission(nodeId, permission.getPermissionName());
		} catch (Exception exc){
			return false;
		}
		return true;
		
	}

	public boolean isGranted(PropertyId propId, SecurityPermission permission) {
		return true;
	}

}
