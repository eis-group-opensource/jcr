/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle;

import static com.exigen.cm.Constants.DBOBJ_POS_AFTER_ALL;
import static com.exigen.cm.Constants.DBOBJ_POS_AFTER_TABLE;
import static com.exigen.cm.Constants.FIELD_BLOB;
import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.RC_FTS_CONV_COMPRESS_ERR;
import static com.exigen.cm.Constants.RC_FTS_CONV_DELETE_ERR;
import static com.exigen.cm.Constants.RC_FTS_CONV_EXTRACT_ERR;
import static com.exigen.cm.Constants.RC_FTS_CONV_NO_ROWS;
import static com.exigen.cm.Constants.RC_FTS_CONV_OK;
import static com.exigen.cm.Constants.RC_FTS_CONV_TOO_MANY_ROWS;
import static com.exigen.cm.Constants.RC_FTS_CONV_UPDATE_ERR;
import static com.exigen.cm.Constants.TABLE_FTS_DATA;
import static com.exigen.cm.Constants.TABLE_FTS_STAGE;
import static com.exigen.cm.Constants.TABLE_FTS_STAGE__FILENAME;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORATXT_FMT_DOC_POLICY;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORATXT_FMT_DOC_PREF;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_FTS_CONVERT_FUNC;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_FTS_ERR_LOG_PROC;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_FTS_REC_PROCESS_FUNC;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_FTS_ZIP_FUNC;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_SECURITY_PKG;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.fts.FTSCommand;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.dialect.oracle.objdef.OraclePackageDef;
import com.exigen.cm.database.dialect.oracle.objdef.OracleStoredFunctionDef;
import com.exigen.cm.database.dialect.oracle.objdef.OracleStoredProcedureDef;
import com.exigen.cm.database.dialect.oracle.objdef.OracleTextPolicyDef;
import com.exigen.cm.database.dialect.oracle.objdef.OracleTextPreferenceDef;
import com.exigen.cm.database.drop.DropSQLProvider;
import com.exigen.cm.database.objdef.DBObjectDef;

public class Oracle11DatabaseDialect extends OracleDatabaseDialect{
	 
    @Override
    public DropSQLProvider getDropProvider(Map config) throws RepositoryException {
        return new DropOracle10SQLObjects();
    }

    public List<TableDefinition> getSpecificTableDefs(boolean supportFTS)  throws RepositoryException {
    	ArrayList<TableDefinition> result = new ArrayList<TableDefinition>();
    	
    	if (supportFTS){
	        TableDefinition stage = new TableDefinition(TABLE_FTS_STAGE, true);
	        stage.addColumn(new ColumnDefinition(stage, FIELD_BLOB, Types.BLOB));
	        stage.addColumn(new ColumnDefinition(stage, TABLE_FTS_STAGE__FILENAME, Types.VARCHAR));
	        result.add(stage);
    	}
        
    	return result;
    }

    protected List<DBObjectDef> _getSpecificDBObjectDefs(DatabaseConnection conn, Map config) throws RepositoryException{
    	HashMap<String,String> subst = new HashMap<String,String>();
    	subst.put("SECURITY-PKG",ORA_SECURITY_PKG);
    	subst.put("CONVERT-PROC",ORA_FTS_CONVERT_FUNC);
    	subst.put("ZIP-PROC",ORA_FTS_ZIP_FUNC);
       	subst.put("DEST-TABLE",TABLE_FTS_DATA);
       	subst.put("DEST-ID",FIELD_ID);
       	subst.put("DEST-BLOB",Constants.FIELD_FTS_DATA_XYZ);
    	subst.put("DEST-INDEX",Constants.FTS_INDEX_NAME);
    	subst.put("SRC-TABLE",TABLE_FTS_STAGE);
    	subst.put("SRC-ID",FIELD_ID);
    	subst.put("SRC-BLOB",FIELD_BLOB);
    	subst.put("FMT-DOC-POLICY",ORATXT_FMT_DOC_POLICY);
    	
    	subst.put("RC-OK",String.valueOf(RC_FTS_CONV_OK));
    	subst.put("RC-NO-ROWS",String.valueOf(RC_FTS_CONV_NO_ROWS));
    	subst.put("RC-TOO-MANY-ROWS",String.valueOf(RC_FTS_CONV_TOO_MANY_ROWS));
    	subst.put("RC-UPDATE-ERR",String.valueOf(RC_FTS_CONV_UPDATE_ERR));
    	subst.put("RC-DELETE-ERR",String.valueOf(RC_FTS_CONV_DELETE_ERR));
    	subst.put("RC-COMPRESS-ERR",String.valueOf(RC_FTS_CONV_COMPRESS_ERR));
    	subst.put("RC-EXTRACT-ERR",String.valueOf(RC_FTS_CONV_EXTRACT_ERR));
    	
    	subst.put("ERR-TABLE",Constants.TABLE_FTS_INDEXING_ERROR);
    	subst.put("ERR-ID-COL",Constants.FIELD_ID);
    	subst.put("ERR-CODE-COL",Constants.TABLE_FTS_INDEXING__ERROR_CODE);
    	subst.put("ERR-TYPE-COL",Constants.TABLE_FTS_INDEXING__ERROR_TYPE);
    	subst.put("ERR-MSG-COL",Constants.TABLE_FTS_INDEXING__COMMENT);
    	subst.put("ERR-MSG-MAX-LEN","254");
    	subst.put("LOG-FTS-ERR-PROC",ORA_FTS_ERR_LOG_PROC);
    	
    	subst.put("PROCESS-STAGE-RECORD-PROC",ORA_FTS_REC_PROCESS_FUNC);
    	subst.put("ERRT-TXT-EXTR-FAIL","."+FTSCommand.ERROR_TYPE_TXT_EXTRACTION);

    	
    	boolean supportFTS = "true".equals(config.get(Constants.PROPERTY_SUPPORT_FTS));
    	ArrayList<DBObjectDef> result = new ArrayList<DBObjectDef>();

    	OraclePackageDef securityPackage=new OraclePackageDef(ORA_SECURITY_PKG,
    			getResourceAsStream("sql/SecurityPackage.sql"));
    	securityPackage.setSubstitutionMap(subst);
    	result.add(securityPackage);    	
    	
    	if (supportFTS){
	        
    		OracleTextPreferenceDef fmtPref = new OracleTextPreferenceDef(
					ORATXT_FMT_DOC_PREF, "AUTO_FILTER");
			fmtPref.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE,
					TABLE_FTS_DATA);
			result.add(fmtPref);

			OracleTextPolicyDef fmtPol = new OracleTextPolicyDef(
					ORATXT_FMT_DOC_POLICY, ORATXT_FMT_DOC_PREF

			);
			fmtPol.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE,
					TABLE_FTS_DATA);
			result.add(fmtPol);
   	
	       	OracleStoredProcedureDef logProc=new OracleStoredProcedureDef(ORA_FTS_ERR_LOG_PROC,
	    			getResourceAsStream("sql/LogFTSProcErr.sql"));
	    	logProc.setPositionInObjectList(DBOBJ_POS_AFTER_ALL);
	    	logProc.setSubstitutionMap(subst);
	    	result.add(logProc);
	    	
	    	OracleStoredFunctionDef func0=new OracleStoredFunctionDef(ORA_FTS_REC_PROCESS_FUNC,
	    			getResourceAsStream("sql/ProcessStageRecord11.sql"));
	    	func0.setPositionInObjectList(DBOBJ_POS_AFTER_ALL);
	    	func0.setSubstitutionMap(subst);
	    	result.add(func0);
	
	       	OracleStoredFunctionDef func1 = new OracleStoredFunctionDef(
					ORA_FTS_CONVERT_FUNC, "CREATE FUNCTION " + ORA_FTS_CONVERT_FUNC
							+ "(p_id IN NUMBER) RETURN NUMBER AS "
							+ "BEGIN RETURN " + ORA_FTS_REC_PROCESS_FUNC
							+ "(p_id,'."
							+ FTSCommand.ERROR_CODE_TXT_CONVERT_AND_MOVE_FAILED
							+ "',0);END;");
	    	func1.setPositionInObjectList(DBOBJ_POS_AFTER_ALL);
	    	result.add(func1);
	
	    	OracleStoredFunctionDef func2 = new OracleStoredFunctionDef(
					ORA_FTS_ZIP_FUNC, "CREATE FUNCTION " + ORA_FTS_ZIP_FUNC
							+ "(p_id IN NUMBER) RETURN NUMBER AS "
							+ "BEGIN RETURN " + ORA_FTS_REC_PROCESS_FUNC
							+ "(p_id,'."
							+ FTSCommand.ERROR_CODE_TXT_ZIP_AND_MOVE_FAILED
							+ "',1);END;");
	    	func2.setPositionInObjectList(DBOBJ_POS_AFTER_ALL);
	    	result.add(func2);
    	}
    	
       	addTrigger(result, subst, Constants.TABLE_NODE_UNSTRUCTURED__BOOLEAN_VALUE, 5);
    	
    	
    	return result;
    }
    
    @Override
    public void sessionSetup(Connection conn) throws SQLException{
    }
    
    protected void appendLobClauseAfterColumnList(StringBuffer sb, TableDefinition tableDef){
    	for(Iterator<ColumnDefinition> iter=tableDef.getColumnIterator();iter.hasNext();){
    		ColumnDefinition c=iter.next();
    		int colType=c.getColumnType();
    		if ((colType==Types.BLOB||colType==Types.CLOB)){
    			sb.append(" LOB(");
    			sb.append(c.getColumnName());
    			sb.append(") STORE AS SECUREFILE");
    			if (!Constants.TABLE_FTS_STAGE.equals(tableDef.getTableName())){
    				sb.append(" (COMPRESS HIGH)");
    			}
    		}
    	}
    }
    
    public String getDatabaseVersion() {
		return "11";
	}

    @Override
    // added types for MS Office 2007
    public boolean isMIMETypeSupported(String MIMEType) {
        //MIME types supported by Oracle (abridged)
          final List <String> types = Arrays.asList(
            "text/plain",
            "application/msword",
            "application/excel",
            "application/vnd.ms-excel",
            "application/vndms-excel",
            "application/x-excel",
            "application/x-msexcel",
            "application/powerpoint",
            "application/mspowerpoint",
            "application/vnd.ms-powerpoint",
            "text/html",
            "application/pdf",
            "image/tiff",
            "text/richtext",
            "text/rtf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
            "application/vnd.openxmlformats-officedocument.presentationml.template"
            
        );
        return types.contains(MIMEType);
    } 
}
