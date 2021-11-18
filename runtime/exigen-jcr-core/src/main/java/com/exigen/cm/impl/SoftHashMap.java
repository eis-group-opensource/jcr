/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class SoftHashMap<K, V> extends AbstractMap<K, V> {
    /** The internal HashMap that will hold the SoftReference. */
    private final Map<K, SoftReference<V>> hash = new HashMap<K, SoftReference<V>>();

    /** The number of "hard" references to hold internally. */
    private final int HARD_SIZE;

    /** The FIFO list of hard references, order of last access. */
    private LinkedList<V> hardCache = new LinkedList<V>();

    /** Reference queue for cleared SoftReference objects. */
    private final ReferenceQueue<V> queue = new ReferenceQueue<V>();

    public SoftHashMap() {
        this(100);
    }

    public SoftHashMap(int hardSize) {
        HARD_SIZE = hardSize;
    }

    public V get(Object key) {
        V result = null;
        // We get the SoftReference represented by that key
        SoftReference<V> soft_ref = null;
        synchronized (hash) {
            soft_ref = (SoftReference<V>) hash.get(key);			
		}
        if (soft_ref != null) {
            // From the SoftReference we get the value, which can be
            // null if it was not in the map, or it was removed in
            // the processQueue() method defined below
            result = soft_ref.get();
            if (result == null) {
                // If the value has been garbage collected, remove the
                // entry from the HashMap.
            	synchronized (hash) {
            		hash.remove(key);
            	}
            } else {
                // We now add this object to the beginning of the hard
                // reference queue. One reference can occur more than
                // once, because lookups of the FIFO queue are slow, so
                // we don't want to search through it each time to remove
                // duplicates.
            	synchronized (hardCache){
	                hardCache.addFirst(result);
	                if (hardCache.size() > HARD_SIZE) {
	                    // Remove the last entry if list longer than HARD_SIZE
	                    hardCache.removeLast();
	                }
            	}
            }
        }
        return result;
    }

    /**
     * We define our own subclass of SoftReference which contains not only the
     * value but also the key to make it easier to find the entry in the HashMap
     * after it's been garbage collected.
     */
    private static class SoftValue<T> extends SoftReference<T> {
        private final Object key; // always make data member final

        /**
         * Did you know that an outer class can access private data members and
         * methods of an inner class? I didn't know that! I thought it was only
         * the inner class who could access the outer class's private
         * information. An outer class can also access private members of an
         * inner class inside its inner class.
         */
        private SoftValue(T k, Object key, ReferenceQueue<T> q) {
            super(k, q);
            this.key = key;
        }
    }

    //private boolean stateChanged = true;
    
    /**
     * Here we go through the ReferenceQueue and remove garbage collected
     * SoftValue objects from the HashMap by looking them up using the
     * SoftValue.key data member.
     */
    @SuppressWarnings("unchecked")
    private void processQueue() {
        SoftValue<V> sv;
        synchronized (hash) {
	        while ((sv = (SoftValue<V>) queue.poll()) != null) {
	            hash.remove(sv.key); // we can access private data!
	            //stateChanged = true;
	        }
        }
    }

    /**
     * Here we put the key, value pair into the HashMap using a SoftValue
     * object.
     */
    public V put(K key, V value) {
        processQueue(); // throw out garbage collected values first
        //stateChanged = true;
        SoftReference<V> result;
        synchronized (hash) {
        	result = hash.put(key, new SoftValue<V>(value, key, queue));
        }
        if (result != null){
            return result.get();
        } else {
            return null;
        }
    }

    public V remove(Object key) {
        //stateChanged = true;
        processQueue(); // throw out garbage collected values first
        synchronized (hash) {
        	SoftReference<V> result = hash.remove(key);
	        if (result != null){
	            return result.get();
	        } else {
	            return null;
	        }
        }
    }

    public void clear() {
    	synchronized(hardCache){
    		//hardCache = new LinkedList<V>();
    		hardCache.clear();
    		processQueue(); // throw out garbage collected values
    		hash.clear();
    	}
    }

    public int size() {
        processQueue(); // throw out garbage collected values first
        synchronized (hash) {

        	return hash.size();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        processQueue(); // throw out garbage collected values first
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        processQueue(); // throw out garbage collected values first
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        processQueue(); // throw out garbage collected values first
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        processQueue(); // throw out garbage collected values first
        Collection<V> result = new ArrayList<V>();
        synchronized (hash) {
	        for(Map.Entry<K, SoftReference<V>> refEntry: hash.entrySet()){
	            V tmp = refEntry.getValue().get();
	            if (tmp != null){
	                result.add(tmp);
	            }
	        }
	        return result;
        }
    }
    
    
    @SuppressWarnings("unchecked")
    public Set<Map.Entry<K,V>> entrySet() {
        processQueue(); // throw out garbage collected values first
        Set<Map.Entry<K,V>> result = new HashSet<Map.Entry<K,V>>();
        synchronized (hash) {

	        for(Map.Entry<K, SoftReference<V>> refEntry: hash.entrySet()){
	            V tmp = refEntry.getValue().get();
	            K key = refEntry.getKey();
	            if (tmp != null){
	                result.add(new Entry(key.hashCode(), key, tmp));
	            }
	        }
	        return result;
        }
    }

    static class Entry<K,V> implements Map.Entry<K,V> {
        final K key;
        V value;
        final int hash;

        Entry(int h, K k, V v) {
            value = v;
            key = k;
            hash = h;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    
        public V setValue(V newValue) {
        V oldValue = value;
            value = newValue;
            return oldValue;
        }
    
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2))) 
                    return true;
            }
            return false;
        }
    
        public int hashCode() {
            return hash;
        }
    
        public String toString() {
            return getKey() + "=" + getValue();
        }

    }   
    
    /*static final Object NULL_KEY = new Object();
    
    static <T> T unmaskNull(T key) {
        return (key == NULL_KEY ? null : key);
    }*/
}