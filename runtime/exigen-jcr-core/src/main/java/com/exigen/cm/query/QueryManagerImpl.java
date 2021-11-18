/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query;

import java.io.StringReader;
import java.text.MessageFormat;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.WorkspaceImpl;
import com.exigen.cm.query.parser.JCRXPathBuilder;
import com.exigen.cm.query.parser.JCRXPathLexer;
import com.exigen.cm.query.parser.JCRXPathParser;

/**
 * Responsible for proper query implementation instantiation.
 * @author Maksims
 */
public class QueryManagerImpl implements QueryManager {
    private final WorkspaceImpl workspace;
    
    /**
     * Constructs query manager for Session specified.
     * @param session
     */
    public QueryManagerImpl(WorkspaceImpl workspace){
        this.workspace = workspace;
    }
    

    /**
     * Creates new query instance.
     * Translates JSR-170 query into SQL query using 
     * current JCR session.
     */
    public Query createQuery(String statement, String language) throws InvalidQueryException, RepositoryException {
        
        if(Query.XPATH.equals(language))
            return new QueryImpl(statement, 
                                Query.XPATH, 
                                createQueryFromXPath(statement));
        
        String message = MessageFormat.format("Query language {0} is not supported.", 
                new Object[]{language});
        throw new InvalidQueryException(message);
    }
    
    protected XPathQuery createQueryFromXPath(String statement) throws InvalidQueryException, RepositoryException {
        try{  
            BuildingContext context = new BuildingContext((SessionImpl)workspace.getSession());
            JCRXPathLexer lex = new JCRXPathLexer(new StringReader(statement));
            JCRXPathParser parser = new JCRXPathParser(lex);
            parser.parse();
            
            XPathQuery queryTree = new XPathQuery(context);
            JCRXPathBuilder builder = new JCRXPathBuilder();
            builder.build(parser.getAST(), queryTree);
              
            return queryTree;
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to parse query {0} in language {1}.", 
                    new Object[]{statement, Query.XPATH});
            
            throw new InvalidQueryException(message, ex);
        }
    }
    
    /**
     * Method is  not implemented.
     */
    public Query getQuery(Node arg0) throws InvalidQueryException,
            RepositoryException {
        throw new UnsupportedOperationException("Query persistence is not implemented.");
    }

    /**
     * Returns supported query languages list.
     */
    public String[] getSupportedQueryLanguages() throws RepositoryException {
        return new String[]{Query.XPATH};//, Query.SQL};
    }

}
/*
 * $Log: QueryManagerImpl.java,v $
 * Revision 1.1  2007/04/26 08:59:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.10  2007/03/01 14:25:56  maksims
 * #1804008 fixed jcxpath grammar
 *
 * Revision 1.9  2006/12/15 13:13:29  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:06  maksims
 * #1801897 Query2 addition
 *
 * Revision 1.7  2006/04/17 06:47:03  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.4  2006/03/27 14:57:36  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.3  2006/03/13 12:59:34  maksims
 * #0144986 filtered properties joins filter built along with main filter
 *
 * Revision 1.2  2006/03/03 12:52:52  maksims
 * #0144986 Query.SQL replaced with Query.XPath for Query instance creation
 *
 * Revision 1.1  2006/03/01 16:12:02  maksims
 * #0144986 Initial addition
 *
 */