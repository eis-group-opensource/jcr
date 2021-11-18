/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database;

import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.EqualsBuilder;

public class ColumnDefinition {

    private int columnType;
    
    private String columnName;

    private boolean isPK;

    private int length;

    private boolean notNull = false;
    
    private TableDefinition foreignKey;
    
    private TableDefinition tableDef = null;

    public ColumnDefinition(TableDefinition tableDef, String columnName, int columnType) throws RepositoryException{
       this(tableDef, columnName, columnType, false); 
    }

    public ColumnDefinition(TableDefinition tableDef, String columnName, int columnType, boolean isPK) throws RepositoryException{
        this.columnName = columnName;
        this.columnType = columnType;
        this.isPK = isPK;
        
        if (isPK && !tableDef.isIndexOrganized()){
        	// check if we have already columns with isPK=true (possibly this is 2nd, 3rd, ... column
        	// for composite PK)
        	ArrayList<String> pkColumns=new ArrayList<String>();
        	for (Iterator<ColumnDefinition> iter=tableDef.getColumnIterator();iter.hasNext();){
        		ColumnDefinition c=iter.next();
        		if (c.isPK){
        			pkColumns.add(c.columnName);
        		}
        	}
        	if (pkColumns.size()==0){ // This is a first column with isPK=true
        		setNotNull(true);
        		IndexDefinition indexDef = new IndexDefinition(tableDef);
        		indexDef.addColumn(this);
        		indexDef.setUnique(true);
        		tableDef.addIndexDefinition(indexDef);
        	}else{ // find index definition with previous PK columns
        		IndexDefinition existingIndexDef=null;
        		for(Iterator<IndexDefinition> iter=tableDef.getIndexeIterator();iter.hasNext();){
        			IndexDefinition idx=iter.next();
        			int posInColumnList=0;
        			boolean matched=true;
        			for(Iterator<ColumnDefinition> citer=idx.getColumnIterator();citer.hasNext();){
        				if (posInColumnList>=pkColumns.size()){
        					matched=false;
        					break;
        				}
        				if (!citer.next().columnName.equals(pkColumns.get(posInColumnList))){
        					matched=false;
        					break;
        				}
        				posInColumnList++;
        			}
        			if (matched && posInColumnList==pkColumns.size() && idx.isUnique()){
        				existingIndexDef=idx;
        				break;
        			}
        		}
        		if (existingIndexDef==null){
        			setNotNull(true);
            		IndexDefinition indexDef = new IndexDefinition(tableDef);
            		for (String colName:pkColumns){
            			indexDef.addColumn(colName);
            		}	
            		indexDef.setUnique(true);
            		tableDef.addIndexDefinition(indexDef);
        		}else{
        			setNotNull(true);
        			existingIndexDef.addColumn(this);
        		}	
        	}
        }
        this.tableDef = tableDef;
    }

    public String getColumnName() {
        return columnName;
    }

    public int getColumnType() {
        return columnType;
    }

    public boolean isPK() {
        return isPK;
    }

    public ColumnDefinition setLength(int length) {
        this.length = length;
        return this;
        
    }

    public int getLength() {
        return length;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public void setNotNull(boolean value){
        this.notNull = value;
    }

    public void setForeignKey(TableDefinition foreignKey) {
        this.foreignKey = foreignKey;
        if (tableDef.isAutoCreateIndex()){
            IndexDefinition indexDef = new IndexDefinition(tableDef);
            indexDef.addColumn(this);
            tableDef.addIndexDefinition(indexDef);
        }
        
    }

    public TableDefinition getForeignKey() {
        return foreignKey;
    }
    
    public boolean equals(Object obj) {
        
        if (obj instanceof ColumnDefinition == false) {
            return false;
          }
          if (this == obj) {
            return true;
          }
          ColumnDefinition rhs = (ColumnDefinition) obj;
          return new EqualsBuilder()
                        .append(getColumnName().toUpperCase(), rhs.getColumnName().toUpperCase())
                        .append(getTableDefinition().getTableName().toUpperCase(), rhs.getTableDefinition().getTableName().toUpperCase())
                        .isEquals();
    }
    
    public int hashCode() {
        int code = 0;
        code = columnName.hashCode();
        code += tableDef.hashCode();
        return code;
    } 
    
    
    public TableDefinition getTableDefinition() {
        return tableDef;
    }
    
    public void setTableDefinition(TableDefinition tableDef) {
        this.tableDef = tableDef;
    }    

}


/*
 * $Log: ColumnDefinition.java,v $
 * Revision 1.1  2007/04/26 09:00:52  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2007/04/04 10:27:13  vpukis
 * PTR#1801827 Oracle Index organized table support, added columns in index over CM_NODE.NODE_PATH for index only access, in case of Oracle - tables CM_NODE_PARENTS and CM_TYPE made index organized
 *
 * Revision 1.2  2006/10/23 14:38:17  dparhomenko
 * PTR#0148641 fix data import
 *
 * Revision 1.1  2006/04/17 06:46:40  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.6  2006/04/05 09:04:07  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.5  2006/03/14 16:08:25  ivgirts
 * PTR #1801059 added creation of indexes for foreign and primary keys
 *
 * Revision 1.4  2006/03/09 11:01:49  ivgirts
 * PTR #1801251 added support for Hypersonic SQL DB
 *
 * Revision 1.3  2006/03/03 10:33:13  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.2  2006/03/03 09:39:22  ivgirts
 * PTR #1801059 Database startup checking added
 *
 * Revision 1.1  2006/02/10 15:50:26  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */