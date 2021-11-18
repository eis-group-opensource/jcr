/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.impl.ItemId;
import com.exigen.cm.impl.ItemImpl;
import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.PropertyId;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.Path;

/**
 * Implementation of the {@link javax.jcr.observation.Event} interface.
 */
public final class EventImpl implements Event {

    /**
     * Logger instance for this class
     */
    private static final Log log = LogFactory.getLog(EventImpl.class);

    /**
     * The session of the {@link javax.jcr.observation.EventListener} this
     * event will be delivered to.
     */
    private final SessionImpl _session;

    /**
     * The shared {@link EventState} object.
     */
    private final EventState eventState;

    /**
     * Cached String value of this <code>Event</code> instance.
     */
    private String stringValue;

	private RepositoryImpl repository;

    /**
     * Creates a new {@link javax.jcr.observation.Event} instance based on an
     * {@link EventState eventState}.
     *
     * @param session    the session of the registerd <code>EventListener</code>
     *                   where this <code>Event</code> will be delivered to.
     * @param eventState the underlying <code>EventState</code>.
     */
    EventImpl(RepositoryImpl repository,SessionImpl session, EventState eventState) {
    	this.repository = repository;
        this._session = session;
        this.eventState = eventState;
    }

    /**
     * {@inheritDoc}
     */
    public int getType() {
        return eventState.getType();
    }

    private Path cachedPath = null;
    private String cachedPathString = null;
    
    /**
     * {@inheritDoc}
     */
    public String getPath() throws RepositoryException {
    	if (cachedPathString == null){
	        try {
	             _getPath();
	            cachedPathString = cachedPath.toJCRPath(repository.getNamespaceRegistry());
	        } catch (NoPrefixDeclaredException e) {
	            String msg = "internal error: encountered unregistered namespace in path";
	            log.debug(msg);
	            throw new RepositoryException(msg, e);
	        }
    	}
        
        return cachedPathString;
    }

    public Path _getPath() throws RepositoryException {
    	if (cachedPath == null){
	        try {
	            //Path p;
	            if (eventState.getChildRelPath().getIndex() > 0) {
	            	cachedPath = Path.create(eventState.getParentPath(), eventState.getChildRelPath().getName(),
	                        eventState.getChildRelPath().getIndex(), false);
	            } else {
	            	cachedPath = Path.create(eventState.getParentPath(), eventState.getChildRelPath().getName(), false);
	            }
	            
	            cachedPathString = cachedPath.toJCRPath(repository.getNamespaceRegistry());
	        } catch (MalformedPathException e) {
	            String msg = "internal error: malformed path for event";
	            log.debug(msg);
	            throw new RepositoryException(msg, e);
	        } catch (NoPrefixDeclaredException e) {
	            String msg = "internal error: encountered unregistered namespace in path";
	            log.debug(msg);
	            throw new RepositoryException(msg, e);
	        }
    	}
    	return cachedPath;
    	
    }

    
    /**
     * {@inheritDoc}
     */
    public String getUserID() {
        return eventState.getUserId();
    }

    /**
     * Returns the uuid of the parent node.
     *
     * @return the uuid of the parent node.
     */
    public NodeId getParentId() {
        return eventState.getParentId();
    }

    /**
     * Returns the id of a child node operation.
     * If this <code>Event</code> was generated for a property
     * operation this method returns <code>null</code>.
     *
     * @return the id of a child node operation.
     */
    public NodeId getChildId() {
        return eventState.getChildId();
    }

    /**
     * Returns a String representation of this <code>Event</code>.
     *
     * @return a String representation of this <code>Event</code>.
     */
    public String toString() {
        if (stringValue == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("Event: Path: ");
            try {
                sb.append(getPath());
            } catch (RepositoryException e) {
                log.error("Exception retrieving path: " + e);
                sb.append("[Error retrieving path]");
            }
            sb.append(", ").append(EventState.valueOf(getType())).append(": ");
            sb.append(", UserId: ").append(getUserID());
            stringValue = sb.toString();
        }
        return stringValue;
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
    	if (_session == null){
    		return eventState.hashCode();
    	} else {
    		return eventState.hashCode() ^ _session.hashCode();
    	}
    }

    /**
     * Returns <code>true</code> if this <code>Event</code> is equal to another
     * object.
     * <p/>
     * Two <code>Event</code> instances are equal if their respective
     * <code>EventState</code> instances are equal and both <code>Event</code>
     * instances are intended for the same <code>Session</code> that registerd
     * the <code>EventListener</code>.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this <code>Event</code> is equal to another
     *         object.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EventImpl) {
            EventImpl other = (EventImpl) obj;
            return this.eventState.equals(other.eventState)
                    && (this._session != null && this._session.equals(other._session) || (this._session == null && other._session == null));
        }
        return false;
    }
    
    public SessionImpl getSession(){
        return getEventSession();
    }
    
    public ItemImpl getEventItem() throws RepositoryException{
    	ItemId itemId = eventState.getItemid();
    	if (itemId instanceof NodeId){
    		return getEventSession().getNodeManager().buildNode(((NodeId)itemId).getId(), false, true);
    	} else {
    		PropertyId propId = ((PropertyId)itemId);
    		NodeImpl node = getEventSession().getNodeManager().buildNode(propId.getParentId(), false, true);
    		return node.getProperty(propId.getName());
    	}
        //return (ItemImpl) eventState.getSession().getItem(getPath());
    }

    public NodeImpl getEventParentItem() throws RepositoryException {
        //return (NodeImpl) eventState.getSession().getItem(eventState.getParentPath());
        return getEventSession().getNodeManager().buildNode(eventState.getParentId().getId(), false, true);
    }

    public _NodeState getEventParentItemState() throws RepositoryException {
        //return (NodeImpl) eventState.getSession().getItem(eventState.getParentPath());
        return getEventSession().getStateManager().getNodeState(eventState.getParentId().getId(), null);
    }

	public ItemId getItemid() {
		return eventState.getItemid();
	}
	
	private SessionImpl getEventSession(){
		return _session;
	}
	
	public String getNodeUUID() throws RepositoryException{
		ItemId itemId = eventState.getItemid();
    	if (itemId instanceof NodeId){
    		return ((NodeId)itemId).getUUID();
    	} else {
    		//PropertyId propId = ((PropertyId)itemId);
    		//NodeImpl node = getEventSession().getNodeManager().buildNode(propId.getParentId(), false, true);
    		//return ((NodeId)node.getId()).getUUID();
    		return eventState.getParentId().getUUID();
    	}
	}

	public String getWorkspace() {
		return eventState.getWorkspaceName();
	}
}
