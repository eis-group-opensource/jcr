/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import com.exigen.cm.impl.security.BaseSecurityPermission;
import com.exigen.cm.impl.security.SecurityPrincipal;

public class SecurityModificationEntry {

    public static int SET_PERMISSION = 1;
    public static int REMOVE_ACE = 2;
    public static int RESET = 0;
    
    private int action;
    
    private Boolean value;
    
    private BaseSecurityPermission permission;
    private Long nodeId;
    private Long valueParent;
    private SecurityPrincipal principal;
	private String valueFrom;
	private boolean direct;
    
    public String getValueFrom() {
		return valueFrom;
	}

	@Deprecated
    public SecurityModificationEntry(Long nodeId, int action, String userId, String groupId, String contextid, BaseSecurityPermission permission, Boolean value, Long valueParent, String valueFrom, boolean direct) {
        this(nodeId, action, SecurityPrincipal.create(userId, groupId, contextid), permission, value, valueParent, valueFrom, direct);
    }

    public SecurityModificationEntry(Long nodeId, int action, SecurityPrincipal principal, BaseSecurityPermission permission, Boolean value, Long valueParent, String valueFrom, boolean direct) {
        this.nodeId =nodeId;
        this.action = action;
        this.value = value;
        this.principal = principal;
        this.permission = permission;
        this.valueParent = valueParent;
        this.valueFrom = valueFrom;
        this.direct= direct;
    }

    public int getAction() {
        return action;
    }

    public SecurityPrincipal getPrincipal(){
        return principal;
    }
    
    @Deprecated
    public String getGroupId() {
        if (principal.isGroup()){
            return principal.getName();
        } else{
            return null;
        }
    }

    public BaseSecurityPermission getPermission() {
        return permission;
    }

    @Deprecated
    public String getUserId() {
        if (principal.isUser()){
            return principal.getName();
        } else{
            return null;
        }
    }

    public Boolean getValue() {
        return value;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public Long getValueParent() {
        return valueParent;
    }

	public boolean isDirect() {
		return direct;
	}

	public String getContextId() {
		return principal.getContextId();
	}

}


/*
 * $Log: SecurityModificationEntry.java,v $
 * Revision 1.5  2008/12/08 13:34:11  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2008/11/27 12:52:28  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/07/15 11:27:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2008/06/11 10:07:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/06/22 12:00:25  dparhomenko
 * PTR#0146672 move operations
 *
 * Revision 1.1  2006/06/15 13:18:02  dparhomenko
 * PTR#0146580 fix sns remove on root node
 *
 */