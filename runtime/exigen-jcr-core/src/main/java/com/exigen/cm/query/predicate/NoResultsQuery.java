/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

/**
 * Exception thrown in case it is known that query won't have results and may not be executed.
 */
public class NoResultsQuery extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public NoResultsQuery(String reason){
        super(reason);
    }
}

/*
 * $Log: NoResultsQuery.java,v $
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.2  2006/12/13 14:27:13  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */