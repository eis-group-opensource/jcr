/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.impl.SessionImpl;

/**
 */
class FilteredEventIterator implements EventIterator,FinishEventIterator {

    /**
     * Logger instance for this class
     */
    private static final Log log = LogFactory.getLog(FilteredEventIterator.class);

    /**
     * The actual {@link EventState}s fired by the workspace (unfiltered)
     */
    private final Iterator actualEvents;

    /**
     * For filtering the {@link javax.jcr.observation.Event}s.
     */
    private final _EventFilter filter;

    /**
     * Set of <code>ItemId</code>s of denied <code>ItemState</code>s.
     */
    private final Set denied;

    /**
     * The next {@link javax.jcr.observation.Event} in this iterator
     */
    private Event next;

    private SessionImpl activeSession;
    
    /**
     * Current position
     */
    private long pos = 0;

	private boolean useOriginalSession;

	//private Throwable createdIn;

	//private Throwable createdIn2;

    /**
     * Creates a new <code>FilteredEventIterator</code>.
     *
     * @param c      an unmodifiable Collection of {@link javax.jcr.observation.Event}s.
     * @param filter only event that pass the filter will be dispatched to the
     *               event listener.
     * @param denied <code>Set</code> of <code>ItemId</code>s of denied <code>ItemState</code>s
     *               rejected by the <code>AccessManager</code>. If
     *               <code>null</code> no <code>ItemState</code> is denied.
     */
    public FilteredEventIterator(EventStateCollection c,
                                 _EventFilter filter,
                                 Set denied,boolean useOriginalSession, Throwable th, SessionImpl originalSession) throws RepositoryException{
    	//this.createdIn = new Throwable();
    	//this.createdIn2 = th;
        actualEvents = c.iterator();
        this.filter = filter;
        this.denied = denied;
        this.useOriginalSession = useOriginalSession;
        if (useOriginalSession){
        	activeSession = originalSession;
        }
        fetchNext();
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        Event e = next;
        try {
			fetchNext();
		} catch (RepositoryException e1) {
			throw new RuntimeException(e1);
		}
        pos++;
        return e;
    }

    /**
     * {@inheritDoc}
     */
    public Event nextEvent() {
        return (Event) next();
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
     * This method is not supported.
     * Always throws a <code>UnsupportedOperationException</code>.
     */
    public void remove() {
        throw new UnsupportedOperationException("EventIterator.remove()");
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    public boolean hasNext() {
        return (next != null);
    }

    /**
     * Fetches the next Event from the collection of events
     * passed in the constructor of <code>FilteredEventIterator</code>
     * that is allowed by the {@link EventFilter}.
     * @throws RepositoryException 
     * @throws AccessDeniedException 
     * @throws UnsupportedRepositoryOperationException 
     * @throws NamespaceException 
     */
    private void fetchNext() throws RepositoryException {
        EventState state;
        next = null;
        while (next == null && actualEvents.hasNext()) {
            state = (EventState) actualEvents.next();
            if (activeSession == null){
            	if (useOriginalSession){
            		//activeSession = state._getSession();
            		if (activeSession == null){
            			throw new UnsupportedRepositoryOperationException("Active session is null");
            		}
            	} else {
            		if (filter instanceof EventFilter){
            			EventFilter _filter = (EventFilter) filter;
            			activeSession = (SessionImpl) state.getRepository().login(_filter.getJCRPrincipals(), _filter.getWorkspace());
            		} else {
            			activeSession = state.getRepository().createTrustedSession(state.getWorkspaceName(), "DSUSER", null);
            		}
            	}
            }
            // check denied set
            if (denied == null || !denied.contains(state.getTargetId())) {
                try {
                    next = filter.blocks(state) ? null : new EventImpl(filter.getRepository(), activeSession, state);
                } catch (RepositoryException e) {
                    log.error("Exception while applying filter.", e);
                }
            }
        }
    }

	public void finish() {
		if (!useOriginalSession){
			if (activeSession != null){
				activeSession.logout();
				activeSession = null;
			}
		}
		
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (!useOriginalSession && activeSession != null){
			System.err.println("activeSession not null !!! for "+filter);
			/*createdIn.printStackTrace();
			if (createdIn2 != null){
				createdIn2.printStackTrace();
			}*/
		}
		finish();
		super.finalize();
	}
}
