/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.security;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;

public interface SecurityConditionFilter {

    public void addSecurityConditions(DatabaseConnection conn, DatabaseSelectAllStatement st, boolean allowBrowse) throws RepositoryException;

}
