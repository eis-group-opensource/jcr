/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.params;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

import com.exigen.cm.database.dialect.DatabaseDialect;

class CalendarSQLParameter extends SQLParameter{

    private Calendar value;

    public CalendarSQLParameter(String name, Calendar value) {
        super(name, Types.DATE, value);
        this.value = value;
    }

    public int _apply(int pos, PreparedStatement st, DatabaseDialect dialect) throws SQLException {
        //TODO ???
        Timestamp ts = new Timestamp(value.getTimeInMillis());
        //st.setTimestamp(pos, new Timestamp(value.getTimeInMillis()));
        //st.setDate()
        st.setTimestamp(pos, ts);
        //st.setTime(pos, new Time(value.getTimeInMillis()), value);
        return 1;
        
    }

    protected boolean isEmpty() {
        return value == null;
    }
     
    protected int getSQLType(DatabaseDialect dialect) {
        return dialect.getColumnTypeTimeStampSQLType();
    }
    
}

