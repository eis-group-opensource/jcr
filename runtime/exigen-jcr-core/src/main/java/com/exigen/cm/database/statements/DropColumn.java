/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.database.DatabaseConnection;

public class DropColumn implements SchemaOperation{

	private String columnName;
	private String tableName;

	public DropColumn(String tableName, String columnName) {
		this.tableName = tableName;
		this.columnName = columnName;
	}

	public void execute(DatabaseConnection conn) throws RepositoryException {
		String sql = conn.getDialect().buildAlterTableDropColumn(tableName, columnName);
        conn.execute(sql);		
	}

	@Override
	public String toString() {
		ToStringBuilder b = new ToStringBuilder(this);
		b.append("table",tableName);
		b.append("column", columnName);
		return b.toString();
	}

	
}
