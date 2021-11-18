/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query;

import java.util.List;

import javax.jcr.RepositoryException;

public class BasicSecurityFilter {

    
    /**
     * Returns statement for declaring Target Node table in a FROM clause
     * @return
     * @throws RepositoryException 
     */
    public StringBuilder getFilterJoin(BuildingContext context, StringBuilder targetAlias) throws RepositoryException{
        return new StringBuilder(0);
    }
    

    /**
     * Applies if necessary security filter to common SQL filter by ANDing
     * @param filter
     * @return
     */
    public StringBuilder getWhereStatement(BuildingContext context,  StringBuilder targetAlias, List<Object> params){
        return new StringBuilder(0);
    }


    /**
     * Returns GroupBy statement if exists in a given Security filter.
     * @param requiredAttrs is a comma separated attributes list 
     * which should be included in grouping in order to make SQL compilable
     * @return
     */
    public StringBuilder getGroupByStatement(BuildingContext context, StringBuilder targetAlias){
        return new StringBuilder(0);
    }

    
    /**
     * Returusn HAVING statement if exists in a given SecurityFilter
     * @return
     */
    public StringBuilder getHaving(BuildingContext context, List<Object> params, StringBuilder targetAlias){
        return new StringBuilder(0);
    }
    
    
    /**
     * Returns <code>true</code> if given security filter has grouping clause and therefore 
     * SELECT statement needs no DISCTINCT as results are always grouped.
     * @return
     */
    public boolean hasGrouping(){
        return false;
    }
    
    public boolean hasWhere(){
        return true;
    }
    
    /**
     * Appends security ID column, which must participate in select/grouping to
     * allow security filter application.
     * Filters which requires security column to be selected must override this method.
     * @return
     */
    public StringBuilder getSecurityColumnForSelect(BuildingContext context, StringBuilder targetAlias){
        return new StringBuilder();
    }
}
/*
 * $Log: BasicSecurityFilter.java,v $
 * Revision 1.2  2007/10/09 07:34:53  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:59:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2006/12/15 13:13:29  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:06  maksims
 * #1801897 Query2 addition
 *
 * Revision 1.2  2006/04/20 11:42:49  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:47:03  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/11 09:04:27  maksims
 * #0144986 hasGrouping flag added to SecurityFilter to avoid unnecessary DISTINCT
 *
 * Revision 1.1  2006/04/11 08:46:32  maksims
 * #0144986 Security filter queries made dialect specific
 *
 */