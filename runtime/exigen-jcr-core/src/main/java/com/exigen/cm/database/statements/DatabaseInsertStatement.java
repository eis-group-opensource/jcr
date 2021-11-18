/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.params.StreamSQLParameter2;

public class DatabaseInsertStatement extends AbstractDatabaseStatement implements ValueChangeDatabaseStatement{

    private ArrayList<SQLParameter> columns = new ArrayList<SQLParameter>(); 
    
    private ArrayList<ArrayList<SQLParameter>> batches = new ArrayList<ArrayList<SQLParameter>>(); 
    
    public DatabaseInsertStatement(String tableName) {
        super(tableName);
    }
    
    public ArrayList<SQLParameter> getValues(){
    	return columns;
    }
    
    public boolean executeBatch(DatabaseConnection conn) throws RepositoryException{
    	if (batches.size() == 0){
    		return true;
    	}
    	return super.execute(conn);
    }

    public void addBatch(){
		Collections.sort(columns);
    	batches.add(columns);
    	columns = new ArrayList<SQLParameter>();
    }
    
    
	public void addBatch(DatabaseInsertStatement ins) {
    	batches.add(ins.columns);
    	//columns = new ArrayList<SQLParameter>();
	}

    
    public String assemblSQL(DatabaseConnection conn) throws RepositoryException {
    	
    	if (batches.size() > 0){
    		int initSize = batches.get(0).size();
    		for(List<SQLParameter> pp:batches){
    			//Collections.sort(pp);
    			if (pp.size() != initSize){
    				throw new RepositoryException("Batches has different parameter count");
    			}
    		}
    		columns = batches.get(0);
    	}
    	
        //INSERT INTO TABLE (1,2,3) VALUES (?,?,?,?) 
        StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO ");
        sb.append(getTableName(conn.getDialect()));
        sb.append(" (" );
        for(Iterator<SQLParameter> it = columns.iterator() ; it.hasNext() ;){
            SQLParameter p = it.next();
            sb.append(p.getName());
            if (it.hasNext()){
                sb.append(",");
            }
        }
        sb.append(" ) VALUES (");
        //add params
        for(Iterator<SQLParameter> it = columns.iterator() ; it.hasNext() ;){
        	SQLParameter column = it.next();
        	column.registerParameter(conn, sb);
            //sb.append("?");
            if (it.hasNext()){
                sb.append(",");
            }
        }
        sb.append(")" );
        return sb.toString();
    }

    public void addValue(SQLParameter p) {
        columns.add(p);
    }


    
    public int applyParameters(PreparedStatement st,DatabaseConnection conn,int startPos) throws RepositoryException {
    	DatabaseDialect dialect = conn.getDialect();
        try {
	    	if (batches.size() == 0){
		        int pos = startPos;
	            for(SQLParameter p:columns){
	                pos+=p.apply(pos, st, dialect);
	            }
	            return columns.size();
	    	} else {
	    		for(List<SQLParameter> pp:batches){
			        int pos = startPos;
		            for(SQLParameter p:pp){
		                pos+=p.apply(pos, st, dialect);
		            }
		            st.addBatch();
	    		}
	    		return batches.get(0).size();
	    	}
        } catch (SQLException exc){
        	exc.printStackTrace();
            throw new RepositoryException("Error setting SQl parameter", exc);
        }

        
    }

    protected boolean isAutoCloseStatement() {
        return true;
    }

    public void setFromRow(RowMap row, String[] excludes) throws RepositoryException {
        List excludesList = Arrays.asList(excludes);
        for(Iterator<String> it = row.keySet().iterator(); it.hasNext();){
            String key  = it.next();
            if (!excludesList.contains(key)){
                addValue(SQLParameter._create(null, key, row.get(key), null));
            }
        }
        
    }

	@Override
	protected boolean _executeStatement(PreparedStatement st, DatabaseConnection conn) throws SQLException, RepositoryException {
		
		boolean result = false;
		if (batches.size() == 0){
			result = super._executeStatement(st, conn);
		} else {
			st.executeBatch();
			return true;
		}
		if (conn.getDialect().getDatabaseVendor().equalsIgnoreCase(DatabaseDialect.VENDOR_ORACLE)){
			//check if params contins StreamParam
			ArrayList<SQLParameter> streamParams = new ArrayList<SQLParameter>();
			SQLParameter id = null;
			for(SQLParameter p:columns){
				if (p instanceof StreamSQLParameter2){
					streamParams.add(p);
				}
				if (p.getName().equals(Constants.FIELD_ID)){
					id = p;
				}
			}
			if (streamParams.size() > 0){
				//process after update
				
				StringBuffer SELECT = new StringBuffer("SELECT ");
				boolean first = true;
				for(SQLParameter p:streamParams){
					if (!first){
						SELECT.append(",");
					}
					first = false;
					SELECT.append(p.getName());
				}
				
				
				
				SELECT.append(" FROM ")
		        .append(getTableName(conn.getDialect()))
		        .append(" WHERE ")
		        .append(Constants.FIELD_ID).append("=? FOR UPDATE");
				PreparedStatement __st = conn.prepareStatement(SELECT.toString(), true);
				__st.setLong(1, (Long)id.getValue());
				__st.execute();
				ResultSet rs = __st.getResultSet();
				rs.next();
				
				for(SQLParameter p:streamParams){
		        	Blob b = rs.getBlob(p.getName());
		        	conn.getDialect().populateBlobData(b, (InputStream)p.getValue());
			        	

				}
			}
		}
		return result;
		
		
	}

    protected HashMap<String, String> buildTableAliasMapping(){
    	return new HashMap<String,String>();
    }

	public boolean allowParams(DatabaseInsertStatement ins) {
		ArrayList<SQLParameter> cc1 = batches.get(0);
		ArrayList<SQLParameter> cc2 = ins.columns;
		if (cc1.size() != cc2.size()){
			return false;
		}
		Collections.sort(cc2);
		for(int i = 0 ; i < cc1.size() ; i++){
			SQLParameter p1 = cc1.get(i);
			SQLParameter p2 = cc2.get(i);
			if (!p1._equals(p2)){
				return false;
			}
		}
		return true;
	}


}


/*
 * $Log: DatabaseInsertStatement.java,v $
 * Revision 1.2  2007/06/15 09:46:52  dparhomenko
 * PTR#0152003 fix insert statemnt for oracle blobs
 *
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.14  2007/03/29 14:16:12  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.13  2006/09/28 12:23:34  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.12  2006/09/27 12:33:00  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.11  2006/09/22 09:23:59  vpukis
 * PTR#1801827  change in _execute() to cache SQL statement (otherwise ORA-01000: maximum open cursors exceeded)
 *
 * Revision 1.10  2006/08/11 09:04:43  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.9  2006/07/12 14:45:12  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.8  2006/07/12 10:10:25  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.7  2006/07/11 10:26:11  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.6  2006/07/06 09:29:29  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.5  2006/07/06 07:54:39  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.4  2006/06/02 07:21:41  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.3  2006/05/10 09:00:42  dparhomenko
 * PTR#0144983 dynamic table names
 *
 * Revision 1.2  2006/05/03 12:07:08  dparhomenko
 * PTR#0144983 make DatabaseStatement as interface
 *
 * Revision 1.1  2006/04/17 06:47:12  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.4  2006/04/05 14:30:42  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.3  2006/03/01 11:54:42  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.2  2006/02/13 12:40:55  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 * Revision 1.1  2006/02/10 15:50:33  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */