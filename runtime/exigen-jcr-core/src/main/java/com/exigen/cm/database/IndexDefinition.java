/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;


/**
 * TODO Put class description here
 * 
 */
public class IndexDefinition {

    private List<ColumnDefinition> indexableColumns = new ArrayList<ColumnDefinition>();
    
    private boolean isUnique = false;
    private TableDefinition tableDef = null;

    private boolean fullTextSearch;

    private String name;

    public IndexDefinition(TableDefinition tableDef) {
        this.tableDef = tableDef;
    }

    public void addColumn(ColumnDefinition column) {
        indexableColumns.add(column);
    }
    
    public void addColumn(String columnName) throws RepositoryException {
        for(Iterator<ColumnDefinition> it = tableDef.getColumnIterator() ; it.hasNext(); ){
            ColumnDefinition columnDef = it.next();
            if (columnDef.getColumnName().equals(columnName)){
                addColumn(columnDef);
                return;
            }
        }
        throw new RepositoryException("Column "+columnName+" not found in table "+tableDef.getTableName());
    }
    
    public Iterator<ColumnDefinition> getColumnIterator() {
        return indexableColumns.iterator();
    }

    public boolean isUnique() {
        return isUnique;
    }
    
    public void setUnique(boolean isUnique) {
        this.isUnique = isUnique;
    }

    public void setFullTextSearch(boolean b) {
        fullTextSearch = b;
        
    }

    public boolean isFullTextSearch() {
        return fullTextSearch;
    }

    public void setName(String name) {
        this.name = name;
        
    }

    public String getName() {
        return name;
    }
    
    public TableDefinition getTableDefinition(){
        return tableDef;
    }

}


/*
 * $Log: IndexDefinition.java,v $
 * Revision 1.1  2007/04/26 09:00:52  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/04/17 06:46:40  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.4  2006/04/05 14:30:41  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.3  2006/04/05 09:04:07  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.2  2006/04/04 11:46:11  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.1  2006/03/14 16:08:25  ivgirts
 * PTR #1801059 added creation of indexes for foreign and primary keys
 *
 */
