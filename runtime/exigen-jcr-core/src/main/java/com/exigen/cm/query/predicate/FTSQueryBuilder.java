/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query.predicate;

import java.util.List;

import javax.jcr.RepositoryException;


/**
 * Used by SQL builder to build FTS based subquery.
 * @author Maksims
 */
public interface FTSQueryBuilder {
    /**
     * Creates FTS statement from JCR filter specified as parameter.
     * @param context
     * @param filterHolder
     * @param attribute
     * @param ftsFilter
     * @param negated 
     * @throws RepositoryException
     */
    public void build(FilterContext context, FilterSQL filterHolder, String attribute, List<List<Comparison>> ftsFilter, boolean negated) throws RepositoryException;
}
/*
 * $Log: FTSQueryBuilder.java,v $
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/21 12:33:17  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 * Revision 1.4  2006/04/17 06:47:03  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/05 09:47:13  maksims
 * #0144985 JCR query adjusting to Oracle Text added
 *
 * Revision 1.2  2006/03/31 16:01:00  maksims
 * #0144985 Oracle specific search implemented
 *
 * Revision 1.1  2006/03/02 15:52:22  maksims
 * #0144986 FTS query generation added
 *
 */
