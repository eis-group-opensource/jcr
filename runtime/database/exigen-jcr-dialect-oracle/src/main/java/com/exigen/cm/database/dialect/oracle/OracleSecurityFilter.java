/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle;

import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_SECURITY_PKG;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_SECURITY_PKG_FUNC;

import java.util.Collection;
import java.util.List;

import com.exigen.cm.Constants;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.query.BasicSecurityFilter;
import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.QueryUtils;

public class OracleSecurityFilter extends BasicSecurityFilter {

    
   @Override
   public StringBuilder getSecurityColumnForSelect(BuildingContext context, StringBuilder targetAlias) {
       return new StringBuilder()
       .append(',').append(QueryUtils.asPrefix(targetAlias)).append(context.getDialect().convertColumnName(Constants.TABLE_NODE__SECURITY_ID));
   }     
    
    @Override
   public StringBuilder getWhereStatement(BuildingContext context, StringBuilder targetAlias, List<Object> params) {
        
        
        DatabaseDialect dialect = context.getDialect();
        
        StringBuilder statement = new StringBuilder(QueryUtils.asPrefix(ORA_SECURITY_PKG))
        .append(ORA_SECURITY_PKG_FUNC+"(")
        .append(QueryUtils.asPrefix(targetAlias)).append(dialect.convertColumnName(Constants.FIELD_ID))
        .append(",")
        .append(QueryUtils.asPrefix(targetAlias)).append(dialect.convertColumnName(Constants.TABLE_NODE__SECURITY_ID))
        .append(",?,?,?,?)>0");

        
        Collection<String> groups = context.getSession().getGroupIDs();
        Collection<String> contexts = context.getSession().getContextIDs();
        StringBuffer groupNames = new StringBuffer();
        int i = 0;
        for(String group:groups){
            if(i++>0)
                groupNames.append(',');
            
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
        params.add(context.getDialect().convertToDBBoolean(context.isAllowBrowse()));
        
        return statement;
    }
}
/*
 * $Log: OracleSecurityFilter.java,v $
 * Revision 1.7  2009/02/04 12:16:38  maksims
 * *** empty log message ***
 *
 * Revision 1.6  2008/10/21 10:49:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2008/07/21 05:24:43  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2008/07/17 09:31:22  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/07/03 08:15:37  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2008/06/18 11:42:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2006/12/15 13:13:26  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:18  maksims
 * #1801897 Query2 addition
 *
 * Revision 1.3  2006/06/15 07:23:16  vpukis
 * PTR#1801827 FTS version 2.0
 *
 * Revision 1.2  2006/04/20 11:43:02  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:54  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/11 08:46:33  maksims
 * #0144986 Security filter queries made dialect specific
 *
 */