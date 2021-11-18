/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.params;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseDialect;

public class SQLEqParameter extends SQLParameter {

	private String value;


	public SQLEqParameter(String name, String value) {
		super(name, Types.VARCHAR, value);
		this.value = value;
	}

	@Override
	public int _apply(int pos, PreparedStatement st, DatabaseDialect dialect)
			throws SQLException {
		return 0;
	}

	@Override
	protected int getSQLType(DatabaseDialect dialect) {
		// TODO Auto-generated method stub
		return Types.VARCHAR;
	}

	@Override
	protected boolean isEmpty() {
		return false;
	}

	
	public void registerParameter(DatabaseConnection conn, StringBuffer sb) throws RepositoryException {
		sb.append(value);
		
	}
}
