/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.security;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.builder.ToStringBuilder;

public class CustomSecurityPermission implements BaseSecurityPermission{

	Long id;
	
    private String permissionName;

    private String columnName;

	private String exportName;
	
	BaseSecurityPermission[] subPermissions;
	
	private boolean direct = false;

	   public CustomSecurityPermission(Long id, String permissionName, String columnName, 
	            String exportName, BaseSecurityPermission[] subPermissions) {
	       this(id, permissionName, columnName, exportName, subPermissions, false);
	   }

	
		public CustomSecurityPermission(Long id, String permissionName, String columnName, 
		        String exportName, BaseSecurityPermission[] subPermissions, boolean direct) {
			super();
			this.id = id;
			this.permissionName = permissionName;
			this.columnName = columnName;
			this.exportName = exportName;
			this.subPermissions = subPermissions;
			this.direct = direct;
		}

		public CustomSecurityPermission(Long id, String permissionName, String columnName, 
		        String exportName, boolean direct) {
		       this(id, permissionName, columnName, exportName, null, false);
		}

	CustomSecurityPermission(){
		
	}
	
	public CustomSecurityPermission copy(){
		CustomSecurityPermission result = instantiate();
		internalCopy(result);
		return result;
	}

	protected CustomSecurityPermission instantiate() {
		return new CustomSecurityPermission();
	}

	protected void internalCopy(CustomSecurityPermission other) {
		other.id = id;
		other.permissionName = permissionName;
		other.columnName = columnName;
		other.exportName = exportName;
		other.subPermissions = new BaseSecurityPermission[subPermissions.length];
		System.arraycopy(subPermissions, 0, other.subPermissions, 0, subPermissions.length);
		
	}

	public String getColumnName() {
		return columnName.toUpperCase();
	}

	

	public String getExportName() {
		return exportName;
	}

	public Long getId() {
		return id;
	}

	public String getPermissionName() {
		return permissionName;
	}

	public String getSubPermissionsAsString() {
		StringBuffer sb = new StringBuffer();
		for(int i= 0 ; i < subPermissions.length ; i++){
			sb.append(subPermissions[i].getExportName());
			if (i < subPermissions.length-1){
				sb.append(",");
			}
		}
		return sb.toString();
	}

	public static BaseSecurityPermission[] parseSubPermissions(String pp, List<BaseSecurityPermission> permissionList) {
		ArrayList<BaseSecurityPermission> result = new ArrayList<BaseSecurityPermission>();
		StringTokenizer tt = new StringTokenizer(pp, ",");
		while (tt.hasMoreTokens()){
			String p = tt.nextToken();
			for(BaseSecurityPermission p1: permissionList){
				if (p1.getExportName().equals(p)){
//					BaseSecurityPermission sp = p1;
					result.add(p1);
					break;
				}
			}
		}
		return (BaseSecurityPermission[]) result.toArray(new BaseSecurityPermission[result.size()]);
	}

	public BaseSecurityPermission[] getAllParents() {
		return SecurityHelper.getAllParents(this);
	}

	public BaseSecurityPermission[] getDirectParents() {
		return subPermissions;
	}

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", getExportName());
        builder.append("parents", subPermissions);
        return builder.toString();
    }

    public boolean isDirect() {
        return direct;
    }	


	
}
