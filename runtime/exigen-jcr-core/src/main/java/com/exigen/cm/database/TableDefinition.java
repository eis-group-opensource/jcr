/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.statements.SchemaOperation;

public class TableDefinition implements SchemaOperation{
    
    private String tableName;
    private ArrayList<ColumnDefinition> columns = new ArrayList<ColumnDefinition>();
    
    private ArrayList<IndexDefinition> indexes = new ArrayList<IndexDefinition>();
    private boolean autoCreateIndex = true;
	private boolean alter = false;
	private boolean indexOrganized = false;
    
    public TableDefinition(String tableName)  throws RepositoryException{
        this(tableName, false);
    }

    public TableDefinition(String tableName, boolean createIdColumn)  throws RepositoryException{
        this.tableName = tableName;
        if (createIdColumn){
            addColumn(new ColumnDefinition(this, Constants.FIELD_ID,Types.INTEGER, true));
        }
    }

    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public ColumnDefinition addColumn(ColumnDefinition column) {
        this.columns.add(column);
        return column;
    }

    public Iterator<ColumnDefinition> getColumnIterator() {
        return columns.iterator();
    }

    public Iterator<ColumnDefinition> getPKColumnIterator() {
        ArrayList<ColumnDefinition> result = new ArrayList<ColumnDefinition>();
        for(Iterator<ColumnDefinition> it = getColumnIterator() ; it.hasNext(); ){
            ColumnDefinition columnDef = it.next();
            if (columnDef.isPK()){
                result.add(columnDef);
            }
        }
        return result.iterator();
    }
    
    
    

    public boolean equals(Object o) {        
        if (this == o) {
            return true;
        }
        if (o instanceof TableDefinition) {
            TableDefinition tableDef = (TableDefinition)o;
            return getTableName().equalsIgnoreCase(tableDef.getTableName());
        }
        return false;
    }
    
    
    public int hashCode() {        
        return tableName.hashCode();
    }
    
    public String toString() {
        return getTableName();
    }
    
    public ColumnDefinition getColumn (String columnName) {
        for (Iterator iter = columns.iterator(); iter.hasNext();) {
            ColumnDefinition column = (ColumnDefinition) iter.next();
            if (column.getColumnName().equalsIgnoreCase(columnName)) {
                return column;
            }            
        }
        return null;
    }    
    
    public boolean containsColumn (ColumnDefinition columnDef) {
        return columns.contains(columnDef);
    }
    
    public void addIndexDefinition(IndexDefinition indexDef) {
        indexes.add(indexDef);
    }
    
    public Iterator<IndexDefinition> getIndexeIterator() {
        return indexes.iterator();
    }

    public void setAutoCreateIndex(boolean value) {
        this.autoCreateIndex = value;
        
    }

    public boolean isAutoCreateIndex() {
        return autoCreateIndex;
    }

	public void setAlter(boolean b) {
		this.alter = b;
		
	}

	public boolean isAlter() {
		return alter;
	}

	public void execute(DatabaseConnection conn) throws RepositoryException {
		conn.createTables(new TableDefinition[] {this});
		
	}

	public boolean isIndexOrganized() {
		return indexOrganized;
	}

	public void setIndexOrganized(boolean indexOrganized) {
		this.indexOrganized = indexOrganized;
	}
}


/*
 * $Log: TableDefinition.java,v $
 * Revision 1.1  2007/04/26 09:00:52  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2007/04/04 10:27:13  vpukis
 * PTR#1801827 Oracle Index organized table support, added columns in index over CM_NODE.NODE_PATH for index only access, in case of Oracle - tables CM_NODE_PARENTS and CM_TYPE made index organized
 *
 * Revision 1.4  2007/01/24 08:46:44  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.3  2006/10/11 13:08:58  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.2  2006/04/20 11:42:51  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:40  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.8  2006/04/05 14:30:41  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.7  2006/04/05 09:04:07  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.6  2006/03/20 09:00:37  ivgirts
 * PTR #1801375 added methods for registering and altering node types
 *
 * Revision 1.5  2006/03/14 16:08:25  ivgirts
 * PTR #1801059 added creation of indexes for foreign and primary keys
 *
 * Revision 1.4  2006/03/03 10:33:13  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.3  2006/03/03 09:39:22  ivgirts
 * PTR #1801059 Database startup checking added
 *
 * Revision 1.2  2006/02/13 12:40:45  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 * Revision 1.1  2006/02/10 15:50:26  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */