/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.BlockingBuffer;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.taskmanager.Task;

/**
 * The class <code>ObservationManagerFactory</code> creates new
 * <code>ObservationManager</code> instances for sessions. It also
 * creates new {@link EventStateCollection}s that can be dispatched
 * to registered {@link javax.jcr.observation.EventListener}s.
 */
public final class ObservationManagerFactory extends EventDispatcher
        implements Task {

    /**
     * Logger instance for this class
     */
    private static final Log log = LogFactory.getLog(ObservationManagerFactory.class);

    /**
     * Dummy DispatchAction indicating the notification thread to end
     */
    private static final DispatchAction DISPOSE_MARKER = new DispatchAction(null, null);

    /**
     * Currently active <code>EventConsumer</code>s for notification.
     */
    private Set activeConsumers = new HashSet();

    /**
     * Currently active synchronous <code>EventConsumer</code>s for notification.
     */
    private Set synchronousConsumers = new HashSet();
    private Set synchronousBeforeConsumers = new HashSet();

    /**
     * Set of <code>EventConsumer</code>s for read only Set access
     */
    private Set readOnlyConsumers;

    /**
     * Set of synchronous <code>EventConsumer</code>s for read only Set access.
     */
    private Set synchronousReadOnlyConsumers;
    private Set synchronousBeforeReadOnlyConsumers;

    /**
     * synchronization monitor for listener changes
     */
    private Object consumerChange = new Object();

    /**
     * Contains the pending events that will be delivered to event listeners
     */
    private BlockingBuffer eventQueue
            = (BlockingBuffer) BufferUtils.blockingBuffer(new UnboundedFifoBuffer());

    /**
     * The background notification thread
     */
    //private Thread notificationThread;

	private RepositoryImpl repository;

    /**
     * Creates a new <code>ObservationManagerFactory</code> instance
     * and starts the notification thread deamon.
     */
    public ObservationManagerFactory(RepositoryImpl repository) throws RepositoryException{
    	this.repository = repository;
        /*notificationThread = new Thread(this, "ObservationManager");
        notificationThread.setDaemon(true);
        notificationThread.start();*/
    	repository.getTaskManager().schedule(this, RepositoryObservationManagerFactory.SCHEDULE_PERIOD);
    }

    /**
     * Disposes this <code>ObservationManager</code>. This will
     * effectively stop the background notification thread.
     */
    public void dispose() {
        // dispatch dummy event to mark end of notification
        eventQueue.add(DISPOSE_MARKER);
        /*try {
            notificationThread.join();
        } catch (InterruptedException e) {
            // FIXME log exception ?
        }*/
        log.info("Notification of EventListeners stopped.");
    }

    /**
     * Returns an unmodifieable <code>Set</code> of <code>EventConsumer</code>s.
     *
     * @return <code>Set</code> of <code>EventConsumer</code>s.
     */
    Set getAsynchronousConsumers() {
        synchronized (consumerChange) {
            if (readOnlyConsumers == null) {
                readOnlyConsumers = Collections.unmodifiableSet(new HashSet(activeConsumers));
            }
            return readOnlyConsumers;
        }
    }

    Set getSynchronousConsumers() {
        synchronized (consumerChange) {
            if (synchronousReadOnlyConsumers == null) {
                synchronousReadOnlyConsumers = Collections.unmodifiableSet(new HashSet(synchronousConsumers));
            }
            return synchronousReadOnlyConsumers;
        }
    }

    Set getBeforeSynchronousConsumers() {
        synchronized (consumerChange) {
            if (synchronousBeforeReadOnlyConsumers == null) {
                synchronousBeforeReadOnlyConsumers = Collections.unmodifiableSet(new HashSet(synchronousBeforeConsumers));
            }
            return synchronousBeforeReadOnlyConsumers;
        }
    }

    /**
     * Creates a new <code>session</code> local <code>ObservationManager</code>
     * with an associated <code>NamespaceResolver</code>.
     *
     * @param session the session.
     * @param itemMgr the <code>ItemManager</code> of the <code>session</code>.
     * @return an <code>ObservationManager</code>.
     */
    public ObservationManagerImpl createObservationManager(SessionImpl session) {//,ItemManager itemMgr
        return new ObservationManagerImpl(this, session); //, itemMgr
    }

    /**
     * Implements the run method of the background notification
     * thread.
     */
    public void run() {
        DispatchAction action;
        long start = System.currentTimeMillis();
        //pppntln("Start Obsevation");
        try {
	        while ((action = (DispatchAction) eventQueue.remove(RepositoryObservationManagerFactory.MAX_WAIT_TIME - (System.currentTimeMillis() - start))) != DISPOSE_MARKER 
	        		) {
	        	if (log.isDebugEnabled()){
		            log.debug("got EventStateCollection "+action.getEventStates().getEvents());
		            log.debug("event delivery to " + action.getEventConsumers().size() + " consumers started...");
	        	}
	            for (Iterator it = action.getEventConsumers().iterator(); it.hasNext();) {
	                EventConsumer c = (EventConsumer) it.next();
	                try {
	                    c.consumeEvents(action.getEventStates(), false, action.getCreatedIn(), null);
	                } catch (Throwable t) {
	                    log.warn("EventConsumer threw exception: " + t.toString());
	                    log.debug("Stacktrace: ", t);
	                    // move on to the next consumer
	                }
	            }
	            log.debug("event delivery finished.");

	            if ((System.currentTimeMillis() - start) > RepositoryObservationManagerFactory.MAX_WAIT_TIME){
	            	break;
	            }

	        }
	        //pppntln("Simple exit");
        } catch (org.apache.commons.collections.BufferUnderflowException exc){
        	//timeout, do nothing
        	//pppntln("Timeout");
        }
        //pppntln("Stop Obsevation");

    }

    /**
     * {@inheritDoc}
     * <p/>
     * Gives this observation manager the oportunity to
     * prepare the events for dispatching.
     */
    void prepareEvents(EventStateCollection events) {
        Set consumers = new HashSet();
        consumers.addAll(getSynchronousConsumers());
        consumers.addAll(getAsynchronousConsumers());
        for (Iterator it = consumers.iterator(); it.hasNext();) {
            EventConsumer c = (EventConsumer) it.next();
            c.prepareEvents(events);
        }
    }

    /**
     * {@inheritDoc}
     */
    void prepareDeleted(EventStateCollection events, ChangeLog changes) {
        Set consumers = new HashSet();
        consumers.addAll(getSynchronousConsumers());
        consumers.addAll(getAsynchronousConsumers());
        for (Iterator it = consumers.iterator(); it.hasNext();) {
            EventConsumer c = (EventConsumer) it.next();
            c.prepareDeleted(events, changes.deletedStates());
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Dispatches the {@link EventStateCollection events} to all
     * registered {@link javax.jcr.observation.EventListener}s.
     */
    void dispatchEvents(EventStateCollection events,SessionImpl originalSession) throws RepositoryException{
    	if (repository.getObservationManagerFactory() != null){
    		repository.getObservationManagerFactory().dispatchEvents(events, originalSession);
    	}
        // notify synchronous listeners
        Set synchronous = getSynchronousConsumers();
        if (log.isDebugEnabled()) {
            log.debug("notifying " + synchronous.size() + " synchronous listeners.");
        }
        for (Iterator it = synchronous.iterator(); it.hasNext();) {
            EventConsumer c = (EventConsumer) it.next();
            try {
                c.consumeEvents(events, true, new Throwable(), originalSession);
            } catch (Throwable t) {
                log.error("Synchronous EventConsumer threw exception.", t);
                // move on to next consumer
            }
        }
        eventQueue.add(new DispatchAction(events, getAsynchronousConsumers()));
    }

    void dispatchBeforeEvents(EventStateCollection events, SessionImpl originalSession) throws RepositoryException {
    	if (repository.getObservationManagerFactory() != null){
    		repository.getObservationManagerFactory().dispatchBeforeEvents(events,originalSession);
    	}
        // notify synchronous listeners
        Set synchronous = getBeforeSynchronousConsumers();
        if (log.isDebugEnabled()) {
            log.debug("notifying " + synchronous.size() + " before synchronous listeners.");
        }
        for (Iterator it = synchronous.iterator(); it.hasNext();) {
            EventConsumer c = (EventConsumer) it.next();
            c.consumeEvents(events, true, new Throwable(), originalSession);
        }
    }

    /**
     * Adds or replaces an event consumer.
     * @param consumer the <code>EventConsumer</code> to add or replace.
     */
    void addConsumer(EventConsumer consumer) {
        synchronized (consumerChange) {
        	if (consumer.getEventListener() instanceof BeforeSaveEventListener){
                // remove existing if any
                synchronousBeforeConsumers.remove(consumer);
                // re-add it
                synchronousBeforeConsumers.add(consumer);
                // reset read only consumer set
                synchronousBeforeReadOnlyConsumers = null;        		
        	} else if (consumer.getEventListener() instanceof AfterSaveEventListener) {
                // remove existing if any
                synchronousConsumers.remove(consumer);
                // re-add it
                synchronousConsumers.add(consumer);
                // reset read only consumer set
                synchronousReadOnlyConsumers = null;
            } else {
                // remove existing if any
                activeConsumers.remove(consumer);
                // re-add it
                activeConsumers.add(consumer);
                // reset read only consumer set
                readOnlyConsumers = null;
            }
        }
    }

    /**
     * Unregisters an event consumer from event notification.
     * @param consumer the consumer to deregister.
     */
    void removeConsumer(EventConsumer consumer) {
        synchronized (consumerChange) {
            if (consumer.getEventListener() instanceof BeforeSaveEventListener) {
                synchronousBeforeConsumers.remove(consumer);
                // reset read only listener set
                synchronousBeforeReadOnlyConsumers = null;            	
            } else if (consumer.getEventListener() instanceof AfterSaveEventListener) {
                synchronousConsumers.remove(consumer);
                // reset read only listener set
                synchronousReadOnlyConsumers = null;
            } else {
                activeConsumers.remove(consumer);
                // reset read only listener set
                readOnlyConsumers = null;
            }
        }
    }

	public RepositoryImpl getRepository() {
		return repository;
	}

	public void release() {
		eventQueue.add(DISPOSE_MARKER);
		
	}

}
