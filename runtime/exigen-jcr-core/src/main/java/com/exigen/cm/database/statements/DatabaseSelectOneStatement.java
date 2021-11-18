/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;

public class DatabaseSelectOneStatement extends AbstractDatabaseStatement{

    String pkColumnName;
    Object pkValue;
    RowMap row;
    
    public DatabaseSelectOneStatement(String tableName, String pkColumnName, 
            Object pkValue) {
        super(tableName);
        this.pkColumnName = pkColumnName;
        this.pkValue = pkValue;
    }

    public int applyParameters(PreparedStatement st, DatabaseConnection conn, int startPos) throws RepositoryException {
        DatabaseTools.bindParameter(st, conn.getDialect(), startPos, pkValue, false);
        return 1;
    }

    public String assemblSQL(DatabaseConnection conn) throws RepositoryException {
        StringBuffer sb = new StringBuffer();
        sb.append("select * from ");
        sb.append(getTableName(conn.getDialect()));
        sb.append(" where ");
        sb.append(pkColumnName);
        sb.append("=?");
        return sb.toString();
    }

    public RowMap getRow() {
        return row;
    }
    
    protected void processResulSet(DatabaseConnection conn) throws RepositoryException {
        try {
            if (resultSet.next()){
                row = buildRow();
            } else {
                throw new ItemNotFoundException("Object "+getTableName(conn.getDialect())+" with pk "+pkValue+" not found");
            }
        } catch (SQLException exc){
            throw new RepositoryException("Error reading result set", exc);
        }
    }
    

    protected boolean isAutoCloseStatement() {
        return true;
    }

    
    
    public void setIgnoreBLOB(boolean b) {
        super.setIgnoreBLOB(b);
    }
    
    protected HashMap<String, String> buildTableAliasMapping(){
    	return new HashMap<String,String>();
    }

}


/*
 * $Log: DatabaseSelectOneStatement.java,v $
 * Revision 1.2  2009/01/26 10:53:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2006/08/11 09:04:43  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.4  2006/08/04 12:33:35  maksims
 * #1802426 SQL Framework used to generate queries in DBContentStore
 *
 * Revision 1.3  2006/05/10 09:00:42  dparhomenko
 * PTR#0144983 dynamic table names
 *
 * Revision 1.2  2006/05/03 12:07:08  dparhomenko
 * PTR#0144983 make DatabaseStatement as interface
 *
 * Revision 1.1  2006/04/17 06:47:12  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.6  2006/03/16 13:13:08  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.5  2006/03/14 11:55:38  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.4  2006/03/03 10:33:19  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.3  2006/03/01 11:54:42  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.2  2006/02/13 12:40:55  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 * Revision 1.1  2006/02/10 15:50:33  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */