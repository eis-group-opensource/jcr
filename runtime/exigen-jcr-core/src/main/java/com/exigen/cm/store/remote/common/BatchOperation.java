/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.common;


/**
 * Enumeration of defined Batch Operations.
 * @author Maksims
 *
 */
public enum BatchOperation {
    INSERT, // Defines Insert Batch operation
    REMOVE; // Defines Remove Batch operation

    /*
     * Returns Enumeration Constant by its ordinal in this class.
     * Replaceable by .valueOf(enum element name)
     */
//    public static BatchOperation byOrdinal(int ordinal){
//        if(ordinal == INSERT.ordinal())
//            return INSERT;
//        
//        if(ordinal == REMOVE.ordinal())
//            return REMOVE;
//
//        return null;
//    }
}

/*
 * $Log: BatchOperation.java,v $
 * Revision 1.1  2007/04/26 09:01:01  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/07/04 14:04:39  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */