/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;

import javax.jcr.RepositoryException;

import com.exigen.cm.impl.SessionImpl;

//import org.apache.jackrabbit.core.state.ChangeLog;

/**
 * Defines an object that prepares and dispatches events. Made into an abstract
 * class rather than an interface in order not to exhibit internal methods
 * that should not be visible to everybody.
 */
abstract class EventDispatcher {

    /**
     * Gives this dispatcher the oportunity to prepare the events for
     * dispatching.
     *
     * @param events the {@link EventState}s to prepare.
     */
    abstract void prepareEvents(EventStateCollection events);

    /**
     * Prepares changes that involve deleted item states.
     *
     * @param events the event state collection.
     * @param changes the changes.
     */
    abstract void prepareDeleted(EventStateCollection events, ChangeLog changes);

    /**
     * Dispatches the {@link EventStateCollection events}.
     *
     * @param events the {@link EventState}s to dispatch.
     */
    abstract void dispatchEvents(EventStateCollection events, SessionImpl originalSession) throws RepositoryException;

    abstract void dispatchBeforeEvents(EventStateCollection events, SessionImpl originalSession) throws RepositoryException;
}
