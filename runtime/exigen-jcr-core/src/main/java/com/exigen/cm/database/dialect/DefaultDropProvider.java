/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect;

import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.PROPERTY_SUPPORT_FTS;
import static com.exigen.cm.Constants.TABLE_SYSTEM_PROPERTIES;
import static com.exigen.cm.Constants.TABLE_SYSTEM_PROPERTIES__VALUE;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.drop.DropSQLProvider;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.vf.commons.logging.LogUtils;

public class DefaultDropProvider extends DropSQLProvider {

	private boolean privileged;
	private Map<String, String> config;
	private String userName;
	private DatabaseConnection privilegedConnection;
	private boolean supportFTS;

	
	public DefaultDropProvider(Map<String, String> config){
		this.config = config;
	}

	@Override
	public void drop() throws RepositoryException {
		DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(TABLE_SYSTEM_PROPERTIES, FIELD_ID, PROPERTY_SUPPORT_FTS);
		try {
			st.execute(connection);
			RowMap row = st.getRow();
			String supportFTSStr = row.getString(TABLE_SYSTEM_PROPERTIES__VALUE);
			this.supportFTS = Boolean.parseBoolean(supportFTSStr);
		} catch (RepositoryException exc){
			//throw new RepositoryException("Error loading FTS settings from database.");
			this.supportFTS = false;
		}
		st.close();
		
		
		if (connection.getDialect().getDatabaseVendor().equals(
				DatabaseDialect.VENDOR_ORACLE)
				&& supportFTS
				&& (connection.getDialect().getDatabaseVersion()
						.startsWith("9") || connection.getDialect()
						.getDatabaseVersion().startsWith("10"))) {
			DatabaseConnection originalConnection = connection;
			this.userName = connection.getUserName();
			String connectPass = config
					.get(Constants.PROPERTY_ORACLE_CTXSYS_PASSWORD);
			this.privilegedConnection = connection.getConnectionProvider()
					.createConnection("ctxsys", connectPass);
			this.privileged = true;
			dropAll();
			privilegedConnection.close();
			connection = originalConnection;
		}
		this.privileged = false;
		dropAll();
		connection.getDialect().dropSequence(connection);
		
	}

	private void dropAll() throws RepositoryException {
		ArrayList<String> clrAssemblies;
		try {
			data.clear();
			data = getObjects(DatabaseObject.TRIGGER);
	        dropObjects(" TRIGGER ", data);

			
			data.clear();
			data = getObjects(DatabaseObject.ORACLE_TEXT_POLICY);
	        dropOracleTextPolicy();
	        
	        data.clear();
	        data = getObjects(DatabaseObject.ORACLE_TEXT_PREFERENCE);
	        dropOracleTextReference();
	        
	        data.clear();
	        data = getObjects(DatabaseObject.ORACLE_TEXTSECTION_GROUP);
	        dropOracleTextSectionGroup();

	        data.clear();
	        data = getJavaSources();
	        dropObjects(" JAVA SOURCE ", data);
	
	        data.clear();
	        data = getPackages();
	        dropObjects(" PACKAGE ", data);
	        
	        clrAssemblies=getObjects(DatabaseObject.ASSEMBLY);
	        // CLR assemblies can be deleted after procedures which use them are removed 
			// with super.drop(); on other hand - list of created assemblies is available
	        // while CM_SYSTEM_OBJECTS is not dropped
        
		} catch (RepositoryException e){
			throw e;
		} catch (Exception e){
			connection.rollback();
			throw new RepositoryException(e);
		}
		
		super.drop();

		try {
			
			if (clrAssemblies.size()>0){
				dropCLRAssemblies(clrAssemblies);
			}	
	        
		} catch (RepositoryException e){
			throw e;
		} catch (Exception e){
			connection.rollback();
			throw new RepositoryException(e);
		}
		
	}
	
	private void dropCLRAssemblies(ArrayList<String> list) throws RepositoryException{
		PreparedStatement st=connection.prepareStatement(
				  "SELECT COUNT(*)"
				+ " FROM sys.assembly_references ar, sys.assemblies a"
				+ " WHERE ar.referenced_assembly_id=a.assembly_id"
				+ " AND a.name=?", false);
		while(list.size()>0){
			String victim=null;
			sqlQueries.clear();
			for(String name:list){
				try{
					st.setString(1, name);
					ResultSet rs=st.executeQuery();
					if (rs.next()){
						if (rs.getInt(1)==0){
							sqlQueries.add("DROP ASSEMBLY [" + name + "] WITH NO DEPENDENTS" );
							victim=name;
							break;
							
						}
					}
					rs.close();
				}catch(SQLException e){
					throw new RepositoryException("SQL error checking CLR assemly dependencies",e);
				}
				
			}
			if (victim==null){
				throw new RepositoryException(
						"Cannot drop CLR assemblies: there are references to them from unknown sources"
				);
			}else{
				try{
					processQueries(sqlQueries);
					list.remove(victim);
				}catch(RepositoryException e){
					log.info("Error deleting CLR assemblies");
				}
			}
		}
		try{
			st.close();
		}catch(SQLException e){
			// nothing
		}
	}

	private void dropOracleTextSectionGroup() throws RepositoryException {
		sqlQueries.clear();
		
		for (String name:data){
			sqlQueries.add("BEGIN CTX_DDL.DROP_SECTION_GROUP('"+name+"'); END;");
		}
		
		processQueries(sqlQueries);
	}

	private void dropOracleTextReference() throws RepositoryException {
		sqlQueries.clear();
		
		for (String name:data){
			sqlQueries.add("BEGIN CTX_DDL.DROP_PREFERENCE('"+name+"'); END;");
		}
		
		processQueries(sqlQueries);
	}

	private void dropOracleTextPolicy() throws RepositoryException {
		// "BEGIN CTX_DDL.DROP_POLICY('"+this.getPolicyName()+"'); END;";
		sqlQueries.clear();
		
		for (String name:data){
			sqlQueries.add("BEGIN CTX_DDL.DROP_POLICY('"+name+"'); END;");
		}
		
		processQueries(sqlQueries);
		
	}

	private ArrayList<String> getPackages() throws RepositoryException {
		return getObjects(DatabaseObject.PACKAGE);
	}

	private ArrayList<String> getJavaSources() throws RepositoryException {
		return getObjects(DatabaseObject.JAVASOURCE);
	}

	@Override
	public ArrayList<String> getConstraints() throws RepositoryException {
		ArrayList<String> list = getObjects(DatabaseObject.CONSTRAINT);
		ArrayList<String> result = new ArrayList<String>();
		for(String s:list){
			StringTokenizer st = new StringTokenizer(s,".");
			String a1 = st.nextToken();
			String a2 = st.nextToken();
			if (st.hasMoreElements()){
				a1  = a2;
				a2 = st.nextToken();
			}
			result.add(a1);
			result.add(a2);
		}
		return result;
	}

	@Override
	public ArrayList<String> getFunctions() throws RepositoryException {
		return getObjects(DatabaseObject.FUNCTION);
	}

	@Override
	public ArrayList<String> getMaterializedViews() throws RepositoryException {
		return getObjects(DatabaseObject.MATERIALIZEDVIEW);
	}

	@Override
	public ArrayList<String> getProcedures() throws RepositoryException {
		return getObjects(DatabaseObject.PROCEDURE);
	}

	@Override
	public ArrayList<String> getSequences() throws RepositoryException {
		return getObjects(DatabaseObject.SEQUENCE);
	}

	@Override
	public ArrayList<String> getTables() throws RepositoryException {
		return getObjects(DatabaseObject.TABLE);
	}


	@Override
	public ArrayList<String> getViews() throws RepositoryException {
		return getObjects(DatabaseObject.VIEW);
	}

	@Override
	public ArrayList<String> getIndexes() throws RepositoryException {
		return getObjects(DatabaseObject.VIEW);
	}

	
	private ArrayList<String> getObjects(DatabaseObject type) throws RepositoryException {
		ArrayList<String> result = new ArrayList<String>();
		String table = Constants.TABLE_SYSTEM_OBJECTS;
		if (privileged){
			table = userName+"."+table;
		}
		DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(table, true);
		st.addCondition(Conditions.eq(Constants.TABLE_SYSTEM_OBJECTS__TYPE, type.name()));
		st.addCondition(Conditions.eq(Constants.TABLE_SYSTEM_OBJECTS__PRIVILEGED, privileged));
		try {
		st.execute(getConnection());
		} catch (Exception exc){
			throw new RepositoryException("Probably you have broken database structure, please run \"CreateRepository --dropAll\"");
		}
		while (st.hasNext()){
			RowMap row = st.nextRow();
			String v = row.getString(Constants.TABLE_SYSTEM_OBJECTS__NAME);
			result.add(v);
		}
		return result;
	}

	
	/**
	 * @param list - collection of SQL Objects to process with
	 * @throws SQLException
     * @throws RepositoryException
	 */
	protected void processQueries(ArrayList<String> list) throws RepositoryException {
		DatabaseConnection c = connection;
		if (privileged){
			c= privilegedConnection;
		}
		for (Iterator it = list.iterator(); it.hasNext();){
			String query = (String) it.next();
			st = c.prepareStatement(query, false);
			LogUtils.debug(log, "SQL query: {0}", query);
			try {
                ((PreparedStatement)st).executeUpdate();
                c.closeStatement(st);
            } catch (Exception e) {
    			LogUtils.warn(log, "Error processing query: {0} - {1}", query, e.getMessage());
            }
			//connection.commit();
		}
        c.commit();
	}
}
