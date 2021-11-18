/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds data of single comparison filter.
 * 
 * Case 1: <== No UNIONS
 * CM_NODE nX <parentJoinType>  <mainTable1> <joinToParentStatement> <mainTableJoins>   <== filterType=SIMPLE
 *            <parentJoinType>  <mainTable2> <joinToParentStatement> <mainTableJoins>   <== ...
 *            <parentJoinType>  <mainTable3> <joinToParentStatement> <mainTableJoins>   <== ...
 *            JOIN (<filterBody>) f1 ON <joinToParentStatement>                         <== filterType=INQUERY

 *  WHERE ... AND <where1> AND <where2> AND <where3>
 *  
 *  IMPORTANT: 
 *  Note that params for sub-filter of type SIMPLE must be added in a WHERE params section
 *  of main query!!!! 
 *  This is true in case of NO-UNIONS but in case of UNIONs params should be added in FROM section ...
 *  Need to think!!!
 *  
 *  IMPORTANT:
 *  In case of INQUERY joinToParentStatement must use parent alias!
 *  
 *  
 * Case 2: UNIONs exist
 * CM_NODE nX JOIN (
 *            <selectStatement> 
 *            FROM              <mainTable1> <joinToParentStatement> <mainTableJoins>   <== filterType=SIMPLE
 *            <parentJoinType>  <mainTable2> <joinToParentStatement> <mainTableJoins>   <== ...
 *            <parentJoinType>  <mainTable3> <joinToParentStatement> <mainTableJoins>   <== ...
 *            JOIN (<filterBody>) <joinToParentStatement>                               <== filterType=INQUERY
 *            WHERE <where1> AND <where2> AND <where3>
 *         UNION
 *            <selectStatement> 
 *            FROM              <mainTable1> <joinToParentStatement> <mainTableJoins>   <== filterType=SIMPLE
 *            <parentJoinType>  <mainTable2> <joinToParentStatement> <mainTableJoins>   <== ...
 *            <parentJoinType>  <mainTable3> <joinToParentStatement> <mainTableJoins>   <== ...
 *            JOIN (<filterBody>) f1 ON <joinToParentStatement>                         <== filterType=INQUERY
 *            WHERE <where1> AND <where2> AND <where3>) f ON nX.ID=f.ID
 *         UNION
 *            ...
 *            ) f ON nX.ID=f.ID
 *  
 */
public class FilterSQL {
    enum JOIN {
              INNER (" JOIN ")
            , LEFT (" LEFT OUTER JOIN ");
            
            private final String sql;
            JOIN(String sql){
                this.sql=sql;
            }
        
            @Override
            public String toString() {
                return sql;
            }
        };
    
     /**
      * Declare types of filter given instance describe
      * INQUERY: SQL containing UNIONS
      * SIMPLE: SQL declaring own TABLE(s) without UNIONs
      * DIRECT: SQL which contains WHERE part only and uses reference to current CM_NODE table
      */
    public enum TYPE {INQUERY, SIMPLE, DIRECT};
    
    /**
     * Default join to parent is JOIN
     */
    private JOIN parentJoinType = JOIN.INNER;
    
    /**
     * Default sub-filter type is SIMPLE
     */
    private TYPE filterType = TYPE.SIMPLE;
    
    /**
     * Holds joining column name
     */
    private String joiningColumn = FilterContext.JOINING_FIELD;
    
    private String mainTable;// = new StringBuilder();
    private String mainAlias;// = new StringBuilder();
    
    private StringBuilder mainTableJoins = new StringBuilder();
    private StringBuilder filterBody = new StringBuilder();
    private StringBuilder where = new StringBuilder();
//    private StringBuilder selectStatement = new StringBuilder();
    
    private List<Object> params;

    /**
     * Holds <code>true</code> if given SQL reuses alias declared by someone also.
     */
    private boolean isAliasReused = false;
    
    /**
     * Returns constant definig type of join to be used to append condition SQL
     * to its predcessor.
     * @return
     */
    public JOIN getParentJoinType() {
        return parentJoinType;
    }
    
    /**
     * Sets constant definig type of join to be used to append condition SQL
     * to its predcessor.
     * @return
     */
    public void setParentJoinType(JOIN join) {
        parentJoinType=join;
    }

    /**
     * Adds condition parameter.
     * @param param
     */
    public void addParameter(Object param){
        
        if(params == null)
            params = new ArrayList<Object>();
        params.add(param);
    }
    
    /**
     * Returns list of condition parameters.
     * @return
     */
    public List<Object> getParameters(){
        return params;
    }
    
    /**
     * Returns <code>true</code> if parameters where added during condition SQL generation.
     * @return
     */
    public boolean hasParameters(){
        return params != null;
    }
    
    /**
     * Returns constant definig type of SQL generated.
     * @return
     */
    public TYPE getFilterType() {
        return filterType;
    }

    /**
     * Sets constant definig type of SQL generated.
     * @return
     * @see TYPE
     */
    public void setFilterType(TYPE type) {
        filterType=type;
    }

    /**
     * Returns buffer for filter body. Used in case of TYPE.INQUERY
     * @return
     */
    public StringBuilder getFilterBody() {
        return filterBody;
    }

    /**
     * Sets joining column name.
     * @param column
     */
    public void setJoiningColumn(String column) {
        joiningColumn = column;
    }
    
    /**
     * Returns joining column name.
     * @return
     */
    public String getJoiningColumn() {
        return joiningColumn;
    }

    /**
     * Returns main main table name used to join with condition predcessor.
     * Used in case of TYPE.SIMPLE
     * @return
     */
    public String getMainTable() {
        return mainTable;
    }

    /**
     * Sets main main table name used to join with condition predcessor.
     * Used in case of TYPE.SIMPLE
     * @return
     */
    public void setMainTable(String table) {
        mainTable=table;
    }
    
    /**
     * Returns main main table alias used to join with condition predcessor.
     * Used in case of TYPE.SIMPLE
     * @return
     */
    public String getMainAlias() {
        return mainAlias;
    }    
    
    /**
     * Sets main main alias name used to join with condition predcessor.
     * Used in case of TYPE.SIMPLE
     * @return
     */
    public void setMainAlias(String alias) {
        mainAlias=alias;
    }    
    
    /**
     * Returns buffer for joins to be appended to main table.
     * Used in case of TYPE.SIMPLE
     * @return
     */
    public StringBuilder getMainTableJoins() {
        return mainTableJoins;
    }

    /**
     * Returns SQL buffer to be appended via AND to main SQL WHERE part.
     * @return
     */
    public StringBuilder getWhere() {
        return where;
    }
    
    /**
     * Returns <code>true</code> if given condition SQL has WHERE part defined.
     * @return
     */
    public boolean hasWhere(){
        return where.length() != 0;
    }
 
    /**
     * Sets flag defining if aliases generated by predcessors can be reused.
     * @param isReused
     */
    void setAliasReused(boolean isReused){
        isAliasReused = isReused;
    }
    
    /**
     * Returns <code>true</code> if given SQL reuses alias declared by someone also.
     * @return
     */
    boolean isAliasReused(){
        return isAliasReused;
    }
}

/*
 * $Log: FilterSQL.java,v $
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/21 12:33:17  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.2  2006/11/22 16:35:37  maksims
 * #1802721 Log category performance added
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */