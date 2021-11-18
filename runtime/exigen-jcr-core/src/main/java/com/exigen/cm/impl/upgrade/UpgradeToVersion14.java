/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.upgrade;

import static com.exigen.cm.Constants.DBOBJ_ACTION_CREATE;
import static com.exigen.cm.Constants.DBOBJ_POS_AFTER_ALL;
import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.PROPERTY_SUPPORT_OCR;
import static com.exigen.cm.Constants.PROPERTY_SUPPORT_OCR_SERVER;
import static com.exigen.cm.Constants.TABLE_ACE;
import static com.exigen.cm.Constants.TABLE_ACE2;
import static com.exigen.cm.Constants.TABLE_ACE_PERMISSION;
import static com.exigen.cm.Constants.TABLE_ACE_RESTRICTION;
import static com.exigen.cm.Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA;
import static com.exigen.cm.Constants.TABLE_INDEXABLE_DATA__FINISH_TIME;
import static com.exigen.cm.Constants.TABLE_INDEXABLE_DATA__RESERVED;
import static com.exigen.cm.Constants.TABLE_INDEXABLE_DATA__TIME;
import static com.exigen.cm.Constants.TABLE_OCR_DATA;
import static com.exigen.cm.Constants.TABLE_OCR_DATA__COMPLETION_DATE;
import static com.exigen.cm.Constants.TABLE_OCR_DATA__OPERATION;
import static com.exigen.cm.Constants.TABLE_OCR_DATA__SERVER_ID;
import static com.exigen.cm.Constants.TABLE_OCR_ERROR;
import static com.exigen.cm.Constants.TABLE_OCR_ERROR__COMMENT;
import static com.exigen.cm.Constants.TABLE_OCR_ERROR__DATE;
import static com.exigen.cm.Constants.TABLE_OCR_ERROR__ERROR_CODE;
import static com.exigen.cm.Constants.TABLE_OCR_ERROR__ERROR_TYPE;
import static com.exigen.cm.Constants.TABLE_OCR_ERROR__WORK_ID;
import static com.exigen.cm.Constants.TABLE_SYSTEM_PROPERTIES;
import static com.exigen.cm.Constants.TABLE_SYSTEM_PROPERTIES__VALUE;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.objdef.AbstractDBObjectDef;
import com.exigen.cm.database.objdef.DBObjectDef;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.tmp.NodeTypeConflictException;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.compact.CompactNodeTypeDefReader;
import com.exigen.cm.jackrabbit.nodetype.compact.ParseException;

/**
 * Upgrade to 14 version.
 */
public final class UpgradeToVersion14 extends AbstractUpgradeExecutor{

	/**
	 * JCR package name.
	 */
	private static final String JCR_PACKAGE_NAME = "JCR";
	/**
	 * Oracle package definition class name.
	 */
	private static final String ORACLE_PACKAGE_DEF = "com.exigen.cm.database.dialect.oracle.objdef.OraclePackageDef";

	/**
	 * Target version
	 * @return version number
	 */
	public Long getTargetVersion() {
		return 14L;
	}

	/**
	 * Upgrades JCR.
	 * 
	 * @param conn
	 *            Database connection
	 * @param repository
	 *            JCR Repository implementation
	 * @throws RepositoryException
	 *             Repository exception
	 */
	public void upgrade(final DatabaseConnection conn, final RepositoryImpl repository) throws RepositoryException {

		// Automatic update was too complex. Manual script should be done for each customer
		validateTable(conn, TABLE_ACE, getTargetVersion());
		validateTable(conn, TABLE_ACE2, getTargetVersion());
		validateTable(conn, TABLE_ACE_PERMISSION, getTargetVersion());
		validateTable(conn, TABLE_ACE_RESTRICTION, getTargetVersion());

		final InputStream inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(Constants.ECR_NODETYPES_RESOURCE_NAME);
		CompactNodeTypeDefReader reader;
		final Reader inputReader = new InputStreamReader(inStream);
		try {			
			reader = new CompactNodeTypeDefReader(inputReader, Constants.ECR_NODETYPES_RESOURCE_NAME);
		} catch (ParseException e) {
			throw new RepositoryException(e);
		} finally {
			try {
				inputReader.close();
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		}
		repository.reloadNamespaceRegistry(conn);
        final List<NodeTypeDef> defs = reader.getNodeTypeDefs();
        for (final NodeTypeDef def : defs) {
			if (def.getName().equals(Constants.ECR_OCR_MIXIN)) {
				repository.registerNodeType(def);
			} else if (def.getName().equals(Constants.EWF_UNLOCKABLE)) {
				try {
					repository.alterNodeType(def);
				} catch (NodeTypeConflictException e) {
					throw new RepositoryException(e);
				}
			} else if (def.getName().equals(Constants.EWF_LOCKABLE)) {
				repository.registerNodeType(def);
			}
		}
       
        final DatabaseInsertStatement stmt = DatabaseTools.createInsertStatement(TABLE_SYSTEM_PROPERTIES);
        stmt.addValue(SQLParameter.create(FIELD_ID, PROPERTY_SUPPORT_OCR));
        stmt.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE, Boolean.toString(false)));
        stmt.addBatch();
        
        stmt.addValue(SQLParameter.create(FIELD_ID, PROPERTY_SUPPORT_OCR_SERVER));
        stmt.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE, ""));
        stmt.addBatch();
        
        stmt.executeBatch(conn);

   		conn.commit();
        
        
		//1. create table OCR_work
		addTable(conn, getTableOCRWork(conn.getDialect()));
   		conn.commit();
		addTable(conn, getTableOCRError(conn.getDialect()));
   		conn.commit();
   		
   		//2. create function PREAD
   		final Map<String, String> dummyconfig = new HashMap<String, String>();
   		final List<DBObjectDef> specObjs = conn.getDialect().getSpecificDBObjectDefs(conn, dummyconfig);
   		
		for (int j = 0; j < specObjs.size(); j++) {
			if (specObjs.get(j) instanceof AbstractDBObjectDef) {	
				try {
					final Field field = AbstractDBObjectDef.class.getDeclaredField("name");
					field.setAccessible(true);
					final AbstractDBObjectDef objDef = (AbstractDBObjectDef) specObjs.get(j);
					if ("PREAD".equalsIgnoreCase(((String) field.get(objDef)))) {
						objDef.create(conn);
					}
					if (ORACLE_PACKAGE_DEF.equals(objDef.getClass().getName()) 
							&& JCR_PACKAGE_NAME.equalsIgnoreCase(((String) field.get(objDef))) 
							&& objDef.getPositionInObjectList() == DBOBJ_POS_AFTER_ALL
							&& objDef.isActionAvailable(DBOBJ_ACTION_CREATE)) {
						objDef.delete(conn);
						conn.commit();
						objDef.create(conn);
					}
				} catch (SecurityException e) {
					throw new RepositoryException(e);
				} catch (NoSuchFieldException e) {
					throw new RepositoryException(e);
				} catch (IllegalArgumentException e) {
					throw new RepositoryException(e);
				} catch (IllegalAccessException e) {
					throw new RepositoryException(e);
				}
			}
		}
    	conn.commit();

   		upgradeToVersion(conn);
   		
   		conn.commit();
	}
	
	/**
	 * OCR Work.
	 * @param dialect Database dialect
	 * @return Table definition
	 * @throws RepositoryException Repository exception
	 */
	public static TableDefinition getTableOCRWork(final DatabaseDialect dialect) throws RepositoryException {
		final TableDefinition ocrData = new TableDefinition(TABLE_OCR_DATA, true);
		ocrData.addColumn(new ColumnDefinition(ocrData, TABLE_OCR_DATA__COMPLETION_DATE, dialect.getColumnTypeTimeStampSQLType()));
		ocrData.addColumn(new ColumnDefinition(ocrData, TABLE_OCR_DATA__OPERATION, Types.INTEGER));
		ocrData.addColumn(new ColumnDefinition(ocrData, TABLE_OCR_DATA__SERVER_ID, Types.VARCHAR));

		ocrData.addColumn(new ColumnDefinition(ocrData, TABLE_INDEXABLE_DATA__RESERVED, Types.BOOLEAN)).setNotNull(true);
		ocrData.addColumn(new ColumnDefinition(ocrData, TABLE_INDEXABLE_DATA__TIME, dialect.getColumnTypeTimeStampSQLType()));
		ocrData.addColumn(new ColumnDefinition(ocrData, TABLE_INDEXABLE_DATA__FINISH_TIME, dialect.getColumnTypeTimeStampSQLType()));
		ocrData.addColumn(new ColumnDefinition(ocrData, TABLE_INDEXABLE_DATA__CONTENT_DATA, Types.VARCHAR));

		return ocrData;
	}

	/**
	 * OCR Error.
	 * @param dialect Database dialect
	 * @return Table definition
	 * @throws RepositoryException Repository exception
	 */
	public static TableDefinition getTableOCRError(final DatabaseDialect dialect) throws RepositoryException {
		final TableDefinition ocrError = new TableDefinition(TABLE_OCR_ERROR, false);
		ocrError.addColumn(new ColumnDefinition(ocrError, FIELD_ID, Types.INTEGER));
		ocrError.addColumn(new ColumnDefinition(ocrError, TABLE_OCR_ERROR__WORK_ID, Types.INTEGER));
		ocrError.addColumn(new ColumnDefinition(ocrError, TABLE_OCR_ERROR__ERROR_CODE, Types.VARCHAR));
		ocrError.addColumn(new ColumnDefinition(ocrError, TABLE_OCR_ERROR__ERROR_TYPE, Types.VARCHAR));
		ocrError.addColumn(new ColumnDefinition(ocrError, TABLE_OCR_ERROR__COMMENT, Types.VARCHAR));
		ocrError.addColumn(new ColumnDefinition(ocrError, TABLE_OCR_ERROR__DATE, dialect.getColumnTypeTimeStampSQLType()));
		return ocrError;
	}

}
