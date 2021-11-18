/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.name;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.exigen.cm.impl.SoftHashMap;

/**
 * Provides default implementations for the methods:
 * <ul>
 * <li>{@link #getQName(String)}</li>
 * <li>{@link #getJCRName(QName)}</li>
 * </ul>
 * Subclasses may overwrite those methods with more efficient implementations
 * e.g. using caching. This class also adds optional support for
 * {@link NamespaceListener}s. To enable listener support call the constructor
 * with <code>supportListeners</code> set to <code>true</code>. The default
 * constructor will not enable listener support and all listener related
 * methods will throw an {@link UnsupportedOperationException} in that case.
 */
public abstract class AbstractNamespaceResolver implements NamespaceResolver {

    private final Set listeners;

    private SoftHashMap<String, QName> cache = new SoftHashMap<String, QName>();
    
    /**
     * @inheritDoc
     */
    public QName getQName(String rawName)
            throws IllegalNameException, UnknownPrefixException {
    	
    	try {
    		synchronized (cache) {
				QName result = cache.get(rawName);
				if (result != null){
					return result;
				}
			}
    	} catch (Throwable th){
    		
    	}

        QName result = QName._fromJCRName(rawName, this);
        
        synchronized (cache) {
			try {
				cache.put(rawName, result);
			} catch (Throwable e) {
				// TODO: handle exception
			}
		}
        return result;
    }

    /**
     * @inheritDoc
     */
    public String getJCRName(QName name) throws NoPrefixDeclaredException {
        return name.toJCRName(this);
    }

    /**
     * Creates a <code>AbstractNamespaceResolver</code> without listener
     * support.
     */
    public AbstractNamespaceResolver() {
        this(false);
    }

    /**
     * Creates a <code>AbstractNamespaceResolver</code> with listener support if
     * <code>supportListeners</code> is set to <code>true</code>.
     *
     * @param supportListeners if <code>true</code> listener are supported by
     *                         this instance.
     */
    public AbstractNamespaceResolver(boolean supportListeners) {
        if (supportListeners) {
            listeners = new HashSet();
        } else {
            listeners = null;
        }
    }

    //--------------------------------------------< NamespaceListener support >

    /**
     * Registers <code>listener</code> to get notifications when namespace
     * mappings change.
     *
     * @param listener the listener to register.
     * @throws UnsupportedOperationException if listener support is not enabled
     *                                       for this <code>AbstractNamespaceResolver</code>.
     */
    public void addListener(NamespaceListener listener) {
        if (listeners == null) {
            throw new UnsupportedOperationException("addListener");
        }
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes the <code>listener</code> from this <code>NamespaceRegistery</code>.
     *
     * @param listener the listener to remove.
     * @throws UnsupportedOperationException if listener support is not enabled
     *                                       for this <code>AbstractNamespaceResolver</code>.
     */
    public void removeListener(NamespaceListener listener) {
        if (listeners == null) {
            throw new UnsupportedOperationException("removeListener");
        }
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Notifies the listeners that a new namespace <code>uri</code> has been
     * added and mapped to <code>prefix</code>.
     *
     * @param prefix the prefix.
     * @param uri    the namespace uri.
     */
    protected void notifyNamespaceAdded(String prefix, String uri) {
        if (listeners == null) {
            throw new UnsupportedOperationException("notifyNamespaceAdded");
        }
        // addition is infrequent compared to listener registration
        // -> use copy-on-read
        NamespaceListener[] currentListeners;
        synchronized (listeners) {
            int i = 0;
            currentListeners = new NamespaceListener[listeners.size()];
            for (Iterator it = listeners.iterator(); it.hasNext(); ) {
                currentListeners[i++] = (NamespaceListener) it.next();
            }
        }
        for (int i = 0; i < currentListeners.length; i++) {
            currentListeners[i].namespaceAdded(prefix, uri);
        }
    }

    /**
     * Notifies listeners that an existing namespace uri has been remapped
     * to a new prefix.
     *
     * @param oldPrefix the old prefix.
     * @param newPrefix the new prefix.
     * @param uri the associated namespace uri.
     */
    protected void notifyNamespaceRemapped(String oldPrefix,
                                           String newPrefix,
                                           String uri) {
        if (listeners == null) {
            throw new UnsupportedOperationException("notifyNamespaceRemapped");
        }
        // remapping is infrequent compared to listener registration
        // -> use copy-on-read
        NamespaceListener[] currentListeners;
        synchronized (listeners) {
            int i = 0;
            currentListeners = new NamespaceListener[listeners.size()];
            for (Iterator it = listeners.iterator(); it.hasNext(); ) {
                currentListeners[i++] = (NamespaceListener) it.next();
            }
        }
        for (int i = 0; i < currentListeners.length; i++) {
            currentListeners[i].namespaceRemapped(oldPrefix, newPrefix, uri);
        }
    }
}
