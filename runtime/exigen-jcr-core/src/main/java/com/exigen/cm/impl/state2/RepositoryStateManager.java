/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.FIELD_TYPE_ID;
import static com.exigen.cm.Constants.TABLE_NODE;
import static com.exigen.cm.Constants.TABLE_NODE_LOCK_INFO__LOCK_OWNER;
import static com.exigen.cm.Constants.TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID;
import static com.exigen.cm.Constants.TABLE_NODE_PARENT;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__FROM;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__TO;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED_VALUES;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__MULTIPLE;
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
import static com.exigen.cm.Constants.TABLE_TYPE;
import static com.exigen.cm.Constants._TABLE_NODE_LOCK_INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.PropertyDefinition;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.DatabaseCondition;
import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.impl.NodeReference;
import com.exigen.cm.impl.NodeTypeContainer;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.NodeTypeManagerImpl;
import com.exigen.cm.impl.ParentNode;
import com.exigen.cm.impl.PropertyDefinitionImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.nodetype.PropDefImpl;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class RepositoryStateManager  implements _StateManager{

	NodeStateSoftHashMap _nodeCache = new NodeStateSoftHashMap(100);
	private RepositoryImpl repository;
	private NodeTypeManagerImpl nodeTypeManager;
	
    HashMap<String, Long> uuids = new HashMap<String, Long>(10000);

    
    public RepositoryStateManager(RepositoryImpl repository){
    	this.repository = repository;
    }

    ArrayList<_NodeState> findNodeState(List<Long> nodeIds, DatabaseConnection conn, String objectDescription) throws RepositoryException{
    	return findNodeState(nodeIds, conn, objectDescription, 0);
    }
    
	private ArrayList<_NodeState> findNodeState(List<Long> nodeIds, DatabaseConnection conn, String objectDescription, int counter) throws RepositoryException{
		ArrayList<_NodeState> results = new ArrayList<_NodeState>();
		ArrayList<Long> notFound = new ArrayList<Long>();
		//1. find in cache
		for(Long id:nodeIds){
			_NodeState n = getFromCache(id);
			if (n != null){ 
				results.add(n);
			} else {
				notFound.add(id);
			}
		}
		//2. check version
		if (results.size() > 0 ){
			ArrayList<_NodeState> incorrectVersion = checkNodeVersions(results, conn);
			results.removeAll(incorrectVersion);
			for(_NodeState st:incorrectVersion){
				notFound.add(st.getNodeId());
			}
		}
		
		if (notFound.size() == 0){
			ArrayList<_NodeState> _results = new ArrayList<_NodeState>();
			for(_NodeState n : results){
				_results.add(n.createCopy());
			}
			_loadReferences(_results, conn);
			loadLockInfo(_results, conn);
			return _results;
		}
		
		/*_NodeState n = getFromCache(nodeId);
		if (n != null){
			//TODO check for version
			Long oldVersion = n.getVersion();
			Long newVersion = getNodeVersion(nodeId, conn);
			if (oldVersion.equals(newVersion)){
				//update references
				_NodeState result;
				synchronized (n) {
					result = n.createCopy();
				}
				loadReferences(result, conn);
				return result;
			}
		}*/
		//for(Long nodeId:notFound) {
			//_NodeState n = loadNode(nodeId, conn);
			results.addAll(loadNode(notFound, conn, objectDescription));
		//}
		return findNodeState(nodeIds, conn, objectDescription, counter+1);
	}

	private void loadLockInfo(ArrayList<_NodeState> nodes, DatabaseConnection conn) throws RepositoryException{
		
		HashMap<Long,_NodeState> ids = new HashMap<Long,_NodeState>();
		for(_NodeState state:nodes){
			ids.put(state.getNodeId(), state);
		}
		
		DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(_TABLE_NODE_LOCK_INFO, true);
        st.addCondition(Conditions.in(FIELD_TYPE_ID, ids.keySet()));
        st.execute(conn);
        while (st.hasNext()){
            RowMap row = st.nextRow();
            Long id =  row.getLong(FIELD_TYPE_ID);
            _NodeState state = ids.get(id);
    	    state.setParentLockId(row.getLong(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID));
    	    state.setLockOwner(row.getString(TABLE_NODE_LOCK_INFO__LOCK_OWNER));
        }		
	}

	private ArrayList<_NodeState> checkNodeVersions(ArrayList<_NodeState> nodes, DatabaseConnection conn) throws RepositoryException {
		JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
		boolean error = false;
		try {
			ArrayList<Long> ids = new ArrayList<Long>();
			for(_NodeState st:nodes){
				ids.add(st.getNodeId());
			}
			
			//ArrayList<_NodeState> stateCopy = new ArrayList<_NodeState>(nodes);
			
			ArrayList<_NodeState> result = new ArrayList<_NodeState>();
			
			DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_NODE, true);
			st.addCondition(Conditions.in(Constants.FIELD_ID, ids));
			st.addResultColumn(Constants.TABLE_NODE__VERSION_);
			st.addResultColumn(Constants.FIELD_ID);
			st.execute(conn);
			while (st.hasNext()){
				RowMap row = st.nextRow();
				Long nodeId =  row.getLong(Constants.FIELD_ID);
				Long versionId = row.getLong(Constants.TABLE_NODE__VERSION_);
				
				//find Node state
				for(_NodeState _st:nodes){
					if (_st.getNodeId().equals(nodeId)){
						_NodeState n = null;
						n = _st;
						ids.remove(n.getNodeId());
						if (!n.getVersion().equals(versionId)){
							result.add(n);
						}
						//break;
					}
				}
			}
			for(Long nodeId:ids){
				//_NodeState n = null;
				for(_NodeState _st:nodes){
					if (_st.getNodeId().equals(nodeId)){
						result.add(_st);
						break;
					}
				}
			}
			
			return result;
			
		} catch (RepositoryException exc){
			error= true;
			throw exc;
		} finally {
			if (tr!= null){
				if (error){
					TransactionHelper.getInstance().rollbackAndResore(tr);
				} else {
					TransactionHelper.getInstance().commitAndResore(tr);
				}
			}

		}
	}

	private Collection<_NodeState> loadNode(ArrayList<Long> nodeIds, DatabaseConnection conn, String objectDescription) throws RepositoryException {
		
		//TODO block multi thread read with same id
		Collection<_NodeState> nodes = loadFromDB(nodeIds, conn, objectDescription);
		
		for(_NodeState n: nodes){
			
			_NodeState fromCache = getFromCache(n.getNodeId());
			if (fromCache != null) {
				JCRTransaction inTransaction = fromCache.getCreateInTransaction();
				if (inTransaction != null) {
					//should set transaction if transaction presents in cache
					//if node has been created and version has been updated during one transaction
					//then info that this node has been created in transaction is lost.
					//Girts Ivans
					n.setCreatedInTransaction(inTransaction);
				}			
			}
			putToCache(n.getNodeId(), n);
		}
		
		return nodes;
	}

	private Collection<_NodeState> loadFromDB(ArrayList<Long> ids, DatabaseConnection conn, String objectDescription) throws RepositoryException {
		long start = System.currentTimeMillis();
		HashMap<Long, _NodeState> result = new HashMap<Long, _NodeState>();
		for(Long id:ids){
			result.put(id, new _NodeState(id, repository));
		}
		
		DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_NODE, true);
		st.addCondition(Conditions.in(Constants.FIELD_ID, ids));
		st.execute(conn);
		int total = 0;
		while (st.hasNext()){
			total++;
			RowMap row = st.nextRow();

			Long id = row.getLong(Constants.FIELD_ID);
			_NodeState state = result.get(id);
			
	        state.setName(JCRHelper.assembleQName(row, getNamespaceRegistry()));
	        state.setParentId(row.getLong(TABLE_NODE__PARENT));
	        state.setStoreConfigurationId(row.getLong(Constants.TABLE_NODE__CONTENT_STORE_CONFIG_NODE));
	        state.setVersion(row.getLong(TABLE_NODE__VERSION_));
	        state.setSecurityId(row.getLong(TABLE_NODE__SECURITY_ID));
	        state.setWorkspaceId(row.getLong(Constants.TABLE_NODE__WORKSPACE_ID));
	        //state.setParentLockId(row.getLong(TABLE_NODE__PARENT_LOCK_ID));
	        state.setIndex(row.getLong(TABLE_NODE__INDEX));
	        state.setSnsMax(row.getLong(TABLE_NODE__INDEX_MAX));
	        
	        state.setInternalDepth(row.getLong(TABLE_NODE__NODE_DEPTH));
	        state.setInternalPath(row.getString(TABLE_NODE__NODE_PATH));
	
	        state.setNodeTypeId(row.getLong(TABLE_NODE__NODE_TYPE));
		}
		if (total != ids.size()){
			if (objectDescription != null){
				throw new ItemNotFoundException(objectDescription);
			} else {
				throw new ItemNotFoundException();
			}
		}
        //load all types
        loadTypes(result, conn);
        loadParentNodes(result, conn);
        createDefaultProperties(result);
        loadProperties(result, conn);
        //loadReferences(state, conn);
        
        
        for(_NodeState state:result.values()){
        	state.setStatusNormal();
        }
		return result.values();
	}


	private void loadParentNodes(HashMap<Long, _NodeState> nodes, DatabaseConnection conn) throws RepositoryException {
		long start = System.currentTimeMillis();
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_NODE_PARENT, false);
        st.addCondition(Conditions.in(FIELD_TYPE_ID, nodes.keySet()));
        st.execute(conn);
        while (st.hasNext()){
            RowMap row = st.nextRow();
            Long nodeId = row.getLong(FIELD_TYPE_ID);
            _NodeState state = nodes.get(nodeId);
            state.addParentNode(new ParentNode(row));
        }
	}
	public void _loadReferencesTo(_NodeState node, DatabaseConnection conn) throws RepositoryException {
		ArrayList<_NodeState> list = new ArrayList<_NodeState>();
		list.add(node);
		HashMap<Long,_NodeState> ids = new HashMap<Long,_NodeState>();
		node.getReferencesTo().clear();
		ids.put(node.getNodeId(), node);
		
		DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_NODE_REFERENCE, true);
        DatabaseCondition c1 = Conditions.in(TABLE_NODE_REFERENCE__FROM, ids.keySet());
        st.addCondition(c1);
        st.execute(conn);
        while (st.hasNext()){
            RowMap row = st.nextRow();
            Long fromId =  row.getLong(TABLE_NODE_REFERENCE__FROM);
            if (ids.containsKey(fromId)){
            	_NodeState state = ids.get(fromId);
            	state.registerReferencesTo((new NodeReference(getNamespaceRegistry(), row)));
            } 
        }		

	}

	public void _loadReferencesFrom(_NodeState node, DatabaseConnection conn) throws RepositoryException {
		HashMap<Long,_NodeState> ids = new HashMap<Long,_NodeState>();
		//state.getReferencesTo().clear();
		node.resetReferencesFrom();
		ids.put(node.getNodeId(), node);
		
		DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_NODE_REFERENCE, true);
        //DatabaseCondition c1 = Conditions.in(TABLE_NODE_REFERENCE__FROM, ids.keySet());
        DatabaseCondition c2 = Conditions.in(TABLE_NODE_REFERENCE__TO, ids.keySet());
        //st.addCondition(Conditions.or(c1,c2));
        st.addCondition(c2);
        st.execute(conn);
        while (st.hasNext()){
            RowMap row = st.nextRow();
            Long toId = row.getLong(TABLE_NODE_REFERENCE__TO);
            if (ids.containsKey(toId)) {
            	_NodeState state = ids.get(toId);
            	state.registerReferencesFrom(new NodeReference(getNamespaceRegistry(), row));
            }
        }		

	}
	public void _loadReferences(ArrayList<_NodeState> nodes, DatabaseConnection conn) throws RepositoryException {
		if (true){
			return;
		}
		/*state.getReferencesTo().clear();
		DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_NODE_REFERENCE, true);
        st.addCondition(Conditions.eq(TABLE_NODE_REFERENCE__FROM, state.getNodeId()));
        st.execute(conn);
        while (st.hasNext()){
            HashMap row = st.nextRow();
            state.addReferencesTo((new NodeReference(getNamespaceRegistry(), row)));
        }		
        
		state.resetReferencesFrom();
        st = DatabaseTools.createSelectAllStatement(TABLE_NODE_REFERENCE, true);
        st.addCondition(Conditions.eq(TABLE_NODE_REFERENCE__TO, state.getNodeId()));
        st.execute(conn);
        while (st.hasNext()){
            HashMap row = st.nextRow();
            state.addReferencesFrom(new NodeReference(getNamespaceRegistry(), row));
        }*/
        
		//Long nodeId = state.getNodeId();
		HashMap<Long,_NodeState> ids = new HashMap<Long,_NodeState>();
		for(_NodeState state:nodes){
			//state.getReferencesTo().clear();
			state.resetReferencesFrom();
			ids.put(state.getNodeId(), state);
		}
		
		DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_NODE_REFERENCE, true);
        DatabaseCondition c1 = Conditions.in(TABLE_NODE_REFERENCE__FROM, ids.keySet());
        //DatabaseCondition c2 = Conditions.in(TABLE_NODE_REFERENCE__TO, ids.keySet());
        //st.addCondition(Conditions.or(c1,c2));
        st.addCondition(c1);
        st.execute(conn);
        while (st.hasNext()){
            RowMap row = st.nextRow();
            Long fromId =  row.getLong(TABLE_NODE_REFERENCE__FROM);
            //Long toId = row.getLong(TABLE_NODE_REFERENCE__TO);
            if (ids.containsKey(fromId)){
            	_NodeState state = ids.get(fromId);
            	//state.registerReferencesTo((new NodeReference(getNamespaceRegistry(), row)));
            } 
            /*if (ids.containsKey(toId)) {
            	_NodeState state = ids.get(toId);
            	state.addReferencesFrom(new NodeReference(getNamespaceRegistry(), row));
            }*/
        }		
		st = DatabaseTools.createSelectAllStatement(TABLE_NODE_REFERENCE, true);
        //DatabaseCondition c1 = Conditions.in(TABLE_NODE_REFERENCE__FROM, ids.keySet());
        DatabaseCondition c2 = Conditions.in(TABLE_NODE_REFERENCE__TO, ids.keySet());
        //st.addCondition(Conditions.or(c1,c2));
        st.addCondition(c2);
        st.execute(conn);
        while (st.hasNext()){
            RowMap row = st.nextRow();
            //Long fromId =  row.getLong(TABLE_NODE_REFERENCE__FROM);
            Long toId = row.getLong(TABLE_NODE_REFERENCE__TO);
            /*if (ids.containsKey(fromId)){
            	_NodeState state = ids.get(fromId);
            	state.addReferencesTo((new NodeReference(getNamespaceRegistry(), row)));
            } */
            if (ids.containsKey(toId)) {
            	_NodeState state = ids.get(toId);
            	state.addReferencesFrom(new NodeReference(getNamespaceRegistry(), row));
            }
        }		
	}

	private void loadTypes(HashMap<Long, _NodeState> nodes, DatabaseConnection conn) throws RepositoryException {
        HashMap<Long, ArrayList<NodeTypeContainer>> sts = new HashMap<Long, ArrayList<NodeTypeContainer>>();
        //load all node types
        //long start = System.currentTimeMillis();
        nodes.keySet();
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_TYPE, false);
        st.addCondition(Conditions.in(FIELD_TYPE_ID, nodes.keySet()));
        st.execute(conn);
        while(st.hasNext()){
            RowMap row = st.nextRow();
            Long nodeId = row.getLong(FIELD_TYPE_ID);
            //_NodeState state = nodes.get(nodeId);
            ArrayList<NodeTypeContainer> result = sts.get(nodeId);
            if (result == null){
            	result = new ArrayList<NodeTypeContainer>();
            	sts.put(nodeId, result);
            }
            

            result.add(new  NodeTypeContainer(row, getNodeTypeManager()));
        }
        for(Long id:sts.keySet()){
        	ArrayList<NodeTypeContainer> result = sts.get(id);
        	_NodeState state = nodes.get(id);
            state.setNodeTypes(result);
        }
	}

	private void loadProperties(HashMap<Long, _NodeState> nodes, DatabaseConnection conn) throws RepositoryException {
        HashMap<String, RowMap> rows = new HashMap<String, RowMap>();
        HashMap<Long, Boolean> unstr = new HashMap<Long, Boolean>();
		for(_NodeState state:nodes.values()){
			boolean keepChanges = false;
	        List<NodeTypeContainer> _types = state.getAllEffectiveTypes();
	        //load property for each type
	        //boolean allowUnstructured = false;
	        //boolean allowUnstructuredMultivalued = false;
	        for(NodeTypeContainer ntc:_types){
	        	for(PropertyDefinition p:ntc.getNodeType().getPropertyDefinitions()){
	        		if (((PropertyDefinitionImpl)p).isUnstructured()){
	        			unstr.put(state.getNodeId(), false);
	        		}
	        	}
	            NodeTypeImpl nt = ntc.getNodeType();
	            String tableName = nt.getTableName();
	            String _tableName = tableName+"__"+state.getNodeId();
	            RowMap row = rows.get(_tableName);
	            if (row == null){
		            //DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(tableName, FIELD_TYPE_ID, state.getNodeId());
	            	DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(tableName, false);
	            	//FIELD_TYPE_ID, state.getNodeId()
	            	st.addCondition(Conditions.in(FIELD_TYPE_ID, nodes.keySet()));
		            st.execute(conn);
		            while (st.hasNext()){
			            row = st.nextRow();
			            rows.put(tableName+"__"+row.getLong(FIELD_TYPE_ID), row);
		            }
		            row = rows.get(_tableName);
		            st.close();
	            }
	            for(Iterator it1 = row.keySet().iterator() ; it1.hasNext() ; ){
	                String name = (String ) it1.next();
	                if (!name.equals(FIELD_TYPE_ID) && row.get(name) != null){
	                    //find property definition
	                    PropertyDefinition[] props = nt.getDeclaredPropertyDefinitionsCache();
	                    for(int i = 0 ; i < props.length ; i++){
	                        PropertyDefinitionImpl p = (PropertyDefinitionImpl) props[i];
	                        if (name.equalsIgnoreCase(p.getColumnName())){
	                            
	                            InternalValue vv = JCRHelper.createInternatValue(p.getRequiredType(), row.get(name), getNamespaceRegistry(), getContentStoreProvider());
	                            if (keepChanges && state.hasProperty(p.getQName())){
	                                /*_PropertyState prop = state.getProperty(p.getQName());
	                                if (!prop.isModified()){
	                                    state.internalSetProperty(p.getQName(), vv  ,false, false);
	                                }*/
	                            } else {
	                            	InternalValue[] val = new InternalValue[]{vv};
	                            	_PropertyState pState = new _PropertyState(repository, state, p.getQName(), p.getRequiredType(), p.getRequiredType(), false, (PropDefImpl)p.unwrap(), null);
	                            	pState.setInitialValues(val);
	                            	state.addProperty(pState);
	                            	pState.setStatusNormal();
	                            	//TODO move to PropertyState
	                            	if (p.getQName().equals(QName.JCR_UUID) && p.getDeclaringNodeType().getName().equals(QName.MIX_REFERENCEABLE)){
	                                	state.setInternalUUID(row.getString(name));
	                                }
	                            }
	                            break;
	                        }
	                    }
	                }
	            }
	        }
		}
		
	        //load unstructured props 
	        if (unstr.size() > 0){
		
	        	
	        	
		        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE_UNSTRUCTURED, false);
		        st.addCondition(Conditions.in(FIELD_TYPE_ID, unstr.keySet()));
		        st.execute(conn);

		        DatabaseSelectAllStatement _st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE_UNSTRUCTURED, false);
		        _st.addResultColumn(Constants.FIELD_ID);
		        _st.addCondition(Conditions.in(FIELD_TYPE_ID, unstr.keySet()));
		        //st.execute(conn);
		        ArrayList<RowMap> multiValueRows = null;
		        
		        while (st.hasNext()){
		            RowMap row = st.nextRow();
		            QName _name = JCRHelper.assembleQName(row, getNamespaceRegistry());
		            Long nodeId = row.getLong(FIELD_TYPE_ID);
		            _NodeState state = nodes.get(nodeId);
		            Long pId = (Long) row.get(Constants.FIELD_ID);
		            //Long fromNodeType = (Long) row.get(TABLE_NODE_UNSTRUCTURED__NODE_TYPE);
		            Long propDefId = (Long) row.get(TABLE_NODE_UNSTRUCTURED__PROP_DEF);
		            Boolean multiValued = (Boolean)row.get(TABLE_NODE_UNSTRUCTURED__MULTIPLE);
		            Long _type = (Long)row.get(TABLE_NODE_UNSTRUCTURED__TYPE);
		
		            PropertyDefinitionImpl def = state.getApplicablePropertyDefinition(_name, _type.intValue(), multiValued.booleanValue());
		            if (!def.unwrap().getSQLId().equals(propDefId)){
		            	throw new RepositoryException("Error loading property "+_name+" for node "+state.getInternalPath()+"  Node Id"+state.getNodeId());
		            }
		            //PropertyState pState = new PropertyState(_name, _type.intValue(), def.unwrap(), getNodeId(), pId);
		            _PropertyState pState = new _PropertyState(repository, state, _name, _type.intValue(), def.getRequiredType(), multiValued.booleanValue(), (PropDefImpl)def.unwrap(), pId);
		            
		            if (multiValued.booleanValue()){
		                //multivalue
		                ArrayList<InternalValue> values = new ArrayList<InternalValue>();
		                //TODO implement order
		                //TODO use batch
		                if (multiValueRows == null){
		                	multiValueRows = new ArrayList<RowMap>();
		                	
			                DatabaseSelectAllStatement st1 = DatabaseTools.createSelectAllStatement(TABLE_NODE_UNSTRUCTURED_VALUES, false);
			                st1.addCondition(Conditions.in(TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY, _st));
			                st1.execute(conn);
			                
			                while (st1.hasNext()){
			                	multiValueRows.add(st1.nextRow());	
			                }
			                st1.close();
		                }
			            for(RowMap rowValue:multiValueRows){
		                    //HashMap rowValue = st1.nextRow();
		                    //Value v = JCRHelper.createValue(_type.intValue(), rowValue.get(JCRHelper.getValueColumn(_type.intValue())), getValueFactory(), session.getContentStore());
		                    InternalValue iv;
		                    Long vId = (Long) rowValue.get(FIELD_ID);
		                    Long pid = rowValue.getLong(TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY);
		                    if (pid.equals(pId)){
			                    //if (_type.intValue() == PropertyType.UNDEFINED){
			                    //    iv = InternalValue.create(v, session.getNamespaceResolver());
			                    //} else {
			                    //    iv = InternalValue.create(v, _type.intValue(), session.getNamespaceResolver());
			                    //}
			                    iv = JCRHelper.createInternatValue(_type.intValue(), rowValue.get(JCRHelper.getValueColumn(_type.intValue())), getNamespaceRegistry(), getContentStoreProvider());
			                    iv.setSQLId(vId);
			                    
			                    values.add(iv);
		                    }
		                }
		                
		                
		                
		                InternalValue[] vv = values.toArray(new InternalValue[values.size()]);
		                //prop.internalSetValue(vv, _type.intValue());
		                pState.setInitialValues(vv);
		            } else {
		                //single value
		                //Value v = JCRHelper.createValue(_type.intValue(), row.get(JCRHelper.getValueColumn(_type.intValue())), getValueFactory(), session.getContentStore());
		                //InternalValue vv = InternalValue.create(v, _type.intValue(), session.getNamespaceResolver());
		                InternalValue vv = JCRHelper.createInternatValue(_type.intValue(), row.get(JCRHelper.getValueColumn(_type.intValue())), getNamespaceRegistry(), getContentStoreProvider());
		                //prop.internalSetValue(new InternalValue[]{vv}, _type.intValue());
		                vv.setSQLId(pId);
		                pState.setInitialValues(new InternalValue[]{vv});
		            }
		        
	            
	            
	            /*if (keepChanges && hasProperty(_name)){
	                PropertyImpl prop = getProperty(_name);
	                if (!prop.isModified()){
	                    if (def.isMultiple()){
	                        internalSetProperty(_name, pState.getValues() , _type.intValue() ,false);
	                    } else {
	                        internalSetProperty(_name, pState.getValues()[0] ,false, false);
	                    }
	                }
	            } else {*/
	                //_PropertyImpl prop = getOrCreateProperty(_name, _type.intValue(), multiple.booleanValue(), _status, pId);
	            	state.addProperty(pState);
	            	pState.setStatusNormal();
	                //prop.resetStatus();
	            //}
		        }
		        st.close();
	        }
		//}
        
	}

	private void createDefaultProperties(HashMap<Long, _NodeState> nodes) throws RepositoryException {
		//create primaryType property
		/*_PropertyState primaryTypeProperty = new _PropertyState(state, QName.JCR_PRIMARYTYPE, PropertyType.NAME, false);
		state.addProperty(primaryTypeProperty);
		primaryTypeProperty.setStatusNormal();

        //init mixin property
        if (state.getMixinTypeNames().size() > 0){
    		_PropertyState mixinProperty = new _PropertyState(state, QName.JCR_MIXINTYPES, PropertyType.NAME, false);
    		state.addProperty(mixinProperty);
    		mixinProperty.setStatusNormal();
        }*/		
	}


	private _NodeState getFromCache(Long nodeId) {
		synchronized (_nodeCache) {
			return _nodeCache.get(nodeId);			
		}
	}

	private void putToCache(Long nodeId, _NodeState state){
		synchronized (_nodeCache) {
			_nodeCache.put(nodeId, state);
		}
	}

	public _NodeState getNodeState(Long id, ArrayList<Long> readAheadIds) throws RepositoryException{
		/*ArrayList<Long> ids = new ArrayList<Long>();
		ids.add(id);
		result = findNodeState(id, (Data)null);*/
		throw new UnsupportedOperationException();
	}
	

	private NamespaceRegistryImpl getNamespaceRegistry() throws RepositoryException {
		return repository.getNamespaceRegistry();
	}
	
	private StoreContainer getContentStoreProvider() {
		//TODO may be return repositoryStoreContainer ???
		return null;
	}

    protected NodeTypeManagerImpl getNodeTypeManager() throws RepositoryException {
    	if (nodeTypeManager == null){
    		this.nodeTypeManager = new NodeTypeManagerImpl(repository.getNamespaceRegistry(), repository.getNodeTypeReader(), repository);
    	}
		return nodeTypeManager;
	}

	public void unregisterUUID(String internalUUID) {
		synchronized (uuids){
			uuids.remove(internalUUID);
		}
		
	}
	
	public Long  getNodeIdByUUID(String uuid, DatabaseConnection conn) throws RepositoryException{
		synchronized (uuids){
			//FIXME use cache instead of HashMap
			if (uuids.containsKey(uuid)){
				return uuids.get(uuid);
			}
			//TODO use cache of UUID
	        DatabaseSelectOneStatement st = null;
	        //find mix:referenceable
	        
	        NodeTypeImpl nt = (NodeTypeImpl) getNodeTypeManager().getNodeType(QName.MIX_REFERENCEABLE);
	        String tableName = nt.getTableName();
	        PropDef prop = nt.getEffectiveNodeType().getApplicablePropertyDef(QName.JCR_UUID, PropertyType.STRING, false);
	        String fieldName = prop.getColumnName();
	        
	        st = DatabaseTools.createSelectOneStatement(tableName, fieldName, uuid);
	        st.execute(conn);
	        HashMap row = st.getRow();
	        Long id = (Long) row.get(Constants.FIELD_TYPE_ID);
	        //2.b build node
	        uuids.put(uuid, id);
	        return id;
		}
	}

	public NamespaceResolver getNamespaceResolver() {
		throw new UnsupportedOperationException();
	}

	public void registerNewState(_NodeState state) throws RepositoryException {
		synchronized (_nodeCache) {
			_nodeCache.put(state.getNodeId(), state.createCopy());
		}
		
	}

	public void evictAll() throws RepositoryException {
		synchronized (_nodeCache) {
			_nodeCache.clear();
			this.nodeTypeManager = new NodeTypeManagerImpl(repository.getNamespaceRegistry(), repository.getNodeTypeReader(),  repository);
		}
		
	}

	_NodeState findNodeState(Long nodeId, DatabaseConnection conn, String objectDescription) throws RepositoryException {
		ArrayList<Long> ids = new ArrayList<Long>();
		ids.add(nodeId);
		return findNodeState(ids, conn, objectDescription).get(0);
		
		
	}

	public RepositoryImpl getRepository() {
		return repository;
	}





}
