/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;



import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import com.exigen.cm.impl.ItemId;
import com.exigen.cm.impl.SessionImpl;

/**
 * The <code>EventConsumer</code> class combines the {@link
 * javax.jcr.observation.EventListener} with the implementation of specified
 * filter for the listener: {@link EventFilter}.
 * <p/>
 * Collections of {@link EventState} objects will be dispatched to {@link
 * #consumeEvents}.
 */
class EventConsumer {

    /**
     * The default Logger instance for this class.
     */
    //private static final Log log = LogFactory.getLog(EventConsumer.class);

    /**
     * The <code>Session</code> associated with this <code>EventConsumer</code>.
     */
    private final SessionImpl session;

    /**
     * The listener part of this <code>EventConsumer</code>.
     */
    private final EventListener listener;

    /**
     * The <code>EventFilter</code> for this <code>EventConsumer</code>.
     */
    private final EventFilter filter;

    /**
     * A map of <code>Set</code> objects that hold references to
     * <code>ItemId</code>s of denied <code>ItemState</code>s. The map uses the
     * <code>EventStateCollection</code> as the key to reference a deny Set.
     */
    private final Map accessDenied = Collections.synchronizedMap(new WeakHashMap());

    /**
     * cached hash code value
     */
    private int hashCode;

    /**
     * An <code>EventConsumer</code> consists of a <code>Session</code>, the
     * attached <code>EventListener</code> and an <code>EventFilter</code>.
     *
     * @param session  the <code>Session</code> that created this
     *                 <code>EventConsumer</code>.
     * @param listener the actual <code>EventListener</code> to call back.
     * @param filter   only pass an <code>Event</code> to the listener if the
     *                 <code>EventFilter</code> allows the <code>Event</code>.
     * @throws NullPointerException if <code>session</code>, <code>listener</code>
     *                              or <code>filter</code> is<code>null</code>.
     */
    EventConsumer(SessionImpl session, EventListener listener, EventFilter filter)
            throws NullPointerException {
        if (session == null) {
            throw new NullPointerException("session");
        }
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        if (filter == null) {
            throw new NullPointerException("filter");
        }

        this.session = session;
        this.listener = listener;
        this.filter = filter;
    }

    /**
     * Returns the <code>Session</code> that is associated
     * with this <code>EventConsumer</code>.
     *
     * @return the <code>Session</code> of this <code>EventConsumer</code>.
     */
    Session getSession() {
        return session;
    }

    /**
     * Returns the <code>EventListener</code> that is associated with this
     * <code>EventConsumer</code>.
     *
     * @return the <code>EventListener</code> of this <code>EventConsumer</code>.
     */
    EventListener getEventListener() {
        return listener;
    }

    /**
     * Checks for what {@link EventState}s this <code>EventConsumer</code> has
     * enough access rights to see the event.
     *
     * @param events the collection of {@link EventState}s.
     */
    void prepareEvents(EventStateCollection events) {
        Iterator it = events.iterator();
        Set denied = null;
        while (it.hasNext()) {
            EventState state = (EventState) it.next();
            if (state.getType() == Event.NODE_REMOVED
                    || state.getType() == Event.PROPERTY_REMOVED) {

                if (session.getSessionId().equals(state.getSessionId())) {
                    // if we created the event, we can be sure that
                    // we have enough access rights to see the event
                    continue;
                }

                // check read permission
                ItemId targetId = state.getTargetId();
                boolean granted = false;
                //try {
                    //granted = session.getAccessManager().isGranted(targetId, AccessManager.READ);
                    if (true) throw new UnsupportedOperationException();
                /*} catch (RepositoryException e) {
                    log.warn("Unable to check access rights for item: " + targetId);
                }*/
                if (!granted) {
                    if (denied == null) {
                        denied = new HashSet();
                    }
                    denied.add(targetId);
                }
            }
        }
        if (denied != null) {
            accessDenied.put(events, denied);
        }
    }

    /**
     * Checks for which deleted <code>ItemStates</code> this
     * <code>EventConsumer</code> has enough access rights to see the event.
     *
     * @param events       the collection of {@link EventState}s.
     * @param deletedItems Iterator of deleted <code>ItemState</code>s.
     */
    void prepareDeleted(EventStateCollection events, Iterator deletedItems) {
        Set denied = null;
        while (deletedItems.hasNext()) {
            /*ItemState item = (ItemState) deletedItems.next();
            // check read permission
            boolean granted = false;
            try {
                granted = session.getAccessManager().isGranted(item.getId(), AccessManager.READ);
            } catch (RepositoryException e) {
                log.warn("Unable to check access rights for item: " + item.getId());
            }
            if (!granted) {
                if (denied == null) {
                    denied = new HashSet();
                }
                denied.add(item.getId());
            }*/
            throw new UnsupportedOperationException();
        }
        if (denied != null) {
            accessDenied.put(events, denied);
        }
    }

    /**
     * Dispatches the events to the <code>EventListener</code>.
     *
     * @param events a collection of {@link EventState}s
     *               to dispatch.
     * @param th2 
     */
    synchronized void consumeEvents(EventStateCollection events, boolean useOriginalSession, Throwable th2, SessionImpl originalSession) throws RepositoryException {
    	
        // Set of ItemIds of denied ItemStates
        Set denied = (Set) accessDenied.remove(events);
        // check permissions
        for (Iterator it = events.iterator(); it.hasNext() && session.isLive();) {
            EventState state = (EventState) it.next();
            if (state.getType() == Event.NODE_ADDED
                    || state.getType() == Event.PROPERTY_ADDED
                    || state.getType() == Event.PROPERTY_CHANGED) {
                ItemId targetId = state.getTargetId();
                //TODO check security
                /*if (!session.getSecurityManager().isGranted(targetId, SecurityPermission.READ)) {
                    if (denied == null) {
                        denied = new HashSet();
                    }
                    denied.add(targetId);
                }*/
                //throw new UnsupportedOperationException();
            }
        }
        // only deliver if session is still live
        if (!session.isLive()) {
            return;
        }
        //session.getStateManager().evictAll();
        // check if filtered iterator has at least one event
        EventIterator it = new FilteredEventIterator(events, filter, denied, useOriginalSession, th2, originalSession);
        try {
	        if (it.hasNext()) {
	            listener.onEvent(it);
	        } else {
	            // otherwise skip this listener
	        }
        } finally {
	        ((FilteredEventIterator)it).finish();
        }
    }

    /**
     * Returns <code>true</code> if this <code>EventConsumer</code> is equal to
     * some other object, <code>false</code> otherwise.
     * <p/>
     * Two <code>EventConsumer</code>s are considered equal if they refer to the
     * same <code>Session</code> and the <code>EventListener</code>s they
     * reference are equal. Note that the <code>EventFilter</code> is ignored in
     * this check.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this <code>EventConsumer</code> is equal the
     *         other <code>EventConsumer</code>.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EventConsumer) {
            EventConsumer other = (EventConsumer) obj;
            return session.equals(other.session)
                    && listener.equals(other.listener);
        }
        return false;
    }

    /**
     * Returns the hash code for this <code>EventConsumer</code>.
     *
     * @return the hash code for this <code>EventConsumer</code>.
     */
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = session.hashCode() ^ listener.hashCode();
        }
        return hashCode;
    }
}
