/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.security;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SecurityHelper {

	public static BaseSecurityPermission[] getAllParents(BaseSecurityPermission permission) {
		Set<BaseSecurityPermission> permissions = getAllParentList(permission);
		BaseSecurityPermission[] result = new BaseSecurityPermission[permissions.size()];
		int i = 0 ;
		for(Iterator<BaseSecurityPermission> it = permissions.iterator();it.hasNext();){
			BaseSecurityPermission p = it.next();
			result[i++] = p.copy();
		}
		
		return result;
	}

	
	private static Set<BaseSecurityPermission> getAllParentList(BaseSecurityPermission permission) {
		HashSet<BaseSecurityPermission> result = new HashSet<BaseSecurityPermission>();
		for(BaseSecurityPermission p : permission.getDirectParents()){
			result.add(p);
			result.addAll(getAllParentList(p));
		}
		return result;
	}

}
