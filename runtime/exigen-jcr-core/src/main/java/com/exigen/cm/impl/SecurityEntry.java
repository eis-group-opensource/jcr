/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.TABLE_ACE2___FROM_SUFFIX;
import static com.exigen.cm.Constants.TABLE_ACE2___PARENT_SUFFIX;
import static com.exigen.cm.Constants.TABLE_ACE2___SEQUENCE_SUFFIX;
import static com.exigen.cm.Constants.TABLE_ACE__CONTEXT_ID;
import static com.exigen.cm.Constants.TABLE_ACE__GROUP_ID;
import static com.exigen.cm.Constants.TABLE_ACE__USER_ID;
import static com.exigen.cm.Constants.TABLE_ACE___DIRECT_SUFFIX;
import static com.exigen.cm.Constants.TABLE_NODE__SECURITY_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.impl.security.BaseSecurityPermission;
import com.exigen.cm.impl.security.RepositorySecurityManager;
import com.exigen.cm.impl.security.SecurityPermission;
import com.exigen.cm.impl.security.SecurityPrincipal;
import com.exigen.cm.impl.state2._AbstractsStateManager;

public class SecurityEntry {

    /** Log for this class */
    private static final Log log = LogFactory.getLog(SecurityEntry.class);

	private _AbstractsStateManager stateManager;

	private Long securityId;

	private HashMap<BaseSecurityPermission, PermissionEntry> entries = new HashMap<BaseSecurityPermission, PermissionEntry>();

	private Long id;
	
	SecurityPrincipal principal;
	
	private List<SecurityPrincipal> restrictions = new ArrayList<SecurityPrincipal>();
	

	public SecurityEntry(RowMap row, _AbstractsStateManager stateManager, List<BaseSecurityPermission> permissions, RepositorySecurityManager securityManager, DatabaseConnection conn) throws RepositoryException {
		this.stateManager = stateManager;
		this.securityId = row.getLong(TABLE_NODE__SECURITY_ID);
		this.id = row.getLong(FIELD_ID);

		String _userId = row.getString(TABLE_ACE__USER_ID);
		String _groupId = row.getString(TABLE_ACE__GROUP_ID);
		String _contextId = row.getString(TABLE_ACE__CONTEXT_ID);
		if (_userId != null){
		    principal = new SecurityPrincipal.UserPrincipal(_userId, _contextId);
		} else {
		    principal = new SecurityPrincipal.GroupPrincipal(_groupId, _contextId);
		}
		

		
		for (BaseSecurityPermission p : permissions) {
			Boolean value = row.getBoolean(p.getColumnName());
            Long parentId = row.getLong(p.getColumnName() + TABLE_ACE2___PARENT_SUFFIX);
            Long secquenceId = row.getLong(p.getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX);
            String fromPerm = row.getString(p.getColumnName() + TABLE_ACE2___FROM_SUFFIX);
            Boolean direct = row.getBoolean(p.getColumnName() + TABLE_ACE___DIRECT_SUFFIX);
			entries.put(p, new PermissionEntry(p, value, parentId, findFromPermission(permissions, fromPerm), direct));
		}
		
		//TODO should be improved
		 List<RowMap> _r = securityManager.getRestrictions(conn, id);
		 for(RowMap r:_r){
		     restrictions.add(SecurityPrincipal.create(r.getString(TABLE_ACE__USER_ID), r.getString(TABLE_ACE__GROUP_ID), null));
		 }

	}

	private static BaseSecurityPermission findFromPermission(List<BaseSecurityPermission> permissions, String fromPerm) {
		if (fromPerm == null){
			return null;
		}
		for(BaseSecurityPermission p:permissions){
			if (p.getExportName().equals(fromPerm)){
				return p;
			}
		}
		return null;
	}

	public SecurityEntry(SecurityModificationEntry sme, _AbstractsStateManager stateManager) {
		this.stateManager = stateManager;
		this.securityId = sme.getNodeId();

		this.principal = sme.getPrincipal();

		for (SecurityPermission p : SecurityPermission.values()) {
			Boolean value = null;
			Long parentId = null;
			entries.put(p, new PermissionEntry(p, value, parentId, null, false));
		}
		modifyValue(sme.getPermission(), sme.getValue(), sme.getValueParent());
	}

	@Deprecated
	public String getGroupId() {
	    if (principal.isGroup()){
	        return principal.getName();
	    } else {
	        return null;
	    }
	}

	@Deprecated
	public Boolean getAddNodePermission() {
		return getPermission(SecurityPermission.ADD_NODE);
	}

	@Deprecated
	public Boolean getGrantPermission() {
		return getPermission(SecurityPermission.X_GRANT);
	}

	@Deprecated
	public Boolean getReadPermission() {
		return getPermission(SecurityPermission.READ);
	}

	@Deprecated
	public Boolean getRemovePermission() {
		return getPermission(SecurityPermission.REMOVE);
	}

	@Deprecated
	public Boolean getSetPropertyPermission() {
		return getPermission(SecurityPermission.SET_PROPERTY);
	}

	@Deprecated
	public String getUserId() {
	      if (principal.isUser()){
	            return principal.getName();
	        } else {
	            return null;
	        }

	}

	public boolean isUserEntry() {
		return principal.isUser();
	}

	public boolean isGroupEntry() {
		return principal.isGroup();
	}

	public String getPrincipal() {
		return principal.getName();
	}

	public Node getSecurityOwner() throws RepositoryException {
		return buildNode(this.securityId);
	}

	private Node buildNode(Long nodeID) throws RepositoryException {
		return new NodeImpl(stateManager.getNodeState(nodeID, null), stateManager);
	}

	public Boolean getPermission(BaseSecurityPermission p) {
		PermissionEntry entry = entries.get(p);
		return entry.getValue();
	}

    public Boolean isDirectPermission(BaseSecurityPermission p) {
        PermissionEntry entry = entries.get(p);
        return entry.isDirect();
    }
	
	public Long getPermissionParentId(BaseSecurityPermission p)
			throws RepositoryException {
		PermissionEntry entry = entries.get(p);
		return entry.getParentId();
	}

	public String getPermissionFromAsString(BaseSecurityPermission p)
			throws RepositoryException {
		PermissionEntry entry = entries.get(p);
		BaseSecurityPermission r = entry.getFromPermission();
		return r == null ? null:r.getExportName();
	}

	
	public BaseSecurityPermission getFromPermission(BaseSecurityPermission p) {
		PermissionEntry entry = entries.get(p);
		return entry.getFromPermission();
		}	

	public Node getPermissionParent(BaseSecurityPermission p)
			throws RepositoryException {
		PermissionEntry entry = entries.get(p);
		if (entry.getParentId() == null){
		    return null;
		} else {
		    return buildNode(entry.getParentId());
		}
	}

	@Override
	public String toString() {
		ToStringBuilder sb = new ToStringBuilder(this);
		if (isGroupEntry()) {
			sb.append("group", getGroupId());
		} else {
			sb.append("user", getUserId());
		}
		for (PermissionEntry sp : entries.values()) {
			Boolean v = sp.getValue();
			if (v != null) {
				try {
					sb.append(sp.getPermission().getPermissionName(), v
							+ ", parent:" + sp.getParentId());
				} catch (Exception exc) {
					log.debug(exc.getMessage());
				}
			}
		}
		if (restrictions.size() > 0){
		    sb.append("restrictions", restrictions);
		}
		return sb.toString();
	}

	public Long getSecurityId() {
		return this.securityId;
	}

	public void modifyValue(BaseSecurityPermission permission, Boolean value, Long valueParent) {
		if (permission != null){
			PermissionEntry e = entries.get(permission);
			if (e != null){
				e.changeValue(value, valueParent);
			}
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Deprecated
	public String getContextId() {
		return principal.getContextId();
	}
	
	public SecurityPrincipal getPrincipalEntry(){
	    return principal;
	}


	public SecurityPrincipal[] getRestrictions(){
	    return (SecurityPrincipal[]) restrictions.toArray(new SecurityPrincipal[restrictions.size()]);
	}




}

class PermissionEntry {

	private BaseSecurityPermission permission;

	private Boolean value;

	private Long parentId;
	
	private BaseSecurityPermission from;

	private Boolean direct;
	
	public Long getParentId() {
		return this.parentId;
	}

	public Boolean isDirect() {
        return direct;
    }

    public BaseSecurityPermission getFromPermission() {
		return from;
	}

	public void changeValue(Boolean value2, Long valueParent) {
		this.value = value2;
		this.parentId = valueParent;
	}

	public BaseSecurityPermission getPermission() {
		return this.permission;
	}

	public Boolean getValue() {
		return this.value;
	}

	public PermissionEntry(BaseSecurityPermission permission, Boolean value,
			Long parentId, BaseSecurityPermission from, Boolean direct) {
		super();
		this.permission = permission;
		this.value = value;
		this.parentId = parentId;
		this.from = from;
		this.direct = direct;
	}

}

/*
 * $Log: SecurityEntry.java,v $
 * Revision 1.11  2008/12/08 13:34:11  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.10  2008/11/27 12:52:28  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.9  2008/07/17 06:57:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2008/07/16 13:06:43  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/07/15 11:27:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2008/07/02 08:52:54  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2008/07/02 08:07:53  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2008/06/11 10:07:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/06/02 11:36:11  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2008/04/29 10:55:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.12  2006/12/15 11:54:37  dparhomenko
 * PTR#1803217 code reorganization
 *
 * Revision 1.11  2006/12/07 15:38:51  dparhomenko
 * PTR#0149339 fix security merge on node move
 *
 * Revision 1.10  2006/11/14 07:37:19  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.9  2006/09/07 10:36:54  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.8  2006/08/15 09:44:48  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.7  2006/08/10 10:26:06  dparhomenko
 * PTR#1802383 implement copy in workspace
 * Revision 1.6 2006/06/22 12:00:25 dparhomenko
 * PTR#0146672 move operations
 * 
 * Revision 1.5 2006/06/15 13:18:02 dparhomenko PTR#0146580 fix sns remove on
 * root node
 * 
 * Revision 1.4 2006/06/02 08:33:43 dparhomenko PTR#1802035 fix version history
 * 
 * Revision 1.3 2006/06/02 07:21:28 dparhomenko PTR#1801955 add new security
 * 
 * Revision 1.2 2006/04/20 11:42:46 zahars PTR#0144983 Constants and JCRHelper
 * moved to com.exigen.cm
 * 
 * Revision 1.1 2006/04/17 06:46:37 dparhomenko PTR#0144983 restructurization
 * 
 * Revision 1.1 2006/04/06 13:09:28 dparhomenko PTR#0144983 optimization
 * 
 * Revision 1.1 2006/03/29 12:56:19 dparhomenko PTR#0144983 optimization
 * 
 */