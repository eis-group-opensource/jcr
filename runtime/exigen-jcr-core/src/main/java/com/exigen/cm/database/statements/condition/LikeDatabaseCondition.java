/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.sql.PreparedStatement;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;


public class LikeDatabaseCondition extends DatabaseCondition {

    private String fieldName;
    private String value;
    private LikeType likeType;
    private boolean caseSensetive;

    public LikeDatabaseCondition(String fieldName, String value, LikeType likeType, boolean caseSensetive){
        this.fieldName = fieldName;
        this.value = value;
        this.likeType = likeType;
        
        this.caseSensetive = caseSensetive;
        if (likeType ==LikeType.After){
            this.value = value+"%";
        } else if (likeType ==LikeType.Before) {
            this.value="%"+value;
        }else if (likeType ==LikeType.Any) {
            this.value="%"+value+"%";
        } else {
            throw new UnsupportedOperationException("unsupported LikeType "+likeType);
        }
    }


    public StringBuffer createSQLPart(String alias,DatabaseConnection conn) throws RepositoryException {
        StringBuffer result = new StringBuffer();

        if (caseSensetive){
            result.append(createFieldName(alias, fieldName));
            result.append(" LIKE ?");
        } else {
            result.append("upper(");
            result.append(createFieldName(alias, fieldName));
            result.append(") LIKE upper(?)");
        }
        return result;
    }

    @Override
    public String getLocalName(){
    	return fieldName;
    }

    

    public int bindParameters(int pos, DatabaseConnection conn, PreparedStatement st) throws RepositoryException {
        int total = 0;
        DatabaseTools.bindParameter(st, conn.getDialect(), pos+total, value, isPureMode());
        return total+1;
    }

	@Override
	public String toString() {
		ToStringBuilder b = new ToStringBuilder(this);
        b.append("field",fieldName);
        b.append("value",value);
        b.append("likeType",likeType);
		return b.toString();
	}

}


/*
 * $Log: LikeDatabaseCondition.java,v $
 * Revision 1.2  2009/01/26 10:53:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2008/06/26 09:07:03  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2007/08/08 07:45:30  dparhomenko
 * PTR#1805084 fix ptr
 
 */