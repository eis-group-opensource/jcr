/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.FIELD_TYPE_ID;
import static com.exigen.cm.Constants.TABLE_NODE;
import static com.exigen.cm.Constants.TABLE_NODE_LOCK;
import static com.exigen.cm.Constants.TABLE_NODE_LOCK_INFO__LOCK_OWNER;
import static com.exigen.cm.Constants.TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID;
import static com.exigen.cm.Constants.TABLE_NODE_PARENT;
import static com.exigen.cm.Constants.TABLE_NODE_PARENT__LEVEL;
import static com.exigen.cm.Constants.TABLE_NODE_PARENT__PARENT_ID;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__FROM;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__PROPERTY_NAME;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__PROPERTY_NAMESPACE;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__TO;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__UUID;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED_VALUES;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__MULTIPLE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__NODE_TYPE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__PROP_DEF;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__TYPE;
import static com.exigen.cm.Constants.TABLE_NODE__INDEX;
import static com.exigen.cm.Constants.TABLE_NODE__INDEX_MAX;
import static com.exigen.cm.Constants.TABLE_NODE__NODE_DEPTH;
import static com.exigen.cm.Constants.TABLE_NODE__NODE_PATH;
import static com.exigen.cm.Constants.TABLE_NODE__NODE_TYPE;
import static com.exigen.cm.Constants.TABLE_NODE__PARENT;
import static com.exigen.cm.Constants.TABLE_NODE__SECURITY_ID;
import static com.exigen.cm.Constants.TABLE_NODE__VERSION_;
import static com.exigen.cm.Constants.TABLE_NODE__WORKSPACE_ID;
import static com.exigen.cm.Constants.TABLE_TYPE;
import static com.exigen.cm.Constants.TABLE_TYPE__FROM_NODE_TYPE;
import static com.exigen.cm.Constants.TABLE_TYPE__NODE_TYPE;
import static com.exigen.cm.Constants._TABLE_NODE_LOCK_INFO;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.AndDatabaseCondition;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.DatabaseCondition;
import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.impl.NodeReference;
import com.exigen.cm.impl.NodeTypeContainer;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.ParentNode;
import com.exigen.cm.impl.PropertyDefinitionImpl;
import com.exigen.cm.jackrabbit.lock.LockManagerImpl;
import com.exigen.cm.jackrabbit.lock.LockManagerListener;
import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.value.BLOBFileValue;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class ChangeState {

	private static Log log = LogFactory.getLog(ChangeState.class);

	
	private ArrayList<ParentNode> removedPN = new ArrayList<ParentNode>(); 
	private ArrayList<ParentNode> newPN = new ArrayList<ParentNode>(); 
	private ArrayList<_NodeState> newNodes = new ArrayList<_NodeState>();
	private ArrayList<NodeTypeContainer> newTypes = new ArrayList<NodeTypeContainer>();
	private NamespaceRegistryImpl namespaceRegistry;
	private ArrayList<_PropertyState> newFTSProperties = new ArrayList<_PropertyState>();
	private ArrayList<_PropertyState> removeFTSProperties = new ArrayList<_PropertyState>();
	private ArrayList<NodeReference> newReferences = new ArrayList<NodeReference>();
	private StoreContainer sc; 
	private HashMap<Long, _NodeState> newOcrWorks = new HashMap<Long, _NodeState>();
	private HashMap<Long,_NodeState> removedNodes = new HashMap<Long, _NodeState>();
	private HashMap<_NodeState, DatabaseUpdateStatement> basePropertyChanged = new HashMap<_NodeState, DatabaseUpdateStatement>();
	
	public ChangeState(NamespaceRegistryImpl namespaceRegistry, StoreContainer sc) {
		this.namespaceRegistry = namespaceRegistry;
		this.sc = sc;
	}

	public void addNewParentNode(ParentNode pn) {
		newPN.add(pn);
		
	}

	public void addRemovedParentNode(ParentNode pn) {
		removedPN.add(pn);
	}
	
	public void processRemovedParentNode(DatabaseConnection conn) throws RepositoryException{
		if (removedPN.size() > 0){
			DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_NODE_PARENT);
			//while (removedPN.size() > 0){
				//ArrayList<Long> values = new ArrayList<Long>();
				//ArrayList<ParentNode> rr = new ArrayList<ParentNode>();
				for(Iterator<ParentNode> it = removedPN.iterator();it.hasNext();){
					ParentNode pn = it.next();
					/*rr.add(pn);
					if (pn.getId()!= null){
						values.add(pn.getId());
					}*/
					st.addCondition(Conditions.eq(Constants.FIELD_TYPE_ID, pn.getChildId()));
					st.addCondition(Conditions.eq(Constants.TABLE_NODE_PARENT__PARENT_ID, pn.getParentId()));
					st.addCondition(Conditions.eq(Constants.TABLE_NODE_PARENT__LEVEL, pn.getPosition()));
					st.addBatch();
				}
		        //DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_NODE_PARENT, FIELD_ID, pn.getId());
				//st = DatabaseTools.createDeleteStatement(TABLE_NODE_PARENT);
				//st.addCondition(Conditions.in(FIELD_ID, values));
				//st.execute(conn);
				//removedPN.removeAll(rr);
			//}
			
			st.execute(conn);
		}
	}
	
	public void processNewParentNode(DatabaseConnection conn) throws RepositoryException{
		if (newPN.size() > 0){
            DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(TABLE_NODE_PARENT);
			for(ParentNode pn:newPN){
				
                //insert.addValue(SQLParameter.create(FIELD_ID, pn.getId()));
                insert.addValue(SQLParameter.create(FIELD_TYPE_ID, pn.getChildId()));
                insert.addValue(SQLParameter.create(TABLE_NODE_PARENT__PARENT_ID, pn.getParentId()));
                insert.addValue(SQLParameter.create(TABLE_NODE_PARENT__LEVEL, pn.getPosition()));
                insert.addBatch();
                //insert.execute(conn);
                //insert = DatabaseTools.createInsertStatement(TABLE_NODE_PARENT);
			}
	        insert.execute(conn);
		}
	}

	public void addNewNode(_NodeState state) {
		newNodes.add(state);
		
	}	
	
	boolean checkIds = false;//"true".equals(System.getenv().get("jcr.checkIds"));
	
	public void preocessNewNodes(DatabaseConnection conn, LockManagerImpl lockManager) throws RepositoryException{
		if (newNodes.size() > 0){
			if (checkIds){
				HashMap<Long, _NodeState> _ids = new HashMap<Long,_NodeState>();
				for(_NodeState state:newNodes){
					if (_ids.containsKey(state.getNodeId())){
						StringBuffer sb = new StringBuffer();
						sb.append("--------------------- FATAL ERROR 1------------------\r\n");
						sb.append("Node "+state+" added more that 1 time; "+_ids.get(state.getNodeId()));
						System.err.println(sb.toString());
						log.error(sb.toString());
						throw new RuntimeException(sb.toString());
					} else {
						_ids.put(state.getNodeId(), state);
					}
				}
			}

			
	        DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(TABLE_NODE);
	        DatabaseInsertStatement insert1 = DatabaseTools.createInsertStatement(TABLE_NODE_LOCK);
	        DatabaseInsertStatement insert2 = DatabaseTools.createInsertStatement(_TABLE_NODE_LOCK_INFO);
	        for(_NodeState state:newNodes){
	        	
	        	if (checkIds){
	        		try {
						DatabaseSelectOneStatement stt = new DatabaseSelectOneStatement(Constants.TABLE_NODE, Constants.FIELD_ID, state.getNodeId());
						stt.execute(conn);
						RowMap row = stt.getRow();
						StringBuffer sb = new StringBuffer();
						sb.append("--------------------- FATAL ERROR 2------------------\r\n");
						sb.append("Node "+state+" added more that 1 time; "+row);
						System.err.println(sb.toString());
						log.error(sb.toString());
						throw new RuntimeException(sb.toString());
					} catch (Exception e) {
					}
	        	}
	        	if (checkIds){
	        		insert.close();
	        		insert = DatabaseTools.createInsertStatement(TABLE_NODE);
	        	}
		        insert.addValue(SQLParameter.create(FIELD_ID, state.getNodeId()));
		        JCRHelper.populateQName(insert, state.getName(), namespaceRegistry, false);
		        insert.addValue(SQLParameter.create(TABLE_NODE__PARENT, state.getParentId()));
		        insert.addValue(SQLParameter.create(TABLE_NODE__SECURITY_ID, state.getSecurityId()));
		        insert.addValue(SQLParameter.create(TABLE_NODE__INDEX,state.getIndexLong()));
		        insert.addValue(SQLParameter.create(TABLE_NODE__INDEX_MAX, state.getSnsMax()));
		        insert.addValue(SQLParameter.create(TABLE_NODE__VERSION_,new Long(1)));
		        insert.addValue(SQLParameter.create(TABLE_NODE__NODE_TYPE,state.getNodeTypeId()));
		        insert.addValue(SQLParameter.create(TABLE_NODE__NODE_PATH, state.getInternalPath()));
		        insert.addValue(SQLParameter.create(TABLE_NODE__NODE_DEPTH, state.getInternalDepth()));
		        insert.addValue(SQLParameter.create(TABLE_NODE__WORKSPACE_ID, state.getWorkspaceId()));
		        insert.addValue(SQLParameter.create(Constants.TABLE_NODE__CONTENT_STORE_CONFIG_NODE, state.getStoreConfigurationId()));
		        for(_NodeState st:basePropertyChanged.keySet()){
		        	if (st.getParentId() != null){
			        	if (st.getParentId().equals(state.getParentId())
			        			&& st.getName().equals(state.getName())){
			        		DatabaseUpdateStatement upSt = basePropertyChanged.get(st);
			        		System.out.println("Fk fix for "+st);
			        		upSt.execute(conn);
			        	}
			        		
			        	}
		        }
		        if (checkIds){
		        	try {
		        		insert.execute(conn);
		        	} catch (RepositoryException exc){
		        		throw exc;
		        	}
		        } else {
		        	insert.addBatch();
		        }
		        
		        insert1.addValue(SQLParameter.create(FIELD_TYPE_ID, state.getNodeId()));
		        insert1.addBatch();

		        insert2.addValue(SQLParameter.create(FIELD_TYPE_ID, state.getNodeId()));
		        insert2.addValue(SQLParameter.create(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID, state.getParentLockId()));
		        insert2.addValue(SQLParameter.create(TABLE_NODE_LOCK_INFO__LOCK_OWNER, state.getParentLockOwner()));
		        insert2.addBatch();

		        //insert.execute(conn);
	        }
	        if (!checkIds){
	        	insert.execute(conn);
	        }
	        insert1.execute(conn);
	        insert2.execute(conn);
	        for(_NodeState state:newNodes){
	            DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(_TABLE_NODE_LOCK_INFO);
                HashMap<String, Object> options = state.getLockOptions();
                if (lockManager != null && options != null){
                    Collection<LockManagerListener> listeners = lockManager.getListeners();
                    for(LockManagerListener listener : listeners){
                        listener.internalSetParentLockId(conn, st, options, state.getParentLockId());
                    }
                    if (listeners.size() > 0){
                        st.execute(conn);
                    }
                    st.close();

                }
	            
	        }
		}
		
	}

	public void addNewType(NodeTypeContainer ntc) {
		newTypes.add(ntc);
		
	}
	
	public void processNewTypes(DatabaseConnection conn) throws RepositoryException{
/*
		if (ntc.getNodeType().getTableName().equals("CM_TYPE_BASE")){
            DatabaseInsertStatement insert = JCRHelper.createNodeTypeDetailsStatement(state.getNodeId(), ntc.getNodeType().getTableName());
            statements.add(insert);
        }
	*/	
		
		if (newTypes.size() > 0){
	        DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(TABLE_TYPE);
	        //DatabaseInsertStatement baseinsert = DatabaseTools.createInsertStatement("CM_TYPE_BASE");
	        //boolean basePresent = false;
	        for(NodeTypeContainer state:newTypes){
		        //insert.addValue(SQLParameter.create(FIELD_ID, state.getId()));
		        insert.addValue(SQLParameter.create(FIELD_TYPE_ID, state.getNodeId()));
		        insert.addValue(SQLParameter.create(TABLE_TYPE__NODE_TYPE, state.getNodeTypeId()));
		        insert.addValue(SQLParameter.create(TABLE_TYPE__FROM_NODE_TYPE, state.getFromTypeId()));
		        insert.addBatch();
		        /*if (state.getNodeType().getTableName().equals("CM_TYPE_BASE")){
		        	basePresent = true;
		        	baseinsert.addValue(SQLParameter.create(FIELD_TYPE_ID, state.getNodeId()));
		        	baseinsert.addBatch();
		        }*/
	        }
	        insert.execute(conn);
	        /*if (basePresent){
	        	baseinsert.execute(conn);
	        }*/
		}
	}

	public void addNewFTSProperty(_PropertyState prop) {
		newFTSProperties .add(prop);
		
	}
	
	public void addRemoveFTSProperty(_PropertyState prop) {
		removeFTSProperties .add(prop);
		
	}
	
	public void processNewFTSProperties(DatabaseConnection conn) throws RepositoryException{
		if (newFTSProperties.size() > 0){
	        DatabaseInsertStatement ftsinsert = DatabaseTools.createInsertStatement(Constants.TABLE_FTS_DATA);
	        DatabaseInsertStatement ftsinsert2 = DatabaseTools.createInsertStatement(Constants.TABLE_FTS_STAGE);
	        DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(Constants.TABLE_INDEXABLE_DATA);
	        boolean ftsinsert2Present = false;
	        for(_PropertyState prop:newFTSProperties){
	        	
                Long ftsId = conn.nextId();
                String contentId = prop.getValues()[0].getContentId();
                //PropertyDefinitionImpl definition = prop.getDefinition();
                
                
                ftsinsert.addValue(SQLParameter.create(Constants.FIELD_ID, ftsId));
                ftsinsert.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID, prop.getParent().getNodeId()));
                JCRHelper.populateQName(ftsinsert, prop.getName(), namespaceRegistry);
                ftsinsert.addBatch();
                
                BLOBFileValue v = (BLOBFileValue) prop.getValues()[0].internalValue();
                InputStream textStream = v.getTextStream();
                if (textStream != null){
                	//ftsinsert2 = new DatabaseInsertStatement(Constants.TABLE_FTS_STAGE);
                	ftsinsert2.addValue(SQLParameter.create(Constants.FIELD_ID, ftsId));
                	ftsinsert2.addValue(SQLParameter.create(Constants.TABLE_FTS_STAGE__DATA, textStream,(int) v.getTextStreamLength()));
                    //st.addValue(SQLParameter.create(Constants.TABLE_FTS_STAGE__FILENAME, "extracted.txt"));
                    ftsinsert2.addBatch();
                    ftsinsert2Present = true;
                }
	        	
	        	
	        	
	        	insert.addValue(SQLParameter.create(Constants.FIELD_ID, conn.nextId()));
                //st.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID, parent.getNodeId()));
                //JCRHelper.populateQName(st, getQName(), _getNamespaceRegistry());

                insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__FTS_DATA_ID, ftsId));
                insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA, contentId));
                if (textStream != null){
                	insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__FTS_STAGE_ID, ftsId));
                }
                insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__RESERVED, 0));
                insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__TIME, Calendar.getInstance()));
                
                QName mimeTypePropName = null;
                PropDef[] _props = prop.getParent().getEffectiveNodeType().getAllPropDefs();
                for(PropDef p : _props){
                    if (p.getName().getLocalName().equals("mimeType")){
                        mimeTypePropName = p.getName();
                        break;
                    }
                }
                
                /*PropertyDefinition[] props = (PropertyDefinition[]) definition.getDeclaringNodeType().getPropertyDefinitions();
                for(PropertyDefinition p : props){
                    if (p.getName().endsWith(":mimeType")){
                        mimeTypePropName = ((PropertyDefinitionImpl)p).getQName();
                        break;
                    }
                }*/
                if (mimeTypePropName == null){
                    //throw new RepositoryException("Can not find mimeType property for FTS property "+prop.getName());
                } else {
                	insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__MIME_TYPE, prop.getParent().getProperty(mimeTypePropName, true).getString()));
                }
                
                insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__OPERATION, Constants.OPERATION_INSERT));
		        insert.addBatch();
	        }
	        ftsinsert.execute(conn);
	        if (ftsinsert2Present){
	        	ftsinsert2.execute(conn);
	        }
	        insert.execute(conn);
		}
	}

	public void addNewReference(NodeReference nr) {
		newReferences .add(nr);
		
	}
	
	public void processNewReferences(DatabaseConnection conn) throws RepositoryException{
		if (newReferences.size() > 0){
			DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(TABLE_NODE_REFERENCE);
	        for(NodeReference nr:newReferences){
                insert.addValue(SQLParameter.create(FIELD_ID, nr.getId()));
                insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__FROM, nr.getFromId()));
                insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__TO, nr.getToId()));
                insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__PROPERTY_NAME, nr.getPropertyName()));
                insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__PROPERTY_NAMESPACE, nr.getPropertyNamespaceId()));
                insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__UUID, nr.getUUID()));
		        insert.addBatch();
	        }
	        insert.execute(conn);
		}
	}

	
	public void processRemoveFTSProperties(DatabaseConnection conn) throws RepositoryException{
		
	if (removeFTSProperties.size() > 0){
			
			ArrayList<DatabaseCondition> conditions = new ArrayList<DatabaseCondition>();
			ArrayList<_PropertyState> completed = new ArrayList<_PropertyState>();
			HashMap<String, _PropertyState> cache = new HashMap<String, _PropertyState>();
			for(_PropertyState prop:removeFTSProperties){
				
				DatabaseCondition c1 = Conditions.eq(Constants.FIELD_TYPE_ID, prop.getNodeId());
		        AndDatabaseCondition c = Conditions.and();
		        c.add(c1);
				ArrayList<SQLParameter> params = JCRHelper.getSQLParametersFromQname(prop.getName(), this.namespaceRegistry, true);
		        for(SQLParameter p : params){
		            c.add(Conditions.eq(p.getName(), p.getValue()));
		        }
		        conditions.add(c);

		        Long namespaceId = namespaceRegistry._getByURI(prop.getName().getNamespaceURI()).getId();
		        cache.put(prop.getNodeId().toString()+"_"+namespaceId+"_"+prop.getName().getLocalName(), prop);
			}
			DatabaseInsertStatement st1 = new DatabaseInsertStatement(Constants.TABLE_INDEXABLE_DATA);
			DatabaseInsertStatement st2 = new DatabaseInsertStatement(Constants.TABLE_INDEXABLE_DATA);
			boolean hasSt1 = false;
			boolean hasSt2 = false;
			
			while (conditions.size() > 0){
				DatabaseSelectAllStatement select = new DatabaseSelectAllStatement(Constants.TABLE_FTS_DATA, true);
				select.addResultColumn(Constants.FIELD_ID);
				select.addResultColumn(Constants.FIELD_TYPE_ID);
				select.addResultColumn(Constants.FIELD_NAME);
				select.addResultColumn(Constants.FIELD_NAMESPACE);
				
				ArrayList<DatabaseCondition> processedConditions = new ArrayList<DatabaseCondition>();
				for(int i = 0 ; i < 50 && i <  conditions.size() ; i++){
					processedConditions.add(conditions.get(i));
				}
				select.addCondition(Conditions.or(processedConditions));
				select.execute(conn);
				while (select.hasNext()){
					hasSt1 = true;
					RowMap row = select.nextRow();
					long ftsDataId = row.getLong(Constants.FIELD_ID);
		            st1.addValue(SQLParameter.create(Constants.FIELD_ID, conn.nextId()));
	            	st1.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__FTS_DATA_ID, ftsDataId));

	            	//find prop
	            	Long nodeId = row.getLong(Constants.FIELD_TYPE_ID);
	            	Long namespaceId = row.getLong(Constants.FIELD_NAMESPACE);
	            	String name = row.getString(Constants.FIELD_NAME);
	            	String id = nodeId+"_"+namespaceId+"_"+name;
	            	_PropertyState prop = cache.get(id);
	            	completed.add(prop);
		            String contentId = prop.getInitialValues()[0].getContentId();
		            
		            st1.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA, contentId));
		            st1.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__OPERATION, Constants.OPERATION_DELETE));
		            st1.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__RESERVED, 0));
		            st1.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__TIME, Calendar.getInstance()));
		            st1.addBatch();
	            	//throw new UnsupportedOperationException();
					
				}
				select.close();
				conditions.removeAll(processedConditions);
			}
			removeFTSProperties.removeAll(completed);
			for(_PropertyState prop:removeFTSProperties){
				hasSt2= true;
	            st2.addValue(SQLParameter.create(Constants.FIELD_ID, conn.nextId()));

	            String contentId = prop.getInitialValues()[0].getContentId();
	            
	            st2.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA, contentId));
	            st2.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__OPERATION, Constants.OPERATION_DELETE));
	            st2.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__RESERVED, 0));
	            st2.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__TIME, Calendar.getInstance()));
	            st2.addBatch();
			}
			
			if (hasSt1){
				st1.execute(conn);
			}
			if (hasSt2){
				st2.execute(conn);
			}
			
			/*
            DatabaseSelectAllStatement select = new DatabaseSelectAllStatement(Constants.TABLE_FTS_DATA, true);
            select.addCondition(Conditions.eq(Constants.FIELD_TYPE_ID, node.getNodeId()));
            JCRHelper.populateQNameCondition(select, prop.getName(), getNamespaceRegistry());
            select.execute(conn);
            Long ftsDataId = null;
            if (select.hasNext()){
                RowMap row = select.nextRow(); 
                ftsDataId = row.getLong(Constants.FIELD_ID);   
            }
            DatabaseInsertStatement st = new DatabaseInsertStatement(Constants.TABLE_INDEXABLE_DATA);
            st.addValue(SQLParameter.create(Constants.FIELD_ID, conn.nextId()));
            if (ftsDataId != null){
            	st.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__FTS_DATA_ID, ftsDataId));
            }

            String contentId = prop.getInitialValues()[0].getContentId();
            st.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA, contentId));
            st.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__OPERATION, Constants.OPERATION_DELETE));
            st.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__RESERVED, 0));
            st.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__TIME, Calendar.getInstance()));
            result.add(st);
            
            select.close();
            
            */
            
			
			
			
	        /*DatabaseInsertStatement ftsinsert = DatabaseTools.createInsertStatement(Constants.TABLE_FTS_DATA);
	        DatabaseInsertStatement ftsinsert2 = DatabaseTools.createInsertStatement(Constants.TABLE_FTS_STAGE);
	        DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(Constants.TABLE_INDEXABLE_DATA);*/
	        //boolean ftsinsert2Present = false;
	        //for(_PropertyState prop:removeFTSProperties){
	        	
/*                Long ftsId = conn.nextId();
                String contentId = prop.getValues()[0].getContentId();
                PropertyDefinitionImpl definition = prop.getDefinition();
                
                ftsinsert.addValue(SQLParameter.create(Constants.FIELD_ID, ftsId));
                ftsinsert.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID, prop.getParent().getNodeId()));
                JCRHelper.populateQName(ftsinsert, prop.getName(), namespaceRegistry);
                ftsinsert.addBatch();
                
                BLOBFileValue v = (BLOBFileValue) prop.getValues()[0].internalValue();
                InputStream textStream = v.getTextStream();
                if (textStream != null){
                	//ftsinsert2 = new DatabaseInsertStatement(Constants.TABLE_FTS_STAGE);
                	ftsinsert2.addValue(SQLParameter.create(Constants.FIELD_ID, ftsId));
                	ftsinsert2.addValue(SQLParameter.create(Constants.TABLE_FTS_STAGE__DATA, textStream,(int) v.getTextStreamLength()));
                    //st.addValue(SQLParameter.create(Constants.TABLE_FTS_STAGE__FILENAME, "extracted.txt"));
                    ftsinsert2.addBatch();
                    ftsinsert2Present = true;
                }
	        	
	        	
	        	
	        	insert.addValue(SQLParameter.create(Constants.FIELD_ID, conn.nextId()));
                //st.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID, parent.getNodeId()));
                //JCRHelper.populateQName(st, getQName(), _getNamespaceRegistry());

                insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__FTS_DATA_ID, ftsId));
                insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA, contentId));
                if (textStream != null){
                	insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__FTS_STAGE_ID, ftsId));
                }
                insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__RESERVED, 0));
                insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__TIME, Calendar.getInstance()));
                
                QName mimeTypePropName = null;
                PropertyDefinition[] props = (PropertyDefinition[]) definition.getDeclaringNodeType().getPropertyDefinitions();
                for(PropertyDefinition p : props){
                    if (p.getName().endsWith(":mimeType")){
                        mimeTypePropName = ((PropertyDefinitionImpl)p).getQName();
                        break;
                    }
                }
                if (mimeTypePropName == null){
                    throw new RepositoryException("Can not find mimeType property for FTS property "+prop.getName());
                }
                
                insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__MIME_TYPE, prop.getParent().getProperty(mimeTypePropName, true).getString()));
                
                insert.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__OPERATION, Constants.OPERATION_INSERT));
		        insert.addBatch();*/
	        //}
	        //ftsinsert.execute(conn);
	        //if (ftsinsert2Present){
	        //	ftsinsert2.execute(conn);
	        //}
	        //insert.execute(conn);
		}
	}

	HashMap<Long, Long> nodeVersions = new HashMap<Long, Long>(); 
	
	public void addSetNodeVersion(Long nodeId, long l) {
		nodeVersions.put(nodeId, l);
		
	}
	
	public void processSetNodeVersions(DatabaseConnection conn) throws RepositoryException{
		if (nodeVersions.size() > 0){
			DatabaseUpdateStatement insert = DatabaseTools.createUpdateStatement(TABLE_NODE);
	        for(Long id:nodeVersions.keySet()){
	        	insert.addCondition(Conditions.eq(FIELD_ID, id));
                insert.addValue(SQLParameter.create(TABLE_NODE__VERSION_, nodeVersions.get(id)));
                insert.addBatch();
	        }
	        insert.execute(conn);
		}
	}
	
	private ArrayList<_PropertyState> unstructPropMultiValue = new ArrayList<_PropertyState>();

	public void addNewUnstructuredMultiValueProperty(_PropertyState state, PropertyDefinitionImpl definition) {
		unstructPropMultiValue.add(state);
	}
	
	public void processNewUnstructuredMultiValueProperty(DatabaseConnection conn) throws RepositoryException{
		if (unstructPropMultiValue.size() > 0){
			
        	DatabaseInsertStatement st1 = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED);;
        	DatabaseInsertStatement st2 = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED);;
        	
        	DatabaseInsertStatement st_1 = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED_VALUES);
        	DatabaseInsertStatement st_2 = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED_VALUES);
        	DatabaseInsertStatement st_3 = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED_VALUES);
        	DatabaseInsertStatement st_4 = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED_VALUES);
        	DatabaseInsertStatement st_5 = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED_VALUES);
        	DatabaseInsertStatement st_6 = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED_VALUES);
        	DatabaseInsertStatement st_7 = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED_VALUES);
        	DatabaseInsertStatement st_8 = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED_VALUES);
        	DatabaseInsertStatement st_9 = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED_VALUES);
        	DatabaseInsertStatement st_10 = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED_VALUES);

        	for(_PropertyState state:unstructPropMultiValue){
        		PropertyDefinitionImpl definition = state.getDefinition();
        		DatabaseInsertStatement st;
        		if (state.getName().getNamespaceURI() != null && state.getName().getNamespaceURI().length() > 0){
        			st = st1;
        		} else {
        			st = st2;
        		}
        		/*System.out.println(state.getName().getLocalName());
        		if (state.getName().getLocalName().length() > 0){
        			 Namespace iddd = namespaceRegistry._getByURI(state.getName().getLocalName());
        			System.out.println(iddd);
        		}*/
        		
                st.addValue(SQLParameter.create(FIELD_ID, state.getUnstructuredPropertyId()));
                st.addValue(SQLParameter.create(FIELD_TYPE_ID, state.getNodeId()));
                st.addValue(SQLParameter.create(TABLE_NODE_UNSTRUCTURED__NODE_TYPE, ((NodeTypeImpl)definition.getDeclaringNodeType()).getSQLId()));
                st.addValue(SQLParameter.create(TABLE_NODE_UNSTRUCTURED__PROP_DEF, definition.getSQLId()));
                JCRHelper.populateQName(st, state.getName(), this.namespaceRegistry);
                st.addValue(SQLParameter.create(TABLE_NODE_UNSTRUCTURED__TYPE, new Long(state.getType()) ));
                st.addValue(SQLParameter.create(TABLE_NODE_UNSTRUCTURED__MULTIPLE, definition.isMultiple()));
                st.addBatch();
                
                InternalValue values[] = state.getValues();
                for(int i = 0 ; i < values.length ; i++){
                    Value vv = values[i].toJCRValue(namespaceRegistry);
                    //save unstructured property
                    //TODO implement order
                    switch (vv.getType()) {
					case PropertyType.BINARY:
						st = st_1;
						break;
					case PropertyType.BOOLEAN:
						st = st_2;
						break;
					case PropertyType.DATE:
						st = st_3;
						break;
					case PropertyType.DOUBLE:
						st = st_4;
						break;
					case PropertyType.LONG:
						st = st_5;
						break;
					case PropertyType.NAME:
						st = st_6;
						break;
					case PropertyType.PATH:
						st = st_7;
						break;
					case PropertyType.REFERENCE:
						st = st_8;
						break;
					case PropertyType.STRING:
						st = st_9;
						break;
					case PropertyType283.WEAKREFERENCE:
						st = st_10;
						break;

					default:
						throw new UnsupportedRepositoryOperationException("Unsupported property type");
					}
                    
                    Long nextId = conn.nextId();
                    values[i].setSQLId(nextId);
                    st.addValue(SQLParameter.create(FIELD_ID, nextId));
                    st.addValue(SQLParameter.create(TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY, state.getUnstructuredPropertyId()));
                    st.addValue(SQLParameter.create(TABLE_NODE_UNSTRUCTURED__PROP_DEF, definition.getSQLId()));
                    st.addValue(SQLParameter.create(FIELD_TYPE_ID, state.getNodeId()));
                    if (vv.getType() == PropertyType.LONG || vv.getType() == PropertyType.DATE){
                        st.addValue(SQLParameter._create(sc ,TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE, vv.getDouble(), values[i]));
                    }
                    st.addValue(SQLParameter._create(sc ,JCRHelper.getValueColumn(vv.getType()), JCRHelper.getValueObject(vv), values[i]));
                    st.addBatch();
                }
                
                
        	}
        	
        	
    		st1.executeBatch(conn);
    		st2.executeBatch(conn);
    		st_1.executeBatch(conn);
    		st_2.executeBatch(conn);
    		st_3.executeBatch(conn);
    		st_4.executeBatch(conn);
    		st_5.executeBatch(conn);
    		st_6.executeBatch(conn);
    		st_7.executeBatch(conn);
    		st_8.executeBatch(conn);
    		st_9.executeBatch(conn);
    		st_10.executeBatch(conn);

		}
	}

	public void processNewOCRProperties(DatabaseConnection conn) throws RepositoryException, IllegalNameException, UnknownPrefixException{
		if (newOcrWorks.size() > 0){
			boolean execute = false;
			DatabaseInsertStatement st1 = DatabaseTools.createInsertStatement(Constants.TABLE_OCR_DATA);
			Calendar c = Calendar.getInstance();
			for(Long id:newOcrWorks.keySet()){
				_NodeState state = newOcrWorks.get(id);
				String propName = (String )state.getProperty(Constants.ECR_OCR_MIXIN__BINARY_PROPERTY_NAME, true).getValue().internalValue();
				_PropertyState binaryValue = state.getProperty(QName.fromJCRName(propName, namespaceRegistry), false);
				if (binaryValue != null) {
					InternalValue value = binaryValue.getValue();
					BLOBFileValue vv =  (BLOBFileValue) value.internalValue();
					
					st1.addValue(SQLParameter.create(Constants.FIELD_ID, id));
					st1.addValue(SQLParameter.create(Constants.TABLE_OCR_DATA__COMPLETION_DATE, c));
					st1.addValue(SQLParameter.create(Constants.TABLE_OCR_DATA__OPERATION, 0L));
					st1.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__RESERVED, false));
					st1.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__TIME, Calendar.getInstance()));		
					st1.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA, vv.getContentId()));
					st1.addBatch();
					execute = true;
				}
			}
			if (execute){
				st1.executeBatch(conn);
			}
		}
	}

	public void processRemoveOCRProperties(DatabaseConnection conn) {
		
	}

	public void scheduleOCRWork(Long workId, _NodeState _state) {
		newOcrWorks.put(workId, _state);
		
	}

	public void addRemoveNode(Long nodeId, _NodeState state) {
		removedNodes.put(nodeId, state);
	}

	public void processRemovedNodesStep1(DatabaseConnection conn) throws RepositoryException{
		for(Long nodeId:removedNodes.keySet()){
			System.out.println("Update index for "+nodeId+" : "+removedNodes.get(nodeId));
            DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(TABLE_NODE);
            st.addCondition(Conditions.eq(FIELD_ID, nodeId));
            st.addValue(SQLParameter.create(TABLE_NODE__INDEX, -conn.nextId()));
            st.execute(conn);

		}
	}

	
	public void addBasePropertyChanged(_NodeState _state, DatabaseUpdateStatement st) {
		basePropertyChanged.put(_state, st);
		
	}

}
