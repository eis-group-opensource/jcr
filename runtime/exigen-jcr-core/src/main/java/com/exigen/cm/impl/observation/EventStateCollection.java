/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.NodeTypeManagerImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.state.NodeState;
import com.exigen.cm.impl.state2._ItemState;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.jackrabbit.name.Path;

/**
 * The <code>EventStateCollection</code> class implements how {@link EventState}
 * objects are created based on the {@link org.apache.jackrabbit.core.state.ItemState}s
 * passed to the {@link #createEventStates} method.
 * <p/>
 * The basic sequence of method calls is:
 * <ul>
 * <li>{@link #createEventStates} or {@link #addAll} to create or add event
 * states to the collection</li>
 * <li>{@link #prepare} or {@link #prepareDeleted} to prepare the events. If
 * this step is omitted, EventListeners might see events of deleted item
 * they are not allowed to see.</li>
 * <li>{@link #dispatch()} to dispatch the events to the EventListeners.</li>
 * </ul>
 */
public final class EventStateCollection {

    /**
     * Logger instance for this class
     */
    private static final Log log = LogFactory.getLog(EventStateCollection.class);


    /**
     * List of events
     */
    private final List<EventState> events = new ArrayList<EventState>();

    /**
     * Event dispatcher.
     */
    private final EventDispatcher dispatcher;

    /**
     * The session that created these events
     */
    private final SessionImpl session;

    /**
     * The prefix to use for the event paths or <code>null</code> if no prefix
     * should be used.
     */
    private final Path pathPrefix;

    /**
     * Creates a new empty <code>EventStateCollection</code>.
     * <p/>
     * Because the item state manager in {@link #createEventStates} may represent
     * only a subset of the over all item state hierarchy, this constructor
     * also takes a path prefix argument. If non <code>null</code> all events
     * created by this collection are prefixed with this path.
     *
     * @param dispatcher event dispatcher
     * @param session    the session that created these events.
     * @param pathPrefix the path to prefix the event paths or <code>null</code>
     *                   if no prefix should be used.
     * @param events2 
     */
    EventStateCollection(EventDispatcher dispatcher,
                         SessionImpl session,
                         Path pathPrefix, List<EventState> events2) {
        this.dispatcher = dispatcher;
        this.session = session;
        this.pathPrefix = pathPrefix;
        this.events.addAll(events2);
    }

    /**
     * Creates {@link EventState} instances from <code>ItemState</code>
     * <code>changes</code>.
     *
     * @param rootNodeId   the id of the root node.
     * @param changes      the changes on <code>ItemState</code>s.
     * @param stateMgr     an <code>ItemStateManager</code> to provide <code>ItemState</code>
     *                     of items that are not contained in the <code>changes</code> collection.
     * @throws RepositoryException 
     * @throws ItemStateException if an error occurs while creating events
     *                            states for the item state changes.
     */
    public void createEventStates(NodeId rootNodeId, ChangeLog changes) throws RepositoryException {//, ItemStateManager stateMgr // throws ItemStateException
        // create a hierarchy manager, that is based on the ChangeLog and
        // the ItemStateProvider
        // todo use CachingHierarchyManager ?
        /*ChangeLogBasedHierarchyMgr hmgr =
                new ChangeLogBasedHierarchyMgr(rootNodeId, stateMgr, changes,
                        session.getNamespaceResolver());
*/
    	
    	events.addAll(changes.getEvents());
        /**
         * Important:
         * Do NOT change the sequence of events generated unless there's
         * a very good reason for it! Some internal SynchronousEventListener
         * implementations depend on the order of the events fired.
         * LockManagerImpl for example expects that for any given path a
         * childNodeRemoved event is fired before a childNodeAdded event.
         */

        // 1. modified items

        for (Iterator<_ItemState> it = changes.modifiedStates(); it.hasNext();) {
            _ItemState state = (_ItemState) it.next();
            if (state.isNode()) {
                // node changed
                // covers the following cases:
                // 1) property added
                // 2) property removed
                // 3) child node added
                // 4) child node removed
                // 5) node moved
                // 6) node reordered
                // cases 1) and 2) are detected with added and deleted states
                // on the PropertyState itself.
                // cases 3) and 4) are detected with added and deleted states
                // on the NodeState itself.
                // in case 5) two or three nodes change. two nodes are changed
                // when a child node is renamed. three nodes are changed when
                // a node is really moved. In any case we are only interested in
                // the node that actually got moved.
                // in case 6) only one node state changes. the state of the
                // parent node.
                _NodeState n = (_NodeState) state;

                if (n.hasOverlayedState()) {
                    NodeId oldParentId = n.getOverlayedState().getParentId();
                    NodeId newParentId = new NodeId(n.getParent().getNodeId(), n.getParent().getInternalUUID());
                    if (newParentId != null &&
                            !oldParentId.equals(newParentId)) {
                        // node moved
                        // generate node removed & node added event
                        _NodeState oldParent;
                        //try {
                            oldParent = (_NodeState) changes.get(oldParentId);
                        //} catch (NoSuchItemStateException e) {
                            // old parent has been deleted, retrieve from
                            // shared item state manager
                            //oldParent = (NodeState) stateMgr.getItemState(oldParentId);
                        //}

                        NodeTypeImpl oldParentNodeType = getNodeType(oldParent, session);
                        Set mixins = oldParent.getMixinTypeNames();
                        //Path newPath = getPath(n.getNodeId(), hmgr);
                        Path newPath = n.getPrimaryPath();
                        //Path oldPath = getZombiePath(n.getNodeId(), hmgr);
                        Path oldPath = n.getOverlayedState().getPath();
                        if (!oldPath.equals(newPath)) {
                            events.add(EventState.childNodeRemoved(oldParentId,
                                    getParent(oldPath),
                                    n.getNodeItemId(),
                                    oldPath.getNameElement(),
                                    oldParentNodeType,
                                    mixins,
                                    n.getOverlayedState().getParentUUID(),
                                    session.getSessionInfo()));

                            _NodeState newParent = (_NodeState) changes.get(newParentId);
                            NodeTypeImpl newParentNodeType = getNodeType(newParent, session);
                            mixins = newParent.getMixinTypeNames();
                            events.add(EventState.childNodeAdded(newParentId,
                                    getParent(newPath),
                                    n.getNodeItemId(),
                                    newPath.getNameElement(),
                                    newParentNodeType,
                                    mixins,
                                    getParentUUID(n),
                                    session.getSessionInfo()));
                        } else {
                            log.error("Unable to calculate old path of moved node");
                        }
                    } else {
                        // a moved node always has a modified parent node
                        NodeState parent = null;
                        //try {
                            // root node does not have a parent UUID
                            if (state.getParent() != null) {
                                parent = (NodeState) changes.get(new NodeId(state.getParent().getNodeId(),(String) null));
                            }
                        //} catch (NoSuchItemStateException e) {
                            // should never happen actually. this would mean
                            // the parent of this modified node is deleted
                            //String msg = "Parent of node " + state.getId() + " is deleted.";
                            //log.error(msg);
                            //throw new ItemStateException(msg, e);
                        //}
                        if (parent != null) {
                            // check if node has been renamed
                            if (true){
                                throw new UnsupportedOperationException();
                            }
                            /*NodeState.ChildNodeEntry moved = null;
                            for (Iterator removedNodes = parent.getRemovedChildNodeEntries().iterator(); removedNodes.hasNext();) {
                                NodeState.ChildNodeEntry child = (NodeState.ChildNodeEntry) removedNodes.next();
                                if (child.getId().equals(n.getNodeId())) {
                                    // found node re-added with different name
                                    moved = child;
                                }
                            }
                            if (moved != null) {
                                NodeTypeImpl nodeType = getNodeType(parent, session);
                                Set mixins = parent.getMixinTypeNames();
                                //Path newPath = getPath(state.getId(), hmgr);
                                Path newPath = state.getPrimaryPath();
                                Path parentPath = getParent(newPath);
                                Path oldPath;
                                //try {
                                    if (moved.getIndex() == 0) {
                                        oldPath = Path.create(parentPath, moved.getName(), false);
                                    } else {
                                        oldPath = Path.create(parentPath, moved.getName(), moved.getIndex(), false);
                                    }
                                //} catch (MalformedPathException e) {
                                    // should never happen actually
                                    //String msg = "Malformed path for item: " + state.getId();
                                    //log.error(msg);
                                    //throw new ItemStateException(msg, e);
                                //}
                                events.add(EventState.childNodeRemoved(parent.getNodeItemId(),
                                        parentPath,
                                        n.getNodeItemId(),
                                        oldPath.getNameElement(),
                                        nodeType,
                                        mixins,
                                        session));
                                events.add(EventState.childNodeAdded(parent.getNodeItemId(),
                                        parentPath,
                                        n.getNodeItemId(),
                                        newPath.getNameElement(),
                                        nodeType,
                                        mixins,
                                        session));
                            }*/
                        }
                    }
                }

                // TODO check if child nodes of modified node state have been reordered
                /*List reordered = n.getReorderedChildNodeEntries();
                NodeTypeImpl nodeType = getNodeType(n, session);
                Set mixins = n.getMixinTypeNames();
                if (reordered.size() > 0) {
                    // create a node removed and a node added event for every
                    // reorder
                    for (Iterator ro = reordered.iterator(); ro.hasNext();) {
                        NodeState.ChildNodeEntry child = (NodeState.ChildNodeEntry) ro.next();
                        QName name = child.getName();
                        int index = (child.getIndex() != 1) ? child.getIndex() : 0;
                        Path parentPath = getPath(n.getNodeId(), hmgr);
                        Path.PathElement addedElem = Path.create(name, index).getNameElement();
                        // get removed index
                        NodeState overlayed = (NodeState) n.getOverlayedState();
                        NodeState.ChildNodeEntry entry = overlayed.getChildNodeEntry(child.getId());
                        if (entry == null) {
                            throw new ItemStateException("Unable to retrieve old child index for item: " + child.getId());
                        }
                        int oldIndex = (entry.getIndex() != 1) ? entry.getIndex() : 0;
                        Path.PathElement removedElem = Path.create(name, oldIndex).getNameElement();

                        events.add(EventState.childNodeRemoved(n.getNodeItemId(),
                                parentPath,
                                child.getId(),
                                removedElem,
                                nodeType,
                                mixins,
                                session));

                        events.add(EventState.childNodeAdded(n.getNodeItemId(),
                                parentPath,
                                child.getId(),
                                addedElem,
                                nodeType,
                                mixins,
                                session));
                    }
                }*/
            } else {
                // property changed
                //Path path = getPath(state.getId(), hmgr);
                Path path = state.getPrimaryPath();
                _NodeState parent = (_NodeState) state.getParent();
                NodeTypeImpl nodeType = getNodeType(parent, session);
                Set mixins = parent.getMixinTypeNames();
                events.add(EventState.propertyChanged(new NodeId(parent.getNodeId(), parent.getInternalUUID()),
                        getParent(path),
                        path.getNameElement(),
                        nodeType,
                        mixins,
                        session.getSessionInfo()));
            }
        }

        // 2. removed items

        for (Iterator it = changes.deletedStates(); it.hasNext();) {
            _ItemState state = (_ItemState) it.next();
            if (state.isNode()) {
                // node deleted
                _NodeState n = (_NodeState) state;
                //NodeState parent = (NodeState) stateMgr.getItemState(n.getParentId());
                _NodeState parent = n.getParent();
                //NodeTypeImpl nodeType = getNodeType(parent, session);
                NodeTypeImpl nodeType = getNodeType(parent, session);
                Set mixins = parent.getMixinTypeNames();
                //Path path = getZombiePath(state.getId(), hmgr);
                Path path = n.getPrimaryPath();
                events.add(EventState.childNodeRemoved(new NodeId(parent.getNodeId(), parent.getInternalUUID()),
                        getParent(path),
                        new NodeId(n.getNodeId(), n.getInternalUUID()),
                        path.getNameElement(),
                        nodeType,
                        mixins,
                        n.getInternalUUID(),
                        session.getSessionInfo()));
            } else {
                // property removed
                // only create an event if node still exists
                //try {
                    //NodeState n = (NodeState) changes.get(state.getParentId());
                    _NodeState n = state.getParent();
                    // node state exists -> only property removed
                    NodeTypeImpl nodeType = getNodeType(n, session);
                    Set mixins = n.getMixinTypeNames();
                    //Path path = getZombiePath(state.getId(), hmgr);
                    Path path = state.getPrimaryPath();
                    events.add(EventState.propertyRemoved(new NodeId(n.getNodeId(), n.getInternalUUID()),
                            getParent(path),
                            path.getNameElement(),
                            nodeType,
                            mixins,
                            session.getSessionInfo()));
                //} catch (NoSuchItemStateException e) {
                    // node removed as well -> do not create an event
                //}
            }
        }

        // 3. added items

        for (Iterator<_ItemState> it = changes.addedStates(); it.hasNext();) {
            _ItemState state =  it.next();
            if (state.isNode()) {
                // node created
                _NodeState n = (_NodeState) state;
                _NodeState parent = n.getParent();
                NodeId parentId = new NodeId(parent.getNodeId(), parent.getInternalUUID());
                // unknown if parent node is also new
                /*if (stateMgr.hasItemState(parentId)) {
                    parent = (NodeState) stateMgr.getItemState(parentId);
                } else {
                    parent = (NodeState) changes.get(parentId);
                }*/
                NodeTypeImpl nodeType = getNodeType(parent, session);
                Set mixins = parent.getMixinTypeNames();
                //Path path = getPath(n.getNodeId(), hmgr);
                Path path = n.getPrimaryPath();
                events.add(EventState.childNodeAdded(parentId,
                        getParent(path),
                        new NodeId(n.getNodeId(), n.getInternalUUID()),
                        path.getNameElement(),
                        nodeType,
                        mixins,
                        n.getInternalUUID(),
                        session.getSessionInfo()));
            } else {
                // property created / set
                //NodeState n = (NodeState) changes.get(state.getParentId());
                //NodeState n = (NodeState) changes.get(state.getParentId());
                _NodeState n = state.getParent();
                NodeTypeImpl nodeType = getNodeType(n, session);
                Set mixins = n.getMixinTypeNames();
                //Path path = getPath(state.getId(), hmgr);
                Path path = state.getPrimaryPath();
                events.add(EventState.propertyAdded(new NodeId(n.getNodeId(), n.getInternalUUID()), //may be parent ???
                        getParent(path),
                        path.getNameElement(),
                        nodeType,
                        mixins,
                        n.getInternalUUID(),
                        session.getSessionInfo()));
            }
        }
    }

    private String getParentUUID(_NodeState n) throws AccessDeniedException, RepositoryException {
        return n.getParent().getInternalUUID();
    }

    /**
     * Adds all event states in the given collection to this collection
     *
     * @param c
     */
    public void addAll(Collection c) {
        events.addAll(c);
    }

    /**
     * Prepares already added events for dispatching.
     */
    public void prepare() {
        dispatcher.prepareEvents(this);
    }

    /**
     * Prepares deleted items from <code>changes</code>.
     *
     * @param changes the changes to prepare.
     */
    public void prepareDeleted(ChangeLog changes) {
        dispatcher.prepareDeleted(this, changes);
    }

    /**
     * Dispatches the events to the {@link javax.jcr.observation.EventListener}s.
     */
    public void dispatch() throws RepositoryException{
        dispatcher.dispatchEvents(this, session);
    }

    public void dispatchBefore() throws RepositoryException {
        dispatcher.dispatchBeforeEvents(this, session);
    }

    /**
     * Returns the path prefix for this event state collection or <code>null</code>
     * if no path prefix was set in the constructor of this collection. See
     * also {@link EventStateCollection#EventStateCollection}.
     *
     * @return the path prefix for this event state collection.
     */
    public Path getPathPrefix() {
        return pathPrefix;
    }

    /**
     * Returns an iterator over {@link EventState} instance.
     *
     * @return an iterator over {@link EventState} instance.
     */
    Iterator iterator() {
        return events.iterator();
    }

    /**
     * Return the list of events.
     * @return list of events
     */
    List getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Return the session who is the origin of this events.
     * @return event source
     */
    SessionImpl getSession() {
        return session;
    }

    /**
     * Resolves the node type name in <code>node</code> into a {@link NodeType}
     * object using the {@link NodeTypeManager} of <code>session</code>.
     *
     * @param node    the node.
     * @param session the session.
     * @return the {@link NodeType} of <code>node</code>.
     * @throws RepositoryException 
     * @throws ItemStateException if the nodetype cannot be resolved.
     */
    public static NodeTypeImpl getNodeType(_NodeState node, SessionImpl session) throws RepositoryException
            { //throws ItemStateException 
        /*try {
            return (NodeTypeImpl) session.getNodeTypeManager().getNodeTypeBySQLId(node.getNodeTypeId());
        } catch (Exception e) {
            // also catch eventual runtime exceptions here
            // should never happen actually
            String msg = "Item " + node.getNodeId() + " has unknown node type: " + node.getNodeTypeId();
            log.error(msg);
            throw new RepositoryException(msg, e);
        }*/
    	return getNodeType(node, session.getNodeTypeManager());
    }
    public static NodeTypeImpl getNodeType(_NodeState node, NodeTypeManagerImpl ntManager) throws RepositoryException
    { //throws ItemStateException 
		try {
		    return (NodeTypeImpl) ntManager.getNodeTypeBySQLId(node.getNodeTypeId());
		} catch (Exception e) {
		    // also catch eventual runtime exceptions here
		    // should never happen actually
		    String msg = "Item " + node.getNodeId() + " has unknown node type: " + node.getNodeTypeId();
		    log.error(msg);
		    throw new RepositoryException(msg, e);
		}
	}

    /**
     * Returns the path of the parent node of node at <code>path</code>..
     *
     * @param p the path.
     * @return the parent path.
     * @throws ItemStateException if <code>p</code> does not have a parent
     *                            path. E.g. <code>p</code> designates root.
     */
    public static Path getParent(Path p) throws RepositoryException {
        try {
            return p.getAncestor(1);
        } catch (PathNotFoundException e) {
            // should never happen actually
            String msg = "Unable to resolve parent for path: " + p;
            log.error(msg);
            throw new RepositoryException(msg, e);
        }
    }

	@Override
	public String toString() {
		return events.toString();
	}

    /**
     * Resolves the path of the Item with id <code>itemId</code>.
     *
     * @param itemId the id of the item.
     * @return the path of the item.
     * @throws ItemStateException if the path cannot be resolved.
     */
    /*private Path getPath(ItemId itemId, HierarchyManager hmgr)
            throws ItemStateException {
        try {
            return prefixPath(hmgr.getPath(itemId));
        } catch (RepositoryException e) {
            // should never happen actually
            String msg = "Unable to resolve path for item: " + itemId;
            log.error(msg);
            throw new ItemStateException(msg, e);
        }
    }*/

    /**
     * Resolves the <i>zombie</i> (i.e. the old) path of the Item with id
     * <code>itemId</code>.
     *
     * @param itemId the id of the item.
     * @return the path of the item.
     * @throws ItemStateException if the path cannot be resolved.
     */
    /*private Path getZombiePath(ItemId itemId, ChangeLogBasedHierarchyMgr hmgr)
            throws ItemStateException {
        try {
            return prefixPath(hmgr.getZombiePath(itemId));
        } catch (RepositoryException e) {
            // should never happen actually
            String msg = "Unable to resolve zombie path for item: " + itemId;
            log.error(msg);
            throw new ItemStateException(msg, e);
        }
    }*/

    /**
     * Prefixes the Path <code>p</code> with {@link #pathPrefix}.
     *
     * @param p the Path to prefix.
     * @return the prefixed path or <code>p</code> itself if {@link #pathPrefix}
     *         is <code>null</code>.
     * @throws RepositoryException if the path cannot be prefixed.
     */
    /*private Path prefixPath(Path p) throws RepositoryException {
        if (pathPrefix == null) {
            return p;
        }
        Path.PathBuilder builder = new Path.PathBuilder(pathPrefix.getElements());
        Path.PathElement[] elements = p.getElements();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].denotesRoot()) {
                continue;
            }
            builder.addLast(elements[i]);
        }
        try {
            return builder.getPath();
        } catch (MalformedPathException e) {
            throw new RepositoryException(e);
        }
    }*/
}
