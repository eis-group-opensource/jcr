/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.mssql;

import java.util.Collection;
import java.util.List;

import com.exigen.cm.Constants;
import com.exigen.cm.query.BasicSecurityFilter;
import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.QueryUtils;

//import com.exigen.cm.query.QueryBuilder;
//import com.exigen.cm.query.AbstractQueryBuilder;
/*
  SELECT n.id
 FROM cm_node n LEFT OUTER JOIN cm_ace sec ON n.security_id=sec.security_id
 
 GROUP BY n.id
 HAVING MAX(
     CASE
         WHEN n.security_id IS NULL OR sec.id IS NULL THEN 0
         WHEN sec.user_id='.superuser' THEN COALESCE(4+sec.p_read,1)
         WHEN sec.group_id IN ('.group1','.group2') THEN COALESCE(3-sec.p_read,1)
         ELSE 1
         END
         ) IN (0,2,5)
 */

public class MSSQLSecurityFilter extends BasicSecurityFilter {
    /**
     * Holds security grouping statement
     */
//    private static final StringBuffer GROUPING = new StringBuffer("GROUP BY ")
//        .append(PathSQL.TARGET_ALIAS).append('.').append(Constants.FIELD_ID);

    
//    private static final StringBuffer HAVING = new StringBuffer(" HAVING MAX( CASE WHEN ")
//        .append(PathSQL.TARGET_ALIAS).append('.').append(Constants.TABLE_NODE__SECURITY_ID)
//        .append(" IS NULL OR ")
//        .append(PathSQL.TARGET_ALIAS).append('.').append(Constants.FIELD_ID)
//        .append(" IS NULL THEN 0")
//        
//        .append(" WHEN ")
//        .append(PathSQL.TARGET_ALIAS_ACE).append('.').append(Constants.TABLE_ACE__USER_ID)
//        .append("=? THEN COALESCE (4+")
//        .append(PathSQL.TARGET_ALIAS_ACE).append('.').append(SecurityPermission.READ.getColumnName())
//        .append(",1) WHEN ")
//        .append(PathSQL.TARGET_ALIAS_ACE).append('.').append(Constants.TABLE_ACE__GROUP_ID)
//        .append(" IN ($) THEN COALESCE(3-")
//        .append(PathSQL.TARGET_ALIAS_ACE).append('.').append(SecurityPermission.READ.getColumnName())
//        .append(",1) ELSE 1 END) IN (0,2,5)");
//
//    private static final int GROUP_LIST_IDX= HAVING.indexOf("$"); // $ should be replaced onto groups list
//    
//    static{
//        HAVING.deleteCharAt(GROUP_LIST_IDX);
//    }
    
    /* *
     * Holds ACE table joining statement
     */
//    private static final StringBuilder SECURITY_JOIN = new StringBuilder(Constants.TABLE_NODE).append(' ')
//        .append(PathSQL.TARGET_ALIAS)
//        .append(" LEFT OUTER JOIN ")
//        .append(Constants.TABLE_ACE).append(' ').append(PathSQL.TARGET_ALIAS_ACE)
//        .append(" ON ")
//        .append(PathSQL.TARGET_ALIAS).append('.').append(Constants.TABLE_NODE__SECURITY_ID)
//        .append('=')
//        .append(PathSQL.TARGET_ALIAS_ACE).append('.').append(Constants.TABLE_NODE__SECURITY_ID);    

    @Override
    public StringBuilder getSecurityColumnForSelect(BuildingContext context, StringBuilder targetAlias) {
        return new StringBuilder()
        .append(',').append(QueryUtils.asPrefix(targetAlias)).append(context.getDialect().convertColumnName(Constants.TABLE_NODE__SECURITY_ID));
    };
    
    
    @Override
    public StringBuilder getWhereStatement(BuildingContext context, StringBuilder targetAlias, List<Object> params) {
         
         //DatabaseDialect dialect = context.getDialect();
         AbstractMsSQLDatabaseDialect dialect = (AbstractMsSQLDatabaseDialect)context.getDialect(); 
         String schemaName;
         try{
             schemaName = dialect.getSchemaName();
         }catch(Exception e){
             throw new RuntimeException("Failed to get schema name from MSSQL Dialect", e);
         }
         
         StringBuilder statement = new StringBuilder((schemaName.length() != 0 ? schemaName+"." : ""))
                 .append("PREAD")
                 .append("(")
                 .append(QueryUtils.asPrefix(targetAlias)).append(dialect.convertColumnName(Constants.FIELD_ID))
                 .append(",")
                 .append(QueryUtils.asPrefix(targetAlias)).append(dialect.convertColumnName(Constants.TABLE_NODE__SECURITY_ID))
                 .append(",?,?,?,?)>0");

         
         Collection<String> groups = context.getSession().getGroupIDs();
         Collection<String> contexts = context.getSession().getContextIDs();
         StringBuffer groupNames = new StringBuffer();
         int i = 0;
         for(String group:groups){
             if(i++>0) {
                 groupNames.append(',');
             }
             
             groupNames.append(dialect.convertStringToSQL(group));
         }
         
         StringBuffer contextNames = new StringBuffer();
         i = 0;
         for(String c:contexts){
             if(i++>0)
                 contextNames.append(',');
             
             contextNames.append(dialect.convertStringToSQL(c));
         }
         
         
         params.add(dialect.convertStringToSQL(context.getSession().getUserID()));
         params.add(groupNames.toString());
         params.add(contextNames.toString());
         params.add(context.isAllowBrowse());
         
         return statement;
     }    
    
    
    /*
    @Override
    public StringBuilder getFilterJoin(BuildingContext context, StringBuilder targetAlias) throws RepositoryException {
        
        DatabaseDialect dialect = context.getDialect();
        
        StringBuilder securityJoin = new StringBuilder(" LEFT OUTER JOIN ")
        .append(dialect.convertTableName(Constants.TABLE_ACE)).append(' ').append(PathSQL.TARGET_ALIAS_ACE)
        .append(" ON ")
        .append(QueryUtils.asPrefix(targetAlias))
            .append(dialect.convertColumnName(Constants.TABLE_NODE__SECURITY_ID))
        .append('=')
        .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS_ACE))
            .append(dialect.convertColumnName(Constants.TABLE_NODE__SECURITY_ID));
        
        return securityJoin;
    }
    
    @Override
    public StringBuilder getGroupByStatement(BuildingContext context, StringBuilder targetAlias) {
        return new StringBuilder(QueryUtils.asPrefix(targetAlias)).append(context.getDialect().convertColumnName(Constants.FIELD_ID))
        .append(getSecurityColumnForSelect(context, targetAlias));
    }

    
    @Override
    public StringBuilder getHaving(BuildingContext context, List<Object> params, StringBuilder targetAlias) {
        if (true){
            throw new UnsupportedOperationException();
        }
        DatabaseDialect dialect = context.getDialect();
        
        
        params.add(dialect.convertStringToSQL(context.getSession().getUserID()));

        List<String> groups = context.getSession().getGroupIDs();
        StringBuilder groupNames = new StringBuilder(1);
        
        if(groups.size() ==0){
            groupNames.append("?");
            params.add(dialect.convertStringToSQL("@#$_FAKE_GROUP"));
        }else{
            for(int i=0; i<groups.size(); i++){
                if(i>0)
                    groupNames.append(',');
                groupNames.append('?');
                params.add(dialect.convertStringToSQL(groups.get(i)));
            }
                    
        }
//      groupNames = builder.addCollection(groups, PropertyType.STRING).toString();
//      StringBuffer result = new StringBuffer(HAVING.length()+groupNames.length());
//      result.append(HAVING);
//      result.insert(GROUP_LIST_IDX, groupNames);
        
        StringBuilder having = new StringBuilder("HAVING MAX( CASE WHEN ")
        .append(QueryUtils.asPrefix(targetAlias)).append(dialect.convertColumnName(Constants.TABLE_NODE__SECURITY_ID))
        .append(" IS NULL OR ")
        .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS_ACE)).append(dialect.convertColumnName(Constants.FIELD_ID))
        .append(" IS NULL THEN 0")
        
        .append(" WHEN ")
        .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS_ACE)).append(dialect.convertColumnName(Constants.TABLE_ACE__USER_ID))
        .append("=? THEN COALESCE (4+")
        .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS_ACE)).append(SecurityPermission.READ.getColumnName())
        .append(",1) WHEN ")
        .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS_ACE)).append(dialect.convertColumnName(Constants.TABLE_ACE__GROUP_ID))
        .append(" IN (").append(groupNames).append(") THEN COALESCE(3-")
        .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS_ACE)).append(SecurityPermission.READ.getColumnName())
        .append(",1) ELSE 1 END) IN (0,2,5)");        
        
        
        return having;
    }
    
    @Override
    public boolean hasGrouping(){
        return true;
    }
    
    public boolean hasWhere(){
        return false;
    }//*/
}
/*
 * $Log: MSSQLSecurityFilter.java,v $
 * Revision 1.4  2008/10/21 10:49:46  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/08/26 12:59:44  maksims
 * *** empty log message ***
 *
 * Revision 1.2  2008/07/03 08:15:38  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/10/09 07:34:52  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:59:19  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.7  2006/12/15 13:13:28  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.2  2006/11/14 07:37:42  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.1  2006/11/02 17:28:20  maksims
 * #1801897 Query2 addition
 *
 * Revision 1.5  2006/06/02 07:21:42  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.4  2006/04/21 11:32:51  maksims
 * #0144986 fixed doubled addition of security filter
 *
 * Revision 1.3  2006/04/21 09:00:02  maksims
 * #0144986 empty param is replaced on fake value
 *
 * Revision 1.2  2006/04/20 11:42:59  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:46  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/11 09:04:28  maksims
 * #0144986 hasGrouping flag added to SecurityFilter to avoid unnecessary DISTINCT
 *
 * Revision 1.1  2006/04/11 08:46:35  maksims
 * #0144986 Security filter queries made dialect specific
 *
 */