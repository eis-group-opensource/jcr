/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.xml;

import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.TABLE_NODE;
import static com.exigen.cm.Constants.TABLE_NODE__PARENT;
import static com.exigen.cm.Constants.TABLE_NODE__WORKSPACE_ID;
import static com.exigen.cm.jackrabbit.name.QName.JCR_ROOT;
import static com.exigen.cm.jackrabbit.name.QName.REP_ROOT;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.zip.ZipFile;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.NodeTypeManagerImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.state2.IdIterator;
import com.exigen.cm.impl.state2._AbstractsStateManager;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.impl.state2._StandaloneStatemanager;
import com.exigen.cm.jackrabbit.core.util.ReferenceChangeTracker;
import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.EffectiveNodeType;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.cm.jackrabbit.value.LongValue;
import com.exigen.cm.jackrabbit.value.StringValue;
import com.exigen.cm.jackrabbit.value.ValueHelper;
import com.exigen.cm.jackrabbit.version.NodeStateEx;
/**
 * <code>SessionImporter</code> ...
 */
public class RepositoryImporter implements Importer {

    private static Log log = LogFactory.getLog(RepositoryImporter.class);

    private final ZipFile zipFile;

    private Stack<NodeStateEx> parents;

	private HashMap<Long, _AbstractsStateManager> workspaces = new HashMap<Long, _AbstractsStateManager>();
    
    
    /**
     * helper object that keeps track of remapped uuid's and imported reference
     * properties that might need correcting depending on the uuid mappings
     */
    private final ReferenceChangeTracker refTracker;

	private RepositoryImpl repository;

	private Long workspaceId;

	private DatabaseConnection conn;

	private Long systemRootId;

	private _StandaloneStatemanager sm;

	private NodeStateEx rootState;
	private NodeStateEx linksState;

	private int newNodes = 0;

	private NamespaceRegistryImpl nsResolver;  
    
    
    /**
     * Creates a new <code>SessionImporter</code> instance.
     *
     * @param importTargetNode
     * @param session
     * @param uuidBehavior     any of the constants declared by
     *                         {@link ImportUUIDBehavior}
     * @param zin ZipInputStream with binary properties (if binary property exported into zip), null otherwise
     * @throws RepositoryException 
     */
    public RepositoryImporter(RepositoryImpl repository,
                           ZipFile zipFile) throws RepositoryException {
        this.zipFile = zipFile;
        this.repository = repository;
        
        this.conn = repository.getConnectionProvider().createConnection();
        conn.setAllowClose(false);
        
        refTracker = new ReferenceChangeTracker();
        
        this.nsResolver = repository.getNamespaceRegistry();

        parents = new Stack();
        //parents.push(importTargetNode);
    }

    protected NodeStateEx createNode(NodeStateEx parent,
                                  QName nodeName,
                                  QName nodeTypeName,
                                  QName[] mixinNames,
                                  String uuid)
            throws RepositoryException {
    	NodeStateEx node;
        
        newNodes++;
    	if (newNodes > 100){
    		System.out.print(".");
            rootState.save(false);
            newNodes = 0;
    	}

    	if (parent.getDepth() == 0 && nodeName.equals(QName.JCR_ROOT)){
    		return parent;
    	}
    	
        if (parent.hasProperty(nodeName)) {
            /**
             * a property with the same name already exists; if this property
             * has been imported as well (e.g. through document view import
             * where an element can have the same name as one of the attributes
             * of its parent element) we have to rename the onflicting property;
             *
             * see http://issues.apache.org/jira/browse/JCR-61
             */
            Property conflicting = parent.getProperty(nodeName);
            if (conflicting.isNew()) {
                // assume this property has been imported as well;
                // rename conflicting property
                // @todo use better reversible escaping scheme to create unique name
                QName newName = new QName(nodeName.getNamespaceURI(), nodeName.getLocalName() + "_");
                if (parent.hasProperty(newName)) {
                    newName = new QName(newName.getNamespaceURI(), newName.getLocalName() + "_");
                }

                if (conflicting.getDefinition().isMultiple()) {
                    parent.setProperty(newName, conflicting.getValues());
                } else {
                    parent.setProperty(newName, conflicting.getValue());
                }
                conflicting.remove();
            }
        }

        // add node
        node = (NodeStateEx)parent.addNode(nodeName, nodeTypeName, uuid);
        // add mixins
//        node.getNodeState().getInternalUUID()
        if (mixinNames != null) {
            for (int i = 0; i < mixinNames.length; i++) {
                node.addMixin(mixinNames[i]);
            }
        }
        if (node.isReferenceable() && uuid != null){
        	node.setProperty(QName.JCR_UUID, new StringValue(uuid));
        	node.getNodeState().setInternalUUID(uuid);
        }
        
        
        return node;
    }

    protected NodeImpl resolveUUIDConflict(NodeImpl parent,
                                           NodeImpl conflicting,
                                           NodeInfo nodeInfo)
            throws RepositoryException {
        /*NodeImpl node;
        if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW) {
            // create new with new uuid
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(), null);
            // remember uuid mapping
            if (node.isNodeType(QName.MIX_REFERENCEABLE)) {
                refTracker.mappedUUID(nodeInfo.getUUID(), node.getUUID());
            }
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW) {
            String msg = "a node with uuid " + nodeInfo.getUUID() + " already exists!";
            log.debug(msg);
            throw new ItemExistsException(msg);
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING) {
            // make sure conflicting node is not importTargetNode or an ancestor thereof
            if (importTargetNode.getPath().startsWith(conflicting.getPath())) {
                String msg = "cannot remove ancestor node";
                log.debug(msg);
                throw new ConstraintViolationException (msg);
            }
            // remove conflicting
            conflicting.remove();
            // create new with given uuid
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getUUID());
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING) {
            if (conflicting.getDepth() == 0) {
                String msg = "root node cannot be replaced";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            // 'replace' current parent with parent of conflicting
            parent = (NodeImpl) conflicting.getParent();
            // remove conflicting
            conflicting.remove();
            // create new with given uuid at same location as conflicting
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getUUID());
        } else {
            String msg = "unknown uuidBehavior: " + uuidBehavior;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        return node;*/
    	throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------------< Importer >
    /**
     * {@inheritDoc}
     */
    public void start() throws RepositoryException {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    public void startNode(NodeInfo nodeInfo, List propInfos,
                          NamespaceResolver nsContext)
            throws RepositoryException {
    	NodeStateEx parent = (NodeStateEx) parents.peek();

        // process node

    	NodeStateEx node = null;
        String uuid = nodeInfo.getUUID();
        QName nodeName = nodeInfo.getName();
        QName ntName = nodeInfo.getNodeTypeName();
        QName[] mixins = nodeInfo.getMixinNames();

        if (parent == null) {
            // parent node was skipped, skip this child node also
            parents.push(null); // push null onto stack for skipped node
            log.debug("skipping node " + nodeName);
            return;
        }
        if (parent.hasNode(nodeName)) {
            // a node with that name already exists...
        	NodeStateEx existing =(NodeStateEx) parent.getNode(nodeName);
            NodeDefinition def = existing.getDefinition();
            if (!def.allowsSameNameSiblings()) {
                // existing doesn't allow same-name siblings,
                // check for potential conflicts
            	if (def.isAutoCreated() && existing.isNodeType(ntName)) {
                    // this node has already been auto-created, no need to create it
                    node = existing;
                    if (uuid != null){
                    	//EffectiveNodeType ent = node.getNodeState().getEffectiveNodeType();
                    	//PropDef pDef = ent.getApplicablePropertyDef(QName.JCR_UUID, PropertyType.STRING);
                    	node.setProperty("jcr:uuid", uuid, PropertyType.STRING);
                    }
                } else if (def.isProtected() && existing.isNodeType(ntName)) {
                    // skip protected node
                    parents.push(null); // push null onto stack for skipped node
                    log.debug("skipping protected node " + existing.safeGetJCRPath());
                    return;
                } else {
                    throw new ItemExistsException(existing.safeGetJCRPath());
                }
            }
        	//existing.internalRemove(true, false);
        }        
        
        if (node == null) {
            // create node
            node = createNode(parent, nodeName, ntName, mixins, uuid);
        }

        // process properties

        Iterator iter = propInfos.iterator();
        EffectiveNodeType ent = node.getNodeState().getEffectiveNodeType();
        while (iter.hasNext()) {
            PropInfo pi = (PropInfo) iter.next();
            QName propName = pi.getName();
            TextValue[] tva = pi.getValues();
            int type = pi.getType();

            // find applicable definition
            
            PropDef def;
            // multi- or single-valued property?
            if (tva.length == 1) {
                // could be single- or multi-valued (n == 1)
                def = ent.getApplicablePropertyDef(propName, type);
            } else {
                // can only be multi-valued (n == 0 || n > 1)
                def = ent.getApplicablePropertyDef(propName, type, true);
                
            }

            /*if (def.isProtected()) {
                // skip protected property
                log.debug("skipping protected property " + propName);
                continue;
            }*/

            // convert serialized values to Value objects
            Value[] va = new Value[tva.length];
            int targetType = def.getRequiredType();
            if (targetType == PropertyType.UNDEFINED) {
                if (type == PropertyType.UNDEFINED) {
                    targetType = PropertyType.STRING;
                } else {
                    targetType = type;
                }
            }
            for (int i = 0; i < tva.length; i++) {
                TextValue tv = tva[i];

                if (targetType == PropertyType.NAME
                        || targetType == PropertyType.PATH) {
                    // NAME and PATH require special treatment because
                    // they depend on the current namespace context
                    // of the xml document

                    // retrieve serialized value
                    String serValue;
                    try {
                        serValue = tv.retrieve();
                    } catch (IOException ioe) {
                        String msg = "failed to retrieve serialized value";
                        log.debug(msg, ioe);
                        throw new RepositoryException(msg, ioe);
                    }

                    // convert serialized value to InternalValue using
                    // current namespace context of xml document
                    InternalValue ival =
                            InternalValue.create(serValue, targetType, nsContext, null);
                    // convert InternalValue to Value using this
                    // session's namespace mappings
                    va[i] = ival.toJCRValue(nsResolver);
                } else if (targetType == PropertyType.BINARY) {
                	try {
                        if (tv.length() < 0x10000 && zipFile == null) {
                            // < 65kb: 
                                //deserialize BINARY type using String
                                va[i] = ValueHelper.deserialize(tv.retrieve(), targetType, false, null);
                        } else {
                            // >= 65kb or zip: deserialize BINARY type using Reader
                            Reader reader = tv.reader();
                            try {
                                va[i] = ValueHelper.deserialize(reader, targetType, false, zipFile);
                            } finally {
                                reader.close();
                            }
                        }
                        
                    } catch (IOException ioe) {
                        String msg = "failed to deserialize binary value";
                        log.error(msg, ioe);
                        throw new RepositoryException(msg, ioe);
                    }
                } else {
                    // all other types

                    // retrieve serialized value
                    String serValue;
                    try {
                        serValue = tv.retrieve();
                    } catch (IOException ioe) {
                        String msg = "failed to retrieve serialized value";
                        log.debug(msg, ioe);
                        throw new RepositoryException(msg, ioe);
                    }

                    va[i] = ValueHelper.deserialize(serValue, targetType, true, null);
                }
            }

            // multi- or single-valued property?
            if (type != PropertyType.REFERENCE && type != PropertyType283.WEAKREFERENCE){
	            if (va.length == 1) {
	                // could be single- or multi-valued (n == 1)
	                try {
	                    // try setting single-value
	                    node.setProperty(propName, va[0]);
	                } catch (ValueFormatException vfe) {
	                    // try setting value array
	                    node.setProperty(propName, va, type);
	                } catch (ConstraintViolationException cve) {
	                    // try setting value array
	                    node.setProperty(propName, va, type);
	                }
	            } else {
	                // can only be multi-valued (n == 0 || n > 1)
	                node.setProperty(propName, va, type);
	            }
            }
            if (type == PropertyType.REFERENCE || type == PropertyType283.WEAKREFERENCE) {
            	Long nodeId = node.getNodeId();
            	NodeStateEx link = (NodeStateEx) linksState.addNode(new QName("","Link"+nodeId), QName.NT_UNSTRUCTURED, null);
            	link.setProperty(new QName("","nodeId"), new LongValue(nodeId));
            	link.setProperty(new QName("","type"), new LongValue(type));
            	//link.setProperty(new QName("","path"), new StringValue(node.getNodeState().getInternalPath()));
            	if (workspaceId != null){
            		link.setProperty(new QName("","workspaceId"), new LongValue(workspaceId));
            	}
            	try {
					link.setProperty(new QName("","propertyName"), new StringValue(propName.toJCRName(nsResolver)));
				} catch (NoPrefixDeclaredException e) {
					throw new RepositoryException(e);
				}
	            if (va.length == 1) {
	                // could be single- or multi-valued (n == 1)
	            	Value v = va[0];
	            	link.setProperty(new QName("","link"), new StringValue(v.getString()));
	            } else {
	            	ArrayList<StringValue> r = new ArrayList<StringValue>();
	            	for(Value v: va){
	            		r.add(new StringValue(v.getString()));
	            	}
	            	link.setProperty(new QName("","links"), (Value[]) r.toArray(new Value[r.size()]));
	            	
	            }
            	
            	
                // store reference for later resolution
                //refTracker.processedReference(node.getProperty(propName));
            }
        }

        parents.push(node);
    	//throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void endNode(NodeInfo nodeInfo) throws RepositoryException {
        parents.pop();
    }

    /**
     * {@inheritDoc}
     */
    public void end() throws RepositoryException {
        /**
         * adjust references that refer to uuid's which have been mapped to
         * newly generated uuid's on import
         */
        /*Iterator iter = refTracker.getProcessedReferences();
        while (iter.hasNext()) {
            Property prop = (Property) iter.next();
            // being paranoid...
            if (prop.getType() != PropertyType.REFERENCE) {
                continue;
            }
            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                Value[] newVals = new Value[values.length];
                for (int i = 0; i < values.length; i++) {
                    Value val = values[i];
                    String original = val.getString();
                    String adjusted = refTracker.getMappedUUID(original);
                    if (adjusted != null) {
                        newVals[i] = new ReferenceValue(session.getNodeByUUID(adjusted));
                    } else {
                        // reference doesn't need adjusting, just copy old value
                        newVals[i] = val;
                    }
                }
                prop.setValue(newVals);
            } else {
                Value val = prop.getValue();
                String original = val.getString();
                String adjusted = refTracker.getMappedUUID(original);
                if (adjusted != null) {
                    prop.setValue(session.getNodeByUUID(adjusted));
                }
            }
        }*/
        refTracker.clear();
        
        rootState.save(false);
        linksState.save(false);
    }


	public RepositoryImpl getRepository() {
		return repository;
	}


	public void setRepository(RepositoryImpl repository) {
		this.repository = repository;
	}


	public void setWorkspaceId(Long workspaceId) throws RepositoryException {
		
		this.workspaceId = workspaceId;
		
		while(parents.size() > 0){
			parents.clear();
		}
		
		if (workspaceId == null){
			
			DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_NODE, true);
            st.addCondition(Conditions.isNull(TABLE_NODE__PARENT));
            st.addCondition(Conditions.isNull(TABLE_NODE__WORKSPACE_ID));
            st.execute(conn);
            systemRootId = null;
            if (!st.hasNext()){
                //create system node
                systemRootId = Constants.SYSTEM_WORKSPACE_ROOT_ID;
                List<DatabaseInsertStatement> inserts = JCRHelper.createNodeStatement(systemRootId, JCR_ROOT, new Long(1),
                        ((NodeTypeManagerImpl)repository.getNodeTypeManager()).findNodeTypeDef(REP_ROOT, null).getId(),
                        "", new Long(0),null, systemRootId, null, null, null, repository.getNamespaceRegistry(), (long) 1);
                for(DatabaseInsertStatement st11:inserts){
                	st11.execute(conn);
                }

                
                //add type
                NodeTypeImpl nt = repository.getNodeTypeManager().getNodeType(REP_ROOT);
                //Long typeId = conn.nextId();
                DatabaseInsertStatement insert = JCRHelper.createNodeTypeStatement(systemRootId, nt.getSQLId(), nt.getSQLId());
                insert.execute(conn);

                insert = JCRHelper.createNodeTypeDetailsStatement(systemRootId, nt.getTableName());
                insert.execute(conn);
                
                conn.commit();
                
            } else {
                RowMap row = st.nextRow();
                systemRootId = row.getLong(FIELD_ID); 
            }			
            repository.systemRootId = systemRootId ;
            
            this.sm = repository.createStandaloneStateManager(conn, systemRootId , null);
            rootState = new NodeStateEx(sm.getNodeState(systemRootId, null), sm);
            
            linksState = (NodeStateEx) rootState.addNode(new QName("","links"), QName.NT_UNSTRUCTURED, null);
            
            parents.push(rootState);
			
		} else {
            DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(Constants.TABLE_WORKSPACE, Constants.FIELD_ID, workspaceId);
            try {
            	st.execute(conn);
                HashMap row = st.getRow();
                systemRootId = (Long) row.get(Constants.TABLE_WORKSPACE__ROOT_NODE);
            } catch (ItemNotFoundException exc){
            	throw new NoSuchWorkspaceException(workspaceId.toString());
            } finally {
            	st.close();
            }
			
            this.sm = repository.createStandaloneStateManager(conn, systemRootId , workspaceId);
            rootState = new NodeStateEx(sm.getNodeState(systemRootId, null), sm);
            workspaces.put(workspaceId, sm);

            //dumpNode(sm, rootState.getNodeState(), 0, false);
            
            parents.push(rootState);

            
		}

	}

	
	
    

	public void processLinks() throws RepositoryException{
		linksState.save();
		
		_AbstractsStateManager linksSM = linksState.getStateManager();
		IdIterator _ids = linksSM.getChildNodesId(linksState.getNodeState(), false, null);
		NodeStateEx p = (NodeStateEx) linksState._getParent();
		
		for(_AbstractsStateManager sm:workspaces.values()){
			sm.evictAll();
		}
		
		int pos=0;
		for(Long id:_ids){
			pos++;
			_NodeState state = linksSM.getNodeState(id, _ids.getNextIds());
			_AbstractsStateManager sm = null;
			if (state.getProperty(new QName("","workspaceId"), false) != null){
				Long wId = (Long) state.getProperty(new QName("","workspaceId"), false).getValues()[0].internalValue();
				sm = workspaces.get(wId);
			} else {
				sm = linksSM;
			}

			Long nodeid = (Long) state.getProperty(new QName("","nodeId"), false).getValues()[0].internalValue();
			Long typeL = (Long) state.getProperty(new QName("","type"), false).getValues()[0].internalValue();
			int type = typeL.intValue();
			String propertyName = (String) state.getProperty(new QName("","propertyName"), false).getValues()[0].internalValue();
			
			_NodeState _state = sm.getNodeState(nodeid, _ids.getNextIds());
			NodeStateEx node = new NodeStateEx(_state, sm);
			QName pName;
			try {
				pName = QName.fromJCRName(propertyName, nsResolver);
			} catch (IllegalNameException e) {
				throw new RepositoryException(e);
			} catch (UnknownPrefixException e) {
				throw new RepositoryException(e);
			}
			if (state.hasProperty(new QName("","link"))){
				_PropertyState vp = state.getProperty(new QName("","link"), true);
				String uuid = (String) vp.getValues()[0].internalValue();
				try {
                    // try setting single-value
					node.setProperty(propertyName, uuid, type);
                } catch (ValueFormatException vfe) {
                    // try setting value array
					node.setProperty(pName, new Value[]{new StringValue(uuid)}, type);
                } catch (ConstraintViolationException cve) {
                    // try setting value array
                    node.setProperty(pName, new Value[]{new StringValue(uuid)}, type);
                }
			} else {
				_PropertyState vp = state.getProperty(new QName("","links"), true);
				ArrayList<Value> vv = new ArrayList<Value>();
				for(InternalValue v:vp.getValues()){
					vv.add(new StringValue((String) v.internalValue()));
				}
				node.setProperty(pName, (Value[]) vv.toArray(new Value[vv.size()]), type);
			}
			
			node.save(false);
			
			NodeStateEx nn = new NodeStateEx(state, linksSM);
			nn.internalRemove(true, true);
			if (pos > 100){
				pos = 0;
				p.save();
		        System.out.print(".");
			}
			
			
		}
			
		linksState.internalRemove(true, true);
		p.save();
		linksSM.getConnection().commit();
		linksSM.getConnection().close();
		for(_AbstractsStateManager sm:workspaces.values()){
			sm.getConnection().commit();
			sm.getConnection().close();
		}
		
		//dumpNode(linksSM, p.getNodeState(), 0, false);
		
        this.conn.commit();
        this.conn.setAllowClose(true);
        this.conn.close();

        System.out.println(".");
		
	}
}
