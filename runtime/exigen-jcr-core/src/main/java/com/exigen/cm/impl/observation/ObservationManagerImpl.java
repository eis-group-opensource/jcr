/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;


import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.NodeTypeManagerImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.Path;

/**
 * Each <code>Session</code> instance has its own <code>ObservationManager</code>
 * instance. The class <code>SessionLocalObservationManager</code> implements
 * this behaviour.
 */
public class ObservationManagerImpl implements ObservationManager, EventStateCollectionFactory {

    /**
     * The logger instance of this class
     */
    private static final Log log = LogFactory.getLog(ObservationManagerImpl.class);

    /**
     * The <code>Session</code> this <code>ObservationManager</code>
     * belongs to.
     */
    private final SessionImpl session;

    /**
     * The <code>ItemManager</code> for this <code>ObservationManager</code>.
     */
    //private final ItemManager itemMgr;

    /**
     * The <code>ObservationManagerFactory</code>
     */
    private final ObservationManagerFactory obsMgrFactory;

    static {
        // preload EventListenerIteratorImpl to prevent classloader issues during shutdown
        EventListenerIteratorImpl.class.hashCode();
    }

    /**
     * Creates an <code>ObservationManager</code> instance.
     *
     * @param session the <code>Session</code> this ObservationManager
     *                belongs to.
     * @param itemMgr {@link org.apache.jackrabbit.core.ItemManager} of the passed
     *                <code>Session</code>.
     * @throws NullPointerException if <code>session</code> or <code>itemMgr</code>
     *                              is <code>null</code>.
     */
    ObservationManagerImpl(ObservationManagerFactory obsMgrFactory,
                           SessionImpl session
                           ) throws NullPointerException { //ItemManager itemMgr
        if (session == null) {
            throw new NullPointerException("session");
        }
        /*if (itemMgr == null) {
            throw new NullPointerException("itemMgr");
        }*/

        this.obsMgrFactory = obsMgrFactory;
        this.session = session;
        //this.itemMgr = itemMgr;
    }

    /**
     * {@inheritDoc}
     */
    public void addEventListener(EventListener listener,
                                 int eventTypes,
                                 String absPath,
                                 boolean isDeep,
                                 String[] uuid,
                                 String[] nodeTypeName,
                                 boolean noLocal)
            throws RepositoryException {

        // create NodeType instances from names
        NodeTypeImpl[] nodeTypes;
        if (nodeTypeName == null) {
            nodeTypes = null;
        } else {
            NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
            nodeTypes = new NodeTypeImpl[nodeTypeName.length];
            for (int i = 0; i < nodeTypes.length; i++) {
                nodeTypes[i] = (NodeTypeImpl) ntMgr.getNodeType(nodeTypeName[i]);
            }
        }

        Path path;
        try {
            path = Path.create(absPath, session.getNamespaceResolver(), true);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path syntax: " + absPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
        /*String[] uuids = null;
        if (uuid != null) {
            ids = new NodeId[uuid.length];
            for (int i=0; i<uuid.length; i++) {
                //ids[i] = NodeId.valueOf(uuid[i]);
                
                throw new UnsupportedOperationException();
            }
        }*/
        // create filter
        EventFilter filter = new EventFilter(obsMgrFactory.getRepository(),
                session,
                eventTypes,
                path,
                isDeep,
                uuid,
                nodeTypes,
                noLocal);//itemMgr,

        EventConsumer consumer =
                new EventConsumer(session, listener, filter);
        obsMgrFactory.addConsumer(consumer);
    }

    /**
     * {@inheritDoc}
     */
    public void removeEventListener(EventListener listener)
            throws RepositoryException {
        EventConsumer consumer =
                new EventConsumer(session, listener, EventFilter.BLOCK_ALL);
        obsMgrFactory.removeConsumer(consumer);

    }

    /**
     * {@inheritDoc}
     */
    public EventListenerIterator getRegisteredEventListeners()
            throws RepositoryException {
        return new EventListenerIteratorImpl(session,
                obsMgrFactory.getSynchronousConsumers(),
                obsMgrFactory.getAsynchronousConsumers());
    }

    /**
     * Unregisters all EventListeners.
     */
    public void dispose() {
        try {
            EventListenerIterator it = getRegisteredEventListeners();
            while (it.hasNext()) {
                EventListener l = it.nextEventListener();
                log.debug("removing EventListener: " + l);
                removeEventListener(l);
            }
        } catch (RepositoryException e) {
            log.error("Internal error: Unable to dispose ObservationManager.", e);
        }

    }

    //------------------------------------------< EventStateCollectionFactory >

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an <code>EventStateCollection</code> tied to the session
     * which is attached to this <code>ObservationManager</code> instance.
     * @param events 
     */
    public EventStateCollection createEventStateCollection(List<EventState> events) {
        return new EventStateCollection(obsMgrFactory, session, null, events);
    }
}
