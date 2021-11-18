/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseCountStatement;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseLeftOuterJoin;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.Order;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.DatabaseCondition;
import com.exigen.cm.security.JCRPrincipals;

public class SecurityPostProcessor {

    private DatabaseConnection conn;
    private Long sequenceId;
    private HashMap<Long, Set<SecurityPrincipal>> changes = new HashMap<Long, Set<SecurityPrincipal>>();
    private HashMap<Long, Set<SecurityPrincipal>> changesBriwse = new HashMap<Long, Set<SecurityPrincipal>>();
    private RepositorySecurityManager securityManager;
    private JCRPrincipals userPrincipals;

    public SecurityPostProcessor(DatabaseConnection conn, RepositorySecurityManager securityManager, JCRPrincipals userPrincipals) throws RepositoryException {
        this.conn = conn;
        this.securityManager = securityManager;
        this.sequenceId = conn.nextId();
        this.userPrincipals = userPrincipals;
    }

    public void process() throws RepositoryException{
    	processBrowseRemove();
        //System.out.println("Security post process");
        SecurityPostProcessor postProcessor = new SecurityPostProcessor(conn, securityManager, userPrincipals);
        //postProcessor.sequenceId = -1L;
        for(Long securityId : changes.keySet()){
            //System.out.println("SecurityId  "+securityId);
            Set<SecurityPrincipal> set = changes.get(securityId);
            for(SecurityPrincipal p:set){
                //System.out.println("  "+p);
                DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, true);
                DatabaseLeftOuterJoin join = st.addLeftOuterJoin(Constants.TABLE_ACE, "ace", Constants.TABLE_NODE__SECURITY_ID, Constants.TABLE_NODE__SECURITY_ID);
                
                //principals conditions
                join.addCondition(Conditions.eq("ace."+ (p.isUser() ?  Constants.TABLE_ACE__USER_ID : Constants.TABLE_ACE__GROUP_ID), p.getName()));
                if (p.getContextId() != null){
                    join.addCondition(Conditions.eq("ace."+ Constants.TABLE_ACE__CONTEXT_ID, p.getContextId()));
                } else {
                    join.addCondition(Conditions.isNull("ace."+ Constants.TABLE_ACE__CONTEXT_ID));
                }
                
                //parents conditions
                //st.addCondition(Conditions.eqProperty(Constants.FIELD_ID, Constants.TABLE_NODE__SECURITY_ID));
                
                DatabaseSelectAllStatement stParents = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE_PARENT, true);
                stParents.addResultColumn(Constants.TABLE_NODE_PARENT__PARENT_ID);
                stParents.addCondition(Conditions.eq(Constants.FIELD_TYPE_ID, securityId));
                
                DatabaseCondition c1 = Conditions.in(Constants.FIELD_ID, stParents);
                DatabaseCondition c2 = Conditions.eq(Constants.FIELD_ID, securityId);
                st.addCondition(Conditions.or(c1, c2));
                
                //result columns
                st.addResultColumn(st.getRootAlias()+"."+Constants.TABLE_NODE__SECURITY_ID+" as SID");
                st.addResultColumn(st.getRootAlias()+"."+Constants.FIELD_ID+" as NID");
                st.addResultColumn(st.getRootAlias()+"."+Constants.TABLE_NODE__NODE_PATH);
                st.addResultColumn("ace.*");
                //order by depth
                st.addOrder(Order.desc(Constants.TABLE_NODE__NODE_DEPTH));
                
                st.execute(conn);

                List<RowMap> rows = st.getAllRows();
                
                RowMap mainRow = rows.get(0);
                Boolean _browse = mainRow.getBoolean(SecurityPermission.BROWSE.getColumnName());
                Boolean _read = mainRow.getBoolean(SecurityPermission.READ.getColumnName());
                
                boolean allowed = false; 
                if (_browse != null && _browse){
                    allowed = true;
                } else if (_browse == null && _read != null && _read){
                    allowed = true;
                }
                
                if (allowed){
                    //allow browse for all parents 
                    for(RowMap row :rows){
                        //System.out.println(row);
                        //check for read & browse permission
                        boolean read = parseBoolean(row.getBoolean(SecurityPermission.READ.getColumnName()));
                        boolean browse = parseBoolean(row.getBoolean(SecurityPermission.BROWSE.getColumnName()));
                        
                        if (!read && !browse){
                            //browse permission should be defined
                            //System.out.println("browse permission should be defined for "+p.getName());
                            Long nodeId = row.getLong("NID");
                            SecurityPermissionDefinition def = new SecurityPermissionDefinition(SecurityPermission.BROWSE, p, true, false, true);
                            securityManager.assignSecurity(conn, nodeId, nodeId, def , userPrincipals, postProcessor);
                        }
                        
                    }
                } else {
                    //cleanup permissions
                    //1. check child nodes for same principal permissions
                    DatabaseCountStatement st1 = new DatabaseCountStatement(Constants.TABLE_NODE);
                    //DatabaseSelectAllStatement st1 = new DatabaseSelectAllStatement(Constants.TABLE_NODE, true);
                    
                    st1.addJoin(Constants.TABLE_ACE, "ace", Constants.TABLE_NODE__SECURITY_ID, Constants.TABLE_NODE__SECURITY_ID);
                    //add user conditions
                    st1.addCondition(Conditions.eq("ace."+(p.isUser() ? Constants.TABLE_ACE__USER_ID : Constants.TABLE_ACE__GROUP_ID), p.getName()));
                    if (p.getContextId() != null){
                        st1.addCondition(Conditions.eq("ace."+Constants.TABLE_ACE__CONTEXT_ID, p.getName()));
                    } else {
                        st1.addCondition(Conditions.isNull("ace."+Constants.TABLE_ACE__CONTEXT_ID));
                    }
                    DatabaseCondition _c1 = Conditions.eq("ace."+SecurityPermission.BROWSE.getColumnName(), Boolean.TRUE);
                    DatabaseCondition _c2 = Conditions.eq("ace."+SecurityPermission.READ.getColumnName(), Boolean.TRUE);
                    st1.addCondition(Conditions.or(_c1,_c2));
                    //add parent conditions
                    DatabaseSelectAllStatement stParents2 = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE_PARENT, true);
                    stParents2.addResultColumn(Constants.FIELD_TYPE_ID);
                    stParents2.addCondition(Conditions.eq(Constants.TABLE_NODE_PARENT__PARENT_ID, securityId));
                    st1.addCondition(Conditions.in(Constants.FIELD_ID, stParents2));
                     
                    st1.execute(conn);
                    Long result = st1.getCount();
                    //List<RowMap> rows1 = st1.getAllRows();
                    
                    st1.close();
                    
                    //if exist set direct browse
                    if (result > 0){
                        SecurityPermissionDefinition def = new SecurityPermissionDefinition(SecurityPermission.BROWSE, p, true, false, true);
                        securityManager.assignSecurity(conn, securityId, securityId, def , userPrincipals, postProcessor);
                    } else {
                        //clean up parent permissions
                    }
                    
                }
                st.close();
            }
        }
    }

    private void processBrowseRemove() throws RepositoryException {
    	for(Long securityId : changesBriwse.keySet()){
            //System.out.println("SecurityId  "+securityId);
            Set<SecurityPrincipal> set = changesBriwse.get(securityId);
            for(SecurityPrincipal p:set){
            	//System.out.println("BROWSE REMOVE (post processor) "+securityId+"-"+p);
            	DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, true);
                DatabaseLeftOuterJoin join = st.addLeftOuterJoin(Constants.TABLE_ACE, "ace", Constants.TABLE_NODE__SECURITY_ID, Constants.TABLE_NODE__SECURITY_ID);
               // DatabaseLeftOuterJoin join2 = st.addLeftOuterJoin(Constants.TABLE_ACE2, "ace2", Constants.FIELD_ID, Constants.FIELD_TYPE_ID);
                
                //principals conditions
                join.addCondition(Conditions.eq("ace."+ (p.isUser() ?  Constants.TABLE_ACE__USER_ID : Constants.TABLE_ACE__GROUP_ID), p.getName()));
                if (p.getContextId() != null){
                    join.addCondition(Conditions.eq("ace."+ Constants.TABLE_ACE__CONTEXT_ID, p.getContextId()));
                } else {
                    join.addCondition(Conditions.isNull("ace."+ Constants.TABLE_ACE__CONTEXT_ID));
                }
                join.addCondition(Conditions.eq("ace."+SecurityPermission.BROWSE.getColumnName()+Constants.TABLE_ACE___DIRECT_SUFFIX, true));
                join.addCondition(Conditions.eq("ace."+SecurityPermission.BROWSE.getColumnName(), true));
                for(BaseSecurityPermission pp : securityManager.getAllPermissions()){
                	if (pp != SecurityPermission.BROWSE){
                		join.addCondition(Conditions.isNull("ace."+pp.getColumnName()));
                	}
                }
                
                //parents conditions
                //st.addCondition(Conditions.eqProperty(Constants.FIELD_ID, Constants.TABLE_NODE__SECURITY_ID));
                
                DatabaseSelectAllStatement stParents = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE_PARENT, true);
                stParents.addResultColumn(Constants.TABLE_NODE_PARENT__PARENT_ID);
                stParents.addCondition(Conditions.eq(Constants.FIELD_TYPE_ID, securityId));
                
                DatabaseCondition c1 = Conditions.in(Constants.FIELD_ID, stParents);
                DatabaseCondition c2 = Conditions.eq(Constants.FIELD_ID, securityId);
                st.addCondition(c1);//Conditions.or(c1, c2)
                
                //result columns
                st.addResultColumn(st.getRootAlias()+"."+Constants.TABLE_NODE__SECURITY_ID+" as SID");
                st.addResultColumn(st.getRootAlias()+"."+Constants.FIELD_ID+" as NID");
                st.addResultColumn(st.getRootAlias()+"."+Constants.TABLE_NODE__NODE_PATH);
                st.addResultColumn("ace.*");
                //st.addResultColumn("ace2.*");
                //order by depth
                st.addOrder(Order.desc(Constants.TABLE_NODE__NODE_DEPTH));
                
                st.execute(conn);

                List<RowMap> rows = st.getAllRows();
                HashMap<Long,Long> sids = new HashMap<Long,Long>();
                for(RowMap r:rows){
/*                	System.out.println(r.get("P_BROWSE")+":"+
                					   r.get("P_BROWSE_DIRECT")+":"+ 
                					  // r.get("P_BROWSE_PARENT")+":"+
                					  // r.get("P_BROWSE_SECQUENCE")+":"+
                					  // r.get("ID")+":"+
                					   " = "+ r);*/
                	sids.put(r.getLong("SID"), r.getLong(Constants.FIELD_ID));
                	//Long id = r.getLong("ID");
                	//DatabaseSelectOneStatement st0 = DatabaseTools.createSelectOneStatement(Constants.TABLE_ACE2, Constants.FIELD_TYPE_ID, id);
                	//st0.execute(conn);
                	//System.out.println(st0.getRow());
                	
                }
                
                //System.out.println("======================================");
                //find permission that are used
                HashSet<Long> sidsk = new HashSet<Long>(sids.keySet());
                for(Long id:sidsk){
                	DatabaseCountStatement st1 = DatabaseTools.createCountStatement(Constants.TABLE_ACE);
                	
                	DatabaseSelectAllStatement st0 = RepositorySecurityManager.createSelectAllStatement(id);
                	st1.addCondition(Conditions.in(Constants.TABLE_NODE__SECURITY_ID, st0));
                	
                	ArrayList<DatabaseCondition> cc = new ArrayList<DatabaseCondition>();
                    for(BaseSecurityPermission pp : securityManager.getAllPermissions()){
                    		cc.add(Conditions.notNull(pp.getColumnName()));
                    }
                    st1.addCondition(Conditions.or(cc));
                    st1.addCondition(Conditions.eq( (p.isUser() ?  Constants.TABLE_ACE__USER_ID : Constants.TABLE_ACE__GROUP_ID), p.getName()));
                    if (p.getContextId() != null){
                    	st1.addCondition(Conditions.eq(Constants.TABLE_ACE__CONTEXT_ID, p.getContextId()));
                    } else {
                    	st1.addCondition(Conditions.isNull( Constants.TABLE_ACE__CONTEXT_ID));
                    }
                    st1.addCondition(Conditions.not(Conditions.in(Constants.TABLE_NODE__SECURITY_ID, sids.keySet())));

                    //System.out.println(sids.keySet());
                    st1.execute(conn);
                    if (st1.getCount() > 0){
                    	sids.remove(id);
                    }
                	
                	//st1.execute(conn);
                	st1.close();
                }
                
                //remove permission 
                if (sids.size() > 0){
	            	DatabaseDeleteStatement st1 = DatabaseTools.createDeleteStatement(Constants.TABLE_ACE);
	            	st1.addCondition(Conditions.in(Constants.FIELD_ID, sids.values()));
	            	DatabaseDeleteStatement st2 = DatabaseTools.createDeleteStatement(Constants.TABLE_ACE2);
	            	st2.addCondition(Conditions.in(Constants.FIELD_TYPE_ID, sids.values()));
	            	
	            	
	            	st2.execute(conn);
	            	st1.execute(conn);
	            	
	            	st1.close();
	            	st2.close();
                }
            	st.close();
            	
            }
    	}
		
	}

	private boolean parseBoolean(Boolean value) {
        return value == null ? false : value.booleanValue();
    }

    public synchronized void registerUpdate(Long securityId, SecurityPrincipal principal) {
        if (!changes.containsKey(securityId)){
            changes.put(securityId, new HashSet<SecurityPrincipal>());
        }
        Set<SecurityPrincipal> set = changes.get(securityId);
        set.add(principal);
    }

    public Long getSequenceId() {
        return sequenceId;
    }

    public void addNewSecurityId(Long nodeId) {
        
    }

	public void registerBrowseRemove(Long securityId,
			SecurityPrincipal principal) {
		if (!changesBriwse.containsKey(securityId)){
			changesBriwse.put(securityId, new HashSet<SecurityPrincipal>());
        }
        Set<SecurityPrincipal> set = changesBriwse.get(securityId);
        set.add(principal);
		
	}

}
