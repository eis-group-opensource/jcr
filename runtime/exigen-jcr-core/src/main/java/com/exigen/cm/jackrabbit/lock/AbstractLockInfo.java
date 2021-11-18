/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.lock;



import java.util.Map;

import javax.jcr.Session;

import com.exigen.cm.impl.SessionImpl;

/**
 * Common information about a lock.
 */
abstract class AbstractLockInfo {

    /**
     * Lock token
     */
    protected final LockToken lockToken;

    /**
     * Flag indicating whether lock is session scoped
     */
    protected final boolean sessionScoped;

    /**
     * Flag indicating whether lock is deep
     */
    protected final boolean deep;

    /**
     * Lock owner, determined on creation time
     */
    protected final String lockOwner;

    /**
     * Session currently holding lock
     */
    protected SessionImpl lockHolder;

    /**
     * Flag indicating whether this lock is live
     */
    protected boolean live;

	private Map<String, Object> options;

    /**
     * Create a new instance of this class.
     *
     * @param lockToken     lock token
     * @param sessionScoped whether lock token is session scoped
     * @param deep          whether lock is deep
     * @param lockOwner     owner of lock
     */
    public AbstractLockInfo(LockToken lockToken, boolean sessionScoped, boolean deep,
                    String lockOwner, Map<String, Object> options) {
        this.lockToken = lockToken;
        this.sessionScoped = sessionScoped;
        this.deep = deep;
        this.lockOwner = lockOwner;
        this.options = options;
    }

    /**
     * Set the live flag
     * @param live live flag
     */
    public void setLive(boolean live) {
        this.live = live;
    }

    /**
     * Return the UUID of the lock holding node
     * @return uuid
     */
    public Long getNodeId() {
        return lockToken.nodeId;
    }

    /**
     * Return the session currently holding the lock
     *
     * @return session currently holding the lock
     */
    public SessionImpl getLockHolder() {
        return lockHolder;
    }

    /**
     * Set the session currently holding the lock
     *
     * @param lockHolder session currently holding the lock
     */
    public void setLockHolder(SessionImpl lockHolder) {
        this.lockHolder = lockHolder;
        if (lockHolder != null){
        	setLive(true);
        } else{
        	setLive(false);
        }
    }

    /**
     * Return the lock token as seen by the session passed as parameter. If
     * this session is currently holding the lock, it will get the lock token
     * itself, otherwise a <code>null</code> string
     */
    public String getLockToken(Session session) {
    	if (session == null){
    		return null;
    	}
        if (session.equals(lockHolder)) {
        	setLive(true);
            return lockToken.toString();
        } else if (session.getUserID() != null && session.getUserID().equals(this.lockOwner)){
        	if (((SessionImpl)session)._getRepository().allowAutoLockToken()){
	            session.addLockToken(lockToken.toString());
	            return lockToken.toString();
        	}
        }
        return null;
    }

    /**
     * Return a flag indicating whether the lock is live
     *
     * @return <code>true</code> if the lock is live; otherwise <code>false</code>
     */
    public boolean isLive() {
        return live;
    }

    /**
     * Return a flag indicating whether the lock information may still change.
     */
    public boolean mayChange() {
        return live;
    }

    /**
     * Return a flag indicating whether the lock is session-scoped
     *
     * @return <code>true</code> if the lock is session-scoped;
     *         otherwise <code>false</code>
     */
    public boolean isSessionScoped() {
        return sessionScoped;
    }

	public Object getAttribute(String name) {
		return options.get(name);
	}
}
