/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;


import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.Path;

/**
 * The <code>EventFilter</code> class implements the filter logic based
 * on the session's access rights and the specified filter rules.
 */
class RepositoryEventFilter implements _EventFilter{

    /**
     * Logger instance for this class.
     */
    //private static final Log log = LogFactory.getLog(EventFilter.class);

    static final RepositoryEventFilter BLOCK_ALL = new BlockAllFilter();

    /**
     * This <code>EventFilter</code> should only allow events with the
     * specified types.
     */
    private final long eventTypes;

    /**
     * Only allow Items with the specified <code>path</code>
     */
    private final Path path;
    private final boolean isRootPath;

    /**
     * If <code>isDeep</code> is <code>true</code> also Items under <code>absPath</code>
     * are allowed.
     */
    private final boolean isDeep;

    /**
     * Only allow Nodes with the specified <code>uuids</code>.
     */
    private final String[] uuids;

    /**
     * Only allow Nodes with the specified {@link javax.jcr.nodetype.NodeType}s.
     */
    private final NodeTypeImpl[] nodeTypes;

	private RepositoryImpl repository;

    /**
     * Creates a new <code>EventFilter</code> instance.
     *
     * @param itemMgr    the <code>ItemManager</code> of the <code>session</code>.
     * @param session    the <code>Session</code> that registered the {@link
     *                   javax.jcr.observation.EventListener}.
     * @param eventTypes only allow specified {@link javax.jcr.observation.Event} types.
     * @param path       only allow {@link javax.jcr.Item} with
     *                   <code>path</code>.
     * @param isDeep     if <code>true</code> also allow events for {@link
     *                   Item}s below <code>absPath</code>.
     * @param ids        only allow events for {@link javax.jcr.Node}s with
     *                   specified NodeIDs. If <code>null</code> is passed no
     *                   restriction regarding NodeIds is applied.
     * @param nodeTypes  only allow events for specified {@link
     *                   javax.jcr.nodetype.NodeType}s. If <code>null</code> no
     *                   node type restriction is applied.
     * @param noLocal    if <code>true</code> no events are allowed that were
     *                   created from changes related to the <code>Session</code>
     *                   that registered the {@link javax.jcr.observation.EventListener}.
     */
    RepositoryEventFilter(
                RepositoryImpl repository,
                long eventTypes,
                Path path,
                boolean isDeep,
                String[] uuids,
                NodeTypeImpl[] nodeTypes) { //ItemManager itemMgr,

        //this.itemMgr = itemMgr;
        this.repository = repository;
        this.eventTypes = eventTypes;
        this.path = path;
        this.isDeep = isDeep;
        this.uuids = uuids;

        this.nodeTypes = nodeTypes;
        if (path == null){
        	isRootPath = true;
        } else {
            isRootPath = path.denotesRoot();
        }
    }

    /**
     * Returns the <code>Session</code> associated with this
     * <code>EventFilter</code>.
     *
     * @return the <code>Session</code> associated with this
     *         <code>EventFilter</code>.
     */
    public RepositoryImpl getRepository() {
        return repository;
    }

    /**
     * Returns the <code>ItemManager</code> associated with this
     * <code>EventFilter</code>.
     *
     * @return the <code>ItemManager</code> associated with this
     *         <code>EventFilter</code>.
     */
    /*ItemManager getItemManager() {
        return itemMgr;
    }*/

    /**
     * Returns <code>true</code> if this <code>EventFilter</code> does not allow
     * the specified <code>EventState</code>; <code>false</code> otherwise.
     *
     * @param eventState the <code>EventState</code> in question.
     * @return <code>true</code> if this <code>EventFilter</code> blocks the
     *         <code>EventState</code>.
     * @throws RepositoryException if an error occurs while checking.
     */
    public boolean blocks(EventState eventState) throws RepositoryException {
        // first do cheap checks

        // check event type
        long type = eventState.getType();
        if ((eventTypes & type) == 0) {
            return true;
        }

        // check UUIDs
        String parentUUID= eventState.getParentUUID();
        if (uuids != null) {
            boolean match = false;
            for (int i = 0; i < uuids.length && !match; i++) {
                match |= parentUUID.equals(uuids[i]);
            }
            if (!match) {
                return true;
            }
        }

        // check node types
        if (nodeTypes != null) {
            boolean match = false;
            for (int i = 0; i < nodeTypes.length && !match; i++) {
                match |= eventState.getNodeType().getQName().equals(nodeTypes[i].getQName())
                        || eventState.getMixinNames().contains(nodeTypes[i].getQName())
                        || eventState.getNodeType().isDerivedFrom(nodeTypes[i].getQName());
            }
            if (!match) {
                return true;
            }
        }

        // finally check path
        try {
            // the relevant path for the path filter depends on the event type
            // for node events, the relevant path is the one returned by
            // Event.getPath().
            // for property events, the relevant path is the path of the
            // node where the property belongs to.
            Path eventPath = null;
            if (type == Event.NODE_ADDED || type == Event.NODE_REMOVED) {
                Path.PathElement nameElem = eventState.getChildRelPath();
                if (nameElem.getIndex() == 0) {
                    eventPath = Path.create(eventState.getParentPath(), nameElem.getName(), false);
                } else {
                    eventPath = Path.create(eventState.getParentPath(), nameElem.getName(), nameElem.getIndex(), false);
                }
            } else {
                eventPath = eventState.getParentPath();
            }
            boolean match = eventPath.equals(path);
            if (!match && isDeep) {
            	if (isRootPath){
            		match = true;
            	} else {
            		match = eventPath.isDescendantOf(path);
            	}
            }

            return !match;
        } catch (MalformedPathException mpe) {
            // should never get here...
            throw new RepositoryException("internal error: failed to check path filter", mpe);
        }
    }

    /**
     * This class implements an <code>EventFilter</code> that blocks
     * all {@link EventState}s.
     */
    private static final class BlockAllFilter extends RepositoryEventFilter {

        /**
         * Creates a new <code>BlockAllFilter</code>.
         */
        BlockAllFilter() {
            super(null, 0, null, true, null, null);//null,
        }

        /**
         * Always return <code>true</code>.
         *
         * @return always <code>true</code>.
         */
        public boolean blocks(EventState eventState) {
            return true;
        }
    }

	public SessionImpl getSession() {
		return null;
	}

}
