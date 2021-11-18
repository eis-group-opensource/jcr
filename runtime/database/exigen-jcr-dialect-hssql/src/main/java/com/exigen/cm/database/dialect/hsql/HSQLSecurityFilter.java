/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.hsql;

import java.util.ArrayList;
import java.util.List;

import com.exigen.cm.Constants;
import com.exigen.cm.query.BasicSecurityFilter;
import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.QueryUtils;


/**
 * @author Maksims
 *
 */
public class HSQLSecurityFilter extends BasicSecurityFilter {
    
    @Override
    public StringBuilder getWhereStatement(BuildingContext context, StringBuilder targetAlias, List<Object> params) {
        StringBuilder statement = new StringBuilder();
        statement.append("\"com.exigen.cm.database.dialect.hsql.StoredProcedures.checkPermissionRead\"(")
        .append(QueryUtils.asPrefix(targetAlias)).append(context.getDialect().convertColumnName(Constants.FIELD_ID))
        .append(",")
        .append(QueryUtils.asPrefix(targetAlias)).append(context.getDialect().convertColumnName(Constants.TABLE_NODE__SECURITY_ID))
        .append(",?,?,?,?)>0");
        
        List<String> groups = new ArrayList<String>(context.getSession()._getWorkspace().getPrincipals().getGroupIdList());
        
        StringBuffer groupNames = new StringBuffer();
        for(int i=0; i<groups.size(); i++){
            if(i>0)
                groupNames.append(',');
            
            groupNames.append(context.getDialect().convertStringToSQL(groups.get(i)));
        }
        
        List<String> contexts = new ArrayList<String>(context.getSession()._getWorkspace().getPrincipals().getContextIdList());
        StringBuffer contextNames = new StringBuffer();
        for(int i=0; i<contexts.size(); i++){
            if(i>0)
                contextNames.append(',');
            
            contextNames.append(context.getDialect().convertStringToSQL(contexts.get(i)));
        }
        

        params.add(context.getDialect().convertStringToSQL(context.getSession().getUserID()));
        params.add(groupNames.toString());
        params.add(contextNames.toString());
        params.add(context.isAllowBrowse());
        
        return statement;
    }
}

/*
 * $Log: HSQLSecurityFilter.java,v $
 * Revision 1.6  2008/10/21 10:49:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2008/07/17 11:02:30  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2008/07/17 06:57:46  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/07/03 08:15:37  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2008/06/02 11:36:11  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 09:00:20  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/12/15 13:13:31  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:19  maksims
 * #1801897 Query2 addition
 *
 * Revision 1.1  2006/04/21 09:15:53  maksims
 * #0144986 security filter added
 *
 */