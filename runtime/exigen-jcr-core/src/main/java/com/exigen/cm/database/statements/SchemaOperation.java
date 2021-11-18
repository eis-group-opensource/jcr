/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;

public interface SchemaOperation extends DatabaseOperation {

	/**
	 * Executes operation
	 * @param conn
	 *            Connection
	 * @throws RepositoryException
	 *             Exception
	 */
	void execute(DatabaseConnection conn) throws RepositoryException;

}
