/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database;

import java.io.InputStream;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.dialect.AbstractDatabaseDialect;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseCallableStatement;
import com.exigen.cm.database.statements.DatabaseCountStatement;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.DatabaseStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.store.StoreHelper;
import com.exigen.vf.commons.logging.LogUtils;

public class DatabaseTools {

    /** Log for this class */
    private static final Log log = LogFactory.getLog(DatabaseTools.class);
    

    /**
     * Returns map of {@link TableDefinition}, that represents database table state.
     * The key and object, that is mapped to the key, is the same instance of {@link TableDefinition}.   
     * @param conn
     * @return list of {@link TableDefinition}
     * @throws SQLException
     * @throws RepositoryException
     */
    public static Map<String, TableDefinition> getDatabaseTables(DatabaseConnection conn) throws RepositoryException{
        DatabaseMetaData dbMD = conn.getConnectionMetaData();
        ResultSet result = null;       
        Map<String, TableDefinition> dbTables = new HashMap<String, TableDefinition>();
        try {
        	String catalog = conn.getCatalog();
        	ResultSet rs = dbMD.getCatalogs();
        	while (rs.next()){
        		String s = rs.getString(1);
        		if (s.equalsIgnoreCase(catalog)){
        			catalog = s;
        			break;
        		}
        	}
        	rs.close();
        	
        	String schemaName = conn.getDialect().getSchemaName(conn);
        	rs = dbMD.getSchemas();
        	while (rs.next()){
        		String s = rs.getString(1);
        		if (s.equalsIgnoreCase(schemaName)){
        			catalog = s;
        			break;
        		}
        	}
        	rs.close();
        	
            result = dbMD.getColumns(catalog, schemaName,  "%" , null);            
            TableDefinition table = new TableDefinition(null);
            while (result.next()){
                String tableName = result.getString("TABLE_NAME");
                /*for(int i = 1 ; i <= result.getMetaData().getColumnCount(); i++){
                	String name =  result.getMetaData().getColumnName(i);
                	
                }*/
                String _schemaName = conn.getDialect().getSchemaName(conn);
                if (conn.getDialect().getDatabaseVendor().equals(AbstractDatabaseDialect.VENDOR_MSSQL)
                		&& _schemaName != null){
                	tableName = _schemaName+"."+tableName;
                }
                
                if (tableName.indexOf('$') < 0){
	                if (!tableName.equals(table.getTableName())) {
	                	if (dbTables.containsKey(tableName)){
	                		table = dbTables.get(tableName);
	                	} else {
		                    table = new TableDefinition(tableName);
		                    dbTables.put(table.getTableName(), table);
	                	}
	                }   
	                
	                String columnName = result.getString("COLUMN_NAME");                
	                int type = result.getInt("DATA_TYPE");
	                ColumnDefinition columnDef = new ColumnDefinition(table, columnName, type);
	                table.addColumn(columnDef);
                }
                              
            }
        }catch (Exception e) {
            throw new RepositoryException(e);
        } finally {
            DatabaseTools.closeResultSet(result);
        }
        return dbTables;
    }


    public static boolean validateTable(DatabaseConnection conn, TableDefinition tDef){
        ResultSet result = null;
        try{
            DatabaseMetaData dbMD = conn.getConnectionMetaData();
            result = dbMD.getColumns(null, conn.getDialect().getSchemaName(conn), extractTableName(tDef.getTableName(), conn) , null);
            Set<ColumnDefinition> columns = new HashSet<ColumnDefinition>();
            while(result.next()){
                String columnName = result.getString("COLUMN_NAME");                
                int type = result.getInt("DATA_TYPE");
                ColumnDefinition columnDef = new ColumnDefinition(tDef, columnName, type);
                columns.add(columnDef);
            }
            
            Iterator<ColumnDefinition> columnIterator = tDef.getColumnIterator();
            while(columnIterator.hasNext())
                if(!columns.contains(columnIterator.next())){
                    return false;
                }
            
            return true;
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to validate table {0}", tDef.getTableName());
            log.error(message, ex);
        } finally {
            DatabaseTools.closeResultSet(result);
        }
        
        return false;
    }
    
    private static String extractTableName(String tableName, DatabaseConnection conn) throws RepositoryException {
		return conn.getDialect().extractTableName(tableName);
		
	}


	public static void closeResultSet(ResultSet resultSet) {
        try {
        	if (resultSet != null){
        		resultSet.close();
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    public static void executeStatements(List<DatabaseStatement> statements, DatabaseConnection conn) throws RepositoryException {
    	
    	
        ArrayList<DatabaseStatement> removedSt = new ArrayList<DatabaseStatement>();
        HashMap<String,ArrayList<DatabaseInsertStatement>> inserts = new HashMap<String, ArrayList<DatabaseInsertStatement>>();
        ArrayList<ArrayList<DatabaseInsertStatement>> _inserts = new ArrayList<ArrayList<DatabaseInsertStatement>>();
        DatabaseDialect dialect = conn.getDialect();
        HashSet<String> deletes = new HashSet<String>();
        for(DatabaseStatement st:statements){
        	if (st instanceof DatabaseDeleteStatement){
        		DatabaseDeleteStatement del = (DatabaseDeleteStatement) st;
        		String tableName = del.getTableName(dialect);
        		deletes.add(tableName);
        	}
        }
        
        for(DatabaseStatement st:statements){
        	if (st instanceof DatabaseInsertStatement){
        		DatabaseInsertStatement ins = (DatabaseInsertStatement) st;
        		String tableName = ins.getTableName(dialect);
        		if (deletes.contains(tableName)){
        			continue;
        		}
        		ArrayList<DatabaseInsertStatement> sts = inserts.get(tableName);
        		if (sts == null){
        			sts = new ArrayList<DatabaseInsertStatement>();
        			inserts.put(tableName, sts);
        			_inserts.add(sts);
        		}
        		boolean founded  = false;
        		for(DatabaseInsertStatement batch:sts){
        			if (batch.allowParams(ins)){
        				batch.addBatch(ins);
        				founded = true;
        				break;
        			}
        		}
        		if (!founded){
        			sts.add(ins);
        			ins.addBatch();
        		}
        		
        		removedSt.add(st);
        	}
        }
        statements.removeAll(removedSt);
    	
        for (ArrayList<DatabaseInsertStatement> sts: _inserts){
        	for(DatabaseInsertStatement batch:sts){
        		batch.executeBatch(conn);
        	}
        }
    	
        for (DatabaseStatement st: statements){
            st.execute(conn);
        }
        
    }

    public static void bindParameter(PreparedStatement st, DatabaseDialect dialect, int pos, Object value, boolean pureMode) throws RepositoryException {
        try {
            if (SQLParameter.getLog().isDebugEnabled()){
                LogUtils.debug(SQLParameter.getLog(), "Set parameter {0} to {1}", new Integer(pos), value);
            }
            if (value == null){
                st.setNull(pos, Types.VARCHAR);
            } else if (value instanceof String){
                st.setString(pos, pureMode ? (String)value:dialect.convertStringToSQL((String) value));
            } else if (value instanceof Integer){
                st.setInt(pos, ((Integer) value).intValue());
            } else if (value instanceof Long){
                st.setLong(pos, ((Long) value).longValue());
            } else if (value instanceof Boolean){
                st.setBoolean(pos, ((Boolean) value).booleanValue());
            } else if (value instanceof Calendar){
                st.setTimestamp(pos, new Timestamp(((Calendar)value).getTimeInMillis()));
            } else {
                throw new RepositoryException("Unsupported sql value type for "+value.getClass());
            }
        } catch (SQLException exc){
            throw new RepositoryException("Error setting SQl parameter", exc);
        }
    }

    public static Object getValueFromResultSet(ResultSet resultSet, DatabaseDialect dialect, int columnPos, int columnType, 
    		int columnLength, String columnName, boolean pureMode) throws SQLException, RepositoryException {
        /*Object value = resultSet.getObject(columnName); 
        if (value == null){
            return null;
        }*/
        Object result = null;
        if (columnType == Types.VARCHAR){
        	result = resultSet.getString(columnPos);
        	if (!pureMode){
        		result = dialect.convertStringFromSQL((String) result, columnName);
        	}
        } else if (columnType == Types.FLOAT || columnType == Types.DOUBLE){
            result = new Double(resultSet.getDouble(columnPos));
        } else if (columnType == Types.NUMERIC && columnLength == 126){
            result = new Double(resultSet.getDouble(columnPos));
        } else if (columnType == Types.NUMERIC){
            result = new Long(resultSet.getLong(columnPos));
        } else if (columnType == Types.INTEGER || columnType == Types.BIGINT){
            result = new Long(resultSet.getLong(columnPos));
        } else if (columnType == Types.BIT){
            result = new Boolean(resultSet.getBoolean(columnPos));
        } else if (columnType == Types.CHAR && columnLength == 1){
        	if (pureMode){
        		result = resultSet.getString(columnPos);
        	} else {
        		result = new Boolean(resultSet.getBoolean(columnPos));
        	} 
        }  else if (columnType == Types.TIMESTAMP){
            Timestamp ts = resultSet.getTimestamp(columnPos);
            if (ts == null){
                result = null;
            } else {
                result = Calendar.getInstance();
                ((Calendar)result).setTimeInMillis(ts.getTime());
            }
        } else if (columnType == Types.DATE){
            Timestamp ts = resultSet.getTimestamp(columnPos);
            if (ts == null){
                result = null;
            } else {
                result = Calendar.getInstance();
                ((Calendar)result).setTimeInMillis(ts.getTime());
            }
        } else if (columnType == Types.BOOLEAN){
            result = new Boolean(resultSet.getBoolean(columnPos));            
        } else if (columnType == Types.LONGVARCHAR){
            //TEXT in MSSQL
            result = null;            
//        } else if (columnType == Types.LONGVARBINARY){
//            //IMAGE in MSSQL
//            result = null;            
        }else if (columnType == Types.VARBINARY){
            //BLOB in HSQL
            result = null;            
        } else if (columnType == Types.BLOB || columnType == Types.LONGVARBINARY){
//          IMAGE in MSSQL or BLOB
            try{
                InputStream blobStream = resultSet.getBinaryStream(columnPos);
                if(blobStream == null)
                    return null;
                
                StoreHelper.FileBackedOutputStream tmp = new StoreHelper.FileBackedOutputStream();
                StoreHelper.transfer(blobStream, tmp);
                result = tmp.toInputStream();//StoreHelper.createMeasuredInputStream(columnLength);
            }catch(Exception ex){
                String message = MessageFormat.format("Failed to read data from {0} column of BLOB type. SQL message is {1}",
                		columnPos, ex.getMessage());
                log.error(message, ex);
                throw new SQLException(message);
            }
        } 
        
        else {
        	//resultSet.getMetaData().getColumnName(columnPos);
            throw new SQLException("Unsupported value type "+columnType +" for column "+columnPos);
        }
        if (resultSet.wasNull()){
            return null;
        }
        return result;
    }

    
    public static DatabaseInsertStatement createInsertStatement(String tableName) {
        return new DatabaseInsertStatement(tableName);
        
    }
    
    public static DatabaseSelectOneStatement createSelectOneStatement(String tableName, String pkColumnName, Object pkValue) {
        return new DatabaseSelectOneStatement(tableName, pkColumnName, pkValue);
    }
    
    public static DatabaseSelectAllStatement createSelectAllStatement(String tableName, boolean loadAll) {
        return new DatabaseSelectAllStatement(tableName, loadAll);
    }
    
    public static DatabaseSelectAllStatement createSelectAllStatement(String tableName, boolean loadAll, boolean cacheStatement) {
        return new DatabaseSelectAllStatement(tableName, loadAll, cacheStatement);
    }
    

    public static DatabaseUpdateStatement createUpdateStatement(String tableName, String pkColumnName, Object pkValue) {
        return new DatabaseUpdateStatement(tableName, pkColumnName, pkValue);
    }

    
    public static DatabaseCountStatement createCountStatement(String tableName) {
        return new DatabaseCountStatement(tableName);
    }

    public static DatabaseDeleteStatement createDeleteStatement(String tableName, String pkColumnName, Object pkValue) {
        return new DatabaseDeleteStatement(tableName, pkColumnName, pkValue);
    }

    public static DatabaseDeleteStatement createDeleteStatement(String tableName) {
        return new DatabaseDeleteStatement(tableName);
    }

    public static DatabaseUpdateStatement createUpdateStatement(String tableName) {
        return new DatabaseUpdateStatement(tableName);
    }    

    public static void closePreparedStatement(Statement st, DatabaseConnection conn) throws RepositoryException{
    	conn.closeStatement(st);
    }

    public static RowMap assembleRow(ResultSet resultSet, DatabaseDialect dialect, 
    		ResultSetMetadata metaData) throws RepositoryException{
        try {
            RowMap row = new RowMap();
            //ResultSetMetaData rsMD = resultSet.getMetaData();
            for (int i = 0 ; i < metaData.getColumnCount() ; i++){
                /*String columnName = rsMD.getColumnName(i+1);
                
                int columnType = rsMD.getColumnType(i+1);
                rsMD.getCatalogName(i+1);
                //String tableName = 
                //resultSet.getString("this_.NODE_ID");
                String label = rsMD.getColumnLabel(i+1);
                //String columnPrefix = tableNameMapping.get(tableName.toUpperCase());
                //if (columnPrefix == null){
               // 	columnPrefix = "";
                //} else {
                //	columnPrefix = columnPrefix+".";
                //}
                if(columnType == Types.CLOB || ignoreBLOB && columnType == Types.BLOB)
                    continue;
                
                columnName = getUpperCase(columnName);
                String _columnName = columnName;
                
                for(String prefix:tableNameMapping.values()){
                	if (columnName.startsWith(prefix+"_")){
                		_columnName = prefix+"."+_columnName.substring(prefix.length()+1);
                	}
                }
                int columnLength = columnType == Types.BLOB ? 0:rsMD.getPrecision(i+1);
                //int scale = 0;
                //try {
                //    scale = rsMD.getScale(i+1);
                //    
                //} catch (Exception e) {
                //    // TODO: handle exception
                //}
                */
                ColumnMetadata column =  metaData.getColumn(i);
                Object obj = getValueFromResultSet(resultSet, dialect, i+1, column.getType(), column.getLength(), column.getName(), metaData.isPureMode());
                row.put(column.getName(), obj);
            }
            return row;
        } catch (SQLException exc){
            exc.printStackTrace();
            throw new RepositoryException("Error building result row", exc);
        }
    }
    
    private static HashMap<String, String> upperCaseNames = new HashMap<String, String>();
    
    public static String getUpperCase(String value) {
		String result = upperCaseNames.get(value);
		if (result == null){
			result = value.toUpperCase();
			synchronized (upperCaseNames) {
				upperCaseNames.put(value, result);
			}
		}
		return result;
    	//return value.toUpperCase();
	}


	public DatabaseCallableStatement createCallableStatement(String procedureName){
        return new DatabaseCallableStatement(procedureName);
    }
    
public static void lockTableRow(DatabaseConnection conn, String tableName, Object id) throws RepositoryException{
    if (conn.getTransactionSynchronization() != null){
        conn.getTransactionSynchronization().registerLock(conn.getConnectionId(), tableName, "ID", id);
    }
    conn.getDialect().lockTableRow(conn, tableName, id);
}
    
public static void lockTableRow(DatabaseConnection conn, String tableName, String pkPropertyName, Object pkValue) throws RepositoryException{
    if (conn.getTransactionSynchronization() != null){
        conn.getTransactionSynchronization().registerLock(conn.getConnectionId(), tableName, pkPropertyName, pkValue);
    }
    conn.getDialect().lockTableRow(conn, tableName, pkPropertyName, pkValue);
}
    
}


/*
 * $Log: DatabaseTools.java,v $
 * Revision 1.12  2009/01/27 14:07:59  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.11  2009/01/27 13:19:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.10  2009/01/26 10:53:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.9  2008/12/08 13:34:12  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2008/11/27 12:52:28  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/06/19 11:57:13  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2008/05/07 09:14:10  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2007/11/12 07:57:39  dparhomenko
 * PTR#1805818
 * 
 * Revision 1.1  2006/02/10 15:50:26  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */