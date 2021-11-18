/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import javax.jcr.RepositoryException;

import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.xml.NodeTypeReader;

/**
 * <code>NodeTypeDefStore</code> ...
 */
public class NodeTypeDefStore {

    /** Map of node type names to node type definitions. */
    private final HashMap<QName, NodeTypeDef> ntDefs;

    /**
     * Empty default constructor.
     */
    public NodeTypeDefStore() throws RepositoryException {
        ntDefs = new HashMap();
    }

    /**
     * @param in
     * @throws IOException
     * @throws InvalidNodeTypeDefException 
     * @throws InvalidNodeTypeDefException
     */
    public void load(InputStream in)
            throws IOException, 
            RepositoryException, InvalidNodeTypeDefException {
        NodeTypeDef[] types = NodeTypeReader.read(in, null);
        for (int i = 0; i < types.length; i++) {
            add(types[i]);
        }
    }

    /**
     * @param out
     * @param registry
     * @throws IOException
     * @throws RepositoryException
     */
/*    public void store(OutputStream out, NamespaceRegistry registry)
            throws IOException, RepositoryException {
        NodeTypeDef[] types = (NodeTypeDef[])
            ntDefs.values().toArray(new NodeTypeDef[ntDefs.size()]);
        NodeTypeWriter.write(out, types, registry);
    }*/

    /**
     * @param ntd
     */
    public void add(NodeTypeDef ntd) {
        ntDefs.put(ntd.getName(), ntd);
    }

    /**
     * @param name
     * @return
     */
    public boolean remove(QName name) {
        return (ntDefs.remove(name) != null);
    }

    /**
     *
     */
    public void removeAll() {
        ntDefs.clear();
    }

    /**
     * @param name
     * @return
     */
    public boolean contains(QName name) {
        return ntDefs.containsKey(name);
    }

    /**
     * @param name
     * @return
     */
    public NodeTypeDef get(QName name) {
        return (NodeTypeDef) ntDefs.get(name);
    }

    /**
     * @return
     */
    public Collection all() {
        return Collections.unmodifiableCollection(ntDefs.values());
    }

	public void load(NodeTypeDefStore ntStore) throws RepositoryException {
		load(ntStore.all());
		
	}

	public void load(Collection<NodeTypeDef> list) {
		for(NodeTypeDef d:list){
			add((NodeTypeDef)d.clone());
		}
	}
}
