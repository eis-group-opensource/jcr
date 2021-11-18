/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.predicate.AbstractFTSQueryBuilder;
import com.exigen.cm.query.predicate.Comparison;
import com.exigen.cm.query.predicate.FilterContext;
import com.exigen.cm.query.predicate.FilterSQL;

/**
 * Implements FTS filter building for Oracle.
 */
public class OracleFTSQueryBuilder extends AbstractFTSQueryBuilder {
    
//    private Log log = LogFactory.getLog(OracleFTSQueryBuilder .class);
    private final String RESERVED_CHARS = "*%_,&?{}\\()[];|$!>~-";
    private static final String KEYWORDS="ABOUT ACCUM AND BT BTG BTI BTP FUZZY HASPATH INPATH MINUS NEAR NOT NT NTG NTI NTP OR PT RT SQE SYN TR TRSYN TT WITHIN";
    
    private static Set<String> keywords = new HashSet<String>();
    
    static{
        StringTokenizer t = new StringTokenizer(KEYWORDS, " ");
        while(t.hasMoreTokens()){
            String kw = t.nextToken();
            keywords.add(kw);
            keywords.add(kw.toLowerCase());
        }
    }
    
    
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

        filterHolder.getWhere()
        .append(" CONTAINS(")
        .append(QueryUtils.asPrefix(mainAlias)).append(context.getRealColumnName(Constants.TABLE_FTS_DATA__TEXT))
        .append(",?)>0");
        
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
            
            if(words.size() == 0)
                throw new IllegalArgumentException("FTS statement cannot contain only or starts with negation.");
            
            
            statement
                .append(buildFTSAndWordsFilter(words))
                .append(buildFTSNotWordsFilter(notWords));
        }
        
        filterHolder.addParameter(context.fixParameter(statement.toString()));
        
    }
    
    
    
    
    
    protected StringBuilder buildFTSAndWordsFilter(List<String> name) {
        StringBuilder result = new StringBuilder();
        
        for(int i=0; i<name.size(); i++){
            if(i>0)
                result.append(" & ");
            result.append(escapeIfReserved(name.get(i)));
        }
        
        return result;
    }



    protected StringBuilder buildFTSNotWordsFilter(List<String> name) {
        StringBuilder result = new StringBuilder();
        
        for(int i=0; i<name.size(); i++){
            result.append(" ~").append(escapeIfReserved(name.get(i)));
        }
        
        return result;
    }
    
    
    private String escapeIfReserved(String word){
        word = escapeReservedChar(word);

        if(keywords.contains(word))
            word = new StringBuffer(word.length()+2).append('{').append(word).append('}').toString();
        
        return word;
    }
    
    private String escapeReservedChar(String word){

        StringBuffer target = new StringBuffer(word.length());
        
        for(int i=0; i<word.length(); i++){
            if(RESERVED_CHARS.indexOf(word.charAt(i))>0){
                
                if(word.charAt(i) == '\\' && i+1<word.length()){
                    switch(word.charAt(i+1)){
                        case '-':
//                          reserved but already escaped in JCR query => copy as is
                            target.append("\\-");

                        case '\'':
                        case '\"':
//                          in case \ hides ' or " which means literal use in JCR => skip both
                            i++;
                            continue;
                    }
                }
                
                target.append('\\');
            }
            
            target.append(word.charAt(i));
        }
        
        return target.toString();
    }    

}

/*
 * $Log: OracleFTSQueryBuilder.java,v $
 * Revision 1.1  2007/04/26 08:59:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/15 13:13:26  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:18  maksims
 * #1801897 Query2 addition
 *
 */