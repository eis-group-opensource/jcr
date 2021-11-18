/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.drop;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.AbstractDatabaseDialect;
import com.exigen.vf.commons.logging.LogUtils;

/**
 * The abstract <code>DropSQLObjectsBase</code> represents general interface & certain implementation for dropping sql objects
 * 
 */
abstract public class DropSQLProvider {
	
	protected ArrayList<String> sqlQueries = new ArrayList<String>();
	protected ArrayList<String> data = new ArrayList<String>();
	protected DatabaseConnection connection = null;
	protected Statement st = null;
	protected StringBuffer sb = null;
	protected ResultSet rs = null;
    
    protected Log log = LogFactory.getLog(DropSQLProvider.class);
	
	/**
	 * collect materialized views from current database
	 */
	abstract public ArrayList<String> getMaterializedViews() throws RepositoryException;

	/**
	 *  collect foreign key constraints (referential integrity) from current database
	 */	
	abstract public ArrayList<String> getConstraints() throws RepositoryException;
	
	/**
	 *  collect tables from current database
	 */
	abstract public ArrayList<String> getTables() throws RepositoryException;
	
	/**
	 *  collect views from current database
	 */
	abstract public ArrayList<String> getViews() throws RepositoryException;
	
	/**
	 *  collect sequences from current database
	 */
	abstract public ArrayList<String> getSequences() throws RepositoryException;
	
	/**
	 * collect procedures from current database
	 */
    abstract public ArrayList<String> getProcedures() throws RepositoryException;
    
    abstract public ArrayList<String> getFunctions() throws RepositoryException;
    
	/**
	 * construcor
	 * @param connection - current connection to database
	 */
	protected DropSQLProvider(){
	}
	
	
	/**
	 * @param list - collection of SQL Objects to process with
	 * @throws SQLException
     * @throws RepositoryException
	 */
	protected void processQueries(ArrayList<String> list) throws RepositoryException {
		for (Iterator it = list.iterator(); it.hasNext();){
			String query = (String) it.next();
			st = connection.prepareStatement(query, false);
			LogUtils.debug(log, "SQL query: {0}", query);
			
			try {
                ((PreparedStatement)st).executeUpdate();
                connection.closeStatement(st);
                st.close();
            } catch (Exception e) {
                throw new RepositoryException(e);
            }
		}
        connection.commit();
	}
	
	/**
	 * @param sqlObject - type of SQL Object to process with
	 * @param data - collection of SQL Objects to drop
	 * @throws Exception
	 */
	public void dropObjects(String sqlObject, ArrayList<String> data) 
		throws Exception {
		
		sqlQueries.clear();
		
		for (Iterator i = data.iterator(); i.hasNext();)
			
			sqlQueries.add("drop " + sqlObject + " " + convertObjectName((String)i.next()));
		
		processQueries(sqlQueries);
	}
	
	private String convertObjectName(String sqlObject) throws RepositoryException, SQLException {
		if (connection.getDialect().getDatabaseVendor().equals(AbstractDatabaseDialect.VENDOR_MSSQL)){
			if (connection.getDialect().getSchemaName(this.connection) != null){
				String name = connection.getDialect().getSchemaName(this.connection);
				if (!sqlObject.startsWith(name)){
					return name+"."+sqlObject;
				}
			}
		}
		return sqlObject;
	}

	/**
	 * @param sql - sql query to pick out collection of sql objects
	 * @param sqlObject - object name as it is in system table
	 * @param constraint - specific for constraints collection
	 * @return collection of sql object
	 * @throws Exception
	 */
	public ArrayList<String> getSQLObjects(String sql, String sqlObject, String constraint) throws RepositoryException {
		st = connection.prepareStatement(sql, true);
		data.clear();
        try {
    		rs = ((PreparedStatement)st).executeQuery();
    		while(rs.next()){
    			data.add(rs.getString(sqlObject));
    			if (constraint != null) data.add(rs.getString(constraint));
    		}
    		connection.closeStatement(st);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
        
		return data;
        
	}
	/**
	 * @return
	 */
	public DatabaseConnection getConnection() {
		return connection;
	}

	/**
	 * @param connection
	 */
	public void setConnection(DatabaseConnection connection) {
		this.connection = connection;
	}

	/**
	 * @param string
	 * @param data2
	 */
	public void dropConstraint(ArrayList<String> data) throws RepositoryException , SQLException{
		sqlQueries.clear();
		
		for (Iterator i = data.iterator(); i.hasNext();)
			sqlQueries.add("alter table " + convertObjectName((String)i.next()) + " drop constraint " + (String)i.next());
        try {
		processQueries(sqlQueries);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
		
	}
    
    public void drop() throws RepositoryException {
        try {
            data = getConstraints();
            dropConstraint( data);

            data = getIndexes();
            dropObjects("index", data);

            
            data.clear();
            data = getViews();
            dropObjects("view ", data);
            
            data.clear();
            data = getMaterializedViews();
            dropObjects("materialized view", data);
            
            
            data.clear();
            data = getSequences();
            dropObjects("sequence", data);
            
            data.clear();
            data = getProcedures();
            dropObjects("procedure", data);

            data.clear();
            data = getFunctions();
            dropObjects("function", data);
            
            data.clear();
            data = getTables();
            dropObjects("table", data);
            
            dropCustomEnd();
            
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
        
    }


	protected void dropCustomEnd() throws Exception {
		
		if (connection.getDialect().getDatabaseVendor().equals(
				AbstractDatabaseDialect.VENDOR_MSSQL)) {
			if (connection.getDialect().getSchemaName(this.connection) != null) {
				data.clear();

				try {
					sqlQueries.clear();
					sqlQueries.add("drop schema " + connection.getUserName()
							+ "_jcr");
					processQueries(sqlQueries);

				} catch (Exception exc) {
					log.debug(exc.getMessage());
				}

			}
		}

		
	}

	abstract protected ArrayList<String> getIndexes() throws RepositoryException;


}
