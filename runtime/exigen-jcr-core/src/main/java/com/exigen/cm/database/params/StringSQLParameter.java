/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.params;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import com.exigen.cm.database.dialect.DatabaseDialect;

/**
 * String SQL Parameter
 */
class StringSQLParameter extends SQLParameter {

	private String value;

	/**
	 * 
	 * @param name Parameter name
	 * @param value Parameter value
	 */
	public StringSQLParameter(final String name, final String value) {
		super(name, Types.VARCHAR, value);
		this.value = value;
	}

	public int _apply(final int pos, final PreparedStatement st, final DatabaseDialect dialect) throws SQLException {
		st.setString(pos, dialect.convertStringToSQL(value));
		return 1;
	}

	protected boolean isEmpty() {
		return value == null;
	}

	protected int getSQLType(final DatabaseDialect dialect) {
		return dialect.getColumnTypeStringSQLType();
	}

}
