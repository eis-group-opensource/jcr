/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.impl.iterators.NodeIteratorImpl;
import com.exigen.cm.impl.state2.IdIterator;
import com.exigen.cm.query.predicate.NoResultsQuery;

/**
 * Represents query execution results.
 * @author Maksims
 *
 */
class QueryResultImpl implements QueryResult {

    private ResultSet result;
    private final XPathQuery query;
    
    private static final Log log = LogFactory.getLog(QueryResultImpl.class);
    


    QueryResultImpl(XPathQuery query) throws Exception{
        this.query=query;
        try{
            PreparedStatement st = query.toPreparedStatement();
//            st.setFetchSize(query.getBuildingContext().getSession()._getRepository().getBatchSize());
            this.result = st.executeQuery();
        }catch(NoResultsQuery nrq){
            this.result = null;
            log.debug("Query cannot have results: " + nrq.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator getNodes() throws RepositoryException {
        return new NodeIteratorImpl(query.getBuildingContext().getSession(), 
                new IdIterator(result, query.getBuildingContext().getSession(), query.getLimitResult()));
    }
    
    /**
     * {@inheritDoc}
     */
    public String[] getColumnNames() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Query results table view is not implemented.");
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator getRows() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Query results table view is not implemented.");
    }

}
/*
 * $Log: QueryResultImpl.java,v $
 * Revision 1.1  2007/04/26 08:59:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.9  2006/12/15 13:13:29  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.4  2006/12/13 14:27:12  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.3  2006/11/20 16:15:47  maksims
 * #0149156 String conversion for columns fixed
 *
 * Revision 1.2  2006/11/14 07:38:51  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.1  2006/11/02 17:28:06  maksims
 * #1801897 Query2 addition
 *
 * Revision 1.5  2006/04/20 11:42:49  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.4  2006/04/17 06:47:03  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.6  2006/04/03 08:21:42  maksims
 * #0144985 NoResultsQuery processing fixed
 *
 * Revision 1.5  2006/03/27 14:57:36  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.4  2006/03/14 17:22:41  maksims
 * #0144986 Value casting to DB specific format dialect methods usage added
 *
 * Revision 1.3  2006/03/14 14:50:57  maksims
 * #0144986 Limit to result constraints
 *
 * Revision 1.2  2006/03/03 14:44:58  maksims
 * #0144986 position() and first() support added. last() situation made recognizable ...
 *
 * Revision 1.1  2006/03/01 16:12:02  maksims
 * #0144986 Initial addition
 *
 */