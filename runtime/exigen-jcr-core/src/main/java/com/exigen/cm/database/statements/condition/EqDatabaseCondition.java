/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.sql.PreparedStatement;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;


public class EqDatabaseCondition extends DatabaseCondition {

    private String fieldName;
    private Object value;
    private DatabaseCondition cond;

    public EqDatabaseCondition(String fieldName, Object value) {
        this.fieldName = fieldName;
        this.value = value;
    }

    public EqDatabaseCondition(DatabaseCondition cond, Object value) {
        this.cond = cond;
        this.value = value;
    }

    public StringBuffer createSQLPart(String alias,DatabaseConnection conn) throws RepositoryException {
        StringBuffer result = new StringBuffer();
        if (cond != null){
            result.append(cond.createSQLPart(alias, conn));
        } else {
            result.append(createFieldName(alias, fieldName));
            
        }
        result.append(" = ?");
        return result;
    }

    @Override
    public String getLocalName(){
    	return fieldName;
    }

    

    public int bindParameters(int pos, DatabaseConnection conn, PreparedStatement st) throws RepositoryException {
        int total = 0;
        if (cond != null){
            total = cond.bindParameters(pos, conn, st);
        }
        DatabaseTools.bindParameter(st, conn.getDialect(), pos+total, value, isPureMode());
        return total+1;
    }

	@Override
	public String toString() {
		ToStringBuilder b = new ToStringBuilder(this);
		if (cond != null){
            b.append("condition",cond);
        } else {
            b.append("field",fieldName);
            
        }
		b.append("value",value);
		return b.toString();
	}

}


/*
 * $Log: EqDatabaseCondition.java,v $
 * Revision 1.3  2009/01/26 10:53:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2007/08/08 07:45:30  dparhomenko
 * PTR#1805084 fix ptr
 *
 * Revision 1.1  2007/04/26 08:59:51  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2007/01/24 08:46:26  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.4  2006/06/30 10:34:30  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/05/10 09:00:39  dparhomenko
 * PTR#0144983 dynamic table names
 *
 * Revision 1.2  2006/04/20 08:20:35  dparhomenko
 * PTR#0144983 stored procedure for Hypersonic check security
 *
 * Revision 1.1  2006/04/17 06:46:39  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/03/14 11:55:36  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.1  2006/02/13 12:40:46  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */