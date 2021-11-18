/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils.dbtest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.ConnectionProviderImpl;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.objdef.DBObjectDef;

public abstract class DatabaseAbstractTest {

	protected DatabaseConnection conn;
	protected Map<String, String> config;

	public DatabaseAbstractTest(Map<String, String> config) throws RepositoryException {
		this.config = config;
		
	}

	public void test() throws RepositoryException {
		System.out.println("======================== Start Database Test ========================");
		ArrayList<String> errors = new ArrayList<String>();

		testConfiguration(errors);

		if (errors.size() == 0){
			ConnectionProviderImpl cp = new ConnectionProviderImpl();
			cp.configure(config, null);
			conn = cp.createConnection();
		}
		
		if (errors.size() == 0) {
			testPreConditions(errors);
		}

		if (errors.size() == 0){
			List<DBObjectDef> dbObjects = conn.getDialect().getSpecificDBObjectDefs(conn, config);
			for(DBObjectDef dbObject:dbObjects){
				//if (!(dbObject instanceof OracleTextPolicyDef)){
				if (!skipDBObject(dbObject)) {
					try {
						if (!dbObject.checkExists(conn)){
							errors.add("Object "+dbObject.getDescription() +" does not exists");
						}
					} catch (Exception exc){
						errors.add("Error chekcing object "+dbObject.getDescription()+"; "+exc.getMessage());
					}
				}
			}
		}

		if (errors.size() == 0) {
			testPermissions(errors);
		}
		
		
		if (errors.size() > 0) {
			System.err.println("Errors:");
			for (String error : errors) {
				System.err.println("\t" + error);
			}
		} else {
			System.out.println("Done.");
		}
		
		conn.close();
	}


	protected boolean skipDBObject(DBObjectDef dbObject) {
		// TODO Auto-generated method stub
		return false;
	}

	abstract protected void testConfiguration(ArrayList<String> errors);

	abstract protected void testPermissions(ArrayList<String> errors) throws RepositoryException;

	abstract protected void testPreConditions(ArrayList<String> errors) throws RepositoryException;
}
