/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;


public class CaseDatabaseCondition extends DatabaseCondition {

    ArrayList<CaseWhen> whens = new ArrayList<CaseWhen>();
    
    String elseValue;
    
    public CaseDatabaseCondition(DatabaseCondition caseWhen1, String when1, String caseElse) {
        whens.add(new CaseWhen(caseWhen1, when1));
        elseValue = caseElse;
    }

    public void addWhen(DatabaseCondition caseWhen2, String thenValue) {
        whens.add(new CaseWhen(caseWhen2, thenValue));
    }

    public StringBuffer createSQLPart(String alias, DatabaseConnection conn) throws RepositoryException {
        StringBuffer sb = new StringBuffer();
        
        sb.append(" CASE ");
        
        for(Iterator it = whens.iterator();it.hasNext();){
            CaseWhen w = (CaseWhen) it.next();
            sb.append(" WHEN ");
            sb.append(w.caseWhen.createSQLPart(alias, conn));
            sb.append(" THEN ");
            sb.append(w.thenCondition);
        }
        
        if (elseValue != null){
            sb.append(" ELSE ");
            sb.append(elseValue);
        }
        sb.append(" END ");
        
        return sb;
    }

    public int bindParameters(int pos, DatabaseConnection conn, PreparedStatement st) throws RepositoryException {
        int total = 0;
        for(Iterator it = whens.iterator();it.hasNext();){
            CaseWhen w = (CaseWhen) it.next();
            total += w.caseWhen.bindParameters(pos+total, conn, st);
        }
        return total;
    }

    class CaseWhen{

        private DatabaseCondition caseWhen;
        private String thenCondition;

        public CaseWhen(DatabaseCondition caseWhen1, String when1) {
            this.caseWhen = caseWhen1;
            this.thenCondition = when1;
        }
        
    }

    @Override
    public String getLocalName(){
    	throw new UnsupportedOperationException();
    }

    
}




/*
 * $Log: CaseDatabaseCondition.java,v $
 * Revision 1.1  2007/04/26 08:59:51  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2007/01/24 08:46:26  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.3  2006/06/30 10:34:30  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/05/10 09:00:39  dparhomenko
 * PTR#0144983 dynamic table names
 *
 * Revision 1.1  2006/04/17 06:46:39  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/05 14:30:49  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.1  2006/03/27 07:22:19  dparhomenko
 * PTR#0144983 optimization
 *
 */