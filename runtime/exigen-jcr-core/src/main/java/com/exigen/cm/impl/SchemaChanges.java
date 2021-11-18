/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.ArrayList;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.statements.DatabaseOperation;
import com.exigen.cm.database.statements.DatabaseStatement;
import com.exigen.cm.database.statements.SchemaOperation;

public class SchemaChanges {

	private ArrayList<DatabaseOperation> operations = new ArrayList<DatabaseOperation>();
	//private DatabaseConnection conn;
	
	public SchemaChanges(DatabaseConnection conn) {
		//this.conn = conn;
	}

	public void add(DatabaseOperation operation){
		operations.add(operation);
	}
	
	public void execute(DatabaseConnection conn) throws RepositoryException{
		for(DatabaseOperation operation:operations){
			if (operation instanceof DatabaseStatement){
				((DatabaseStatement) operation).execute(conn);
			} else if (operation instanceof SchemaOperation){
				((SchemaOperation)operation).execute(conn);
			} else {
				throw new UnsupportedRepositoryOperationException("Unknown DatabaseOperation");
			}
		}
	}
	
	
}
