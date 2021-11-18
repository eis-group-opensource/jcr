/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;

public class CreateColumn implements SchemaOperation{

	private ColumnDefinition column;
	private String tableName;

	public CreateColumn(String tableName, ColumnDefinition column) {
		this.tableName = tableName;
		this.column = column;
	}

	public void execute(DatabaseConnection conn) throws RepositoryException {
		String sql = conn.getDialect().buildAlterTableStatement(tableName, new ColumnDefinition[]{column});
        conn.execute(sql);		
	}

	@Override
	public String toString() {
		ToStringBuilder b = new ToStringBuilder(this);
		b.append("table",tableName);
		b.append("column", column.getColumnName());
		return b.toString();
	}
	
	
}
