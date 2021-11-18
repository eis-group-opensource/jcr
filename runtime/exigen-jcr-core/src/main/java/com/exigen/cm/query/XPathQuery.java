/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query;

import java.sql.PreparedStatement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.query.PathSQL.QUERY_PART;
import com.exigen.cm.query.order.OrderDefinition;
import com.exigen.cm.query.order.OrderDefinitionProducer;
import com.exigen.cm.query.predicate.BooleanOperation;
import com.exigen.cm.query.predicate.ComparisonType;
import com.exigen.cm.query.predicate.Condition;
import com.exigen.cm.query.predicate.PredicateProducer;
import com.exigen.cm.query.step.PathStep;
import com.exigen.cm.query.step.PathStepProducer;


/**
 * Used to collect XPath steps.
 */
public class XPathQuery extends BuildingContextHolder implements QueryBuilder {
    private static final Log log = LogFactory.getLog(XPathQuery.class);
    
    
    private List<PathStep> subqueryRoots;
    private PathStep lastStep;
    
    
    private Map<String, OrderDefinition> orderDef = null;
    
    
    private StepData _tmp;
    
    private int maxResultRowCount = -1;
    
    private final PathStepProducer stepProducer;
    private final PredicateProducer predicateProducer;
    private final OrderDefinitionProducer orderProducer;


//  first row number is 1
    private int resultsShift = 0;
    
    public XPathQuery(BuildingContext context){
        super(context);
        stepProducer = new PathStepProducer(context);
        predicateProducer = new PredicateProducer(context);
        orderProducer = new OrderDefinitionProducer(context);
        
        subqueryRoots = new ArrayList<PathStep>();
    }

    /**
     * Preforms translation of XPath tree to SQL PreparedStatement.
     * @param xPath
     * @param context
     * @return
     * @see PreparedStatement
     */
    public PreparedStatement toPreparedStatement() throws Exception{
        
        List<PathStep> subQueries = getSubqueryRoots();
        String idColumn = getBuildingContext().getDialect().convertColumnName(Constants.FIELD_ID);
        
        BasicSecurityFilter securityFilter = getBuildingContext().getSecurityFilter();
        
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();
        
        if(subQueries.size()==1)
            prepareSimpleStatement(subQueries, sql, params, idColumn, securityFilter);//, orderSQL);
        else
            prepareUnionStatement(subQueries, sql, params, idColumn, securityFilter);//, orderSQL);

        if(getLimitResult()>0 || getResultsShift() > 0)
            getBuildingContext().getDialect().limitResults(sql, getResultsShift(), getLimitResult(), false, params);
        
        if(hasOrdering() && getBuildingContext().areHintsApplicable())
            getBuildingContext().getDialect().applyHints(sql);
        
        StringBuilder debug=null;
        
        if(log.isDebugEnabled())
            debug = new StringBuilder(sql);
        
        PreparedStatement statement = getBuildingContext().getConnection().prepareStatement(sql.toString(), true);
        for(int p=0; p<params.size(); p++){
            statement.setObject(p+1, params.get(p));
            
            if(debug != null){
                if(p==0)
                    debug.append("\nParameters: ").append(params.get(p));
                else
                    debug.append(",").append(params.get(p));
            }
        }
        
        if(debug != null)
            log.debug(debug);
        
        return statement;
    }
    
    /**
     * Prepares SQL from XPath containing unions.
     * @param subQueries
     * @param sql
     * @param params
     * @param idColumn
     * @param securityFilter
     * @throws RepositoryException 
     */
    protected void prepareUnionStatement(List<PathStep> subQueries, StringBuilder sql, List<Object> params, String idColumn, BasicSecurityFilter securityFilter) throws RepositoryException{//, OrderSQL orderSQL){

        StringBuilder from = new StringBuilder();
        from
        .append(" FROM ").append('(');
        
        for(int q=0; q<subQueries.size(); q++){

            if(q != 0)
                from.append(" UNION ");

            PathStep root = subQueries.get(q);
            
            PathSQL pathSQL = root.toSQL(getBuildingContext(), getOrderDefinitions());
            params.addAll(pathSQL.getParameters());
            
            from
            .append("SELECT ");
            
            List<String> selections = pathSQL.getSelections();
            for(int i=0; i<selections.size(); i++){
                if(i>0) from.append(',');
                from.append(selections.get(i));
            }
            

            from.append(securityFilter.getSecurityColumnForSelect(getBuildingContext(),PathSQL.TARGET_ALIAS))
            .append(" FROM ")
            .append(pathSQL.from())
            .append(" WHERE ").append(pathSQL.where());
        }
        
        from
        .append(") ")
        .append(PathSQL.TARGET_ALIAS)
        .append( securityFilter.getFilterJoin(getBuildingContext(), PathSQL.TARGET_ALIAS));
        
        
//      From should be processed first to fill in Ordering data
        String targetIDColumn = QueryUtils.asPrefix(PathSQL.TARGET_ALIAS).append(idColumn).toString();        
        sql.append("SELECT ").append(targetIDColumn);
        
        if(hasOrdering()){
            for(OrderDefinition od:getOrderDefinitions()){
                String[] rcs = od.getReferredColumns();
                for(String rc:rcs)
                    sql.append(',').append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS)).append(rc);
            }
        }
        sql.append(securityFilter.getSecurityColumnForSelect(getBuildingContext(),PathSQL.TARGET_ALIAS));
        
        sql.append(from);
        
        if(securityFilter.hasWhere())
            sql.append(" WHERE ")
                .append(securityFilter.getWhereStatement(getBuildingContext(), PathSQL.TARGET_ALIAS, params));
            

        if(securityFilter.hasGrouping()){ // note there can be other grouping attributes !!!
            sql.append(" GROUP BY ").append(securityFilter.getGroupByStatement(getBuildingContext(), PathSQL.TARGET_ALIAS));
            if(hasOrdering()){
                for(OrderDefinition od:getOrderDefinitions()){
                    String[] rcs = od.getReferredColumns();
                    for(String rc:rcs)
                        sql.append(',').append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS)).append(rc);
//                    sql.append(orderSQL.getSelect());
                }
            }
            
            sql.append(' ')
            .append(securityFilter.getHaving(getBuildingContext(), params, PathSQL.TARGET_ALIAS));
        }
        
        if(hasOrdering()){
            sql.append(" ORDER BY ");
            boolean started = false;
            for(OrderDefinition od:getOrderDefinitions()){
                String[] rcs = od.getReferredColumns();
                for(String rc:rcs){
                    if(started)
                        sql.append(',');
                    else
                        started=true;

                    sql.append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS)).append(rc).append(' ').append(od.getOrder());
                }
            }
        }        
            
    }
    
    
    /**
     * Prepares SQL from XPath containing single path declaration.
     * @param paths
     * @param sql
     * @param params
     * @param idColumn
     * @param securityFilter
     * @throws RepositoryException 
     */
    protected void prepareSimpleStatement(List<PathStep> paths, StringBuilder sql, List<Object> params, String idColumn, BasicSecurityFilter securityFilter) throws RepositoryException{//, OrderSQL orderSQL){

        PathStep path = paths.get(0);
        PathSQL pathSQL = path.toSQL(getBuildingContext(), getOrderDefinitions());
        
        sql.append("SELECT ");
        List<String> selections = pathSQL.getSelections();
        for(int i=0; i<selections.size(); i++){
            if(i>0) sql.append(',');
            sql.append(selections.get(i));
        }

        
        sql
        .append(" FROM ")
        .append(pathSQL.from())
        .append( securityFilter.getFilterJoin(getBuildingContext(), PathSQL.TARGET_ALIAS));

        params.addAll(pathSQL.getParameters(QUERY_PART.FROM));
        
        sql.append(" WHERE ").append(pathSQL.where());
        params.addAll(pathSQL.getParameters(QUERY_PART.WHERE));
        
//      Applies security where to filter!
        if(securityFilter.hasWhere())
            sql.append(" AND ").append(securityFilter.getWhereStatement(getBuildingContext(), PathSQL.TARGET_ALIAS, params));
            

        if(securityFilter.hasGrouping()){ // note there can be other grouping attributes !!!
            sql.append(" GROUP BY ").append(securityFilter.getGroupByStatement(getBuildingContext(), PathSQL.TARGET_ALIAS));
            if(hasOrdering()){
                for(OrderDefinition od:getOrderDefinitions()){
                    String[] rcs = od.getReferredColumns();
                    for(String rc:rcs)
                        sql.append(',').append(pathSQL.getSelectionColumnByAlias(rc));
//                    sql.append(orderSQL.getSelect());
                }
            }
            
            sql
            .append(' ')
            .append(securityFilter.getHaving(getBuildingContext(), params, PathSQL.TARGET_ALIAS));
        }
        
        if(hasOrdering()){
            sql.append(" ORDER BY ");
            boolean started = false;
            for(OrderDefinition od:getOrderDefinitions()){
                String[] rcs = od.getReferredColumns();
                for(String rc:rcs){
                    if(started)
                        sql.append(',');
                    else
                        started=true;

                    sql.append(rc).append(' ').append(od.getOrder());
                }
            }
        }
        
    }
    
    /**
     * Returns list of XPathes declared in a given XPath query.
     * @return
     */
    public List<PathStep> getSubqueryRoots(){
        return subqueryRoots;
    }
    
    /**
     * Starts new subquery e.g. new XPath declaration.
     */
    public void startSubquery() {
        lastStep=null;
    }

    /**
     * Ends current subquery.
     */
    public void endSubquery(){
        if(lastStep != null)
            subqueryRoots.add(lastStep);
    }

    /**
     * Unused!.
     */
    public void addSelectedAttribute(String attribute) {
    }
    
    
    /**
     * Starts new path step.
     */
    public void startPathElement() {
        _tmp = new StepData();
    }
    
    /**
     * Ends path step and updates last step variable to point to just added step.
     */
    public void endPathElement() {
        lastStep = stepProducer.produce(lastStep
                                        , _tmp.descendand
                                        , _tmp.name
                                        , _tmp.stepType
                                        , _tmp.index
                                        , _tmp.referringAttribute
                                        , _tmp.nodeTypeName
                                        , _tmp.filter
                                        , _tmp.isRootQuery);
    }

    
    /**
     * Makes this XPath to be Root Node Query.
     *
     */
    public void setRootQuery(){
        _tmp.isRootQuery = true;
    }    
    
    /**
     * Mark path step to be refering descendents
     */
    public void setPathElementDescOrSelf() {
        _tmp.descendand=true;
    }

    /**
     * Set path step node name.
     */
    public void setPathElementName(String name) {
        _tmp.name=name;
    }

    /**
     * Sets path step is a wildcard.
     */
    public void setPathElementWildcard() {
        _tmp.name=null;
    }

    /**
     * Sets same name sibling index for given path step.
     */
    public void setPathElementIndex(String index) {
        _tmp.index=Integer.parseInt(index);
    }

    /**
     * Marks given path step to be dereference.
     */
    public void setPathStepDeref(String referringAttribute, String targetNodeName) {
        _tmp.stepType=PathStepProducer.STEP_TYPE.DEREF;
        _tmp.referringAttribute=referringAttribute;

        String tn = stripQuotes(targetNodeName);
        if(tn.length() == 1 && tn.charAt(0)=='*')
            tn = null;
        
        _tmp.name=tn;
    }

    /**
     * Marks current path step to be element() function.
     */
    public void setPathStepElement() {
        _tmp.stepType=PathStepProducer.STEP_TYPE.ELEMENT;
    }

    /**
     * Set current path step node type constraint.
     */
    public void setPathElementType(String targetNodeType) {
        _tmp.nodeTypeName=targetNodeType;
    }

    
    
    
    /**
     * Returns <code>true</code> if given XPath has ordering specified.
     * @return
     */
    protected boolean hasOrdering(){
        return orderDef != null;
    }
    
    /**
     * Returns collection of ordering definitions for given XPath or <code>null</code>
     * if no orderings are specified.
     * @return
     */
    protected Collection<OrderDefinition> getOrderDefinitions(){
        return hasOrdering() ? orderDef.values() : null;
    }
    
    /**
     * Adds ordering specification to an XPath
     */
    public void addOrderByAttribute(String propertyName, OrderDefinition.ORDER order) {
        if(orderDef == null)
            orderDef = new HashMap<String, OrderDefinition>();
        
        if(orderDef.containsKey(propertyName)){
            String message = MessageFormat.format("Property {0} declared twice on ordering statement", propertyName);
            log.error(message);
            throw new IllegalArgumentException(message);
        }
        
        orderDef.put(propertyName, orderProducer.produce(propertyName, order));
    }

    public void addOrderByScore(List params, OrderDefinition.ORDER orderType) {
        throw new UnsupportedOperationException("Ordering by jcr:score() is not yet implemented");
    }



    /**
     * Attaches new predicate condition to current path step.
     * @param statement
     */
    protected void attach(Condition statement){
        if(_tmp.filter == null){
            _tmp.filter = statement;
            return;
        }

        if( !_tmp.filter.isLeaf() && !_tmp.filter.isComplete()){
            ((BooleanOperation)_tmp.filter).addChild(statement);
            
            if(!statement.isLeaf())
                _tmp.filter = statement;
            else
            if(_tmp.filter.isComplete()){
                Condition parent = _tmp.filter.getAvailableParent();//getAvailableParent(_tmp.filter);

                if( parent != null )
                    _tmp.filter=parent;
            }
            return;
        }

        throw new RuntimeException("Failed to build query tree! Cannot attach comparison!");
    }
    
//    protected Condition getAvailableParent(Condition c){
//        Condition parent = c.getParent();
//        while(parent != null && parent.isComplete()){
//            if(parent.getParent() == null)
//                return parent;
//            
//            parent = parent.getParent();            
//        }
//        
//        return parent;
//    }
    
    /**
     * Attaches AND to current path step filter and makes it current.
     */
    public void attachAnd() {
        attach(predicateProducer.and(_tmp.filter));
    }

    /**
     * Attaches NOT to current path step filter and makes it current.
     */
    public void attachNot() {
        attach(predicateProducer.not(_tmp.filter));
    }

    /**
     * Attaches OR to current path step filter and makes it current.
     */
    public void attachOr() {
        attach(predicateProducer.or(_tmp.filter));        
    }

    /**
     * Attaches position constraint to current path step filter element.
     */
    public void attachPositionConstraint(String op, String position) {
        ComparisonType.BINARY comparison = ComparisonType.BINARY.getTypeByXPathName(op);
        attach(predicateProducer.binaryComparison(comparison, Integer.parseInt(position)/*, _tmp.comparisonSequence)*/, PredicateProducer.PROPERTY_TYPE.POSITION));
    }

    /**
     * Attaches property comparison to current path step filter element.
     */
    public void attachComparison(String attribute, String op, Object value) {
        ComparisonType.BINARY comparison = ComparisonType.BINARY.getTypeByXPathName(op);
        attach(predicateProducer.binaryComparison(comparison, value/*, _tmp.comparisonSequence*/, attribute));
    }

    /**
     * Attaches jcr:contains() constraint to current path step filter element.
     */
    public void attachContains(String scope, String filter) {
    	if (!((RepositoryImpl)getBuildingContext().getSession().getRepository()).isSupportFTS()){
    		throw new RuntimeException("FTS not supported in this version , please specify fts=true in repository properties and recreate database.");
    	} 
    	attach(predicateProducer.binaryComparison(ComparisonType.BINARY.CONTAINS, filter/*, _tmp.comparisonSequence*/, scope)); 
    	
    }

    /**
     * Attaches jcr:like() constraint to current path step filter element.
     */
    public void attachLike(String attribute, String filter) {
        attach(predicateProducer.binaryComparison(ComparisonType.BINARY.LIKE, filter/*, _tmp.comparisonSequence*/, attribute));                
    }


    /**
     * Attaches IS NULL constraint to current path step filter element.
     */
    public void attachIsNull(String attribute) {
        attach(predicateProducer.unaryComparison(attribute, ComparisonType.UNARY.IS_NULL/*, _tmp.comparisonSequence*/));
    }

    /**
     * Attaches IS NOT NULL constraint to current path step filter element.
     */
    public void attachIsNotNull(String attribute) {
        attach(predicateProducer.unaryComparison(attribute, ComparisonType.UNARY.IS_NOT_NULL/*, _tmp.comparisonSequence*/));        
    }

    
    
    public void attachGrouping() {/*does nothing ...!*/}    

    /**
     * Returns limitation of results count for query or -1 if no limitation is set.
     */
    public int getLimitResult() {
        return maxResultRowCount;
    }

    /**
     * Sets limitation of results count for query.
     * @param limitResult
     */
    public void setLimitResult(int limitResult) {
        maxResultRowCount = limitResult;
    }

    /**
     * Returns limitation of results count for query or -1 if no limitation is set.
     */
    public int getResultsShift() {
        return resultsShift;
    }

    /**
     * 
     * first row number is 1. If less value is passed change it to 1 i.e. first row
     * Sets limitation of results count for query.
     * @param limitResult
     */
    public void setResultsShift(int resultsShift) {
        this.resultsShift  = resultsShift;// < 1 ? 1:resultsShift;
    }
    
    
    /**
     * Strip quotes from string value if value is quoted.
     * Returns value unchanged otherwise.
     * @param src
     * @return
     */
    static String stripQuotes(String src){
        if(src.charAt(0) == '\'' || src.charAt(0) == '"')
            return src.substring(1, src.length()-1);

        return src;
    }    
    
    /**
     * Tmp path step data storage.
     */
    private static class StepData{
        public boolean     descendand = false;
        public String      name;
        public PathStepProducer.STEP_TYPE   stepType = PathStepProducer.STEP_TYPE.NAMED;
        public int         index = PathStep.NO_INDEX;
        public String      referringAttribute;
        public String      nodeTypeName;
        public Condition   filter;
        public boolean     isRootQuery;
//        public Sequence    comparisonSequence = new Sequence();
    }

}

/*
 * $Log: XPathQuery.java,v $
 * Revision 1.6  2009/01/30 14:32:01  maksims
 * *** empty log message ***
 *
 * Revision 1.5  2009/01/30 07:09:08  maksims
 * *** empty log message ***
 *
 * Revision 1.4  2008/07/09 07:50:28  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2007/10/09 07:34:53  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.2  2007/07/19 08:17:33  dparhomenko
 * PTR#0152250 fix property state copy
 *
 * Revision 1.1  2007/04/26 08:59:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2007/03/01 14:25:56  maksims
 * #1804008 fixed jcxpath grammar
 *
 * Revision 1.2  2006/12/20 16:19:18  maksims
 * #1803635 javadocs added
 *
 * Revision 1.1  2006/12/15 13:13:29  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.5  2006/12/13 14:27:12  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.4  2006/11/30 14:54:38  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.3  2006/11/29 13:10:25  maksims
 * #1802721 added ability to use in ordering table containing property column joined by last step filter.
 *
 * Revision 1.2  2006/11/09 12:09:07  maksims
 * #1801897 SQL hints addition method used
 *
 * Revision 1.1  2006/11/02 17:28:06  maksims
 * #1801897 Query2 addition
 *
 */