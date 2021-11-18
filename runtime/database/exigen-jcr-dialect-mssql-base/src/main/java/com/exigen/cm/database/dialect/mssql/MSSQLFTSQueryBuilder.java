/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.mssql;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.predicate.AbstractFTSQueryBuilder;
import com.exigen.cm.query.predicate.Comparison;
import com.exigen.cm.query.predicate.FilterContext;
import com.exigen.cm.query.predicate.FilterSQL;

/**
 * Implements FTS filter building for MSSQL.
 * 
 * 
    IMPORTANT: FTS statement cannot start from NOT(~) or contain single NOT expression!!!
 */
public class MSSQLFTSQueryBuilder extends AbstractFTSQueryBuilder {

    public void build(FilterContext context, FilterSQL filterHolder,
                      String attributeName, List<List<Comparison>> ftsFilter, boolean negated)
                        throws RepositoryException {
        
        
        BuildingContext.DBQName attribute = (attributeName == null || attributeName.length() == 1 && attributeName.charAt(0)=='.') ?
                context.getOwner().getBuildingContext().toDBQname(null) 
              : context.getOwner().getBuildingContext().toDBQname(attributeName);

        
        String mainAlias = context.nextAlias().toString();
        filterHolder.setMainTable(context.getRealTableName(Constants.TABLE_FTS_DATA));
        filterHolder.setMainAlias(mainAlias);
        filterHolder.setJoiningColumn(Constants.FIELD_TYPE_ID);
        
        
        if(!attribute.isWildcard()){
            filterHolder.getWhere()
            .append(QueryUtils.asPrefix(mainAlias)).append(context.getRealColumnName(Constants.FIELD_NAME)).append("=? AND ")
            .append(QueryUtils.asPrefix(mainAlias)).append(context.getRealColumnName(Constants.FIELD_NAMESPACE));

            
            filterHolder.addParameter(attribute.getLocalName());
            
            if(attribute.hasNamespace()){
                filterHolder.getWhere().append("=?");
                filterHolder.addParameter(attribute.getNamespaceId());
            }else{
                filterHolder.getWhere().append(" IS NULL ");                
            }
            
            filterHolder.getWhere().append(" AND ");
        }            

        
        StringBuilder statement = new StringBuilder();
        for(int i=0; i<ftsFilter.size(); i++){
            if(i>0)
                statement.append(" OR ");
            
            
            List<Comparison> ands = ftsFilter.get(i);
            List<String> words = new LinkedList<String>();
            List<String> notWords = new LinkedList<String>();            
            
            split(ands, words, notWords, null, null);
            if(negated){// switch words and not words if negated
                List<String> tmp = words;
                words=notWords;
                notWords=tmp;
            }
            
            filterStopWords(context, notWords);
            filterStopWords(context, words);
            
            if(i==0 && words.size() == 0)
                throw new IllegalArgumentException("FTS statement cannot starts with negation.");
            
            StringBuilder wordsFilter = buildFTSAndWordsFilter(words);
            statement.append(wordsFilter);
            
            StringBuilder notWordsFilter = buildFTSNotWordsFilter(notWords);
            
            if(i==0 && notWordsFilter.length() != 0)
                statement.append(" AND ");
            
            statement.append(notWordsFilter);            
        }
        
        filterHolder.getWhere()
        .append(" CONTAINS(")
        .append(QueryUtils.asPrefix(mainAlias)).append(context.getRealColumnName(Constants.TABLE_FTS_DATA__TEXT))
        // [Microsoft][SQLServer 2000 Driver for JDBC][SQLServer]A variable cannot be used to 
//     specify a search condition in a fulltext predicate when accessed through a cursor.

        .append(",'").append(statement).append("')");
        
//        filterHolder.addParameter(statement.toString());
        
    }

    protected StringBuilder buildFTSAndWordsFilter(List<String> name) {
        StringBuilder result = new StringBuilder();
        
        for(int i=0; i<name.size(); i++){
            if(i>0)
                result.append(" AND ");
            result.append(name.get(i));
        }
        
        return result;
    }

    protected StringBuilder buildFTSNotWordsFilter(List<String> name) {
        StringBuilder result = new StringBuilder();
        
        for(int i=0; i<name.size(); i++){
            if(i>0)
                result.append(" AND");
            
            result.append(" NOT ").append(name.get(i));
        }
        
        return result;
    }
}

/*
 * $Log: MSSQLFTSQueryBuilder.java,v $
 * Revision 1.1  2007/10/09 07:34:52  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:59:19  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/12/15 13:13:28  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:20  maksims
 * #1801897 Query2 addition
 *
 */