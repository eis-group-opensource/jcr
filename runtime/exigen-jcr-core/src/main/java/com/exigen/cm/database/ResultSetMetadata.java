/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jcr.RepositoryException;

public class ResultSetMetadata {

	private ResultSetMetaData metaData;
	private boolean ignoreBLOB;
	//private HashMap<String, String> tableNameMapping;
	
	private ArrayList<ColumnMetadata> columns = new ArrayList<ColumnMetadata>();
	private int columnCount;
	private boolean pureMode;

	public ResultSetMetadata(ResultSet rs, boolean ignoreBLOB, HashMap<String,String> tableNameMapping, boolean pureMode) throws RepositoryException{
		try {
			this.pureMode = pureMode;
			this.metaData = rs.getMetaData();
			this.ignoreBLOB = ignoreBLOB;
			//this.tableNameMapping = tableNameMapping;
			for (int i = 0 ; i < metaData.getColumnCount() ; i++){
				String columnName = metaData.getColumnName(i+1);
				int columnType = metaData.getColumnType(i+1);
				metaData.getCatalogName(i+1);
				String label = metaData.getColumnLabel(i+1);
				if(columnType == Types.CLOB || ignoreBLOB && columnType == Types.BLOB)
                    continue;
                
                columnName = DatabaseTools.getUpperCase(columnName);
                String _columnName = columnName;
                
                for(String prefix:tableNameMapping.values()){
                	if (columnName.startsWith(prefix+"_")){
                		_columnName = prefix+"."+_columnName.substring(prefix.length()+1);
                	}
                }
                int columnLength = columnType == Types.BLOB ? 0:metaData.getPrecision(i+1);
                columns.add(new ColumnMetadata(_columnName, columnLength, columnType));
			}
			this.columnCount = columns.size();
		} catch (SQLException e) {
			throw new RepositoryException("Error reading metadata",e);
		}
	}

	public boolean isPureMode() {
		return pureMode;
	}

	public int getColumnCount() {
		return columnCount;
	}

	public boolean isIgnoreBLOB() {
		return ignoreBLOB;
	}

	public ColumnMetadata getColumn(int i) {
		return columns.get(i);
	}
	
}
