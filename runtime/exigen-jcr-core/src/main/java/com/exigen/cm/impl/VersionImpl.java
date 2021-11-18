/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.impl.state2._AbstractsStateManager;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.jackrabbit.version.InternalFrozenNode;
import com.exigen.cm.jackrabbit.version.InternalVersion;
import com.exigen.cm.jackrabbit.version.InternalVersionHistory;
import com.exigen.cm.jackrabbit.version.VersionHistoryImpl;

/**
 * This Class implements a Version that extends the node interface
 */
public class VersionImpl extends NodeImpl implements Version {

    /**
     * the default logger.
     */
    private static Log log = LogFactory.getLog(VersionImpl.class);

    /**
     * the internal version
     */
    protected InternalVersion _version;

    public VersionImpl( _NodeState state, _AbstractsStateManager sm) throws RepositoryException {
        super( state, sm);
    }

    /**
     * {@inheritDoc}
     */
    public Calendar getCreated() throws RepositoryException {
        return getInternalVersion().getCreated();
    }

    /**
     * {@inheritDoc}
     */
    public Version[] getSuccessors() throws RepositoryException {
        // need to wrap it around proper node
        InternalVersion[] suc = getInternalVersion().getSuccessors();
        Version[] ret = new Version[suc.length];
        for (int i = 0; i < suc.length; i++) {
            ret[i] = (Version) _getSession().getNodeManager().buildNode(suc[i].getId());
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public Version[] getPredecessors() throws RepositoryException {
        // need to wrap it around proper node
        InternalVersion[] pred = getInternalVersion().getPredecessors();
        Version[] ret = new Version[pred.length];
        for (int i = 0; i < pred.length; i++) {
            ret[i] = (Version) _getSession().getNodeManager().buildNode(pred[i].getId());
        }
        return ret;
        
    }

    /**
     * {@inheritDoc}
     */
    //TODO may be uncomment ???
    /*public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        //return version.getId();
        throw new UnsupportedOperationException();

    }*/

    /**
     * {@inheritDoc}
     */
    public VersionHistory getContainingHistory() throws RepositoryException {
        return (VersionHistory) getParent();
    }

    /**
     * Returns the internal version
     *
     * @return
     */
    public InternalVersion getInternalVersion() throws RepositoryException{
        if (_version == null){
            InternalVersionHistory vh = ((VersionHistoryImpl)getParent()).getInternalVersionHistory();
            this._version = vh.getVersion(getUUID());
            
        }
        return _version;
    }

    /**
     * Returns the frozen node of this version
     *
     * @return
     * @throws RepositoryException
     */
    public InternalFrozenNode getFrozenNode() throws RepositoryException {
        return getInternalVersion().getFrozenNode();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSame(Item otherItem) {
        if (otherItem instanceof VersionImpl) {
            // since all versions live in the same workspace, we can compare the uuids
            try {
                return ((VersionImpl) otherItem).getInternalVersion().getId().equals(getInternalVersion().getId());
            } catch (RepositoryException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        } else {
            return false;
        }
    }

    //--------------------------------------< Overwrite "protected" methods >---


    /**
     * Always throws a {@link javax.jcr.nodetype.ConstraintViolationException} since this node
     * is protected.
     *
     * @throws javax.jcr.nodetype.ConstraintViolationException
     */
    public void update(String srcWorkspaceName) throws ConstraintViolationException {
        /*String msg = "update operation not allowed on a version node: " + safeGetJCRPath();
        log.debug(msg);
        throw new ConstraintViolationException(msg);*/
    	//do nothing
    }

    /**
     * Always throws a {@link javax.jcr.nodetype.ConstraintViolationException} since this node
     * is protected.
     *
     * @throws javax.jcr.nodetype.ConstraintViolationException
     */
    public NodeIterator merge(String srcWorkspace, boolean bestEffort)
            throws ConstraintViolationException {
        String msg = "merge operation not allowed on a version node: " + safeGetJCRPath();
        log.debug(msg);
        throw new ConstraintViolationException(msg);
    }

    public void setVersion(InternalVersion version) {
        this._version = version;
    }

    
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
    	throw new UnsupportedRepositoryOperationException();
    }

	@Override
	public void doneMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
		throw new UnsupportedRepositoryOperationException();
	}

	@Override
	public void cancelMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
		throw new UnsupportedRepositoryOperationException();
	}

	@Override
	public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
		throw new UnsupportedRepositoryOperationException();
	}

	@Override
	public void orderBefore(String srcChildRelPath, String destChildRelPath) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
		throw new UnsupportedRepositoryOperationException();
	}

	@Override
	public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
		throw new UnsupportedRepositoryOperationException();
	}

	@Override
	public String toString() {
		try {
			return "Version : "+getUUID();
		} catch (UnsupportedRepositoryOperationException e) {
			return super.toString();
		} catch (RepositoryException e) {
			return super.toString();		}
	}
}
