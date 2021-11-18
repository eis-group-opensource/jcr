/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import javax.jcr.RepositoryException;

/**
 * Filter SQL generator API.
 */
public interface FilterSQLBuilder {

    /**
     * Generates SQL from condition and stores result in a context.
     * @param root
     * @param context
     * @return <code>true</code> if some SQL is added into context.
     * @throws RepositoryException 
     */
    public void generate(Condition root, FilterContext context) throws RepositoryException;
}

/*
 * $Log: FilterSQLBuilder.java,v $
 * Revision 1.2  2007/10/09 07:34:51  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */