/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

/**
 * The <code>SessionListener</code> interface allows an implementing
 * object to be informed about changes on a <code>Session</code>.
 *
 * @see SessionImpl#addListener
 */
public interface SessionListener {

    /**
     * Called when a <code>Session</code> is about to be 'closed' by
     * calling <code>{@link javax.jcr.Session#logout()}</code. At this
     * moment the session is still valid.
     *
     * @param session the <code>Session</code> that is about to be 'closed'
     */
    void loggingOut(SessionImpl session);

    /**
     * Called when a <code>Session</code> has been 'closed' by
     * calling <code>{@link javax.jcr.Session#logout()}</code.
     *
     * @param session the <code>Session</code> that has been 'closed'
     */
    void loggedOut(SessionImpl session);
}
