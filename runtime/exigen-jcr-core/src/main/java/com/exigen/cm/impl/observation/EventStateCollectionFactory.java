/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Defines methods to create an {@link EventStateCollection}
 */
public interface EventStateCollectionFactory {

    /**
     * Creates an <code>EventStateCollection</code>.
     *
     * @return a new <code>EventStateCollection</code>
     * @throws RepositoryException if creation fails for some reason
     */
    public EventStateCollection createEventStateCollection(List<EventState> states) throws RepositoryException;
}
