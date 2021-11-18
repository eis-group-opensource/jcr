/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import java.sql.SQLException;
import java.util.HashMap;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;

public class DatabaseCountStatement extends DatabaseSelectAllStatement{

    private Long count;
    
    public DatabaseCountStatement(String tableName) {
        super(tableName, true);
    }

    public Long getCount() {
        return count;
    }

    protected void assembleSQLFields(StringBuffer sb) {
        sb.append("count(*) as s");
        
    }

    protected void processResulSet(DatabaseConnection conn) throws RepositoryException {
        try {
            resultSet.next();
            HashMap row = buildRow();
            if (row.containsKey("s")){
                count = (Long) row.get("s");
            } else if (row.containsKey("S")){
                count = (Long) row.get("S");
            } else {
                throw new RepositoryException("Count column not found");
            }
            if (count == null) {
                count = new Long(0);
            }
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    } 
    
}



/*
 * $Log: DatabaseCountStatement.java,v $
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/05/10 09:00:42  dparhomenko
 * PTR#0144983 dynamic table names
 *
 * Revision 1.1  2006/04/17 06:47:12  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/03/22 16:45:08  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.2  2006/03/09 11:01:48  ivgirts
 * PTR #1801251 added support for Hypersonic SQL DB
 *
 * Revision 1.1  2006/02/16 13:53:07  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */