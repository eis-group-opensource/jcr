/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.params;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseDialect;


public class StreamSQLParameter2 extends SQLParameter{

    private InputStream value;
    private int length;

    public StreamSQLParameter2(String name, InputStream value, int length) throws RepositoryException {
        super(name, Types.BLOB, value);
        
//        if(length < 0){ // no length provided
//            try{
//                MeasuredInputStream tmp =  StoreHelper.createMeasuredInputStream(value);
//                this.value=tmp;
//                this.length = (int)tmp.getLength();
//            }catch(Exception ex){
//                throw new RepositoryException("Cannot get stream parameter length.", ex);
//            }
//        }else{
            this.value = value;
            this.length = length;
//        }
    }

    public int _apply(int pos, PreparedStatement st, DatabaseDialect dialect) throws SQLException {
		if (dialect.getDatabaseVendor().equalsIgnoreCase(DatabaseDialect.VENDOR_ORACLE)){
			return 0;
		} else {
	        st.setBinaryStream(pos, value, length);
	        return 1;
		}
    }
    
    @Override
	public void registerParameter(DatabaseConnection conn, StringBuffer sb) throws RepositoryException {
		if (conn.getDialect().getDatabaseVendor().equalsIgnoreCase(DatabaseDialect.VENDOR_ORACLE)){
			sb.append("EMPTY_BLOB()");
		} else {
			sb.append("?");
		}
		
	}

    protected boolean isEmpty() {
        return value == null;
    }

    protected int getSQLType(DatabaseDialect dialect) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets number of bytes written in database.
     * @param size
     */
    public void setWrittenBytesCount(int size) {
        length=size;
    }

    /**
     * Returns number of bytes written in database.
     * @return
     */
    public int getWrittenBytesCount(){
        return length;
    }
}
