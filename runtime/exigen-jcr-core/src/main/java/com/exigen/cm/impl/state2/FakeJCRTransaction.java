/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.transaction.JCRTransaction;

public class FakeJCRTransaction implements JCRTransaction{

	private DatabaseConnection conn;
	private _NodeState state;

	public FakeJCRTransaction(DatabaseConnection conn, _NodeState state) {
		this.conn = conn;
		this.state = state;
	}
	
	public void reset(){
			state.setCreatedInTransaction(null);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof FakeJCRTransaction) ){
			return false;
		}
		return conn.equals( ((FakeJCRTransaction)obj).conn  );
	}

    public boolean allowConnectionClose() {
        return true;
    }
	
}
