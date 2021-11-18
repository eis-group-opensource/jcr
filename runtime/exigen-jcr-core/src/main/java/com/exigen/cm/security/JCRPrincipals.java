/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
/**
 * 
 */
package com.exigen.cm.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.impl.WorkspaceImpl;

/**
 * Conatiner for the principals - userId and groups (user belongs to)
 * userId could be null - for "non-authorized environment"
 * groups could be null
 * 
 *
 */
public class JCRPrincipals {
	
	private String userId;
	private Collection<String> groupIdList;
	private Collection<String> contextIdList;
    private boolean ignoreUserCase;
	private WorkspaceImpl externalWorkspace;

	public JCRPrincipals assignToSession(WorkspaceImpl workspace){
		JCRPrincipals result = new JCRPrincipals(userId, groupIdList, contextIdList, ignoreUserCase);
		result.externalWorkspace = workspace;
		return result;
	}
	
    public JCRPrincipals(String userId, String[] groupIdList , String[] contextIdList, boolean ignoreUserCase){
        this(userId, groupIdList == null? null: Arrays.asList(groupIdList), contextIdList == null? null: Arrays.asList(contextIdList), ignoreUserCase);
    }

    
	public JCRPrincipals(String userId, Collection<String> groupIdList , Collection<String> contextIdList, boolean ignoreUserCase){
        if (groupIdList == null){
            groupIdList = new ArrayList<String>();
        }
		this.userId = userId;
		this.ignoreUserCase = ignoreUserCase;
		if (ignoreUserCase){
			this.userId = this.userId.toUpperCase();
		}
		this.groupIdList = groupIdList;
		this.contextIdList = contextIdList;
		if (ignoreUserCase){
			this.userId = this.userId.toUpperCase();
			ArrayList<String> _groups = new ArrayList<String>();
			for(String group:groupIdList){
				_groups.add(group.toUpperCase());
			}
			this.groupIdList = _groups;
		}
        if (this.contextIdList == null){
            this.contextIdList = new ArrayList<String>();
        }
	}

	public Collection<String> getGroupIdList() {
		return groupIdList;
	}

	public String getUserId() {
		/*if ("".equals(userId)){
			return "unknown";
		}*/
		return userId;
	}

	public Collection<String> getContextIdList() {
		if (externalWorkspace != null){
			Set<String> extenalIds = externalWorkspace._getSession().getContextIds();
			if(extenalIds.size() > 0){
				ArrayList<String> result = new ArrayList<String>(contextIdList);
				result.addAll(extenalIds);
				return result;
			} else {
				return contextIdList;
			}
		} else {
			return contextIdList;
		}
		
		
	}

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("user", userId);
        builder.append("groups", groupIdList);
        builder.append("contexts", contextIdList);
        return builder.toString();
    }

    public boolean isIgnoreUserCase() {
        return ignoreUserCase;
    }
    
    @Override
    public boolean equals(Object obj) {
    	if (obj instanceof JCRPrincipals){
    		JCRPrincipals other = (JCRPrincipals) obj;
    		EqualsBuilder b = new EqualsBuilder();
    		b.append(userId, other.getUserId());
    		b.append(groupIdList.toArray(), other.groupIdList.toArray());
    		b.append(contextIdList.toArray(), other.contextIdList.toArray());
    		
    		return b.isEquals();
    	} else {
    		return false;
    	}
    	
    }
    
    

}
