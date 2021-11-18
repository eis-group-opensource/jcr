/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.security;

public interface BaseSecurityPermission {

    public String getPermissionName();

    public String getColumnName() ;

	public String getExportName() ;
	
	public BaseSecurityPermission[] getDirectParents();
	
	public BaseSecurityPermission[] getAllParents();

	public BaseSecurityPermission copy();
	
    public boolean isDirect();

}
