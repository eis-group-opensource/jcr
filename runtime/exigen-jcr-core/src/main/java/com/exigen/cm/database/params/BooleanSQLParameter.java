/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.params;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import com.exigen.cm.database.dialect.DatabaseDialect;

public class BooleanSQLParameter extends SQLParameter{

    private Boolean value;

    public BooleanSQLParameter(String name, Boolean value) {
        super(name, Types.BOOLEAN, value);
        this.value = value;
    }

    public int _apply(int pos, PreparedStatement st, DatabaseDialect dialect) throws SQLException {
        st.setBoolean(pos, value.booleanValue());
        return 1;
    }

    protected boolean isEmpty() {
        return value == null;
    }
    
    protected int getSQLType(DatabaseDialect dialect) {
        return dialect.getColumnTypeBooleanSQLType();
    }



}
