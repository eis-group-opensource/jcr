/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.params;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.StorableInputStream;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.impl.state2.StoreContainer;
import com.exigen.cm.jackrabbit.value.BLOBFileValue;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.cm.store.ContentStore;

class StreamSQLParameter extends SQLParameter{

    private InputStream value;
    //private _SessionImpl session;
    //private InternalValue internalValue;
    private String id;

    public StreamSQLParameter(StoreContainer sc, String name, InputStream value, InternalValue internalValue) throws RepositoryException {
        super(name, Types.VARCHAR, value);
        this.value = value;
        //this.session = session;
        //this.internalValue = internalValue;
        String storeId = null;
        Map storeProperties = null;
        if (value instanceof StorableInputStream){
            StorableInputStream sIn = (StorableInputStream) value;
            storeId = sIn.getStoreName();
            storeProperties = sIn.getStoreProperties();
        }
        if (storeId == null){
        	storeId = ((BLOBFileValue) internalValue.internalValue()).getStoreId();
        }
        ContentStore store = sc.getContentStore(storeId);
        if (!store.isTransactionStarted()){
            store.begin(sc.getConnection());
        }

        /*
         * Store will return Longs as Content ID due to content IDs mapping introduction. 
         */
        Long jcrId = store.put(value, ((BLOBFileValue) internalValue.internalValue()).getLength(), storeProperties);
        id = jcrId.toString();
        if (internalValue != null){
            if (storeId != null){
                id = storeId + Constants.STORE_DELIMITER + id;
            }
        }
        internalValue.setContentId(id);
    }

    public int _apply(int pos, PreparedStatement st, DatabaseDialect dialect) throws SQLException {
        st.setString(pos, dialect.convertStringToSQL(id));
        return 1;
    }

    protected boolean isEmpty() {
        return value == null;
    }

    protected int getSQLType(DatabaseDialect dialect) {
        throw new UnsupportedOperationException();
    }
    
}
