/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;

/*import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.name.Path;
*/
import java.util.Set;

import javax.jcr.observation.Event;

import com.exigen.cm.impl.ItemId;
import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.PropertyId;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.state2.SessionInfo;
import com.exigen.cm.jackrabbit.name.Path;

/**
 * The <code>EventState</code> class encapsulates the session
 * independent state of an {@link javax.jcr.observation.Event}.
 */
public class EventState {

    /**
     * The {@link javax.jcr.observation.Event} of this event.
     */
    private final int type;

    /**
     * The Id of the parent node associated with this event.
     */
    private final NodeId parentId;

    /**
     * The path of the parent node associated with this event.
     */
    private final Path parentPath;

    /**
     * The UUID of a child node, in case this EventState is of type
     * {@link javax.jcr.observation.Event#NODE_ADDED} or
     * {@link javax.jcr.observation.Event#NODE_REMOVED}.
     */
    private final NodeId childId;

    /**
     * The relative path of the child item associated with this event.
     * This is basically the name of the item with an optional index.
     */
    private final Path.PathElement childRelPath;

    /**
     * The node type of the parent node.
     */
    private final NodeTypeImpl nodeType;

    /**
     * Set of mixin QNames assigned to the parent node.
     */
    private final Set mixins;

    /**
     * The session that caused this event.
     */
    //private final SessionImpl _session;

    /**
     * Cached String representation of this <code>EventState</code>.
     */
    private String stringValue;

    /**
     * Cached hashCode value for this <code>Event</code>.
     */
    private int hashCode;
    
    private String parentUUID;

	private ItemId itemId;

	private final String userId;

	private final long sessionId;

	private RepositoryImpl repository;

	private final String workspaceName;

    /**
     * Creates a new <code>EventState</code> instance.
     *
     * @param type       the type of this event.
     * @param parentId   the id of the parent node associated with this event.
     * @param parentPath the path of the parent node associated with this
     *                   event.
     * @param childId    the id of the child node associated with this event.
     *                   If the event type is one of: <code>PROPERTY_ADDED</code>,
     *                   <code>PROPERTY_CHANGED</code> or <code>PROPERTY_REMOVED</code>
     *                   this parameter must be <code>null</code>.
     * @param childPath  the relative path of the child item associated with
     *                   this event.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the {@link javax.jcr.Session} that caused this event.
     */
    private EventState(int type,
                       NodeId parentId,
                       Path parentPath,
                       NodeId childId,
                       Path.PathElement childPath,
                       NodeTypeImpl nodeType,
                       Set mixins,
                       String parentUUID, ItemId itemId,                       
                       SessionInfo info) {
    	this.repository = info.getRepository();
    	this.workspaceName = info.getWorkspaceName();
        int mask = (Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED);
        if ((type & mask) > 0) {
            if (childId != null) {
                throw new IllegalArgumentException("childId only allowed for Node events.");
            }
        } else {
            if (childId == null) {
                throw new IllegalArgumentException("childId must not be null for Node events.");
            }
        }
        this.type = type;
        this.parentId = parentId;
        this.parentPath = parentPath;
        this.childId = childId;
        this.childRelPath = childPath;
        this.nodeType = nodeType;
        this.mixins = mixins;
        //this._session = session;
        this.parentUUID = parentUUID;
        this.itemId = itemId;
        this.userId = info.getUserId();
        this.sessionId = info.getSessionId();
    }

    //-----------------< factory methods >--------------------------------------

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#NODE_ADDED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childId    the id of the child node associated with this event.
     * @param childPath  the relative path of the child node that was added.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that added the node.
     * @return an <code>EventState</code> instance.
     */
    public static EventState childNodeAdded(NodeId parentId,
                                            Path parentPath,
                                            NodeId childId,
                                            Path.PathElement childPath,
                                            NodeTypeImpl nodeType,
                                            Set mixins,
                                            String parentUUID, SessionInfo info) {
        return new EventState(Event.NODE_ADDED,
                parentId,
                parentPath,
                childId,
                childPath,
                nodeType,
                mixins,
                parentUUID, childId, info);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#NODE_REMOVED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childId    the id of the child node associated with this event.
     * @param childPath  the relative path of the child node that was removed.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that removed the node.
     * @return an <code>EventState</code> instance.
     */
    public static EventState childNodeRemoved(NodeId parentId,
                                              Path parentPath,
                                              NodeId childId,
                                              Path.PathElement childPath,
                                              NodeTypeImpl nodeType,
                                              Set mixins,
                                              String parentUUID,SessionInfo info) {
        return new EventState(Event.NODE_REMOVED,
                parentId,
                parentPath,
                childId,
                childPath,
                nodeType,
                mixins,
                parentUUID, childId, info);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PROPERTY_ADDED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childPath  the relative path of the property that was added.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that added the property.
     * @return an <code>EventState</code> instance.
     */
    public static EventState propertyAdded(NodeId parentId,
                                           Path parentPath,
                                           Path.PathElement childPath,
                                           NodeTypeImpl nodeType,
                                           Set mixins,
                                           String parentUUID, 
                                           SessionInfo info) {
        return new EventState(Event.PROPERTY_ADDED,
                parentId,
                parentPath,
                null,
                childPath,
                nodeType,
                mixins,
                parentUUID, new PropertyId(parentId.getId(),childPath.getName() ), info);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PROPERTY_REMOVED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childPath  the relative path of the property that was removed.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that removed the property.
     * @return an <code>EventState</code> instance.
     */
    public static EventState propertyRemoved(NodeId parentId,
                                             Path parentPath,
                                             Path.PathElement childPath,
                                             NodeTypeImpl nodeType,
                                             Set mixins, 
                                             SessionInfo info
                                             ) {
        return new EventState(Event.PROPERTY_REMOVED,
                parentId,
                parentPath,
                null,
                childPath,
                nodeType,
                mixins,
                null, new PropertyId(parentId.getId(),childPath.getName() ), 
        		info);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PROPERTY_CHANGED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childPath  the relative path of the property that changed.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that changed the property.
     * @return an <code>EventState</code> instance.
     */
    public static EventState propertyChanged(NodeId parentId,
                                             Path parentPath,
                                             Path.PathElement childPath,
                                             NodeTypeImpl nodeType,
                                             Set mixins, 
                                             SessionInfo info) {
        return new EventState(Event.PROPERTY_CHANGED,
                parentId,
                parentPath,
                null,
                childPath,
                nodeType,
                mixins,
                null, new PropertyId(parentId.getId(),childPath.getName() ), 
                info);
    }

    /**
     * {@inheritDoc}
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the uuid of the parent node.
     *
     * @return the uuid of the parent node.
     */
    public NodeId getParentId() {
        return parentId;
    }

    /**
     * Returns the path of the parent node.
     *
     * @return the path of the parent node.
     */
    public Path getParentPath() {
        return parentPath;
    }

    /**
     * Returns the Id of a child node operation.
     * If this <code>EventState</code> was generated for a property
     * operation this method returns <code>null</code>.
     *
     * @return the id of a child node operation.
     */
    public NodeId getChildId() {
        return childId;
    }

    /**
     * Returns the relative {@link Path} of the child
     * {@link javax.jcr.Item} associated with this event.
     *
     * @return the <code>Path.PathElement</code> associated with this event.
     */
    public Path.PathElement getChildRelPath() {
        return childRelPath;
    }

    /**
     * Returns the node type of the parent node associated with this event.
     *
     * @return the node type of the parent associated with this event.
     */
    public NodeTypeImpl getNodeType() {
        return nodeType;
    }

    /**
     * Returns a set of <code>QName</code>s which are the names of the mixins
     * assigned to the parent node associated with this event.
     *
     * @return the mixin names as <code>QName</code>s.
     */
    public Set getMixinNames() {
        return mixins;
    }

    /**
     * {@inheritDoc}
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the <code>Session</code> that caused / created this
     * <code>EventState</code>.
     *
     * @return the <code>Session</code> that caused / created this
     *         <code>EventState</code>.
     */
    /*SessionImpl _getSession() {
        return _session;
    }*/

    /**
     * Returns the id of the associated item of this <code>EventState</code>.
     *
     * @return the <code>ItemId</code>.
     */
    ItemId getTargetId() {
        if (childId == null) {
            // property event
            return new PropertyId(parentId.getId(), childRelPath.getName());
            //throw new UnsupportedOperationException();
        } else {
            // node event
            return childId;
        }
    }

    /**
     * Returns a String representation of this <code>EventState</code>.
     *
     * @return a String representation of this <code>EventState</code>.
     */
    public String toString() {
        if (stringValue == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("EventState: ").append(valueOf(type));
            sb.append(", Parent: ").append(parentId);
            try {
                sb.append(", Parent Path: ").append(parentPath);
			} catch (Exception e) {
				
			}
            sb.append(", Child: ").append(childRelPath);
            sb.append(", UserId: ").append(userId);
            stringValue = sb.toString();
        }
        return stringValue;
    }

    /**
     * Returns a hashCode for this <code>EventState</code>.
     *
     * @return a hashCode for this <code>EventState</code>.
     */
    public int hashCode() {
        int h = hashCode;
        if (h == 0) {
            h = 37;
            h = 37 * h + type;
            h = 37 * h + parentId.hashCode();
            h = 37 * h + childRelPath.hashCode();
            h = 37 * h + (int)sessionId;
            hashCode = h;
        }
        return hashCode;
    }

    /**
     * Returns <code>true</code> if this <code>EventState</code> is equal to
     * another object.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if object <code>obj</code> is equal to this
     *         <code>EventState</code>; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof EventState) {
            EventState other = (EventState) obj;
            //TODO replace sessionhash with uniq session identity
            return this.type == other.type
                    && this.parentId.equals(other.parentId)
                    && this.childRelPath.equals(other.childRelPath)
                    && this.sessionId  == other.sessionId;
        }
        return false;
    }

    /**
     * Returns a String representation of <code>eventType</code>.
     *
     * @param eventType an event type defined by {@link Event}.
     * @return a String representation of <code>eventType</code>.
     */
    public static String valueOf(int eventType) {
        if (eventType == Event.NODE_ADDED) {
            return "NodeAdded";
        } else if (eventType == Event.NODE_REMOVED) {
            return "NodeRemoved";
        } else if (eventType == Event.PROPERTY_ADDED) {
            return "PropertyAdded";
        } else if (eventType == Event.PROPERTY_CHANGED) {
            return "PropertyChanged";
        } else if (eventType == Event.PROPERTY_REMOVED) {
            return "PropertyRemoved";
        } else {
            return "UnknownEventType";
        }
    }

    public String getParentUUID() {
        return parentUUID;
    }

	public ItemId getItemid() {
		return itemId;
	}

	public Long getSessionId() {
		// TODO Auto-generated method stub
		return sessionId;
	}

	public RepositoryImpl getRepository() {
		return repository;
	}

	public String getWorkspaceName() {
		return workspaceName;
	}

}
