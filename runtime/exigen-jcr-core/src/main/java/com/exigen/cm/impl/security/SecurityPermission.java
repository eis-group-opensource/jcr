/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.security;


public enum SecurityPermission implements BaseSecurityPermission{
    BROWSE("browse", "P_BROWSE", "browse"), 
    READ("read", "P_READ", "read", new SecurityPermission[]{BROWSE}), 
    ADD_NODE("add_node", "P_ADD_NODE","addNode", new SecurityPermission[]{BROWSE}), 
    SET_PROPERTY("set_property", "P_SET_PROPERTY", "setProperty", new SecurityPermission[]{READ}), 
    REMOVE("remove", "P_REMOVE","remove", new SecurityPermission[]{SET_PROPERTY}), 
    X_GRANT("x_grant", "P_X_GRANT","grant", new SecurityPermission[]{BROWSE}),
    X_UNLOCK("x_unlock", "P_X_UNLOCK","unlock", new SecurityPermission[]{BROWSE}),
    X_SUPER_DENY("x_super_deny", "P_X_SUPER_DENY","superDeny");

    private String permissionName;

    private String columnName;

	private String exportName;
	
	private SecurityPermission[] parents;

    SecurityPermission(String permissionName, String columnName, String exportName) {
        this.permissionName = permissionName;
        this.columnName = columnName;
        this.exportName = exportName;
        parents = new SecurityPermission[0];
    }

    SecurityPermission(String permissionName, String columnName, String exportName, SecurityPermission[] parents) {
        this.permissionName = permissionName;
        this.columnName = columnName;
        this.exportName = exportName;
        this.parents= parents;
    }

    public static SecurityPermission findPermission(String name){
        for(SecurityPermission p:values()){
            if (p.getPermissionName().equals(name)){
                return p;
            }
        }
        throw new RuntimeException("Permission with name "+name+" not found");
    }
    public String getPermissionName() {
        return permissionName;
    }

    public String getColumnName() {
        return columnName;
    }

	public String getExportName() {
		return exportName;
	}
	
	public BaseSecurityPermission[] getDirectParents(){
		SecurityPermission[] result = new SecurityPermission[parents.length];
		System.arraycopy(parents, 0, result, 0, parents.length);
		return result;
	}
	
	public BaseSecurityPermission[] getAllParents(){
		return SecurityHelper.getAllParents(this);
	}

	public BaseSecurityPermission copy() {
		return this;
	}
	
    public boolean isDirect() {
        return false;
    }   


}

/*
 * $Log: SecurityPermission.java,v $
 * Revision 1.4  2008/07/22 09:06:26  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/06/13 10:51:31  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2008/04/29 10:55:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:02  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2006/08/10 10:26:08  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.4  2006/07/05 07:24:50  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/06/02 07:21:29  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.2  2006/04/20 11:42:48  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:47:08  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/10 11:30:29  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.1  2006/04/06 13:09:31  dparhomenko
 * PTR#0144983 optimization
 *
 */