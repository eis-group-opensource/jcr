/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;

import java.util.Collection;

/**
 * The <code>DispatchAction</code> class is a simple struct that defines what
 * <code>EventState</code>s should be dispatched to which
 * <code>EventConsumer</code>s.
 */
class DispatchAction {

    /**
     * The collection of <code>EventState</code>s
     */
    private final EventStateCollection eventStates;

    /**
     * <code>EventStates</code> are dispatched to these
     * <code>EventConsumer</code>s.
     */
    private final Collection eventConsumers;

	private Throwable createdIn;

    /**
     * Creates a new <code>DispatchAction</code> struct with
     * <code>eventStates</code> and <code>eventConsumers</code>.
     */
    DispatchAction(EventStateCollection eventStates, Collection eventConsumers) {
        this.eventStates = eventStates;
        this.eventConsumers = eventConsumers;
        this.createdIn = new Throwable();
    }

    /**
     * Returns a collection of {@link EventState}s to dispatch.
     *
     * @return a collection of {@link EventState}s to dispatch.
     */
    EventStateCollection getEventStates() {
        return eventStates;
    }

    /**
     * Returns a <code>Collection</code> of {@link EventConsumer}s where
     * the events should be dispatched to.
     *
     * @return a <code>Collection</code> of {@link EventConsumer}s.
     */
    Collection getEventConsumers() {
        return eventConsumers;
    }

	public Throwable getCreatedIn() {
		return createdIn;
	}
}
