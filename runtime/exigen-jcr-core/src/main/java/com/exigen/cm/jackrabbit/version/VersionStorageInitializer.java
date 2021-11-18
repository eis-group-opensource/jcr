/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.Order;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.NodeTypeManagerImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.state2.ChangeState;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.jackrabbit.name.QName;

public class VersionStorageInitializer {

	private DatabaseConnection conn;
	private RepositoryImpl repository;
	private String _historyRootPath;
	private NamespaceRegistryImpl nsRegistry;
	private NodeTypeManagerImpl ntManager;
	private HashMap<String, Long> pathsId;

	public VersionStorageInitializer(DatabaseConnection conn, RepositoryImpl repository, NamespaceRegistryImpl nsRegistry,
			NodeTypeManagerImpl ntManager){
		this.conn = conn;
		this.repository = repository;
        _historyRootPath = repository.getVersionStoragePath();
        this.nsRegistry = nsRegistry;
        this.ntManager = ntManager;
	}
	
	public List<Long> initialize(ArrayList<String> newUUID, _NodeState versionStorage) throws RepositoryException{
		boolean repeat = true;
		ArrayList<Long> ids = null;
		conn.loadRow(Constants.TABLE_NODE, Constants.FIELD_ID, repository.getVersionStorageNodeId());
		conn.lockNode(repository.getVersionStorageNodeId());
		 //TransactionHelper.getInstance().getTransactionManager().suspend();
		while (repeat){
			try {
				VersionStorageItem root = new VersionStorageItem("versionStorage", null, repository, repository.getVersionStorageDepth(), ntManager);
				root.setParents(versionStorage.getParentNodes());
				root.setNodeId(versionStorage.getNodeId());
				
				
				
		        ArrayList<String> paths = new ArrayList<String>();
		        for(String uuid:newUUID){
		            String n1 = "h"+uuid.substring(0 * 2, 0 * 2 + 2);
		            String n2 = "h"+uuid.substring(1 * 2, 1 * 2 + 2);
		            String n3 = "h"+uuid.substring(2 * 2, 2 * 2 + 2);
		            //String n4 = "h"+uuid.substring(3 * 2, 3 * 2 + 2);
		
		            VersionStorageItem i1 = root.addChild(n1);
		            VersionStorageItem i2 = i1.addChild(n2);
		            VersionStorageItem i3 = i2.addChild(n3);
		            //VersionStorageItem i4 = i1.addChild(n1);
		            
		            String p1 = _historyRootPath+"/"+n1+"{1}";
		            String p2 = p1+"/"+n2+"{1}";
		            String p3 = p2+"/"+n3+"{1}";
		            //String p4 = p3+"/"+n3+"{1}";
		            paths.add(p1);
		            paths.add(p2);
		            paths.add(p3);
		            //paths.add(p4);
		            
		            i1.setPath(p1);
		            i2.setPath(p2);
		            i3.setPath(p3);
		
		        }
		        
		        HashMap<String, RowMap> values = evaluateValues(paths);
		        //values = evaluateValues(paths);
		        
		        root.initializeTree(values);
		        NodeTypeImpl nt1 = ntManager.getNodeType(QName.NT_BASE);
		        NodeTypeImpl nt2 = ntManager.getNodeType(QName.REP_VERSIONSTORAGE);
		        DatabaseInsertStatement ntBaseSt = DatabaseTools.createInsertStatement(nt1.getTableName());
		        DatabaseInsertStatement ntVersionStorageSt = DatabaseTools.createInsertStatement(nt2.getTableName());
		
		        ChangeState changeState = new ChangeState(nsRegistry, null);
		        
		        
		        root.populate(changeState, ntBaseSt, ntVersionStorageSt);
		        
		        changeState.preocessNewNodes(conn, null);
		        changeState.processNewTypes(conn);
		        
		        changeState.processNewParentNode(conn);
		        
		        ntBaseSt.executeBatch(conn);
		        ntVersionStorageSt.executeBatch(conn);
		        
		        
		        conn.commit();
		        
		        ids = new ArrayList<Long>();
		        root.populateIds(ids);
		        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, true);
		        st.setLockForUpdate(true);
		        st.addCondition(Conditions.in(Constants.FIELD_ID, ids));
		        st.execute(conn);
		        st.close();
		        this.pathsId = new HashMap<String, Long>();
		        root.buildPaths(pathsId);
		        
		        //System.out.println("Done");
		        repeat = false;
			} catch (RepositoryException exc){
				System.out.println("Repeat");
				conn.rollback();
			}
		}
	    return ids;
	}

	private HashMap<String, RowMap> evaluateValues(ArrayList<String> paths) throws RepositoryException {
		ArrayList<String> tmp = new ArrayList<String>(paths);
        HashMap<String, RowMap> values = new HashMap<String, RowMap>(); 
        while(tmp.size() > 0){
        	ArrayList<String> tmp2 = new ArrayList<String>();
        	if (tmp.size() < 299){
        		tmp2.addAll(tmp);
        		tmp.clear();
        	} else {
        		for(int i = 0 ; i < 299 ; i++){
        			tmp2.add(tmp.get(i));
        		}
        		tmp.removeAll(tmp2);
        	}
            DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, true);
            st.addCondition(Conditions.in(Constants.TABLE_NODE__NODE_PATH, tmp2));
            st.addResultColumn(Constants.TABLE_NODE__NODE_PATH);
            st.addResultColumn(Constants.FIELD_ID);
            st.addResultColumn(Constants.TABLE_NODE__SECURITY_ID);
            st.addResultColumn(Constants.TABLE_NODE__CONTENT_STORE_CONFIG_NODE);
            st.addOrder(Order.asc(Constants.TABLE_NODE__NODE_PATH));
            //st.setLockForUpdate(true);
            st.execute(conn);
            
            while(st.hasNext()){
            	RowMap row = st.nextRow();
            	String pp = row.getString(Constants.TABLE_NODE__NODE_PATH);
            	values.put(pp, row);
            }
            st.close();
            
        }
		return values;
	}

	public HashMap<String, Long> buildPaths() {
		return pathsId;
	}

	
}
