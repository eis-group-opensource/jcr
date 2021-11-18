/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.PathSQL.QUERY_PART;



/**
 * Default implementation for Filter SQL generator.
 * @author mzizkuns
 */
public class DefaultFilterSQLBuilder implements FilterSQLBuilder {
    private static final Log log = LogFactory.getLog(DefaultFilterSQLBuilder.class);
    
    /**
     * Generates SQL for filter which root is passed as a parameter in a context
     * which is provided.
     * Algorythm is as following:<br>
     * 1. Open filter to avoid any internal NOTs and cast to a form:<br> 
     * E1 AND E2 ... AND En<br>
     * OR<br>
     * E1 AND E2 ... AND En<br>
     * OR<br> 
     * ...<br>
     * 2. Clear constants results which may appear in a process of filter opening. 
     * For example expression of type not(@jcr:primaryType) always returns <code>false</code>
     * thus ANDs set containing such an expression always evaluates to <code>false</code> and shound't be processed.
     * 3. Executes generation for cases when single ANDs set or multiple ANDs sets are specified.
     * @throws RepositoryException 
     */
    public void generate(Condition root, FilterContext context) throws RepositoryException {
        List<List<Comparison>> opened = root.open();
        if(opened.size()==0){
            String message = MessageFormat.format("Filter at node with name {0} is failed to open."
                    , context.getContextNode().isWildcard()?"*":context.getContextNode().getName());
            throw new RuntimeException(message);
        }

        List<List<Comparison>> cleaned = new ArrayList<List<Comparison>>();
        boolean knownResult = false;
        
        for(List<Comparison> ands : opened){
            switch(cleanConstants(ands)){
                case 1:
                    knownResult = true;
                case -1:                    
                    continue;
                    
                default:
                    cleaned.add(ands);
            }
        }
        
        StringBuilder ownerJoiningColumn = QueryUtils.asPrefix(context.getOwnerAlias()).append(Constants.FIELD_ID);
        
        
        switch(cleaned.size()){
            case 0:
                if(knownResult)
                    return;
                else{
                    String message = MessageFormat.format("Filter at node with name {0} will not produce any result which turns whole result set into 0"
                            , context.getContextNode().isWildcard()?"*":context.getContextNode().getName());
                    throw new NoResultsQuery(message);
                }
                
            case 1:
                context.canUseOwnerTable(true);
//              Set flag set CM_NODE related filters may use owner alias which refers to CM_NODE
                List<Comparison> andset = cleaned.get(0);
                NodeTypeImpl primaryNt = findContextType(andset, context);
                if(andset.size() == 0)
                    throw new NoResultsQuery("Query cannot have results.");

                List<FilterSQL> fd = fillFilterData(andset, context, primaryNt);
// No need to turn on type propagation it is allowed by default ...
                joinToStepTable(fd, context, ownerJoiningColumn);
                break;
                
            default:
                generate(cleaned, context, ownerJoiningColumn);
        }
        
        context.mergeWithOwner();
    }
    
    
    /**
     * Joins filter SQL to current Path Step alias.
     * 
     * Case 1: <== No UNIONS
     * CM_NODE nX <parentJoinType>  <mainTable1> <joinToParentStatement> <mainTableJoins>   <== filterType=SIMPLE
     *            <parentJoinType>  <mainTable2> <joinToParentStatement> <mainTableJoins>   <== ...
     *            <parentJoinType>  <mainTable3> <joinToParentStatement> <mainTableJoins>   <== ...
     *            JOIN (<filterBody>) <joinToParentStatement>                               <== filterType=INQUERY
     *
     *  WHERE ... AND <where1> AND <where2> AND <where3>
     * @param filterData
     * @param context
     */
    protected void joinToStepTable(List<FilterSQL> filterData, FilterContext context, StringBuilder ownerJoiningColumn){

//      Set flag set CM_NODE related filters may use owner alias which refers to CM_NODE
//      context.canUseOwnerTable(true);
//        context.resetTableAliases(); This is called once per step filter ... so in order to share linked tables info with ordering moved to PathStep
        
        for(int i=0; i<filterData.size(); i++){
            FilterSQL fd = filterData.get(i);
            
            switch(fd.getFilterType()){
                case DIRECT: // filter condition is appended directly to main WHERE
                    context.getOwner().where().append(" AND (").append(fd.getWhere()).append(')');
                    if(fd.hasParameters())
                        context.appendParameters(fd.getParameters(), QUERY_PART.WHERE);
                    break;
                    
                case SIMPLE: // filter related tables joined to FROM tables and condition is added to main WHERE
                    
                    if(!fd.isAliasReused()){
                        context.target().append(fd.getParentJoinType());
                        
                        context.target()
                        .append(fd.getMainTable()).append(' ')
                        .append(fd.getMainAlias()).append(' ')
                        .append(" ON ")                    
                        .append(QueryUtils.asPrefix(fd.getMainAlias()))
                        .append(fd.getJoiningColumn())
                        .append('=');
                        if(i==0)
                            context.target().append(ownerJoiningColumn);
                        else{
//                            FilterSQL predcessor = filterData.get(i-1);
                            FilterSQL predcessor = null;
                            for(int j=i-1; j>-1; j--){
                                predcessor = filterData.get(j);
                                if(predcessor.getMainAlias() != null)
                                    break;
                            }
                            
                            if(predcessor == null){
                                throw new RuntimeException(MessageFormat.format("Unable to find table to join to for table {0}", fd.getMainTable()));
                            }
                            
                            if(predcessor.getFilterType() == FilterSQL.TYPE.DIRECT){
                                context.target().append(ownerJoiningColumn);
                            }else{
                                context.target()
                                .append(QueryUtils.asPrefix(predcessor.getMainAlias()))
                                .append(predcessor.getJoiningColumn());
                            }
                        }
                        
                        context.target().append(' ').append(fd.getMainTableJoins());
                    }
                    
                    if(fd.hasWhere())
                        context.getOwner().where().append(" AND (").append(fd.getWhere()).append(')');
                    
                    if(fd.hasParameters())
                        context.appendParameters(fd.getParameters(), QUERY_PART.WHERE);
                    
                    break;
                    
                case INQUERY:// filter is sub-query and is joined as is to FROM tables
                    context.target().append(fd.getParentJoinType())
                    .append(" (")
                    .append(fd.getFilterBody()).append(") ").append(fd.getMainAlias())
                    .append(" ON ")
                    .append(QueryUtils.asPrefix(fd.getMainAlias()))
                    .append(fd.getJoiningColumn())
                    .append('=');
                    
                    if(i==0)
                        context.target().append(ownerJoiningColumn);
                    else{
                        FilterSQL predcessor = filterData.get(i-1);
                        
                        context.target()
                        .append(QueryUtils.asPrefix(predcessor.getMainAlias()))
                        .append(predcessor.getJoiningColumn());
                        
//                        context.target().append(filterData.get(i-1).getJoiningColumn());
                    }
                    
                    if(fd.hasParameters())
                        context.appendParameters(fd.getParameters(), QUERY_PART.FROM);
                    break;
                
                default:
                    String message = MessageFormat.format("Sub-filter type {0} is not yet supported", fd.getFilterType());
                    throw new UnsupportedOperationException(message);
            }
        }
    }

    /**
     * Adds multiple andSet filters to main query at current step path alias.
     * @param opened
     * @param context
     * @throws RepositoryException 
     */
    protected void generate(List<List<Comparison>> andsSet, FilterContext context, StringBuilder ownerJoiningColumn) throws RepositoryException{
        int addedCount =0;

        context.target().append(" JOIN (");
        
        for(int c=0; c < andsSet.size(); c++){
            List<Comparison> ands = andsSet.get(c);
            
            if(addedCount>0){
                context.target().append(" UNION ");
                for(Comparison and:ands)// prepare for recombination ...
                    and.clearCombining();
            }
            
            NodeTypeImpl primaryNt = findContextType(ands, context);
            if(ands.size() == 0) // can be reset to zero size if contain ambigous type comparisons
                continue;
            
            List<FilterSQL> fd = fillFilterData(ands, context, primaryNt);


            generateSubfilter(fd, context, ownerJoiningColumn);
            addedCount++;
        }
        
        if(addedCount == 0)
            throw new NoResultsQuery("None of filter conditions produced valid sql. Query cannot have results");

        StringBuilder predicateAlias = context.nextAlias();
        
        context.target()
        .append(") ").append(predicateAlias)
        .append(" ON ")
        .append(ownerJoiningColumn)
        .append('=')
        .append(QueryUtils.asPrefix(predicateAlias)).append(FilterContext.JOINING_FIELD);
    }    
    
    
    
    
    /**
     * Generates SQL which result set is joined to current path step alias.
     * Case 2: UNIONs exist
     * CM_NODE nX JOIN (
     *            <selectStatement> 
     *            FROM              <mainTable1>                         <mainTableJoins>   <== filterType=SIMPLE
     *            <parentJoinType>  <mainTable2> <joinToParentStatement> <mainTableJoins>   <== ...
     *            <parentJoinType>  <mainTable3> <joinToParentStatement> <mainTableJoins>   <== ...
     *            JOIN (<filterBody>) <joinToParentStatement>                               <== filterType=INQUERY
     *            WHERE <where1> AND <where2> AND <where3>
     *         UNION
     *            <selectStatement> 
     *            FROM              <mainTable1>                         <mainTableJoins>   <== filterType=SIMPLE
     *            <parentJoinType>  <mainTable2> <joinToParentStatement> <mainTableJoins>   <== ...
     *            <parentJoinType>  <mainTable3> <joinToParentStatement> <mainTableJoins>   <== ...
     *            JOIN (<filterBody>) f1 ON <joinToParentStatement>                         <== filterType=INQUERY
     *            WHERE <where1> AND <where2> AND <where3>) f ON nX.ID=f.ID
     *         UNION
     *            ...
     *            ) f ON nX.ID=f.ID
     *            
     * @param filterData
     * @param context
     */
    protected void generateSubfilter(List<FilterSQL> filterData, FilterContext context, StringBuilder ownerJoiningColumn){
        
        StringBuilder where = null;
        List<Object> inqueryParameters = new LinkedList<Object>();
        List<Object> simpleParameters = new LinkedList<Object>();        

//      context.resetTableAliases(); This is called once per step filter ... so in order to share linked tables info with ordering moved to PathStep
        
        
        for(int i=0; i<filterData.size(); i++){
            FilterSQL fd = filterData.get(i);
            
            if(i==0)
                context.target()
                .append(" SELECT ")
                .append(QueryUtils.asPrefix(fd.getMainAlias()))
                .append(fd.getJoiningColumn())
                .append(" AS ").append(FilterContext.JOINING_FIELD)
                .append(" FROM ");
            else
                if(!fd.isAliasReused())
                    context.target().append(fd.getParentJoinType());
            
            switch(fd.getFilterType()){
                case SIMPLE:
                    
                    if(!fd.isAliasReused()){
                        context.target()
                        .append(fd.getMainTable()).append(' ')
                        .append(fd.getMainAlias()).append(' ');
    
                        if(i>0){           
                            FilterSQL predcessor = filterData.get(i-1);
                            
                            context.target()
                            .append(" ON ")
                            .append(QueryUtils.asPrefix(fd.getMainAlias()))
                            .append(fd.getJoiningColumn())
                            .append('=')
                            .append(QueryUtils.asPrefix(predcessor.getMainAlias()))
                            .append(predcessor.getJoiningColumn());
                        }
                        
                        context.target().append(fd.getMainTableJoins());
                    }
                    
                    if(fd.hasWhere()){
                        if(where == null){
                            where = new StringBuilder(" WHERE ");
                        }else
                            where.append(" AND ");
                        
                        where.append('(').append(fd.getWhere()).append(')');
                        
                        if(fd.hasParameters()) simpleParameters.addAll(fd.getParameters());
                    }
                    break;
                    
                case INQUERY:
                    
                    context.target().append('(').append(fd.getFilterBody()).append(") ")
                    .append(fd.getMainAlias()).append(' ');

                    if(i>0){    
                        FilterSQL predcessor = filterData.get(i-1);
                        
                        context.target()
                        .append(" ON ")
                        .append(QueryUtils.asPrefix(fd.getMainAlias()))
                        .append(fd.getJoiningColumn())
                        .append('=')
                        .append(QueryUtils.asPrefix(predcessor.getMainAlias()))
                        .append(predcessor.getJoiningColumn());
                    }
                    
                    if(fd.hasParameters()) inqueryParameters.addAll(fd.getParameters());
                    break;
                
                default:
                    String message = MessageFormat.format("Filter sub-query type {0} is not yet supported", fd.getFilterType());
                    throw new UnsupportedOperationException(message);
            }
            
            context.appendParameters(inqueryParameters, QUERY_PART.FROM);
            context.appendParameters(simpleParameters, QUERY_PART.FROM);            
        }
        
        if(where != null)
            context.target().append(where);
    }    
    
    
    /**
     * Removes comparison with constant value.
     * Always False cases: 
     * - JCR_NAME IS NULL                           <- ConstantResultComparison
     * - JCR_NAME=1 AND JCR_NAME=2
     * - JCR_NAME=1 AND contextNName=2
     * - JCR_PRIMARY_TYPE IS NULL                   <- ConstantResultComparison
     * - JCR_PRIMARY_TYPE=1 AND JCR_PRIMARY_TYPE=2
     * - repositoryId IS NULL                       <- ConstantResultComparison
     * - attributeA=1 AND attribute1=2
     * 
     * Always True cases:   <- such can be just excluded from evaluation ...
     * - JCR_NAME IS NOT NULL                       <- ConstantResultComparison
     * - JCR_PRIMARY_TYPE IS NOT NULL               <- ConstantResultComparison
     * - repositoryId IS NOT NULL                   <- ConstantResultComparison
     * 
     * Repeats cases:       <- such can be replaced by one constraint in ANDs set ...
     * - attributeA=1 AND attribute1=1      => attributeA=1
     * - contextNName=1 AND JCR_NAME=1      => just remove because this constraint is already added on step level
     * @param ands set of anded comparisons
     * @param context 
     * @param contextNName
     * @param contextNT
     * @return
     */
    protected int cleanConstants(List<Comparison> ands){
        for(int i=0; i<ands.size(); i++){
            Comparison c = ands.get(i);
            int cr = c.getConstantResult();
            if(cr<0) // one of comparisons connected by AND has constant FALSE value -> whole ANDs set turns to FALSE
                return -1;
            
            if(cr>0) ands.remove(i--);
        }
        
        return ands.size()>0 ? 0: 1;
    }
    
    
    /**
     * Builds filter for single ANDs set ..
     * Structure is following:
     * 1. Filter by primary type or context NT if provided
     * 2. Filter by mixin types if provided
     * 3. JOIN additional filters taking into account 1 and 2
     * @return <code>true</code> if SQL has been added.
     */
    protected NodeTypeImpl findContextType(List<Comparison> ands, FilterContext context){
        NodeTypeImpl contextNT = null;
        Comparison contextPTComparison = null;        
        
        boolean useMixins = false;
        
        for(int c=0; c<ands.size(); c++){// move to separate list declared primary types
            Comparison comparison = ands.get(c);
            if(comparison.isMixinConstraining()){
                useMixins = true;
            }else
              if(comparison.isTypeConstraining()){
                if(contextPTComparison == null){
                    contextPTComparison = comparison;
                    contextNT = getContextNTFromComparison(comparison, context);
                }else
                    /*
                     *      only one context PrimaryType can be used so if there is more
                     *      and these are copies of first ... eliminate them
                     *      if these are pointing to another node types it is ALWAYS FALSE because
                     *      same node cannot have two primary types at the same time ...
                     */
                    if(!contextPTComparison.equals(comparison)){
                        String message = MessageFormat.format("Filter at node with name {0} contains ambigous reference to two primary types {1} and {2}."
                                , context.getContextNode().isWildcard()?"*":context.getContextNode().getName()
                                , contextNT.getName(), comparison);

                        log.warn(message);
                        ands.clear(); // this condition is always FALSE!
                        return null;
                    }
                
                ands.remove(c--); // remove type constraining comparison from common list ...
            }
        }

        if(contextPTComparison != null) // ... and set it first to allow generalized processing ... 
            ands.add(0, contextPTComparison);

//      Disable node type propagation if no node type found in at least one comparison
        if(contextNT == null && context.isNodeTypePropagateAllowed())
            context.setNodeTypePropagateAllowed(false);

//      Disable mixin propagation if no mixin comparison found in at least one comparison        
        if(!useMixins && context.isMixinPropagateAllowed())
            context.setMixinPropagateAllowed(false);
        
        return contextNT;
    }

    
    /**
     * Performs SQL generation for comparisons linked in a single ANDs set.
     * @param ands
     * @param context
     * @param contextNT
     * @return
     * @throws RepositoryException 
     */
    protected List<FilterSQL> fillFilterData(List<Comparison> ands, FilterContext context, NodeTypeImpl contextNT) throws RepositoryException{
        List<FilterSQL> filterData = new ArrayList<FilterSQL>();
        
        for(int i=0; i<ands.size(); i++){ // try to combine ...
            Comparison c = ands.get(i);
            for(int j=0; j<ands.size(); j++){
                c.combine(ands.get(j));
            }
        }
            
        
        for(int c=0; c<ands.size(); c++){
            FilterSQL fd = ands.get(c).createFilterData(context, contextNT);
            if(fd != null) // can be null if comparison is already processed
                filterData.add(fd);
        }
        
        return filterData;
        
    }
    
    /**
     * Returns node type declared in comparison provided or throws RuntimeException
     * if given comparison isn't of PrimaryTypeComparison.
     * @param ptc
     * @param context
     * @return
     */
    protected NodeTypeImpl getContextNTFromComparison(Comparison ptc, FilterContext context){
        if(ptc == null) // no comparison no primary type
            return null;
        
        if(!(ptc instanceof PrimaryTypeComparison))
                throw new RuntimeException("Something is died ... not PrimaryTypeComparison instance unexpectedly found");
        
        PrimaryTypeComparison p = (PrimaryTypeComparison)ptc;
        String typeName = p.getPrimaryTypeName();
        NodeTypeImpl type = context.getOwner().getBuildingContext().getNodeTypeDefinition(typeName);
        if(type.isMixin()){
            String message = MessageFormat.format("{0} is a mixin name and cannot be used as a parameter for jcr:primaryType",
                    typeName);
            throw new IllegalArgumentException(message);
        }
        
        return type;
    }
}

/*
 * $Log: DefaultFilterSQLBuilder.java,v $
 * Revision 1.4  2008/01/17 12:40:09  maksims
 * #1806270 Fixed problem when new alias should be used after previous alias has been reused but previous belongs to a DIRECT filter type
 *
 * Revision 1.3  2008/01/15 10:47:55  maksims
 * #1806270 Fixed problem when new alias should be used after previous alias has been reused
 *
 * Revision 1.2  2007/10/09 07:34:51  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/12/21 12:33:17  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.2  2006/12/20 16:18:02  maksims
 * #1803572 fix for between condition for general property
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.4  2006/12/13 14:27:13  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.3  2006/11/22 16:35:37  maksims
 * #1802721 Log category performance added
 *
 * Revision 1.2  2006/11/20 16:15:46  maksims
 * #0149156 String conversion for columns fixed
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */