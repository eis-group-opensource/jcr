/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.params;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import com.exigen.cm.database.dialect.DatabaseDialect;

class LongSQLParameter extends SQLParameter{

    private Long value;

    public LongSQLParameter(String name, Long value) {
        super(name, Types.INTEGER, value);
        this.value = value;
    }

    public int _apply(int pos, PreparedStatement st, DatabaseDialect dialect) throws SQLException {
        st.setLong(pos, value.longValue());
        return 1;
    }
    
    protected boolean isEmpty() {
        return value == null;
    }
    
    protected int getSQLType(DatabaseDialect dialect) {
        return dialect.getColumnTypeLongSQLType();
    }


}
