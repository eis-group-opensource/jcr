/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.common;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Batch of operations over Content Store specified by Store Name.
 * @author Maksims
 *
 */
public class Batch {
    private final String storeName;
    private final List<BatchUnit> units;
    
    public Batch(String storeName){
        this(storeName,new LinkedList<BatchUnit>());
    }

    public Batch(String storeName, List<BatchUnit> units){
        this.storeName = storeName;
        this.units = units;
    }
    
    
    public String getStoreName(){
        return storeName;
    }
    
    public Iterator<BatchUnit> getUnitsIterator(){
        return units.iterator();
    }

    public int getSize() {
        return units == null ? 0 : units.size();
    }
}

/*
 * $Log: Batch.java,v $
 * Revision 1.1  2007/04/26 09:01:01  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/07/04 14:04:39  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */