/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.mssql;
 
public class MSSQLConstants {
    
    public static final String MSSQL_FTS_CONVERT_PROC       	= "CONVERT_AND_MOVE";
    public static final String MSSQL_FTS_ZIP_PROC       	    = "ZIP_AND_MOVE";
    public static final String MSSQL_FTS_IFILTER_DETECT_PROC	= "DETECT_IFILTER";
    public static final String MSSQL_FTS_STOPWORD_COPY_PROC		= "COPY_STOPWORDS";
    public static final String MSSQL_FTS_READ_LOB_PROC          = "READ_LOB_FROM_FILE";
    public static final String MSSQL_FTS_SAVE_LOB_PROC          = "SAVE_LOB_TO_FILE";
    public static final String MSSQL_FTS_STAGE_RECORD_PROC      = "PROCESS_STAGE_RECORD";
    public static final String MSSQL_FTS_LOG_ERR_PROC           = "LOG_FTS_ERROR";
    
    // next two can include windows environment variables
    public static final String MSSQL_FTS_TEMP_DIR      			= "%TEMP%";
    // if without full path then must be in directory referenced by %PATH%
    public static final String MSSQL_FTS_EXTUTIL       			= "myfilter.exe";
    
    // database role used in case of MSSQL2005+FTS
    public static final String MSSQL_DB_ROLE					= "jcr";

    
}
