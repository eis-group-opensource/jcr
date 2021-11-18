/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.security;

import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.FIELD_TYPE_ID;
import static com.exigen.cm.Constants.TABLE_ACE;
import static com.exigen.cm.Constants.TABLE_ACE2;
import static com.exigen.cm.Constants.TABLE_ACE2___FROM_SUFFIX;
import static com.exigen.cm.Constants.TABLE_ACE2___PARENT_SUFFIX;
import static com.exigen.cm.Constants.TABLE_ACE2___SEQUENCE_SUFFIX;
import static com.exigen.cm.Constants.TABLE_ACE_RESTRICTION;
import static com.exigen.cm.Constants.TABLE_ACE_RESTRICTION__ACE_ID;
import static com.exigen.cm.Constants.TABLE_ACE__GROUP_ID;
import static com.exigen.cm.Constants.TABLE_ACE__USER_ID;
import static com.exigen.cm.Constants.TABLE_NODE_PARENT;
import static com.exigen.cm.Constants.TABLE_NODE_PARENT__PARENT_ID;
import static com.exigen.cm.Constants.TABLE_NODE__SECURITY_ID;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import com.exigen.cm.Constants;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.CreateColumn;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseOperation;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.DatabaseCondition;
import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SchemaChanges;
import com.exigen.cm.impl.SecurityEntry;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.state2._AbstractsStateManager;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._SessionStateManager;
import com.exigen.cm.security.JCRPrincipals;

public class RepositorySecurityManager {

    private ArrayList<CustomSecurityPermission> customPersmissions = new ArrayList<CustomSecurityPermission>();
    
	private RepositoryImpl repository;
  
    public RepositorySecurityManager(RepositoryImpl repository) throws RepositoryException{
    	this.repository = repository;
    	//loadPermissions();
    }

    

	public List<BaseSecurityPermission> getAllPermissions() {
		List<BaseSecurityPermission> result = new ArrayList<BaseSecurityPermission>(Arrays.asList(SecurityPermission.values())); 
		synchronized (customPersmissions) {
			result.addAll(customPersmissions);
		}
		return result;
	}

    // Custom Permission management
	public void registerCustomPermission(CustomSecurityPermission p) throws RepositoryException{
		if (p.getId() == null){
			registerCustomPermissionInDatabase(p);
		}
		synchronized (customPersmissions) {
			customPersmissions.add(p);
		}
	}
	
	private void registerCustomPermissionInDatabase(CustomSecurityPermission p) throws RepositoryException{
		DatabaseConnection c = null;
		JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
		try {
			//create record in db
			c = repository.getConnectionProvider().createConnection(); 
			Long id = repository.nextId();
			DatabaseInsertStatement st = DatabaseTools.createInsertStatement(Constants.TABLE_ACE_PERMISSION);
			st.addValue(SQLParameter.create(Constants.FIELD_ID, id));
			st.addValue(SQLParameter.create(Constants.TABLE_ACE_PERMISSION__COLUMN_NAME, p.getColumnName()));
			st.addValue(SQLParameter.create(Constants.TABLE_ACE_PERMISSION__EXPORT_NAME, p.getExportName()));
			st.addValue(SQLParameter.create(Constants.TABLE_ACE_PERMISSION__PERMISSION_NAME, p.getPermissionName()));
            st.addValue(SQLParameter.create(Constants.TABLE_ACE_PERMISSION__SUB_PERMISSIONS, p.getSubPermissionsAsString()));
            st.addValue(SQLParameter.create(Constants.TABLE_ACE_PERMISSION__DIRECT, p.isDirect()));
			st.execute(c);
			//modify ACE table
			
			SchemaChanges changes = new SchemaChanges(c);
			List<TableDefinition> allDefs = repository.getStaticTableDefenitions(null);
			TableDefinition tableDef = null;
			for(TableDefinition td : allDefs){
				if (td.getTableName().equals(Constants.TABLE_ACE)){
					tableDef = td;
					break;
				}
			}

			
            ColumnDefinition c1 = new ColumnDefinition(tableDef, p.getColumnName(),Types.BOOLEAN);
            DatabaseOperation op1 = new CreateColumn(Constants.TABLE_ACE, c1);
            changes.add(op1);

            ColumnDefinition c2 = new ColumnDefinition(tableDef, p.getColumnName()+Constants.TABLE_ACE___DIRECT_SUFFIX, Types.BOOLEAN);
            DatabaseOperation op2 = new CreateColumn(Constants.TABLE_ACE, c2);
            changes.add(op2);

	         for(TableDefinition td : allDefs){
	                if (td.getTableName().equals(Constants.TABLE_ACE2)){
	                    tableDef = td;
	                    break;
	                }
	            }

			
	            c1 = new ColumnDefinition(tableDef, p.getColumnName()+TABLE_ACE2___PARENT_SUFFIX, Types.INTEGER);
                op1 = new CreateColumn(Constants.TABLE_ACE2, c1);
                changes.add(op1);
	            c1 = new ColumnDefinition(tableDef, p.getColumnName()+TABLE_ACE2___FROM_SUFFIX, Types.VARCHAR);
                op1 = new CreateColumn(Constants.TABLE_ACE2, c1);
                changes.add(op1);
	            c1 = new ColumnDefinition(tableDef, p.getColumnName()+TABLE_ACE2___SEQUENCE_SUFFIX, Types.INTEGER);
	            op1 = new CreateColumn(Constants.TABLE_ACE2, c1);
	            changes.add(op1);
			
			
			changes.execute(c);
			
			
			c.commit();
			TransactionHelper.getInstance().commitAndResore(tr);
			p.id = id;
		} catch (Exception e) {
			TransactionHelper.getInstance().rollbackAndResore(tr);
			throw new RepositoryException("Error creating custom permission",e);
		} finally {
			if (c != null){
				c.close();
			}
		}
		
	}


    public void loadPermissions() throws RepositoryException{
    	synchronized (customPersmissions) {
    		customPersmissions.clear();
		}
    	JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
    	DatabaseConnection c = repository.getConnectionProvider().createConnection();
		try {
			DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE_PERMISSION, true);
			st.execute(c);
			HashMap<CustomSecurityPermission, String> parentPemrissions = new HashMap<CustomSecurityPermission, String>();
			while (st.hasNext()){
				RowMap r = st.nextRow();
				CustomSecurityPermission p = new CustomSecurityPermission(
						r.getLong(Constants.FIELD_ID),
						r.getString(Constants.TABLE_ACE_PERMISSION__PERMISSION_NAME),
						r.getString(Constants.TABLE_ACE_PERMISSION__COLUMN_NAME),
						r.getString(Constants.TABLE_ACE_PERMISSION__EXPORT_NAME),
						r.getBoolean(Constants.TABLE_ACE_PERMISSION__DIRECT)
						);
				registerCustomPermission(p);
				// 
				
				parentPemrissions.put(p,r.getString(Constants.TABLE_ACE_PERMISSION__SUB_PERMISSIONS));
			}
			for(CustomSecurityPermission p : parentPemrissions.keySet()){
				String parentsString = parentPemrissions.get(p);
				BaseSecurityPermission[] parents = CustomSecurityPermission.parseSubPermissions(parentsString, getAllPermissions());
				p.subPermissions = parents;
			}
		} finally {
			c.close();
			if (tr != null){
				TransactionHelper.getInstance().rollbackAndResore(tr);
			}

		}
		
	}

    public BaseSecurityPermission getSecurityPermission(String exportName){
    	for(SecurityPermission p:SecurityPermission.values()){
    		if (p.getExportName().equals(exportName)){
    			return p;
    		}
    	}
    	return getCustomSecurityPermission(exportName);
    }

    
    public CustomSecurityPermission getCustomSecurityPermission(String exportName){
    	synchronized (customPersmissions) {
    		for(CustomSecurityPermission p:customPersmissions){
    			if (p.getExportName().equals(exportName)){
    				return p;
    			}
    		}
		}
    	return null;
    }

    
    public void checkPermission(DatabaseConnection conn, JCRPrincipals principals, BaseSecurityPermission permission, Long nodeId, Long securityId, 
            SecurityPrincipal targetPrincipal) throws RepositoryException {

        List<RowMap> allRows = loadSecurityPermissions(repository, conn, principals, Arrays.asList(new Long[]{securityId}));
        
        checkPermission(conn, principals, permission, nodeId, securityId, targetPrincipal, allRows);
    }
    
    public void checkPermission(DatabaseConnection conn, JCRPrincipals principals, BaseSecurityPermission permission, Long nodeId, Long securityId, 
            SecurityPrincipal targetPrincipal, List<RowMap> allRows) throws RepositoryException {

         
        Boolean result = JCRSecurityHelper.validateSecurityPermission(nodeId, allRows, principals, permission);
        if (result == null || !result){
            throw new AccessDeniedException();
        }
        
        if (permission.equals(SecurityPermission.X_GRANT)){
            //check restricted grant
            List<ACERestriction> restrictions = collectRestrictions(conn, securityId, principals);
            for(ACERestriction r:restrictions){
                if (targetPrincipal != null){
                    if (targetPrincipal.isUser() ){
                        if (targetPrincipal.getName().equals(r.getUserId())){
                            throw new AccessDeniedException();
                        }
                    }
                    if (targetPrincipal.isGroup()){
                        if (targetPrincipal.getName().equals(r.getGroupId())){
                            throw new AccessDeniedException();
                        }
                    }
                }
            }
        }
    }
    
    public static List<RowMap> loadSecurityPermissions(RepositoryImpl repository, DatabaseConnection conn, 
    		JCRPrincipals principals, java.util.Collection<Long> securityIds) throws RepositoryException{
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE, true);
        try {
            //securityId condition
            if (securityIds.size() == 1){
                st.addCondition(Conditions.eq(Constants.TABLE_NODE__SECURITY_ID, securityIds.iterator().next()));
            } else {
                st.addCondition(Conditions.in(Constants.TABLE_NODE__SECURITY_ID, securityIds));
            }
            //user and group conditions
            DatabaseCondition userCond = Conditions.eq(Constants.TABLE_ACE__USER_ID,principals.getUserId());
            if (principals.getGroupIdList() != null && principals.getGroupIdList().size() > 0){
                DatabaseCondition groupCond = Conditions.in(Constants.TABLE_ACE__GROUP_ID,principals.getGroupIdList());
                st.addCondition(Conditions.or(userCond, groupCond));
            } else {
                st.addCondition(userCond);
            }
            //context ids conditions
            if (repository.isContextSecuritySupported()){
	            DatabaseCondition contextNullCond = Conditions.isNull(Constants.TABLE_ACE__CONTEXT_ID);
	            if (principals.getContextIdList() != null && principals.getContextIdList().size() > 0){
	                DatabaseCondition contextsCond = Conditions.in(Constants.TABLE_ACE__CONTEXT_ID,principals.getContextIdList());
	                st.addCondition(Conditions.or(contextNullCond, contextsCond));
	            } else {
	                st.addCondition(contextNullCond);
	            }
            }
            
            st.execute(conn);
            List<RowMap> allRows = st.getAllRows();
            
            return allRows;
        }finally {
            st.close();
        }
        
    }
    
    //adds security condition to select statemts, check only read permissions
    public void addSecurityConditions(DatabaseConnection conn,JCRPrincipals principals, DatabaseSelectAllStatement st, boolean allowBrowse) throws RepositoryException {
    	conn.getDialect().addSecurityConditions(principals, st, allowBrowse);
    }

    //finds node ace records 
    public List<SecurityEntry> getNodeACL(Long securityId, DatabaseConnection conn, _AbstractsStateManager stateManager) throws RepositoryException{
        ArrayList<SecurityEntry> result = new ArrayList<SecurityEntry>();
        List<BaseSecurityPermission> permissions = getAllPermissions();
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE, true);
        st.addJoin(TABLE_ACE2, "ace2", FIELD_ID, FIELD_TYPE_ID);
       // st.addJoin(TABLE_ACE_RESTRICTION, "aceR", FIELD_ID, FIELD_TYPE_ID);
        try {
            st.addCondition(Conditions.eq(Constants.TABLE_NODE__SECURITY_ID, securityId));
            st.execute(conn);
            while (st.hasNext()){
                RowMap row = st.nextRow();
                result.add(new SecurityEntry(row, stateManager, permissions, this, conn));
            }
            
        } finally{
            st.close();
        }
        
        return result;
        
    }

    
    public void assignSecurity(DatabaseConnection conn,  
            Long nodeId, Long oldSecurityId, SecurityPermissionDefinition def, 
            JCRPrincipals userPrincipals, SecurityPostProcessor postProcessor) throws RepositoryException{
        
        def.setIgnoreCase(repository.isIgnoreCaseInSecurity());
        
    	Long newSecurityId = getOrCreateSecurityId(conn, nodeId, postProcessor);
    	
    	updatePermissionValue(conn, newSecurityId, newSecurityId, def, null, userPrincipals, postProcessor);
    }
    
    
    public void removePermission(DatabaseConnection conn, 
    		_NodeState node, SecurityPermissionDefinition def,
			_AbstractsStateManager stateManager,JCRPrincipals userPrincipals,
			SecurityPostProcessor postProcessor) throws RepositoryException {
        def.setIgnoreCase(repository.isIgnoreCaseInSecurity());

        Long nodeId = node.getNodeId(); 
        
        Long securityId = getOrCreateSecurityId(conn, nodeId, postProcessor);
        RowMap securityRow = findPermission(conn, def.getPrincipal(), securityId, true);
        
        if (securityRow != null){
	        SecurityEntry entry = new SecurityEntry(securityRow, stateManager, getAllPermissions(), this, conn);
	        for(BaseSecurityPermission p:getAllPermissions()){
	        	Long parentId = entry.getPermissionParentId(p);
	        	if (parentId != null && parentId.longValue() == securityId.longValue()){
	        		//remove permission only if it belong to this securityId, otherwise skip
	        		//1.find parent permission and its owner
	        		Long parentPermissionOwner = null;
	        		Boolean parentPermissionValue = null;
	        		SecurityEntry parent = findParentSecurityEntry(conn, node.getParentId(), stateManager, def.getPrincipal(), p);
	        		def = def.createCopy(p);
	        		if (parent != null && (parent.isDirectPermission(p) == null ||!parent.isDirectPermission(p))){
	        			parentPermissionOwner = parent.getPermissionParentId(p);
	        			parentPermissionValue = parent.getPermission(p);
	        			def.setPermit(parentPermissionValue);
	        		} else {
	        		    def.setPermit(null);
	        		}
	        		
	        		
        			updatePermissionValue(conn, securityId, parentPermissionOwner, def, null, userPrincipals, postProcessor);
	        	}
	        }
        }
        

        optimizeACE(conn, node.getSecurityId());
    }
    
	public void optimizeACE(DatabaseConnection conn, Long securityId) throws RepositoryException{
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE, false);
        st.addCondition(Conditions.eq(Constants.TABLE_NODE__SECURITY_ID, securityId));
        st.addResultColumn(Constants.FIELD_ID);
        for(BaseSecurityPermission p:getAllPermissions()){
            st.addCondition(Conditions.isNull(p.getColumnName()));
        }
        
        DatabaseDeleteStatement st1 = DatabaseTools.createDeleteStatement(Constants.TABLE_ACE2);
        st1.addCondition(Conditions.in(FIELD_TYPE_ID, st));
        st1.execute(conn);
        
        st1 = DatabaseTools.createDeleteStatement(Constants.TABLE_ACE_RESTRICTION);
        st1.addCondition(Conditions.in(Constants.TABLE_ACE_RESTRICTION__ACE_ID, st));
        st1.execute(conn);
        
	    
        st1 = DatabaseTools.createDeleteStatement(Constants.TABLE_ACE);
        st1.addCondition(Conditions.in(FIELD_ID, st));
        st1.execute(conn);
    }



	public void propagateNodeSecurity(NodeImpl node,JCRPrincipals userPrincipals)  throws RepositoryException{
		Long securityid = node.getSecurityId();
		DatabaseConnection conn = node._getWorkspace().getConnection();
        SecurityPostProcessor postProcessor = new SecurityPostProcessor(conn, this, userPrincipals);
		_SessionStateManager stateManager = ((SessionImpl)node.getSession()).getStateManager();
		List<SecurityEntry> acl = getNodeACL(securityid, conn, stateManager);
		for(SecurityEntry ace:acl){
			for(BaseSecurityPermission p : getAllPermissions()){
				Boolean value = ace.getPermission(p);
				if (value != null){
					BaseSecurityPermission fromPermission = ace.getFromPermission(p);
					Long permissionParent = ace.getPermissionParentId(p);
					
					SecurityPermissionDefinition def = new SecurityPermissionDefinition(p, ace.getPrincipalEntry(), value, true);
					def.importRestrictions(ace);
					
					updatePermissionValue(conn, securityid, permissionParent, def, fromPermission,  userPrincipals, postProcessor);
				}
			}
		}
		postProcessor.process();
		conn.commit();
	}


	//permission modification in database	
	 public void updatePermissionValue(DatabaseConnection conn, Long securityId, Long permissionParent,
	         SecurityPermissionDefinition def, BaseSecurityPermission fromPermission,
	         JCRPrincipals userPrincipals, SecurityPostProcessor postProcessor) throws RepositoryException{
		 //TODO handle super deny
		 
	     String fromPermissionString = fromPermission == null ? null : fromPermission.getExportName();
	     
	    SecurityPrincipal principal = def.getPrincipal();
	     
        //validate for restrictions
	    if (userPrincipals != null){
	        List<ACERestriction> restrictions = collectRestrictions(conn, securityId, userPrincipals);
	        for(ACERestriction r: restrictions){
	            if (principal.isGroup() && principal.getName().equals(r.getGroupId())){
	                throw new AccessDeniedException("You have no rights to modify security for "+principal);
	            }
	            if (principal.isUser() && principal.getName().equals(r.getUserId())){
	                throw new AccessDeniedException("You have no rights to modify security for "+principal);
	            }
	        }
	    }
	    
        
    	// ---------- find security record , if not exists, thet create new one
        RowMap securityRow = findPermission(conn, principal, securityId, true);
        if (securityRow == null) {
            Long nextId = conn.nextId();
            DatabaseInsertStatement st = DatabaseTools.createInsertStatement(Constants.TABLE_ACE);
            DatabaseInsertStatement st2 = DatabaseTools.createInsertStatement(Constants.TABLE_ACE2);
            try { 
                st.addValue(SQLParameter.create(Constants.FIELD_ID, nextId));
                if (principal.isUser()){ 
                    st.addValue(SQLParameter.create(Constants.TABLE_ACE__USER_ID, principal.getName()));
                }
                if (principal.isGroup()){
                    st.addValue(SQLParameter.create(Constants.TABLE_ACE__GROUP_ID, principal.getName()));
                }
                if (principal.getContextId() != null ){
                	st.addValue(SQLParameter.create(Constants.TABLE_ACE__CONTEXT_ID, principal.getContextId()));
                }
                st.addValue(SQLParameter.create(Constants.TABLE_NODE__SECURITY_ID, securityId));
                st.execute(conn);

                st2.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID, nextId));
                st2.execute(conn);
            } finally {
                st.close();
                st2.close();
            }
            securityRow = findPermission(conn, principal, securityId, true);
        }
        RowMap securityRow2 = findPermission2(securityRow.getLong(FIELD_ID) ,conn);

        postProcessor.registerUpdate(securityId, principal);
        if (def.getPermission() == SecurityPermission.BROWSE && def.isPermit() == null){
        	postProcessor.registerBrowseRemove(securityId, principal);
        }

        
        if (def.getPermission().equals(SecurityPermission.X_SUPER_DENY)){
            //check that security is not assigned or assigned for desired security Id
            Boolean v = securityRow.getBoolean(SecurityPermission.X_SUPER_DENY.getColumnName());
            if (v != null && v){
                //check that security is assigned from this node
                Long nodeId = securityRow.getLong(Constants.TABLE_NODE__SECURITY_ID);
                Long assignedFrom = securityRow2.getLong(SecurityPermission.X_SUPER_DENY.getColumnName()+TABLE_ACE2___PARENT_SUFFIX);
                if (!assignedFrom.equals(nodeId)){
                    throw new AccessDeniedException("You have no rigths to modify "+SecurityPermission.X_SUPER_DENY.getPermissionName()+" permission");
                }
            }
        }
        
        // ----------- statement for debug
       /* {
            DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE, true);
            if (userId != null){
                st.addCondition(Conditions.eq(Constants.TABLE_ACE__USER_ID, userId));
            }
            if (groupId != null){
                st.addCondition(Conditions.eq(Constants.TABLE_ACE__GROUP_ID, groupId));
            }
            if (contextId != null){
                st.addCondition(Conditions.eq(Constants.TABLE_ACE__CONTEXT_ID, contextId));            	
            } else {
            	st.addCondition(Conditions.isNull(Constants.TABLE_ACE__CONTEXT_ID));
            }
            //for current security id
            DatabaseCondition c1 = Conditions.eq(Constants.TABLE_NODE__SECURITY_ID, securityId);
            DatabaseCondition c2 = null;


            
            if (!propagate){
            	Long oldParentId = securityRow.getLong(p.getColumnName()+"_PARENT");
            	DatabaseCondition c2_1 = null;
            	if (oldParentId != null){
            		c2_1 = Conditions.eq(p.getColumnName()+"_PARENT", oldParentId);
            	}
            	
            	//all childs with parent null
            	DatabaseCondition c2_2_1 = Conditions.isNull(p.getColumnName()+"_PARENT");//parent is null
            	
            	DatabaseCondition c2_2_2 = Conditions.in(Constants.TABLE_NODE__SECURITY_ID, createSelectAllStatement(securityId));//define all childs from this security id
            	
            	DatabaseCondition c2_2 = Conditions.and(c2_2_1, c2_2_2);
            	
            	
            	if (c2_1 == null){
            		c2 = c2_2; 
            	} else {
            		c2 = Conditions.or(c2_1,c2_2);
            	}
            } else {
            	c2 = Conditions.in(Constants.TABLE_NODE__SECURITY_ID, createSelectAllStatement(securityId));
            }
            st.addCondition(Conditions.or(c1,c2));

            st.execute(conn);
            
        	
        }*/
        
        
        //update existings ACE
        //1.update current row
        DatabaseUpdateStatement _st1 = DatabaseTools.createUpdateStatement(Constants.TABLE_ACE);
        DatabaseUpdateStatement _st2 = DatabaseTools.createUpdateStatement(Constants.TABLE_ACE2);
        try {
            _st1.addCondition(Conditions.eq(FIELD_ID, securityRow.getLong(FIELD_ID)));
            _st2.addCondition(Conditions.eq(FIELD_TYPE_ID, securityRow2.getLong(FIELD_TYPE_ID)));
            
    
            _st1.addValue(SQLParameter.create(def.getPermission().getColumnName(), def.isPermit()));
            _st1.addValue(SQLParameter.create(def.getPermission().getColumnName()+Constants.TABLE_ACE___DIRECT_SUFFIX, def.isDirectPermission()));
            _st2.addValue(SQLParameter.create(def.getPermission().getColumnName()+TABLE_ACE2___PARENT_SUFFIX, permissionParent));
            _st2.addValue(SQLParameter.create(def.getPermission().getColumnName()+TABLE_ACE2___SEQUENCE_SUFFIX, postProcessor.getSequenceId()));
            _st2.addValue(SQLParameter.create(def.getPermission().getColumnName()+TABLE_ACE2___FROM_SUFFIX, fromPermissionString));
            
            _st1.execute(conn);
            _st2.execute(conn);
        } finally {
            _st1.close();
            _st2.close();
            
        }
        
        /*DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(Constants.TABLE_ACE);
        if (userId != null){
            st.addCondition(Conditions.eq(Constants.TABLE_ACE__USER_ID, userId));
        }
        if (groupId != null){
            st.addCondition(Conditions.eq(Constants.TABLE_ACE__GROUP_ID, groupId));
        }
        if (contextId != null){
            st.addCondition(Conditions.eq(Constants.TABLE_ACE__CONTEXT_ID, contextId));            	
        } else {
        	st.addCondition(Conditions.isNull(Constants.TABLE_ACE__CONTEXT_ID));
        }
        //for current security id
        DatabaseCondition c1 = Conditions.eq(Constants.TABLE_NODE__SECURITY_ID, securityId);
        DatabaseCondition c2 = null;*/


        
        if (!def.isPropogate()){
        	//propogate
          //Long oldParentId = securityRow2.getLong(p.getColumnName() + TABLE_ACE2___PARENT_SUFFIX);
        	/* --very old--
        	DatabaseCondition c2_1 = null;
        	if (oldParentId != null){
        		DatabaseCondition c2_1_1 = Conditions.eq(p.getColumnName()+"_PARENT", oldParentId);
        		DatabaseCondition c2_1_2 = Conditions.in(Constants.TABLE_NODE__SECURITY_ID, createSelectAllStatement(securityId));//define all childs from this security id
        		c2_1 = Conditions.and(c2_1_1, c2_1_2);
        	}
        	
        	//all childs with parnt null
        	DatabaseCondition c2_2_1 = Conditions.isNull(p.getColumnName()+"_PARENT");//parent is null
        	
        	DatabaseCondition c2_2_2 = Conditions.in(Constants.TABLE_NODE__SECURITY_ID, createSelectAllStatement(securityId));//define all childs from this security id
        	
        	DatabaseCondition c2_2 = Conditions.and(c2_2_1, c2_2_2);
        	
        	
        	if (c2_1 == null){
        		c2 = c2_2; 
        	} else {
        		c2 = Conditions.or(c2_1,c2_2);
        	}*/
        	//optimized  query  (a and b) or (c and b) == b and (a or c)
        	/*
        	 //b
    		DatabaseCondition childs = Conditions.in(Constants.TABLE_NODE__SECURITY_ID, createSelectAllStatement(securityId));//define all childs from this security id
    		//a 
        	DatabaseCondition c2_1 = Conditions.eq(p.getColumnName()+"_PARENT", oldParentId); //a
        	//c
    		DatabaseCondition c2_2 = Conditions.isNull(p.getColumnName()+"_PARENT");//parent is null //c
    		if (oldParentId != null){
    			c2_2 = Conditions.or(c2_1,c2_2);
    		} 
    		c2 = Conditions.and(childs, c2_2);*/
            
            
            Long oldSequenceid = securityRow2.getLong(def.getPermission().getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX);
            if (oldSequenceid != null){
                
                //update ACE
                DatabaseUpdateStatement _st3 = DatabaseTools.createUpdateStatement(Constants.TABLE_ACE);
                
                DatabaseSelectAllStatement _st3_all = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE2, true);
                { //logical separation
                    _st3_all.addCondition(Conditions.eq(def.getPermission().getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX, oldSequenceid));
                    _st3_all.addResultColumn(FIELD_TYPE_ID);
                }
                _st3.addCondition(Conditions.in(FIELD_ID, _st3_all));
                _st3.addCondition(Conditions.in(Constants.TABLE_NODE__SECURITY_ID, createSelectAllStatement(securityId)));
                _st3.addValue(SQLParameter.create(def.getPermission().getColumnName(), def.isPermit()));
                _st3.addValue(SQLParameter.create(def.getPermission().getColumnName() + Constants.TABLE_ACE___DIRECT_SUFFIX, def.isDirectPermission()));

                //update ACE2
                DatabaseUpdateStatement _st4 = DatabaseTools.createUpdateStatement(Constants.TABLE_ACE2);
                
                _st4.addCondition(Conditions.eq(def.getPermission().getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX, oldSequenceid));
                DatabaseSelectAllStatement _st4_all = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE, true);
                _st4_all.addCondition(Conditions.in(Constants.TABLE_NODE__SECURITY_ID, createSelectAllStatement(securityId)));
                _st4_all.addResultColumn(Constants.FIELD_ID);
                _st4.addCondition(Conditions.in(Constants.FIELD_TYPE_ID, _st4_all));
                _st4.addValue(SQLParameter.create(def.getPermission().getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX, postProcessor.getSequenceId()));
                _st4.addValue(SQLParameter.create(def.getPermission().getColumnName() + TABLE_ACE2___PARENT_SUFFIX, permissionParent));
                _st4.addValue(SQLParameter.create(def.getPermission().getColumnName()+TABLE_ACE2___FROM_SUFFIX, fromPermissionString));

                //execute
                _st3.execute(conn);
                _st4.execute(conn);
                
                //close
                _st3.close();
                _st4.close();
            }
            
        } else {
        	//propogate
        	/*c1 = Conditions.in(Constants.TABLE_NODE__SECURITY_ID, createSelectAllStatement(securityId));*/
            DatabaseSelectAllStatement st5 = DatabaseTools.createSelectAllStatement(TABLE_ACE, false);
            st5.addCondition(Conditions.in(Constants.TABLE_NODE__SECURITY_ID, createSelectAllStatement(securityId)));
            
            if (principal.isUser()){
                st5.addCondition(Conditions.eq(Constants.TABLE_ACE__USER_ID, principal.getName()));
            }
            if (principal.isGroup()){
                st5.addCondition(Conditions.eq(Constants.TABLE_ACE__GROUP_ID, principal.getName()));
            }
            if (principal.getContextId() != null){
                st5.addCondition(Conditions.eq(Constants.TABLE_ACE__CONTEXT_ID, principal.getContextId()));               
            } else {
                st5.addCondition(Conditions.isNull(Constants.TABLE_ACE__CONTEXT_ID));
            }
            st5.addResultColumn(FIELD_ID);

            DatabaseUpdateStatement _st3 = DatabaseTools.createUpdateStatement(Constants.TABLE_ACE);
            _st3.addCondition(Conditions.in(FIELD_ID, st5));
            _st3.addValue(SQLParameter.create(def.getPermission().getColumnName(), def.isPermit()));
            _st3.addValue(SQLParameter.create(def.getPermission().getColumnName() + Constants.TABLE_ACE___DIRECT_SUFFIX, def.isDirectPermission()));
            _st3.execute(conn);
            
            DatabaseUpdateStatement _st4 = DatabaseTools.createUpdateStatement(Constants.TABLE_ACE2);
            _st4.addCondition(Conditions.in(FIELD_TYPE_ID, st5));
            _st4.addValue(SQLParameter.create(def.getPermission().getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX, postProcessor.getSequenceId()));
            _st4.addValue(SQLParameter.create(def.getPermission().getColumnName() + TABLE_ACE2___PARENT_SUFFIX, permissionParent));
            _st4.addValue(SQLParameter.create(def.getPermission().getColumnName()+TABLE_ACE2___FROM_SUFFIX, fromPermissionString));
            _st4.execute(conn);
            
            _st3.close();
            _st4.close();
            
            
           // throw new UnsupportedOperationException();
        }
/*        st.addCondition(Conditions.or(c1,c2));

        //new values
        st.addValue(SQLParameter.create(p.getColumnName(), permissionValue));
        st.addValue(SQLParameter.create(p.getColumnName()+"_PARENT", permissionParent));
        
        st.execute(conn);*/
        
        //create missing ACE
        

        if (def.isPermit() != null && !def.isDirectPermission()){
	        DatabaseSelectAllStatement allMissed = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, false);
	        allMissed.setDistinct(true);
	        allMissed.addResultColumn(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID);
	        allMissed.addCondition(Conditions.not(Conditions.eq(Constants.TABLE_NODE__SECURITY_ID, securityId)));
	        allMissed.addCondition(Conditions.in(Constants.TABLE_NODE__SECURITY_ID, createSelectAllStatement(securityId)));
	        
	        DatabaseSelectAllStatement st2 = createSelectAllStatement(securityId);
	        st2.addJoin(Constants.TABLE_ACE, "security_", Constants.TABLE_NODE__SECURITY_ID, Constants.TABLE_NODE__SECURITY_ID );
	        if (principal.isUser()){
	            st2.addCondition(Conditions.eq("security_."+Constants.TABLE_ACE__USER_ID, principal.getName()));
	        }
	        if (principal.isGroup()){
	            st2.addCondition(Conditions.eq("security_."+Constants.TABLE_ACE__GROUP_ID, principal.getName()));
	        }
	        if (principal.getContextId() != null){
	            st2.addCondition(Conditions.eq("security_."+Constants.TABLE_ACE__CONTEXT_ID, principal.getContextId()));            	
	        } else {
	        	st2.addCondition(Conditions.isNull("security_."+Constants.TABLE_ACE__CONTEXT_ID));
	        }
	        allMissed.addCondition(Conditions.notIn(Constants.TABLE_NODE__SECURITY_ID, st2));
	        //createSelectAllStatement(securityId)
	        
	        allMissed.execute(conn);
            DatabaseInsertStatement st10 = null;
            DatabaseInsertStatement st11 = null;
           // DatabaseInsertStatement st12 = null;
	        while (allMissed.hasNext()){
	        	if (st10 == null){
	        		st10 = DatabaseTools.createInsertStatement(Constants.TABLE_ACE);
	        		st11 = DatabaseTools.createInsertStatement(Constants.TABLE_ACE2);
	        		//st12 = DatabaseTools.createInsertStatement(Constants.TABLE_ACE_RESTRICTION);
	        	}
	        	RowMap row = allMissed.nextRow();
	        	Long sId = row.getLong(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID);
	        	
	        	
	        	Long aceId = conn.nextId();
                st10.addValue(SQLParameter.create(Constants.FIELD_ID, aceId));
                if (principal.isUser()){
                    st10.addValue(SQLParameter.create(Constants.TABLE_ACE__USER_ID, principal.getName()));
                } else {
                    st10.addValue(SQLParameter.create(Constants.TABLE_ACE__USER_ID, (String) null));
                }
                
                if (principal.isGroup()){
                    st10.addValue(SQLParameter.create(Constants.TABLE_ACE__GROUP_ID, principal.getName()));
                } else {
                    st10.addValue(SQLParameter.create(Constants.TABLE_ACE__GROUP_ID, (String) null));
                }
                
                if (principal.getContextId() != null){
                    st10.addValue(SQLParameter.create(Constants.TABLE_ACE__CONTEXT_ID, principal.getContextId()));
                } else {
                    st10.addValue(SQLParameter.create(Constants.TABLE_ACE__CONTEXT_ID, (String) null));
                }
                st10.addValue(SQLParameter.create(Constants.TABLE_NODE__SECURITY_ID, sId));
                st10.addValue(SQLParameter.create(def.getPermission().getColumnName(), def.isPermit()));
                st10.addValue(SQLParameter.create(def.getPermission().getColumnName() + Constants.TABLE_ACE___DIRECT_SUFFIX, def.isDirectPermission()));
                st10.addBatch();
                
                st11.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID, aceId));
                st11.addValue(SQLParameter.create(def.getPermission().getColumnName()+TABLE_ACE2___PARENT_SUFFIX, permissionParent));
                st11.addValue(SQLParameter.create(def.getPermission().getColumnName()+TABLE_ACE2___FROM_SUFFIX, fromPermissionString));
                st11.addValue(SQLParameter.create(def.getPermission().getColumnName()+TABLE_ACE2___SEQUENCE_SUFFIX, postProcessor.getSequenceId()));
                st11.addBatch();
                
                //st11.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID, aceId));
                //st11.addBatch();
                
	        }
	        if (st10 != null){
                st10.execute(conn);
                st10.close();

                st11.execute(conn);
                st11.close();
                
                //st12.execute(conn);
                //st12.close();
	        }
        }
        
    	for(BaseSecurityPermission sp : def.getPermission().getDirectParents()){
    	    
    	    //Long parentId = securityRow2.getLong(p.getColumnName()+TABLE_ACE2___PARENT_SUFFIX);
            Long sequenceId2 = securityRow2.getLong(def.getPermission().getColumnName()+TABLE_ACE2___SEQUENCE_SUFFIX);
            Long sequenceId3 = securityRow2.getLong(sp.getColumnName()+TABLE_ACE2___SEQUENCE_SUFFIX);
    	    Boolean value = securityRow.getBoolean(sp.getColumnName());
    	    boolean process = false;
    	    if (value == null){
    	        process = true;
    	    } else if (sequenceId3.equals(sequenceId2)){
    	        process = true;
    	    } else if(!value.equals(def.isPermit())){
    	    	process = true;
    	    }
    	    if (process){
    	        SecurityPermissionDefinition _def = def.createCopy(sp);
    	        updatePermissionValue(conn, securityId, permissionParent, _def, def.getPermission(), userPrincipals, postProcessor);
    	    }
    	}
    	
    	//update restrictions
    	if (def.getPermission() == SecurityPermission.X_GRANT && def.getRestrictions().length > 0){
    	    //remove old restrictions
    	    DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_ACE_RESTRICTION);
    	    st.addCondition(Conditions.eq(TABLE_ACE_RESTRICTION__ACE_ID, securityRow.getLong(FIELD_ID)));
    	    st.execute(conn);
    	    
    	    //add new restrictions
    	    for(SecurityPrincipal p:def.getRestrictions()){
    	        p.setIgnoreCase(repository.isIgnoreCaseInSecurity());
    	        DatabaseInsertStatement st1 = DatabaseTools.createInsertStatement(TABLE_ACE_RESTRICTION);
    	        st1.addValue(SQLParameter.create(FIELD_ID, conn.nextId()));
    	        st1.addValue(SQLParameter.create(TABLE_ACE_RESTRICTION__ACE_ID, securityRow.getLong(FIELD_ID)));
    	        st1.addValue(SQLParameter.create(TABLE_ACE__USER_ID, p.isUser()? p.getName():null));
    	        st1.addValue(SQLParameter.create(TABLE_ACE__GROUP_ID, p.isGroup()? p.getName():null));
    	        st1.execute(conn);
    	    }
    	}
    	
	}
	 
	 
	    private RowMap findPermission(DatabaseConnection conn, SecurityPrincipal principal, Long securityId, boolean addParentInfo) throws RepositoryException{
	        if (repository.isIgnoreCaseInSecurity()){
	            principal.setIgnoreCase(true);
	        }
	        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE, true);
	        if (addParentInfo){
	            st.addJoin(TABLE_ACE2, "ace2", FIELD_ID, FIELD_TYPE_ID);
	        }
	        try {
	            st.addCondition(Conditions.eq(Constants.TABLE_NODE__SECURITY_ID, securityId));
	            if (principal.isUser()){
	                st.addCondition(Conditions.eq(Constants.TABLE_ACE__USER_ID, principal.getName()));
	            } else if (principal.isGroup()){
	                st.addCondition(Conditions.eq(Constants.TABLE_ACE__GROUP_ID, principal.getName()));
	            }
	            if (principal.getContextId() == null){
	                st.addCondition(Conditions.isNull(Constants.TABLE_ACE__CONTEXT_ID));
	            } else {
	                st.addCondition(Conditions.eq(Constants.TABLE_ACE__CONTEXT_ID, principal.getContextId()));
	            }
	            st.execute(conn);
	            if (st.hasNext()){
	                RowMap result = st.nextRow();
	                if (st.hasNext()){
	                    throw new RepositoryException("Duplicate permissions for principal "+principal);
	                }
	                return result;
	            } else {
	                return null;
	            }
	        } finally {
	            st.close();
	        }
	    }   
	        
	    private RowMap findPermission2(Long aceId, DatabaseConnection conn) throws RepositoryException{
	        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE2, true);
	        try {
	            st.addCondition(Conditions.eq(Constants.FIELD_TYPE_ID, aceId));
	            st.execute(conn);
	            if (st.hasNext()){
	                RowMap result = st.nextRow();
	                if (st.hasNext()){
	                    throw new RepositoryException("Duplicate permissions for ace "+aceId);
	                }
	                return result;
	            } else {
	                return null;
	            }
	        } finally {
	            st.close();
	        }
	    }   
	        
    //service methods
	public static DatabaseSelectAllStatement createSelectAllStatement(Long securityId) {
		DatabaseSelectAllStatement stAllChilds = null;
		stAllChilds = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, false);
        stAllChilds.setDistinct(true);
        stAllChilds.addResultColumn(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID);
        stAllChilds.addJoin(Constants.TABLE_NODE_PARENT, "parents", Constants.FIELD_ID, Constants.FIELD_TYPE_ID );
        stAllChilds.addCondition(Conditions.eq("parents."+Constants.TABLE_NODE_PARENT__PARENT_ID, securityId));
        stAllChilds.addCondition(Conditions.eqProperty(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID, Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID));
        stAllChilds.addCondition(Conditions.not(Conditions.eq(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID, securityId)));
        stAllChilds.addGroup(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID);
        return stAllChilds;
	}
	
	
	private Long getOrCreateSecurityId(DatabaseConnection conn,Long nodeId, SecurityPostProcessor postProcessor) throws RepositoryException{
        RowMap row = conn.loadRow(Constants.TABLE_NODE, Constants.FIELD_ID, nodeId);
        Long securityId = row.getLong(Constants.TABLE_NODE__SECURITY_ID);
        //Long securityId = nodeSecurityId;
        if (nodeId.longValue() != securityId.longValue()){
            securityId = clonePermissions(conn, securityId, nodeId, postProcessor);
        }
        return securityId;
    }
	
	private Long clonePermissions(DatabaseConnection conn, Long securityId, Long nodeId, SecurityPostProcessor postProcessor) throws RepositoryException {
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE, true);
        try{
            st.addCondition(Conditions.eq(Constants.TABLE_NODE__SECURITY_ID, securityId));
            st.execute(conn);
            //TODO use batches for insert
            //1.copy ACE's from parent
            while (st.hasNext()){
                RowMap row = st.nextRow();
                DatabaseSelectOneStatement stACE2 = new DatabaseSelectOneStatement(TABLE_ACE2, FIELD_TYPE_ID, row.getLong(FIELD_ID));
                stACE2.execute(conn);
                RowMap row2 = stACE2.getRow();
                stACE2.close();
                Long nextId = conn.nextId();
                DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(Constants.TABLE_ACE);
                DatabaseInsertStatement insert2 = DatabaseTools.createInsertStatement(Constants.TABLE_ACE2);
  //              DatabaseInsertStatement insert3 = DatabaseTools.createInsertStatement(Constants.TABLE_ACE_RESTRICTION);
                //try {
                    insert.addValue(SQLParameter.create(Constants.FIELD_ID, nextId));
                    insert.addValue(SQLParameter.create(Constants.TABLE_NODE__SECURITY_ID, nodeId));
    
                    boolean skip= true;
                    for(BaseSecurityPermission p:getAllPermissions()){
                        Boolean value = row.getBoolean(p.getColumnName());
                        Boolean direct = row.getBoolean(p.getColumnName()+Constants.TABLE_ACE___DIRECT_SUFFIX);
                        if (direct != null && direct){
                            continue;
                        }
                        if (value == null){
                            continue;
                        }
                        skip = false;
                        insert.addValue(SQLParameter.create(p.getColumnName(), value));
                        insert.addValue(SQLParameter.create(p.getColumnName()+Constants.TABLE_ACE___DIRECT_SUFFIX, direct));
                        
                        Long v1 = row2.getLong(p.getColumnName()+Constants.TABLE_ACE2___PARENT_SUFFIX);
                        Long v2 = row2.getLong(p.getColumnName()+Constants.TABLE_ACE2___SEQUENCE_SUFFIX);
                        String v3 = row2.getString(p.getColumnName()+Constants.TABLE_ACE2___FROM_SUFFIX);
                        
                        insert2.addValue(SQLParameter.create(p.getColumnName()+Constants.TABLE_ACE2___PARENT_SUFFIX, v1));
                        insert2.addValue(SQLParameter.create(p.getColumnName()+Constants.TABLE_ACE2___SEQUENCE_SUFFIX, v2));
                        insert2.addValue(SQLParameter.create(p.getColumnName()+Constants.TABLE_ACE2___FROM_SUFFIX, v3));
                    }
                    if (!skip){
	                    //insert.setFromRow(row, new String[]{Constants.FIELD_ID, Constants.TABLE_NODE__SECURITY_ID});
	                    insert.addValue(SQLParameter.create(Constants.TABLE_ACE__USER_ID, row.getString(Constants.TABLE_ACE__USER_ID)));
	                    insert.addValue(SQLParameter.create(Constants.TABLE_ACE__GROUP_ID, row.getString(Constants.TABLE_ACE__GROUP_ID)));
	                    insert.addValue(SQLParameter.create(Constants.TABLE_ACE__CONTEXT_ID, row.getString(Constants.TABLE_ACE__CONTEXT_ID)));
	                    insert.execute(conn);
	                    insert.close();
	
	                    insert2.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID, nextId));
	                    
	                    //insert2.setFromRow(row2, new String[]{Constants.FIELD_TYPE_ID});
	                    insert2.addBatch();
	                    insert2.execute(conn);
	                    insert2.close();
                    }
                    
//                    insert3.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID, nextId));
//                    insert3.addBatch();
                /*} finally {
                    insert.execute(conn);
                    insert.close();
                    insert2.execute(conn);
                    insert2.close();
//                    insert3.execute(conn);
//                    insert3.close();
                }*/
            }
            //2.update child nodes security id
          /*  DatabaseSelectAllStatement st2 = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, false);
            try {
                st2.addResultColumn(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);
                st2.addJoin(Constants.TABLE_NODE_PARENT, "parents", Constants.FIELD_ID, Constants.FIELD_TYPE_ID );
                st2.addCondition(Conditions.eq("parents."+Constants.TABLE_NODE_PARENT__PARENT_ID, nodeId));
                st2.addCondition(Conditions.eq(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID, securityId));
                st2.addGroup(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);
                st2.execute(conn);
                //TODO use batch or in condition in where clause
                while(st2.hasNext()){
                    RowMap row = st2.nextRow();
                    Long id = row.getLong(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);
                    
                    DatabaseUpdateStatement ust = DatabaseTools.createUpdateStatement(Constants.TABLE_NODE, Constants.FIELD_ID, id);
                    ust.addValue(SQLParameter.create(Constants.TABLE_NODE__SECURITY_ID, nodeId));
                    ust.addValue(SQLParameter.createSQL(Constants.TABLE_NODE__VERSION_, Constants.TABLE_NODE__VERSION_+"+1"));
                    ust.execute(conn);
                }
            } finally {
                st.close();
            }
            //3. update node security id
            DatabaseUpdateStatement upSt = DatabaseTools.createUpdateStatement(Constants.TABLE_NODE, Constants.FIELD_ID, nodeId);
            upSt.addValue(SQLParameter.create(Constants.TABLE_NODE__SECURITY_ID, nodeId));
            upSt.addValue(SQLParameter.createSQL(Constants.TABLE_NODE__VERSION_, Constants.TABLE_NODE__VERSION_+"+1"));
            //evictList.add(new CacheKey(Constants.TABLE_NODE, nodeId));            
            upSt.execute(conn);*/
            
            //the same as above
            DatabaseUpdateStatement ust = DatabaseTools.createUpdateStatement(Constants.TABLE_NODE);
            
            DatabaseSelectAllStatement st2 = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, false);
            st2.addResultColumn(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);
            st2.addJoin(Constants.TABLE_NODE_PARENT, "parents", Constants.FIELD_ID, Constants.FIELD_TYPE_ID );
            st2.addCondition(Conditions.eq("parents."+Constants.TABLE_NODE_PARENT__PARENT_ID, nodeId));
            st2.addCondition(Conditions.eq(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID, securityId));
            st2.addGroup(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);
            
            DatabaseCondition c1 = Conditions.in(Constants.FIELD_ID, st2);
            DatabaseCondition c2 = Conditions.eq(Constants.FIELD_ID, nodeId);
            ust.addCondition(Conditions.or(c1,c2));
            
            ust.addValue(SQLParameter.create(Constants.TABLE_NODE__SECURITY_ID, nodeId));
            ust.addValue(SQLParameter.createSQL(Constants.TABLE_NODE__VERSION_, Constants.TABLE_NODE__VERSION_+"+1"));
            ust.execute(conn);

            postProcessor.addNewSecurityId(nodeId);
        } finally{
            st.close();
        }
        return nodeId;
    }
	
    private SecurityEntry findParentSecurityEntry(DatabaseConnection conn ,Long nodeId, _AbstractsStateManager stateManager, 
    		SecurityPrincipal principal,
    		BaseSecurityPermission p) throws RepositoryException {
    	if (nodeId == null){
    		return null;
    	}
    	if (repository.isIgnoreCaseInSecurity()){
    	    principal.setIgnoreCase(true);
    	}
    	RowMap nodeRow = conn.loadRow(Constants.TABLE_NODE, Constants.FIELD_ID, nodeId);
    	Long securityId = nodeRow.getLong(Constants.TABLE_NODE__SECURITY_ID);
    	RowMap secRow = findPermission(conn, principal, securityId, true);
    	if (secRow != null){
    		SecurityEntry result = new SecurityEntry(secRow, stateManager, getAllPermissions(), this, conn);
    		return result;
    	}
    	if (securityId.longValue() != nodeId.longValue()){
    		nodeRow = conn.loadRow(Constants.TABLE_NODE, Constants.FIELD_ID, securityId);    		
    	}
    	Long parentId = nodeRow.getLong(Constants.TABLE_NODE__PARENT);
    	if (parentId != null){
    		return findParentSecurityEntry(conn, nodeRow.getLong(Constants.TABLE_NODE__PARENT), stateManager, principal, p);
    	} else{
    		return null;
    	}
	}
    
    
    public List<ACERestriction> collectRestrictions(DatabaseConnection conn, Long securityId, JCRPrincipals principals)
        throws RepositoryException{

        Collection<String> groups = principals.getGroupIdList();
        Collection<String> contexts = principals.getContextIdList();
        String userId = principals.getUserId();
        
        if (groups == null){
            groups = new ArrayList<String>();
        }
        if (contexts == null){
            contexts = new ArrayList<String>();
        }
        
        List<ACERestriction> result = new ArrayList<ACERestriction>();
        if (repository.isIgnoreCaseInSecurity()){
            groups = CollectionUtils.collect(groups, new Transformer(){
                public Object transform(Object input) {
                    return ((String)input).toUpperCase();
                }});
            
            //groupId = groupId == null ? null : groupId.toUpperCase();
            userId = userId == null ? null : userId.toUpperCase();
        }
        
        //1. find all parent nodes
        DatabaseSelectAllStatement parent = DatabaseTools.createSelectAllStatement(TABLE_NODE_PARENT, false);
        parent.addCondition(Conditions.eq(FIELD_TYPE_ID, securityId));
        parent.addResultColumn(TABLE_NODE_PARENT__PARENT_ID);
        
       //parent.execute(conn);
        
        //System.out.println(parent.getAllRows());
        
        //find all ace
        DatabaseSelectAllStatement aces = DatabaseTools.createSelectAllStatement(TABLE_ACE, false);
        DatabaseCondition c1 = Conditions.eq(TABLE_NODE__SECURITY_ID, securityId);
        DatabaseCondition c2 = Conditions.in(TABLE_NODE__SECURITY_ID, parent);
        DatabaseCondition aceCond = Conditions.or(c1,c2);
        aces.addCondition(aceCond);
        //aces.addCondition(c1);
        DatabaseCondition c14;
        DatabaseCondition c15 = null;
        {
            DatabaseCondition c10 = Conditions.eq(Constants.TABLE_ACE__USER_ID, userId);
            DatabaseCondition c11 = Conditions.isNull(Constants.TABLE_ACE__CONTEXT_ID);
            DatabaseCondition c13;
            if (contexts.size() == 0){
                c13 = c11;
            } else {
                DatabaseCondition c12 = Conditions.in(Constants.TABLE_ACE__CONTEXT_ID, contexts);
                c13 = Conditions.or(c11,c12);
            }
            c14 = Conditions.and(c13,c10);
            
        }
        if (groups.size() > 0) {
            DatabaseCondition c10 = Conditions.in(Constants.TABLE_ACE__GROUP_ID, groups);
            DatabaseCondition c11 = Conditions.isNull(Constants.TABLE_ACE__CONTEXT_ID);
            DatabaseCondition c13;
            if (contexts.size() == 0){
                c13 = c11;
            } else {
                DatabaseCondition c12 = Conditions.in(Constants.TABLE_ACE__CONTEXT_ID, contexts);
                c13 = Conditions.or(c11,c12);
            }
            c15 = Conditions.and(c13,c10);
        }

        if (c15 == null) {
            aces.addCondition(c14);
        } else {
            aces.addCondition(Conditions.or(c14,c15));
        }
        
        aces.addResultColumn(FIELD_ID);
        
       /* aces.execute(conn);
        List<RowMap> r1 = aces.getAllRows();
        System.out.println(r1);*/
        //find restrictions
        
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_ACE_RESTRICTION, true);
        st.addCondition(Conditions.in(TABLE_ACE_RESTRICTION__ACE_ID,aces));
        st.execute(conn);
        
        List<RowMap> restrictions = st.getAllRows();
        //build result
        for(RowMap row:restrictions){
            ACERestriction r = new ACERestriction(row);
            result.add(r);
        }
        return result;
    }
    
    void assignRestriction(DatabaseConnection conn, Long securityId, 
            SecurityPrincipal targetUser, SecurityPrincipal restriction) throws RepositoryException{
        
        if (repository.isIgnoreCaseInSecurity()){
            targetUser.setIgnoreCase(true); 
            restriction.setIgnoreCase(true); 
        }
        
        RowMap securityRow = findPermission(conn, targetUser, securityId, false);
        Long id = securityRow.getLong(FIELD_ID);
        List<RowMap> restrictions = getRestrictions(conn, id);
        for(RowMap row:restrictions){
            if (restriction.isUser() && restriction.getName().equals(row.getString(TABLE_ACE__USER_ID))){
                //restriction already exists
                return;
            }
            if (restriction.isGroup() && restriction.getName().equals(row.getString(TABLE_ACE__GROUP_ID))){
                //restriction already exists
                return;
            }
        }
        //add restriction
        DatabaseInsertStatement  st  = DatabaseTools.createInsertStatement(TABLE_ACE_RESTRICTION);
        Long restrictionId = conn.nextId();
        System.out.println(">>>>>>>>>>>>>. "+restrictionId+" - "+id);
        st.addValue(SQLParameter.create(FIELD_ID, restrictionId));
        st.addValue(SQLParameter.create(TABLE_ACE_RESTRICTION__ACE_ID, id));
        if (restriction.isUser()){
            st.addValue(SQLParameter.create(TABLE_ACE__USER_ID, restriction.getName()));
        } else {
            st.addValue(SQLParameter.create(TABLE_ACE__GROUP_ID, restriction.getName()));
        }
        st.execute(conn);
        
    }



    void removeRestriction(DatabaseConnection conn, Long securityId, 
            SecurityPrincipal targetUser, SecurityPrincipal restriction) throws RepositoryException{
        
        if (repository.isIgnoreCaseInSecurity()){
            restriction.setIgnoreCase(true);
            targetUser.setIgnoreCase(true);
        }

        
        RowMap securityRow = findPermission(conn, targetUser, securityId, false);
        Long id = securityRow.getLong(FIELD_ID);
        List<RowMap> restrictions = getRestrictions(conn, id);
        Long restrictionId = null;
        for(RowMap row:restrictions){
            if (restriction.isUser() && restriction.getName().equals(row.getString(TABLE_ACE__USER_ID))){
                restrictionId = row.getLong(FIELD_ID);
                break;
            }
            if (restriction.isGroup()&& restriction.getName().equals(row.getString(TABLE_ACE__GROUP_ID))){
                restrictionId = row.getLong(FIELD_ID);
                break;
            }
        }

        if (restrictionId != null){
            DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_ACE_RESTRICTION, FIELD_ID, restrictionId);
            st.execute(conn);
        }
    }
    
    
    public List<RowMap> getRestrictions(DatabaseConnection conn, Long aceId) throws RepositoryException{
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_ACE_RESTRICTION, true);
        st.addCondition(Conditions.eq(TABLE_ACE_RESTRICTION__ACE_ID, aceId));
        st.execute(conn);
        List<RowMap> rows = st.getAllRows();
        return rows;
        
    }

}


/*
 * $Log: RepositorySecurityManager.java,v $
 * Revision 1.31  2009/03/16 12:13:20  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.30  2008/12/13 13:02:48  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.29  2008/12/03 08:52:28  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.28  2008/11/27 12:52:28  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.27  2008/11/13 09:41:52  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.26  2008/11/13 09:40:51  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.25  2008/11/03 12:07:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.24  2008/11/03 11:10:04  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.23  2008/10/21 10:49:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.22  2008/10/01 12:44:00  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.21  2008/09/29 11:32:40  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.20  2008/09/29 06:45:33  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.19  2008/09/02 11:01:30  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.18  2008/07/22 09:06:26  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.17  2008/07/17 11:02:30  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.16  2008/07/17 06:35:02  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.15  2008/07/16 13:06:43  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.14  2008/07/16 11:42:51  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.13  2008/07/16 08:45:04  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.12  2008/07/03 08:39:31  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.11  2008/06/26 12:05:18  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.10  2008/06/26 11:42:04  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.9  2008/06/26 07:20:49  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2008/06/13 09:35:29  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/06/11 10:07:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2008/06/09 12:36:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2008/06/02 11:36:11  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2008/04/29 10:55:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2007/07/31 07:41:48  dparhomenko
 * PTR#1804803 fix ptr
 *
 *
 */