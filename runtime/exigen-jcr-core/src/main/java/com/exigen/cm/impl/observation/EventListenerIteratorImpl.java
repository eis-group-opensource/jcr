/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.Session;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;

/**
 */
class EventListenerIteratorImpl implements EventListenerIterator {

    /**
     * This iterator will return {@link EventListener}s registered by this
     * <code>Session</code>.
     */
    private final Session session;

    /**
     * Iterator over {@link EventConsumer} instances
     */
    private final Iterator consumers;

    /**
     * The next <code>EventListener</code> that belongs to the session
     * passed in the constructor of this <code>EventListenerIteratorImpl</code>.
     */
    private EventListener next;

    /**
     * Current position
     */
    private long pos = 0;

    /**
     * Creates a new <code>EventListenerIteratorImpl</code>.
     *
     * @param session
     * @param sConsumers synchronous consumers.
     * @param aConsumers asynchronous consumers.
     * @throws NullPointerException if <code>ticket</code> or <code>consumer</code>
     *                              is <code>null</code>.
     */
    EventListenerIteratorImpl(Session session, Collection sConsumers, Collection aConsumers)
            throws NullPointerException {
        if (session == null) {
            throw new NullPointerException("session");
        }
        if (sConsumers == null) {
            throw new NullPointerException("consumers");
        }
        if (aConsumers == null) {
            throw new NullPointerException("consumers");
        }
        this.session = session;
        Collection allConsumers = new ArrayList(sConsumers);
        allConsumers.addAll(aConsumers);
        this.consumers = allConsumers.iterator();
        fetchNext();
    }

    /**
     * {@inheritDoc}
     */
    public EventListener nextEventListener() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        EventListener l = next;
        fetchNext();
        pos++;
        return l;
    }

    /**
     * {@inheritDoc}
     */
    public void skip(long skipNum) {
        while (skipNum-- > 0) {
            next();
        }
    }

    /**
     * Always returns <code>-1</code>.
     *
     * @return <code>-1</code>.
     */
    public long getSize() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public long getPosition() {
        return pos;
    }

    /**
     * Remove is not supported on this Iterator.
     *
     * @throws UnsupportedOperationException
     */
    public void remove() {
        throw new UnsupportedOperationException("EventListenerIterator.remove()");
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the consumers has more elements.
     */
    public boolean hasNext() {
        return (next != null);
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        return nextEventListener();
    }

    /**
     * Fetches the next {@link javax.jcr.observation.EventListener} associated
     * with the <code>Session</code> passed in the constructor of this
     * <code>EventListenerIteratorImpl</code> from all register
     * <code>EventListener</code>s
     */
    private void fetchNext() {
        EventConsumer consumer;
        next = null;
        while (next == null && consumers.hasNext()) {
            consumer = (EventConsumer) consumers.next();
            // only return EventConsumers that belong to our session
            if (consumer.getSession().equals(session)) {
                next = consumer.getEventListener();
            }

        }
    }
}
