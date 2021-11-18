/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;



import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.NodeModification;
import com.exigen.cm.impl.NodeTypeContainer;
import com.exigen.cm.impl.PropertyImpl;
import com.exigen.cm.impl.SecurityEntry;
import com.exigen.cm.impl.SecurityModificationEntry;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl._NodeImpl;
import com.exigen.cm.impl.state2.ChildMaxPosition;
import com.exigen.cm.impl.state2._AbstractsStateManager;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.EffectiveNodeType;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class NodeStateEx extends _NodeImpl{

    /** Logger for this class */
    //private static final Log log = LogFactory.getLog(NodeStateEx.class);
        
	public NodeStateEx(_NodeState s, _AbstractsStateManager sm) throws RepositoryException {
        super(s, sm);
	}

	@Override
    protected void sanityCheck() throws RepositoryException {
    	
    }
	
	@Override
    protected void checkLock() throws LockException, RepositoryException {
    	//TODO
    }

	protected _NodeImpl instantiate(_NodeState state2) throws RepositoryException {
		return new NodeStateEx(state2, stateManager);
	}

	@Override
	protected SessionImpl _getSession() {
		throw new UnsupportedOperationException();
	}

	public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void accept(ItemVisitor visitor) throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected _NodeImpl buildNode(_NodeState nodeState) throws RepositoryException {
		return new NodeStateEx(nodeState, stateManager);
	}
	
	
	protected boolean checkProtection(){
		return false;
	}

	public List<NodeStateEx> getNodes() throws RepositoryException{
		ArrayList<NodeStateEx> result = new ArrayList<NodeStateEx>();
		for(_NodeState s:stateManager.getNodesWithName(this.state, null, false)){
			result.add(new NodeStateEx(s, stateManager));
		}
		return result;
	}

	public List<_PropertyState> getProperties() {
		return state.getProperties();
	}

	/*@Override
	public String getName() throws RepositoryException {
		throw new UnsupportedOperationException("Please use getQName() method");
	}*/

	public String getUUID() {
		return state.getInternalUUID();
	}

	public void setPropertyValue(QName name, InternalValue value)
			throws ValueFormatException, VersionException, LockException,
			ConstraintViolationException, RepositoryException {
		setPropertyValue(name, value, true, true);
	}

	public void setPropertyValue(QName name, InternalValue value,
			boolean checkCollision, boolean checkLocks)
			throws ValueFormatException, VersionException, LockException,
			ConstraintViolationException, RepositoryException {
		if (value == null) {
			setProperty(name, (Value) null, checkCollision, checkLocks);
		} else {
			setProperty(name, value.toJCRValue(stateManager
					.getNamespaceResolver()), checkCollision, checkLocks);
		}

	}

	public void reload() throws RepositoryException {
		this.stateManager.evictState(state);
		this.state = this.stateManager.getNodeState(state.getNodeId(), null);
		for(_NodeState s: stateManager.getModifiedNodesDirect(state.getNodeId(), true)){
			NodeStateEx st = new NodeStateEx(s, stateManager);
			st.reload();
		}
	}

	public void copyFrom(PropertyImpl prop) throws RepositoryException {
		if (prop.getQName().equals(QName.JCR_UUID)){
            return;
        }
        if (prop.getDefinition().isMultiple()) {
            InternalValue[] values = prop.internalGetValues();
            int type;
            if (values.length > 0) {
                type = values[0].getType();
            } else {
                type = prop.getDefinition().getRequiredType();
            }
            Value[] copiedValues = new Value[values.length];
            for (int i = 0; i < values.length; i++) {
                copiedValues[i] = values[i].createCopy().toJCRValue(stateManager.getNamespaceResolver());
            }
            setProperty(prop.getQName(), copiedValues, type);
        } else {
            setPropertyValue(prop.getQName(), prop.internalGetValue().createCopy());
        }	}

	public void setPropertyValues(QName name, InternalValue[] values, int type) throws ValueFormatException, RepositoryException {
		setPropertyValues(name, values, type, true, true);
		
	}
	public void setPropertyValues(QName name, InternalValue[] values, int type, boolean checkCollision, boolean checkLocks) throws ValueFormatException, RepositoryException {
		internalSetProperty(name, values, type, true, checkCollision, checkLocks);
		
	}

    public void renameChildNodeEntry(QName fromName, int srcNameIndex, QName toName) throws RepositoryException {
        //TODO implement same name sibling
    	
    	if (fromName.equals(toName)){
    		return;
    	}
    	
    	ChildMaxPosition max = stateManager.getMaxChildPos(state, toName);
        NodeStateEx child = (NodeStateEx) getNode(fromName, srcNameIndex, false);
        long newIndex = 1;
        
        child.changeName(toName, newIndex, max.getMax()+1);
        
        child.registerModification();
        max.inc(child.getNodeState());
        ChildMaxPosition old = stateManager.getMaxChildPos(state, fromName);
        old.dec();
    }
    
    private void changeName(QName toName, long index2,long snsMax) throws RepositoryException {
    	state.updateName(toName, index2, state.getParent(), new NodeModification() , true);
        state.setName(toName);
        state.setIndex(index2);
        state.setSnsMax(snsMax);
        state.buildInternalPath(null);
    }

    public _NodeState deassociateChild(QName fromName, int srcNameIndex, boolean rename) throws RepositoryException {
    	ChildMaxPosition max = stateManager.getMaxChildPos(state, fromName);
        _NodeState child = getNode(fromName, srcNameIndex, false).getNodeState();
        boolean updateIds = false;
        child.fireRemoveEvent(true);
        //if (!rename){
        	child.setParentId(null);
            child.setInternalDepth(null);
            child.setInternalPath(null);
            child.setIndex(null);
        //}
        child.setSnsMax(null);
        child.setBasePropertiesChanged(true);
        stateManager.registerModifiedState(child);
        
        max.dec();

        if (child.getDefinition().allowsSameNameSiblings()){
        	if (max.getMax() > 0){
        		long pos = 1;
        		for(Long id : stateManager.getChildNodesId(state, false, fromName)){
//        			TODO optimize read Ahead
        			_NodeState s = stateManager.getNodeState(id, null);
        			s.updateName(fromName,pos, state, new NodeModification(), true);
        			pos++;
        		}
        	}
        	
        }
        
        return child;
    }

    public void associateChild(_NodeState child, QName toName, boolean moveSecurity) throws RepositoryException {
        //TODO support sns
        
    	ChildMaxPosition max = stateManager.getMaxChildPos(state, toName);
    	
    	/*child.setParentId(null);
        child.setInternalDepth(null);
        child.setInternalPath(null);
        child.setIndex(null);
    	*/
    	
    	//child.updateName(toName, max.getMax() + 1, state, new NodeModification(), true);
        child.setParentId(getNodeId());
        child.setBasePropertiesChanged(true);
        child.setName(toName);
        child.setIndex(max.getMax() + 1);
        child.setSnsMax(max.getMax() + 1);
        //child.setSnsMax((long)1);
        child.setWorkspaceId(state.getWorkspaceId());

        child.buildInternalPath(state);
        rebuildPaths(new NodeStateEx(child, stateManager));
        
        //update security

        DatabaseConnection conn = getConnection();
        try {
            List<SecurityEntry> _acl = state.getACEList();
            child.applySecurityOnMove( _acl, new ArrayList<Long>(), getNodeId(), moveSecurity);
        } finally {
            conn.close();
        }
        max.inc(child);
        
        child.fireAddEvent(true);
        
        
        
    }

	private void rebuildPaths(NodeStateEx child) throws RepositoryException {
		//new NodeStateEx(child, stateManager))
        for(NodeStateEx n:child.getNodes()){
        	n.getNodeState().buildInternalPath(child.getNodeState());
        	rebuildPaths(n);
        }
		
	}

	public EffectiveNodeType getEffectiveNodeType() throws RepositoryException {
		return state.getEffectiveNodeType();
	}

	public List<SecurityEntry> getACEList() throws RepositoryException {
		// TODO Auto-generated method stub
		return state.getACEList();
	}

	public void addSecurityModificationEntry(SecurityModificationEntry entry) {
		state.getModifiedSecurity().add(entry);
		
	}

	public boolean isReferenceable() throws RepositoryException {
		for(NodeTypeContainer ntc:state.getAllEffectiveTypes()){
			if (ntc.getName().equals(QName.MIX_REFERENCEABLE)){
				return true;
			}
		}
		return false;
	}

	@Deprecated
	public QName getNodeTypeName() throws RepositoryException {
		return state.getPrimaryTypeName();
	}

	public void setMixinTypeNames(Set<QName> mixinTypeNames) throws RepositoryException {
		for(QName name:mixinTypeNames){
			boolean founded  = false;
			for(NodeTypeContainer c:state.getAllEffectiveTypes()){
				if (c.getName().equals(name)){
					founded = true;
					break;
				}
			}
			if (!founded){
				addMixin(name);				
			}
			
		}
		
	}

	public void addProperty(_PropertyState newChildState) throws RepositoryException {
		state.addProperty(newChildState);
		
	}
    
    public NodeStateEx addNode(QName nodeName, QName nodeTypeName, NodeId id,
            boolean referenceable, Boolean ownSecurity, boolean createAutoCreatedChilds, boolean checkLock) throws RepositoryException {

    	
        NodeStateEx node = (NodeStateEx) addNode(nodeName, nodeTypeName, id == null? null:id.getUUID(),
        		true, checkLock, createAutoCreatedChilds, null);
        if (ownSecurity){
        	node.getNodeState().setSecurityId(node.getNodeId());
        }
        if (id == null){
        	id = (NodeId)node.getId();
        }
        if (referenceable) {
            //TODO may be get UUID from nodeName for version history ???
        	if (!node.isReferenceable()){
        		node.addMixin(QName.MIX_REFERENCEABLE);
        	}
            String uuid = id.getUUID();
            node.setPropertyValue(QName.JCR_UUID, InternalValue.create(uuid));
        }
        return node;
    }

	public Long getParentNodeId() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
		return state.getParentId();
	}

	public void setSecurityId(Long securityId){
		state.setSecurityId(securityId);
	}

	public Long getSecurityId() {
		return state.getSecurityId();
	}


}


/*
 * $Log: NodeStateEx.java,v $
 * Revision 1.8  2009/03/12 10:57:00  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2009/02/23 14:30:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2009/01/28 06:50:59  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2009/01/27 14:07:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2008/09/19 10:14:16  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/07/15 11:27:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2008/06/13 09:35:29  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:58:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.8  2007/03/12 08:24:11  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.7  2007/03/02 09:32:07  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.6  2006/11/14 07:37:32  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.5  2006/11/03 13:09:15  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.4  2006/11/01 14:11:29  dparhomenko
 * PTR#1803326 new features
 *
 * Revision 1.3  2006/11/01 12:01:22  dparhomenko
 * PTR#0148728 fix isNodeTypeUsed
 *
 * Revision 1.2  2006/10/30 15:03:55  dparhomenko
 * PTR#1803272 a lot of fixes
 *
 * Revision 1.1  2006/10/17 10:46:42  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.23  2006/10/09 08:59:47  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.22  2006/10/02 15:07:12  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.21  2006/09/27 12:32:59  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.20  2006/09/26 10:11:08  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.19  2006/09/11 12:07:42  dparhomenko
 * PTR#0148153 fix FTS
 *
 * Revision 1.18  2006/09/07 10:37:01  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.17  2006/08/16 10:09:03  dparhomenko
 * PTR#1802558 add new features
 *
 */