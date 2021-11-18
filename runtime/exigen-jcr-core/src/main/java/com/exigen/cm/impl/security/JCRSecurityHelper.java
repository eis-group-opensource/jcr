/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.security;

import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.security.JCRPrincipals;

public class JCRSecurityHelper {

    public static Boolean validateSecurityPermission(Long nodeId, List<RowMap> rows, JCRPrincipals principals, BaseSecurityPermission permission) throws RepositoryException {
    	boolean contextUserAllow = false;
    	boolean contextUserDeny = false;
    	boolean contextGroupAllow = false;
    	boolean contextGroupDeny = false;
    
    	boolean userAllow = false;
    	boolean userDeny = false;
    	boolean groupAllow = false;
    	boolean groupDeny = false;
    	for(RowMap row: rows){
    	    
    		//1.check super deny
    		Boolean sdValue = row.getBoolean(SecurityPermission.X_SUPER_DENY.getColumnName());
    		if (sdValue != null && sdValue){
    			return false;
    		}
    		//check real permission
    		Boolean value = row.getBoolean(permission.getColumnName());
    		if (value == null){
    			//value not set
    			continue;
    		}
    		
            Long securityId = row.getLong(Constants.TABLE_NODE__SECURITY_ID);
            boolean direct = row.getBoolean(permission.getColumnName()+Constants.TABLE_ACE___DIRECT_SUFFIX);
    		if (direct && !securityId.equals(nodeId)){
    		    continue;
    		}
    		
    		String groupId = row.getString(Constants.TABLE_ACE__GROUP_ID);
    		String contextid = row.getString(Constants.TABLE_ACE__CONTEXT_ID); 
    		if (groupId != null) {
    			//group permission
    			if (contextid != null){
    				if (value){
    					contextGroupAllow = true;
    				} else {
    					contextGroupDeny = true;
    				}
    			} else {
    				if (value){
    					groupAllow = true;
    				} else {
    					groupDeny = true;
    				}
    				
    			}
    			continue;
    		}
    		
    		String userId = row.getString(Constants.TABLE_ACE__USER_ID);
    		if (userId != null){
    			//user permission
    			if (contextid != null){
    				if (value){
    					contextUserAllow = true;
    				} else {
    					contextUserDeny = true;
    					//TODO ??? do we allow return at this positttion ???
    					return false;
    				}
    			} else {
    				if (value){
    					userAllow = true;
    				} else {
    					userDeny = true;
    				}
    				
    			}
    		}
    	}
    
    	//context permissions
    	if (contextUserDeny){
    		//TODO return false on all rows processing
    		return false;
    	}
    	
    	if (contextUserAllow && ! contextGroupDeny){
    		return true;
    	}
    
    	if (contextGroupDeny){
    		return false;
    	}
    	
    	if (contextGroupAllow){
    		return true;
    	}
    	
    	//normal permissions
    	if (userDeny){
    		return false;
    	}
    	
    	if (userAllow){ //changed 03.07.2008 ( && !groupDeny)
    		return true;
    	}
    	
    	if (groupDeny){
    		return false;
    	}
    	
    	if (groupAllow){
    		return true;
    	}
    	
    	return null;
    	
    }

}
