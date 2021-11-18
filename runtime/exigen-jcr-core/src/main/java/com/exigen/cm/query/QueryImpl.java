/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query;

import java.text.MessageFormat;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;


/**
 * Provides querying functionality.
 * @author Maksims
 */
public class QueryImpl implements Query {

    private static final Log performanceLogger = LogFactory.getLog(Constants.LOG_CATEGORY_PERFORMANCE);
    
    private final String statement;
    private final String language;
    private final XPathQuery query;
    
    
    /**
     * Constructs new query instance for specified statement in specified language.
     * @param statement is a statement to be executed.
     * @param language is a statement's language.
     */
    QueryImpl(  String statement, 
                String language, 
                XPathQuery query){
        this.statement = statement;
        this.language = language;
        this.query = query;
    }
    
    public QueryResult execute() throws RepositoryException {
        try{
            long d = System.currentTimeMillis();
            QueryResult res = new QueryResultImpl(query);
            d = System.currentTimeMillis()-d;
            
            if(performanceLogger.isDebugEnabled()){
                String message = MessageFormat.format("{0} {1} {2}",
                        Constants.LOG_CATEGORY_PERFORMANCE__FIND
                        , d
                        , statement);
                performanceLogger.debug(message);
            }
                  
            return res;
        }catch(IllegalArgumentException ex){
            throw ex;
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to execute query: ", 
                    new Object[]{statement});
            throw new RepositoryException(message, ex);
        }
    }

    /**
     * Returns original query statement.
     * @return returns original query statement.
     */
    public String getStatement() {
        return statement;
    }

    /**
     * Returns query language.
     * @return returns query language.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * This method is not imlemented.
     */
    public String getStoredQueryPath() throws ItemNotFoundException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException("Query persistence is not implemented.");
    }

    /**
     * This method is not imlemented.
     */    
    public Node storeAsNode(String arg0) throws ItemExistsException,
            PathNotFoundException, VersionException,
            ConstraintViolationException, LockException,
            UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException("Query persistence is not implemented.");
    }
    
    /**
     * Returns number of rows in a result set. 
     * In case of 0 all result records should be returned;
     * @return
     */
    public int getLimitResult(){
        return query.getLimitResult();
    }

    /**
     * Sets number of rows in a result set. 
     * In case of 0 all result records should be returned;
     * @param limitResult is a number of result items to be returned.
     */    
    public void setLimitResult(int limitResult){
        query.setLimitResult(limitResult);
    }

    public void setAllowBrowse(boolean value) {
        query.getBuildingContext().setAllowBrowse(value);
        
    }

    /**
     * Sets first row shift in a result set. 
     * @param resultsShift
     */    
    public void setResultsShift(int resultsShift){
        query.setResultsShift(resultsShift);
    }
    /**
     * Returns first row shift in a result set. 
     * @param resultsShift
     */
    public int getResultsShift(){
        return query.getResultsShift();
    }

}
/*
 * $Log: QueryImpl.java,v $
 * Revision 1.4  2009/01/30 07:09:08  maksims
 * *** empty log message ***
 *
 * Revision 1.3  2008/10/09 13:27:28  maksims
 * #0153705 IllegalArgumentException will be reported if query against Name type property is executed
 *
 * Revision 1.2  2008/07/03 08:15:37  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/12/15 13:13:29  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.3  2006/12/05 15:52:22  maksims
 * #1803540 Added ability to search by uuid
 *
 * Revision 1.2  2006/11/22 16:35:39  maksims
 * #1802721 Log category performance added
 *
 * Revision 1.1  2006/11/02 17:28:06  maksims
 * #1801897 Query2 addition
 *
 * Revision 1.4  2006/04/17 06:47:03  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/03 13:08:19  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.2  2006/03/14 14:50:57  maksims
 * #0144986 Limit to result constraints
 *
 * Revision 1.1  2006/03/01 16:12:02  maksims
 * #0144986 Initial addition
 *
 */