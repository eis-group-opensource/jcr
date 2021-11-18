/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import java.sql.PreparedStatement;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;

public class DatabaseJoin {
    protected String fkField;
    protected String idFiled;
    private String table_alias;
    protected String _table;
	private boolean addToResult;

    public DatabaseJoin(String table, String table_alias, String idFiled, String fkField, boolean addToResult) {
        this._table = table;
        this.table_alias = table_alias;
        this.idFiled =idFiled;
        this.fkField =fkField;
        this.addToResult = addToResult;
        if (table_alias == null || table_alias.length() == 0){
            this.table_alias = table;
        }
    }

    public String getFkField() {
        return fkField;
    }


    public String getIdFiled() {
        return idFiled;
    }
    public String _getTable() {
        return _table;
    }

    public String getTableAlias() {
        return table_alias;
    }

    public void generateSQLFromFragment(StringBuffer sb, DatabaseConnection connection) throws RepositoryException {
        sb.append(", ");
        sb.append(connection.getDialect().convertTableName(_getTable()));
        sb.append(" ");
        if (getTableAlias() != null && getTableAlias().length() > 0){
            sb.append(getTableAlias());
        }    
    }

	public boolean isAddToResult() {
		return addToResult;
	}

    public int bindParameters(int i, DatabaseConnection conn, PreparedStatement st) throws RepositoryException {
        return 0;
    }

    
}


/*
 * $Log: DatabaseJoin.java,v $
 * Revision 1.4  2008/07/17 06:57:46  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/07/16 08:45:05  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2007/10/09 07:34:52  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2006/08/11 09:04:43  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.3  2006/05/11 07:00:21  dparhomenko
 * PTR#0144983 add FTS test
 *
 * Revision 1.2  2006/05/10 09:00:42  dparhomenko
 * PTR#0144983 dynamic table names
 *
 * Revision 1.1  2006/04/17 06:47:12  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/03/27 07:22:21  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.1  2006/03/01 11:54:42  dparhomenko
 * PTR#0144983 support locking
 *
 */