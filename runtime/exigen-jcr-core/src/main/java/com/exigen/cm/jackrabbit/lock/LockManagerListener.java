/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.lock;

import java.util.Map;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.RowMap;

public interface LockManagerListener {

	void internalSetParentLockId(DatabaseConnection conn, DatabaseUpdateStatement updateStatement, Map<String, Object> options, Long lockId);

	void internalSetParentDeepLockId(DatabaseConnection conn, DatabaseUpdateStatement updateStatement, Map<String, Object> options, Long lockId);

	void collectOptions(RowMap row,Map<String, Object> options);

	void addResultColumns(DatabaseSelectAllStatement st);

}
