/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.VersionImpl;
import com.exigen.cm.impl.state2._AbstractsStateManager;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;

/**
 * This Class implements a version history that extends a node.
 */
public class VersionHistoryImpl extends NodeImpl implements VersionHistory {

    /**
     * the default logger.
     */
    private static Log log = LogFactory.getLog(VersionHistoryImpl.class);

    /**
     * the internal version history
     */
    private InternalVersionHistory _history;

	private VersionImpl rootVersion;

    public VersionHistoryImpl(_NodeState state, _AbstractsStateManager sm) throws RepositoryException {
        super(state,sm);
    }

    public _NodeState getRootVersionState() throws RepositoryException {
    	return getInternalVersionHistory().getRootVersion().getNodeState();
    }

    
    /**
     * @see VersionHistory#getRootVersion()
     */
    public Version getRootVersion() throws RepositoryException {
        //return (Version) session.getNodeByUUID(history.getRootVersion().getId());
    	if (rootVersion == null){
    		
    		_NodeState _n = getInternalVersionHistory().getRootVersion().getNodeState();
	        VersionImpl result = (VersionImpl) _getSession().getNodeManager().buildNode(_n);
	        result.setVersion(getInternalVersionHistory().getRootVersion());
	        rootVersion = result;
    	}
    	return rootVersion;
    }

    /**
     * @see VersionHistory#getAllVersions()
     */
    public VersionIterator getAllVersions() throws RepositoryException {
        return new VersionIteratorImpl(_getSession(), getInternalVersionHistory().getRootVersion());
    }

    /**
     * @see VersionHistory#getVersion(String)
     */
    public Version getVersion(String versionName)
            throws VersionException, RepositoryException {
        try {
            QName name = QName.fromJCRName(versionName, _getSession().getNamespaceResolver());
            InternalVersion v = getInternalVersionHistory().getVersion(name);
            if (v == null) {
                throw new VersionException("No version with name '" + versionName + "' exists in this version history.");
            }
            //return (Version) session.getNodeByUUID(v.getId());
            VersionImpl result = (VersionImpl) _getSession().getNodeManager().buildNode(v.getId());
            result.setVersion(v);
            return result;
        } catch (IllegalNameException e) {
            throw new VersionException(e);
        } catch (UnknownPrefixException e) {
            throw new VersionException(e);
        }
    }

    /**
     * @see VersionHistory#getVersionByLabel(String)
     */
    public Version getVersionByLabel(String label) throws RepositoryException {
        try {
            QName qLabel = QName.fromJCRName(label, _getSession().getNamespaceResolver());
            InternalVersion v = getInternalVersionHistory().getVersionByLabel(qLabel);
            if (v == null) {
                throw new VersionException("No version with label '" + label + "' exists in this version history.");
            }
            VersionImpl result =  (VersionImpl) _getSession().getNodeManager().buildNode(v.getId());
            result.setVersion(v);
            return result;
        } catch (IllegalNameException e) {
            throw new VersionException(e);
        } catch (UnknownPrefixException e) {
            throw new VersionException(e);
        }
    }

    /**
     * @see VersionHistory#addVersionLabel(String, String, boolean)
     */
    public void addVersionLabel(String versionName, String label, boolean move)
            throws VersionException, RepositoryException {
        try {
        	_getSession().getVersionManager().setVersionLabel(this,
                    QName.fromJCRName(versionName, _getSession().getNamespaceResolver()),
                    QName.fromJCRName(label, _getSession().getNamespaceResolver()),
                    move);
        } catch (IllegalNameException e) {
            throw new VersionException(e);
        } catch (UnknownPrefixException e) {
            throw new VersionException(e);
        }
    }

    /**
     * @see VersionHistory#removeVersionLabel(String)
     */
    public void removeVersionLabel(String label) throws RepositoryException {
        try {
            Version existing = _getSession().getVersionManager().setVersionLabel(this,
                    null,
                    QName.fromJCRName(label, _getSession().getNamespaceResolver()),
                    true);
            if (existing == null) {
                throw new VersionException("No version with label '" + label + "' exists in this version history.");
            }
        } catch (IllegalNameException e) {
            throw new VersionException(e);
        } catch (UnknownPrefixException e) {
            throw new VersionException(e);
        }
    }


    /**
     * @see VersionHistory#getVersionLabels
     */
    public String[] getVersionLabels() {
        try {
            QName[] labels = getInternalVersionHistory().getVersionLabels();
            String[] ret = new String[labels.length];
            for (int i = 0; i < labels.length; i++) {
                ret[i] = labels[i].toJCRName(_getSession().getNamespaceResolver());
            }
            return ret;
        } catch (NoPrefixDeclaredException e) {
            throw new IllegalArgumentException("Unable to resolve label name: " + e.toString());
        } catch (RepositoryException e) {
            throw new IllegalArgumentException("Unable to resolve label name: " + e.toString());
        }
    }

    /**
     * @see VersionHistory#getVersionLabels(Version)
     */
    public String[] getVersionLabels(Version version)
            throws VersionException, RepositoryException {
        checkOwnVersion(version);
        try {
            QName[] labels = ((VersionImpl) version).getInternalVersion().getLabels();
            String[] ret = new String[labels.length];
            for (int i = 0; i < labels.length; i++) {
                ret[i] = labels[i].toJCRName(_getSession().getNamespaceResolver());
            }
            return ret;
        } catch (NoPrefixDeclaredException e) {
            throw new IllegalArgumentException("Unable to resolve label name: " + e.toString());
        }

    }

    /**
     * @see VersionHistory#hasVersionLabel(String)
     */
    public boolean hasVersionLabel(String label) {
        try {
            QName qLabel = QName.fromJCRName(label, _getSession().getNamespaceResolver());
            return getInternalVersionHistory().getVersionByLabel(qLabel) != null;
        } catch (IllegalNameException e) {
            throw new IllegalArgumentException("Unable to resolve label: " + e);
        } catch (UnknownPrefixException e) {
            throw new IllegalArgumentException("Unable to resolve label: " + e);
        } catch (RepositoryException e) {
            throw new IllegalArgumentException(e);
        }

    }

    /**
     * @see VersionHistory#hasVersionLabel(Version, String)
     */
    public boolean hasVersionLabel(Version version, String label)
            throws VersionException, RepositoryException {
        checkOwnVersion(version);
        try {
            QName qLabel = QName.fromJCRName(label, _getSession().getNamespaceResolver());
            return ((VersionImpl) version).getInternalVersion().hasLabel(qLabel);
        } catch (IllegalNameException e) {
            throw new VersionException(e);
        } catch (UnknownPrefixException e) {
            throw new VersionException(e);
        }

    }

    /**
     * @see VersionHistory#removeVersion(String)
     */
    public void removeVersion(String versionName)
            throws UnsupportedRepositoryOperationException, VersionException,
            RepositoryException {
        try {
        	_getSession().getVersionManager().removeVersion(this,
                    QName.fromJCRName(versionName, _getSession().getNamespaceResolver()));
            childNodesId.clear();
        } catch (IllegalNameException e) {
            throw new RepositoryException(e);
        } catch (UnknownPrefixException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see javax.jcr.Node#getUUID()
     */
    //TODO may be uncomment ??
    /*public String getUUID()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        //return history.getId();
        throw new UnsupportedOperationException();
    }*/

    /**
     * @see javax.jcr.Item#isSame(javax.jcr.Item)
     */
    public boolean isSame(Item otherItem) {
        if (otherItem instanceof VersionHistoryImpl) {
            // since all version histories live in the same workspace, we can compare the uuids
            try {
                return ((VersionHistoryImpl) otherItem).getInternalVersionHistory().getId().equals(getInternalVersionHistory().getId());
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        } else {
            return false;
        }
    }

    /**
     * Returns the UUID of the node that was versioned.
     *
     * @return
     */
    public String getVersionableUUID() throws RepositoryException {
        return getInternalVersionHistory().getVersionableUUID();
    }

    /**
     * Checks if the given version belongs to this history
     *
     * @param version
     * @throws VersionException
     * @throws RepositoryException
     */
    private void checkOwnVersion(Version version)
            throws VersionException, RepositoryException {
        if (!version.getParent().isSame(this)) {
            throw new VersionException("Specified version not contained in this history.");
        }
    }

    /**
     * Returns the internal version history
     *
     * @return
     * @throws RepositoryException 
     * @throws ValueFormatException 
     * @throws ItemNotFoundException 
     */
    public InternalVersionHistory getInternalVersionHistory() throws RepositoryException {
        if (_history == null){
            String nodeUUID =  getProperty(QName.JCR_VERSIONABLEUUID).getString(); 
            _history = ((VersionManagerImpl)_getSession().getVersionManager()).getInternalVersionHistory(nodeUUID);
        }
        //TODO remove this call
        //((InternalVersionHistoryImpl) _history).init();
         return _history;
    }

    //--------------------------------------< Overwrite "protected" methods >---


    /**
     * Always throws a {@link javax.jcr.nodetype.ConstraintViolationException} since this node
     * is protected.
     *
     * @throws javax.jcr.nodetype.ConstraintViolationException
     */
    public void update(String srcWorkspaceName) throws ConstraintViolationException {
        /*String msg = "update operation not allowed on a version history node: " + safeGetJCRPath();
        log.debug(msg);
        throw new ConstraintViolationException(msg);*/
//    	 should do nothing and return quietly
    }

    /**
     * Always throws a {@link javax.jcr.nodetype.ConstraintViolationException} since this node
     * is protected.
     *
     * @throws javax.jcr.nodetype.ConstraintViolationException
     */
    public NodeIterator merge(String srcWorkspace, boolean bestEffort)
            throws ConstraintViolationException {
        String msg = "merge operation not allowed on a version history node: " + safeGetJCRPath();
        log.debug(msg);
        throw new ConstraintViolationException(msg);
    	//return new NodeIteratorImpl(_getSession(), new ArrayList());
    }

    public void setHistory(InternalVersionHistory history) {
        this._history = history;
    }

    public InternalVersionHistory getHistory() throws RepositoryException {
        return getInternalVersionHistory();
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
	public void refreshState(boolean b) throws RepositoryException {
	}

	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		StringBuffer sb = new  StringBuffer(super.toString());
		try {
			sb.append("\r\nRoot Version"+getRootVersion().getUUID());
			sb.append("\r\nRoot Version"+getRootVersion().getUUID());
		} catch (RepositoryException exc){
			
		}
		
		return sb.toString();
	}

}
