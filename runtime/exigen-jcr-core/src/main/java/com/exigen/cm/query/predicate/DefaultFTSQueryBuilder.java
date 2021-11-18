/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/



package com.exigen.cm.query.predicate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.QueryUtils;



/**
 * Default implementation of FTS query builder.
 * @author Maksims
 *
 */
public class DefaultFTSQueryBuilder extends AbstractFTSQueryBuilder {
    
    /**
     * Builds SQL for default (Visiflow model based) implementation
     * of FTS query.
     * 
     * Attribute name can be: as in JSR-170 6.6.5.2
     * 1. .         - any attribute
     * 2. attr      - namespace=NULL name='attr'
     * 3. ns:attr   - namespace=nsId name='attr'
     */
    public void build(FilterContext context, FilterSQL filterHolder, String attributeName, List<List<Comparison>> ftsFilter, boolean negated) throws RepositoryException{
        
        BuildingContext.DBQName attribute = (attributeName == null || attributeName.length() == 1 && attributeName.charAt(0)=='.') ?
                                                      context.getOwner().getBuildingContext().toDBQname(null) 
                                                    : context.getOwner().getBuildingContext().toDBQname(attributeName);
        
        
        StringBuilder sql = filterHolder.getFilterBody();
        
        filterHolder.setFilterType(FilterSQL.TYPE.INQUERY);
        filterHolder.setMainAlias(context.nextAlias().toString());
        filterHolder.setJoiningColumn(Constants.FIELD_TYPE_ID);
        
        boolean needsDistinct = ftsFilter.size() == 1;
        
        for(int i=0; i<ftsFilter.size(); i++){
            if(i>0)
                filterHolder.getFilterBody().append(" UNION ");
            
            
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
            
            StringBuilder dataAlias = context.nextAlias();
            StringBuilder entryAlias = context.nextAlias();
            StringBuilder wordsAlias = context.nextAlias();

            sql.append("SELECT ");
            if(needsDistinct) sql.append(" DISTINCT ");
            
            
            sql.append(QueryUtils.asPrefix(dataAlias)).append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
            .append(" FROM ")
            .append(context.getRealTableName(Constants.TABLE_FTS_DATA)).append(' ').append(dataAlias);
            
            
            
            StringBuilder where = new StringBuilder();
            if(!attribute.isWildcard()){
                where
                .append(QueryUtils.asPrefix(dataAlias)).append(context.getRealColumnName(Constants.FIELD_NAME)).append("=? AND ")
                .append(QueryUtils.asPrefix(dataAlias)).append(context.getRealColumnName(Constants.FIELD_NAMESPACE));

                
                filterHolder.addParameter(attribute.getLocalName());
                
                if(attribute.hasNamespace()){
                    where.append("=?");
                    filterHolder.addParameter(attribute.getNamespaceId());
                }else{
                    where.append(" IS NULL ");                
                }
            }            
            
            
            boolean entryTableAdded = buildWordsFilter(context, filterHolder, words, sql, where, dataAlias, entryAlias, wordsAlias);
            buildNotWordsFilter(context, filterHolder, notWords, sql, where, dataAlias, entryAlias, wordsAlias, entryTableAdded);

            sql.append(" WHERE ").append(where);
        }
    }

    /**
     * Joins index entry table.
     * @param context
     * @param dataAlias
     * @param entryAlias
     * @param sql
     * @throws RepositoryException 
     */
    private void joinEntryTable(FilterContext context, StringBuilder dataAlias, StringBuilder entryAlias, StringBuilder sql) throws RepositoryException{
        sql.append(" JOIN ")
        .append(context.getRealTableName(Constants.TABLE_INDEX_ENTRY)).append(' ').append(entryAlias)
        .append(" ON ")
        .append(QueryUtils.asPrefix(entryAlias)).append(context.getRealColumnName(Constants.TABLE_INDEX_ENTRY__DATA_ID))
        .append('=')
        .append(QueryUtils.asPrefix(dataAlias)).append(context.getRealColumnName(Constants.FIELD_ID));
    }
    
    /**
     * Builds words filter SQL e.g. words connected via AND.
     * @param context
     * @param filterHolder
     * @param words
     * @param sql
     * @param where
     * @param dataAlias
     * @param entryAlias
     * @param wordsAlias
     * @return
     * @throws RepositoryException
     */
    private boolean buildWordsFilter(FilterContext context
                                    , FilterSQL filterHolder
                                    , List<String> words
                                    ,StringBuilder sql
                                    ,StringBuilder where
                                    ,StringBuilder dataAlias
                                    ,StringBuilder entryAlias
                                    ,StringBuilder wordsAlias) throws RepositoryException{
        if(words.size() == 0)
            return false;


        List<Long> wordIds = new ArrayList<Long>(words.size());
        
        DatabaseConnection connection = context.getOwner().getBuildingContext().getConnection().getNewConnection();
        DatabaseSelectAllStatement selectWordIds = DatabaseTools.createSelectAllStatement(Constants.TABLE_WORD, true);
        selectWordIds.addCondition(Conditions.in(Constants.TABLE_WORD__DATA, words));
        selectWordIds.addResultColumn(Constants.FIELD_ID);
          
        try{
            selectWordIds.execute(connection);
            while(selectWordIds.hasNext())
                wordIds.add(selectWordIds.nextRow().getLong(Constants.FIELD_ID));
        }finally{
            connection.close();
        }
        

          
        if(wordIds.size() != words.size())
            throw new NoResultsQuery("Not all words found in document index. Query can't have results.");
        
        if(wordIds.size() == 1) // just one word
            return buildFTSSingleWord(context, filterHolder, wordIds.get(0), sql, where, dataAlias, entryAlias, wordsAlias);

        return buildFTSAndWordsByRareWordCombination(context, filterHolder, wordIds, sql, where, dataAlias, entryAlias, wordsAlias);
    }

    /*
    SELECT top <max number of params in IN +1> ie.NODE_ID, count(ie.NODE_ID)
    FROM INDEX_ENTRY ie
    WHERE ie.WORD_ID IN (w1ID, w2ID ...)
    GROUP BY ie.NODE_ID
    ORDER BY count(ie.NODE_ID) asc  <= meaningless as we have HAVING
    HAVING count(ie.NODE_ID)=N <== number of words 
  */
    /**
     * Builds SQL for rare word combinations.
     */
    private boolean buildFTSAndWordsByRareWordCombination(FilterContext context, FilterSQL filterHolder, List<Long> wordIds, StringBuilder sql, StringBuilder where, StringBuilder dataAlias, StringBuilder entryAlias, StringBuilder wordsAlias) throws RepositoryException{
       
        
        BuildingContext buildContext = context.getOwner().getBuildingContext();

        
        StringBuilder query = new StringBuilder("SELECT fts.")
        .append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
        .append(" FROM ")
        .append(context.getRealTableName(Constants.TABLE_FTS_DATA)).append(" fts JOIN ")        
        .append(context.getRealTableName(Constants.TABLE_INDEX_ENTRY)).append(" ie ON (fts.")
        .append(context.getRealColumnName(Constants.FIELD_ID))
        .append("=ie.").append(context.getRealColumnName(Constants.TABLE_INDEX_ENTRY__DATA_ID))
        .append(" AND fts.").append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
        .append(" IS NOT NULL) WHERE ")
        .append("ie.").append(context.getRealColumnName(Constants.TABLE_INDEX_ENTRY__WORD))
        .append(" IN (").append(getCollectionPlaceholders(wordIds)).append(") GROUP BY ")
        .append("fts.").append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
        .append(" HAVING ").append(buildContext.getDialect().getCountAllStatement()).append("=?");
        
        
        int maxInParams = buildContext.getDialect().getInMaxParamsCount();
        
        Long[] needsParam = buildContext.getDialect().limitResults(query, 0, maxInParams+1, false, null);

        List<Long> nodeIds = new ArrayList<Long>();
        
        DatabaseConnection connection = buildContext.getConnection().getNewConnection();
        PreparedStatement rareStatement = connection.prepareStatement(query.toString(), true);
        
        try{
            int i=0;
            for(i=0; i<wordIds.size(); i++)
                rareStatement.setObject(i+1, wordIds.get(i));
            
            rareStatement.setObject(i+1, new Integer(wordIds.size()));
            if(needsParam != null){
                for(Long v: needsParam){
                    rareStatement.setObject(i+2, v);
                }
            }
            
            ResultSet data = rareStatement.executeQuery();
    
            while(data.next()){
                nodeIds.add(buildContext.getDialect().convertFromDBLong((Number)data.getObject(1)));
            }
            
            rareStatement.close();
        }catch(SQLException ex){
            throw new RepositoryException(ex);
        }finally{
            connection.close();
        }
        
        if(nodeIds.size() == 0) // no nodes corresponding to words at all !
            throw new NoResultsQuery("No nodes found to correspond FTS filter");
        
        if(nodeIds.size() < maxInParams){
//      Can query using IN on Node.IDs
/*  
         owner.getCurrentAlias().ID IN (???)
 */         if(where.length()>0)
             where.append(" AND ");
            
            where.append(QueryUtils.asPrefix(dataAlias)).append(Constants.FIELD_TYPE_ID)
            .append(" IN (");
            addCollection(filterHolder,where,nodeIds);
            where.append(')');
            
            return false;
        }

        
        return buildFTSAndWordsGeneral(context, filterHolder, wordIds, sql, where, dataAlias, entryAlias, wordsAlias);
    }
    
    /**
     * Builds general type FTS SQL for case when neither single word not rare word combination
     * is provided within FTS filter.
     * @param context
     * @param filterHolder
     * @param wordIds
     * @param sql
     * @param where
     * @param dataAlias
     * @param entryAlias
     * @param wordsAlias
     * @return
     * @throws RepositoryException 
     */
    private boolean buildFTSAndWordsGeneral(FilterContext context, FilterSQL filterHolder, List<Long> wordIds, StringBuilder sql, StringBuilder where, StringBuilder dataAlias, StringBuilder entryAlias, StringBuilder wordsAlias) throws RepositoryException {
/*
         buffer.append(ftsDataAlias).append('.').append(Constants.FIELD_ID)
        .append(" IN ( SELECT ")
        .append(entryAlias).append('.').append(Constants.TABLE_INDEX_ENTRY__DATA_ID)
        .append(" FROM ")
        .append(Constants.TABLE_INDEX_ENTRY).append(' ').append(entryAlias)
        .append(" WHERE ")
        .append(entryAlias).append('.').append(Constants.TABLE_INDEX_ENTRY__WORD)
        .append(" IN (").append(owner.addCollection(wordsIds,PropertyType.LONG)).append(')')
        .append(" GROUP BY ")
        .append(entryAlias).append('.').append(Constants.TABLE_INDEX_ENTRY__DATA_ID)
        .append(" HAVING ").append(owner.getDialect().getCountAllStatement())
        .append("=?))");

        owner.addFilterParameter(new Integer(wordsIds.size())); // set count parameter
 */       
        if(where.length()>0)
            where.append(" AND ");
        
        where.append(QueryUtils.asPrefix(dataAlias)).append(Constants.FIELD_ID)
        .append(" IN ( SELECT entry.").append(context.getRealColumnName(Constants.TABLE_INDEX_ENTRY__DATA_ID))
        .append(" FROM ")
        .append(context.getRealTableName(Constants.TABLE_INDEX_ENTRY)).append(" entry")
        .append(" WHERE ")
        .append("entry.").append(context.getRealColumnName(Constants.TABLE_INDEX_ENTRY__WORD))
        .append(" IN (");
        addCollection(filterHolder, where, wordIds);
        where.append(')')
        .append(" GROUP BY ")
        .append(" entry.").append(context.getRealColumnName(Constants.TABLE_INDEX_ENTRY__DATA_ID))
        .append(" HAVING ").append(context.getOwner().getBuildingContext().getDialect().getCountAllStatement())
        .append("=?))");
        
        filterHolder.addParameter(wordIds.size());
        
        return false;
    }


    /*
    FROM ...  INDEX_ENTRY ie
    WHERE ... AND ie.WORD_ID=?
    */
    /**
     * Builds FTS SQL for single word FTS query.
     * @throws RepositoryException 
     */
    private boolean buildFTSSingleWord(FilterContext context, FilterSQL filterHolder, Long wordId, StringBuilder sql, StringBuilder where, StringBuilder dataAlias, StringBuilder entryAlias, StringBuilder wordsAlias) throws RepositoryException {
        if(where.length() > 0)
            where.append(" AND ");
        
        joinEntryTable(context, dataAlias, entryAlias, sql);
        
        where.append(QueryUtils.asPrefix(entryAlias)).append(context.getRealColumnName(Constants.TABLE_INDEX_ENTRY__WORD)).append("=?");
        filterHolder.addParameter(wordId);
        
        return true;
    }


    /*
    SELECT DISTINCT _n.nodeId 
     FROM FTS_DATA _n 
             join CM_INDEX_ENTRY _nX on _nX.FTS_DATA_ID = _n.ID 
             join CM_WORD _nXX on _nX.WORD_ID=_nXX.ID
     WHERE _n.name=? AND _n.NAMESPACE=? AND _nXX.DATA NOT IN (data) AND _nX.WORD_ID=_nXX.ID
    */
    /**
     * Builds FTS SQL for negated words.
     * @throws RepositoryException 
     */
    private void buildNotWordsFilter(FilterContext context
                                            , FilterSQL filterHolder
                                            , List<String> notWords
                                            , StringBuilder sql
                                            , StringBuilder where
                                            , StringBuilder dataAlias
                                            , StringBuilder entryAlias
                                            , StringBuilder wordsAlias
                                            , boolean entryTableAdded) throws RepositoryException {


        if(notWords.size() == 0)
            return;
        
        if(!entryTableAdded)
            joinEntryTable(context, dataAlias, entryAlias, sql);
            
        sql.append(" JOIN ")
            .append(context.getRealTableName(Constants.TABLE_WORD)).append(' ').append(wordsAlias)
            .append(" ON ")
            .append(QueryUtils.asPrefix(entryAlias)).append(context.getRealColumnName(Constants.TABLE_INDEX_ENTRY__WORD))
            .append('=')
            .append(QueryUtils.asPrefix(wordsAlias)).append(context.getRealColumnName(Constants.FIELD_ID));
        
        
        if(where.length() > 0)
            where.append(" AND ");
        
        where.append(QueryUtils.asPrefix(wordsAlias)).append(context.getRealColumnName(Constants.TABLE_WORD__DATA))
        .append(" NOT IN (");
        addCollection(filterHolder, where, notWords);
        where.append(')');
    }


    /**
     * Adds list of parameters into filter holders' parameters list and
     * appends corresponding number of question marks to sql.
     * @param filterHolder
     * @param target
     * @param values
     */
    private void addCollection(FilterSQL filterHolder, StringBuilder target,  List<? extends Object> values) {
        for(int i=0; i<values.size(); i++){
            if(i>0)
                target.append(',');
            target.append('?');
            
            filterHolder.addParameter(values.get(i));
        }
    }
    
    /**
     * Returns string of question marks separated by commas as
     * a placeholder for SQL IN() operator.
     * @param values
     * @return
     */
    private StringBuilder getCollectionPlaceholders(List values) {
        StringBuilder target = new StringBuilder();
        
        for(int i=0; i<values.size(); i++){
            if(i>0)
                target.append(',');
            target.append('?');
        }
        return target;
    }
}
/*
 * $Log: DefaultFTSQueryBuilder.java,v $
 * Revision 1.4  2008/07/09 10:13:07  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/07/09 07:50:28  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2007/10/09 07:34:51  dparhomenko
 * Add mssql2005 support
 *
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
 * Revision 1.11  2006/09/11 11:25:20  maksims
 * #1802865 Fixed case when FTS query by words which aren't in existing in documents.
 *
 * Revision 1.10  2006/09/08 12:37:49  maksims
 * #1802865 Records with FTS_DATA.NODE ID null are excluded from words rare combination test
 *
 * Revision 1.9  2006/08/16 13:25:03  maksims
 * #1802658 Statistics column removed from CM_WORD table
 *
 * Revision 1.8  2006/07/12 15:05:45  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 * Revision 1.7  2006/07/12 12:34:10  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 * Revision 1.6  2006/07/12 07:44:32  dparhomenko
 * PTR#1802389 for update statement
 *
 * Revision 1.5  2006/06/26 14:53:59  maksims
 * #0147128 support for empty attribute data tables added
 *
 * Revision 1.4  2006/04/27 08:20:25  maksims
 * #0144986 filter converted into lowercase
 *
 * Revision 1.3  2006/04/24 13:42:52  maksims
 * #0144986 support for special FTS_DATA table added
 *
 * Revision 1.2  2006/04/20 11:42:49  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:47:03  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/07 16:46:07  maksims
 * #0144985 MSSQL FTS query builder added
 *
 * Revision 1.2  2006/04/05 09:47:13  maksims
 * #0144985 JCR query adjusting to Oracle Text added
 *
 * Revision 1.1  2006/03/31 16:01:00  maksims
 * #0144985 Oracle specific search implemented
 *
 */