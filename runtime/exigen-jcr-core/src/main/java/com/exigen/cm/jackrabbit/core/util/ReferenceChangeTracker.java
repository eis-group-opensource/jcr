/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.value.ReferenceValue;

/**
 * Simple helper class that can be used to keep track of uuid mappings
 * (e.g. if the uuid of an imported or copied node is mapped to a new uuid)
 * and processed (e.g. imported or copied) reference properties that might
 * need correcting depending on the uuid mappings.
 */
public class ReferenceChangeTracker {
    /**
     * mapping <original uuid> to <new uuid> of mix:referenceable nodes
     */
    private final HashMap uuidMap = new HashMap();
    /**
     * list of processed reference properties that might need correcting
     */
    private final ArrayList references = new ArrayList();

    /**
     * Creates a new instance.
     */
    public ReferenceChangeTracker() {
    }

    /**
     * Resets all internal state.
     */
    public void clear() {
        uuidMap.clear();
        references.clear();
        refs.clear();
    }

    /**
     * Store the given uuid mapping for later lookup using
     * <code>{@link #getMappedUUID(String)}</code>.
     *
     * @param oldUUID old uuid
     * @param newUUID new uuid
     */
    public void mappedUUID(String oldUUID, String newUUID) {
        uuidMap.put(oldUUID, newUUID);
    }

    /**
     * Store the given reference property for later retrieval using
     * <code>{@link #getProcessedReferences()}</code>.
     *
     * @param refProp reference property
     */
    public void processedReference(Object refProp) {
        references.add(refProp);
    }

    /**
     * Returns the new UUID to which <code>oldUUID</code> has been mapped
     * or <code>null</code> if no such mapping exists.
     *
     * @param oldUUID old uuid
     * @return mapped new uuid or <code>null</code> if no such mapping exists
     * @see #mappedUUID(String, String)
     */
    public String getMappedUUID(String oldUUID) {
        return (String) uuidMap.get(oldUUID);
    }

    /**
     * Returns an iterator over all processed reference properties.
     *
     * @return an iterator over all processed reference properties
     * @see #processedReference(Object)
     */
    public Iterator getProcessedReferences() {
        return references.iterator();
    }

    private ArrayList<ReferenceInfo> refs = new ArrayList<ReferenceInfo>();
    
	public void addReference(NodeImpl node, QName propName, Value[] va, int type) {
		refs.add(new ReferenceInfo(node, propName, va, type));
	}

	public void processRefs() throws VersionException, LockException, RepositoryException {
		for(ReferenceInfo info:refs){
			info.process(this);
		}
		
	}
}

class ReferenceInfo {

	private NodeImpl node;
	private QName propName;
	private Value[] va;
	private int type;

	public ReferenceInfo(NodeImpl node, QName propName, Value[] va, int type) {
		this.node = node;
		this.propName = propName;
		this.va = va;
		this.type = type;
	}
	
	public void process(ReferenceChangeTracker tracker) throws VersionException, LockException, RepositoryException{
		 Value[] values = va;
         Value[] newVals = new Value[values.length];
         for (int i = 0; i < values.length; i++) {
             Value val = values[i];
             String original = val.getString();
             String adjusted = tracker.getMappedUUID(original);
             if (adjusted != null) {
                 newVals[i] = new ReferenceValue(adjusted);
             } else {
                 // reference doesn't need adjusting, just copy old value
                 newVals[i] = val;
             }
         }		
		va = newVals;
		
		if (va.length == 1) {
            // could be single- or multi-valued (n == 1)
            try {
                // try setting single-value
                node.setProperty(propName, va[0]);
            } catch (ValueFormatException vfe) {
                // try setting value array
                node.setProperty(propName, va, type);
            } catch (ConstraintViolationException cve) {
                // try setting value array
                node.setProperty(propName, va, type);
            }
        } else {
            // can only be multi-valued (n == 0 || n > 1)
            node.setProperty(propName, va, type);
        }
	}
	
}
