/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.util.Collection;
import java.util.List;

import com.exigen.cm.database.statements.DatabaseSelectAllStatement;


public class Conditions {

    public static DatabaseCondition eq(String fieldName, Object value) {
        return new EqDatabaseCondition(fieldName, value);
    }

    public static DatabaseCondition eq(DatabaseCondition cond, Object value) {
        return new EqDatabaseCondition(cond, value);
    }

    public static DatabaseCondition notNull(String fieldName) {
        return new NotNullDatabaseCondition(fieldName);
    }

    public static DatabaseCondition isNull(String fieldName) {
        return new IsNullDatabaseCondition(fieldName);
    }

    public static DatabaseCondition eqProperty(String prop1, String prop2) {
        return new EqPropertyDatabaseCondition(prop1, prop2);
    }

    public static DatabaseCondition or(DatabaseCondition c1, DatabaseCondition c2) {
        return or(new DatabaseCondition[]{c1, c2});
    }

    public static DatabaseCondition and(DatabaseCondition c1, DatabaseCondition c2) {
        return new AndDatabaseCondition(c1, c2);
    }

    public static DatabaseCondition and(DatabaseCondition conditions[]) {
        return new AndDatabaseCondition(conditions);
    }

    public static DatabaseCondition and(List<DatabaseCondition> conditions) {
        return new AndDatabaseCondition(conditions);
    }

    public static DatabaseCondition in(String fieldName, DatabaseSelectAllStatement st) {
        return new InDatabaseCondition(fieldName, st, false);
    }

    public static DatabaseCondition exists(DatabaseSelectAllStatement st) {
        return new ExistsDatabaseCondition(st);
    }

    public static DatabaseCondition notExists(DatabaseSelectAllStatement st) {
        return new NotExistsDatabaseCondition(st);
    }

    public static DatabaseCondition notIn(String fieldName, DatabaseSelectAllStatement st) {
        return new InDatabaseCondition(fieldName, st, true);
    }

    public static DatabaseCondition in(String fieldName, String[] values) {
        return new InArrayDatabaseCondition(fieldName, values);    
    }
    
    public static DatabaseCondition in(String fieldName, Collection values) {
        return new InArrayDatabaseCondition(fieldName, values);    
    }

    public static DatabaseCondition in(DatabaseCondition c1, Collection values) {
        return new InArrayDatabaseCondition(c1, values);    
    }

    public static DatabaseCondition not(DatabaseCondition condition) {
        return new NotDatabaseCondition(condition);
    }

    public static DatabaseCondition or(DatabaseCondition[] conditions) {
        return new OrDatabaseCondition(conditions);
    }

    public static DatabaseCondition or(List<DatabaseCondition> conditions) {
        return new OrDatabaseCondition(conditions);
    }

    public static AndDatabaseCondition and() {
        return new AndDatabaseCondition();
    }

    public static DatabaseCondition notEq(DatabaseCondition condition, Object value) {
        return new NotEqDatabaseCondition(condition, value);
    }

    public static DatabaseCondition  max(DatabaseCondition condition) {
        return new MaxDatabaseCondition(condition);
    }

    public static CaseDatabaseCondition caseCondition(DatabaseCondition caseWhen1, String when1,String caseElse) {
        return new CaseDatabaseCondition(caseWhen1, when1, caseElse);
    }

    public static DatabaseCondition gt(DatabaseCondition condition, Object value) {
        return new GtDatabaseCondition(condition, value);
    }

    public static DatabaseCondition gte(DatabaseCondition condition, Object value) {
        return new GteDatabaseCondition(condition, value);
    }

    public static DatabaseCondition gte(String fieldName, Object value) {
        return gte(new FieldNameDatabaseCondition(fieldName), value);
    }
    
    public static DatabaseCondition gt(String fieldName, Object value) {
        return gt(new FieldNameDatabaseCondition(fieldName), value);
    }
    
    
    public static StoredProcedureDatabaseCondition storedProcedure(String procedureName) {
        return new StoredProcedureDatabaseCondition(procedureName);
    }

    public static DatabaseCondition lt(FieldNameDatabaseCondition condition, Object value) {
        return new LtDatabaseCondition(condition, value);
    }

    public static DatabaseCondition lte(FieldNameDatabaseCondition condition, Object value) {
        return new LteDatabaseCondition(condition, value);
    }

    public static DatabaseCondition lt(String fieldName, Object value) {
        return lt(new FieldNameDatabaseCondition(fieldName), value);
    }

    public static DatabaseCondition lte(String fieldName, Object value) {
        return lte(new FieldNameDatabaseCondition(fieldName), value);
    }

    public static DatabaseCondition like(String fieldName, String value, LikeType likeType, boolean caseSensetive) {
        return new LikeDatabaseCondition(fieldName, value, likeType, caseSensetive);
    }

}


/*
 * $Log: Conditions.java,v $
 * Revision 1.5  2009/02/23 14:30:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2008/08/28 09:56:09  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/06/26 09:07:03  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2008/04/30 09:28:50  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:51  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.7  2007/03/27 11:20:55  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.6  2006/08/15 08:23:15  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.5  2006/07/04 09:27:24  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.4  2006/07/03 11:45:36  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/06/30 10:34:30  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/04/20 08:20:35  dparhomenko
 * PTR#0144983 stored procedure for Hypersonic check security
 *
 * Revision 1.1  2006/04/17 06:46:39  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.10  2006/04/12 12:49:00  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.9  2006/03/31 13:41:20  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.8  2006/03/27 14:27:25  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.7  2006/03/27 07:22:19  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.6  2006/03/22 11:18:53  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.5  2006/03/21 13:19:25  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.4  2006/03/16 13:13:06  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.3  2006/03/03 10:33:16  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.2  2006/03/01 11:54:44  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.1  2006/02/13 12:40:46  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */