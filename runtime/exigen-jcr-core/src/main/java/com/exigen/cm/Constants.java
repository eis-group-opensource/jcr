/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm;

import com.exigen.cm.jackrabbit.name.QName;

/**
 * JCR Constants
 */
public final class Constants {

	/**
	 * Shouldn't be instantiated
	 */
	private Constants() {
	}

	public static final String ECR_URI = "http://www.exigen.com/ecr";
	public static final String ECR_NT_URI = "http://www.exigen.com/ecr_nt";
	public static final String ECR_MIX_URI = "http://www.exigen.com/ecr_mix";

	/** Database schema version */
	public static final String DATABASE_VERSION = "14";

	public static final String PROPERTY_DELIMITER = ".";

	public static final Long SYSTEM_WORKSPACE_ROOT_ID = (long) -1;

	/**
	 * Holds QName for internal Node.ID
	 */
	public static final QName FIELD_INTERNAL_ID = QName.valueOf("{internal}id");

	/**
	 * Log performance category constants
	 */
	public static final String LOG_CATEGORY_PERFORMANCE = "com.exigen.cm.performance";
	public static final String LOG_CATEGORY_PERFORMANCE__FIND = "FIND";
	public static final String LOG_CATEGORY_PERFORMANCE__LOAD = "LOAD";

	// public static final int MAX_FIELD_LENGTH = 27;

	public static final String PROPERTY_SUPPORT_FTS = "fts";
	public static final String PROPERTY_SUPPORT_OCR = "ocr";
	public static final String PROPERTY_SUPPORT_OCR_SERVER = "ocr.server";
	public static final String PROPERTY_SUPPORT_SECURITY = "security";
	public static final String PROPERTY_SUPPORT_NODETYPE_CHECK = "nodetype.check";
	public static final String PROPERTY_SUPPORT_LOCK_DISABLE = "lock.disable";
	public static final String PROPERTY_SUPPORT_VERSIONING_CHECK = "versioning.strongcheck";
	public static final String PROPERTY_IGNORE_CASE_IN_SECURITY = "ignoreCaseInSecurity";

	public static final String PROPERTY_ROOT_USER = "root.user";
	public static final String PROPERTY_ROOT_PASSWORD = "root.password";

	public static final String DEFAULT_ROOT_USER_NAME = "superuser";
	public static final String DEFAULT_ROOT_PASSWORD = "";

	public static final String DEFAULT_ROOT_ALIAS = "THIS_";
	public static final String DEFAULT_SEQUENCE_NAME = "JCR_SEQUENCE";

	public static final String DEFAULT_REPOSITORY_NAME = "default";
	public static final String PROPERTY_DATASOURCE_JNDI_NAME = "datasource.jndi_name";
	public static final String PROPERTY_DATASOURCE_DIALECT_CLASSNAME = "db.dialect.classname";
	public static final String PROPERTY_DATASOURCE_USER = "connection.user";
	public static final String PROPERTY_DATASOURCE_PASSWORD = "connection.password";
	public static final String PROPERTY_DATASOURCE_DRIVER_CLASSNAME = "connection.driver.classname";
	public static final String PROPERTY_DATASOURCE_URL = "connection.url";
	public static final String PROPERTY_DEVELOPMENT_MODE = "development.mode";
	public static final String PROPERTY_AUTO_ADD_LOCK_TOKEN = "lock.autoAddToken";
	public static final String PROPERTY_DATASOURCE_DROP_CREATE = "db.dropcreate";
	public static final String PROPERTY_DATASOURCE_SKIP_CHECK = "db.skipcheck";
	public static final String PROPERTY_DATASOURCE_REDUCED_VERSION_CHECK_CHECK = "db.reducedVersionCheck";
	public static final String PROPERTY_DATASOURCE_ALLOW_UPGRADE = "db.upgrade";
	public static final String PROPERTY_SECURITY_MOVE_WITH_NODE = "security.moveWithNode";

	public static final String PROPERTY_CONFIGURATOR = "configurator";
	public static final String PROPERTY_INITIALIZER = "initializer";

	public static final String IMPORT_PREFIX = "import";
	public static final String EXPORT_PREFIX = "export";
	public static final String DATA_FILE_PATH = "data";
	public static final String NODETYPE_FILE_PATH = "nodetypes";
	public static final String NODETYPE_SKIP_BUILTIN = "builtin.skip";
	public static final String SECURITY_FILE_PATH = "security";
	public static final String BINARY_ZIP_FILE_PATH = "bzip";

	public static final String PROPERTY_IMPORT_NODETYPES = IMPORT_PREFIX + PROPERTY_DELIMITER + NODETYPE_FILE_PATH;
	public static final String PROPERTY_IMPORT_DATA = IMPORT_PREFIX + PROPERTY_DELIMITER + DATA_FILE_PATH;
	public static final String PROPERTY_IMPORT_SECURITY = IMPORT_PREFIX + PROPERTY_DELIMITER + SECURITY_FILE_PATH;
	public static final String PROPERTY_IMPORT_ZIP_BINARY = IMPORT_PREFIX + PROPERTY_DELIMITER + BINARY_ZIP_FILE_PATH;

	public static final String PROPERTY_EXPORT_NODETYPES = EXPORT_PREFIX + PROPERTY_DELIMITER + NODETYPE_FILE_PATH;
	public static final String PROPERTY_EXPORT_DATA = EXPORT_PREFIX + PROPERTY_DELIMITER + DATA_FILE_PATH;
	public static final String PROPERTY_EXPORT_SECURITY = EXPORT_PREFIX + PROPERTY_DELIMITER + SECURITY_FILE_PATH;
	public static final String PROPERTY_EXPORT_ZIP_BINARY = EXPORT_PREFIX + PROPERTY_DELIMITER + BINARY_ZIP_FILE_PATH;

	/**
	 * Used as password for CTXSYS user on db creation. Needed for stored
	 * procedure creation
	 */
	public static final String PROPERTY_ORACLE_CTXSYS_PASSWORD = "oracle.ctxsys.password";

	public static final String PROPERTY_AUTHENTICATOR_CLASS_NAME = "authenticator.classname";
	public static final String PROPERTY_AUTHENTICATOR_ROOT_USER = "authenticator." + PROPERTY_ROOT_USER;
	public static final String PROPERTY_AUTHENTICATOR_ROOT_PASSWORD = "authenticator." + PROPERTY_ROOT_PASSWORD;
	public static final String DEFAULT_JNDI_PREFIX = "jcr/exigen";
	public static final String DEFAULT_JNDI_DATASOURCE_NAME = "JSR170_DataSource";

	public static final String CONFIG_MS_FTS_STOPWORD = "ms.fts.stopwords";

	public static final String IMPORT_ROOT_USER = "import." + PROPERTY_ROOT_USER;
	public static final String IMPORT_ROOT_PASSWORD = "import." + PROPERTY_ROOT_PASSWORD;

	public static final String DEFAULT_WORKSPACE = "default";

	public static final String TABLE_NAMESPACE = "CM_NAMESPACE";
	public static final String TABLE_NODETYPE = "CMT_NODETYPE";
	public static final String TABLE_NODETYPE_SUPERTYPES = "CMT_NT_SUPERTYPE";
	public static final String TABLE_NODETYPE_PROPERTY = "CMT_NT_PROPERTY";
	public static final String TABLE_NODETYPE_PROPERTY_DEFAULTVALUE = "CMT_NT_PROP_DEFAULT";
	public static final String TABLE_NODETYPE_PROPERTY_CONSTRAINT = "CMT_NT_PROP_CONSTRAINT";
	public static final String TABLE_NODETYPE_CHILDS = "CMT_NT_CHILDS";
	public static final String TABLE_NODETYPE_CHILDS_REQUIRED_TYPES = "CMT_NT_CHILDS_REQ_TYPE";
	public static final String TABLE_FTS_DATA = "CM_FTS_DATA";
	public static final String TABLE_FTS_DATA__EXT = "EXT";
	public static final String TABLE_FTS_DATA__MIME_SOURCE = "MIME_SOURCE";
	public static final String TABLE_FTS_DATA__TEXT_SOURCE = "TEXT_SOURCE";
	public static final String TABLE_FTS_DATA__NODE_ID = "NODE_ID";
	public static final String TABLE_FTS_DATA__TEXT = "FTS_TEXT2";
	public static final String TABLE_SESSION_MANAGER = "CM_SESSION_MANAGER";

	public static final String TABLE_FTS_STAGE = "CM_FTS_STAGE";
	public static final String TABLE_FTS_STAGE_CONV = "CM_FTS_STAGE_CONV";
	public static final String TABLE_FTS_STAGE__FILENAME = "FILENAME";
	public static final String TABLE_FTS_STAGE__DATA = "DATA";

	public static final String TABLE_ID_GENERATOR = "CM_ID_GENERATOR";

	public static final String TABLE_ID_GENERATOR__NAME = "NAME";
	public static final String TABLE_ID_GENERATOR__NEXT_ID = "NEXT_ID";

	public static final String FIELD_ID = "ID";
	public static final String FIELD_NAME = "NAME";
	public static final String FIELD_NAMESPACE = "NAMESPACE";
	public static final String FIELD_TYPE_ID = "NODE_ID";

	public static final String DEFAULT_GENERATOR = "default";

	public static final String TABLE_NODETYPE__MIXIN = "MIXIN";
	public static final String TABLE_NODETYPE__ORDERABLE_CHILDS = "IS_ORDERABLE_CHILD_NODES";
	public static final String TABLE_NODETYPE__PRIMARY_ITEM_NAME = "PRYMARY_ITEM_NAME";
	public static final String TABLE_NODETYPE__PRIMARY_ITEM_NAMESPACE = "PRYMARY_ITEM_NAMESPACE";
	// public static final String TABLE_NODETYPE__EMBEDED = "TABLEEMBEDED";
	public static final String TABLE_NODETYPE__TABLENAME = "TABLENAME";
	public static final String TABLE_NODETYPE__PRESENCECOLUMN = "TYPE_COLUMN";

	public static final String TABLE_NAMESPACE__PREFIX = "PREFIX";
	public static final String TABLE_NAMESPACE__URI = "URI";

	public static final String TABLE_NODETYPE_SUPERTYPES__PARENT = "PARENT_ID";
	public static final String TABLE_NODETYPE_SUPERTYPES__CHILD = "CHILD_ID";

	public static final String TABLE_NODETYPE_PROPERTY__NODE_TYPE = "NODETYPE_ID";
	public static final String TABLE_NODETYPE_PROPERTY__COLUMN_NAME = "COLUMN_NAME";
	public static final String TABLE_NODETYPE_PROPERTY__MILTIPLE = "MULTIPLE";
	public static final String TABLE_NODETYPE_PROPERTY__PROTECTED = "PROTECTED";
	public static final String TABLE_NODETYPE_PROPERTY__MANDATORY = "MANDATORY";
	public static final String TABLE_NODETYPE_PROPERTY__AUTO_CREATE = "AUTO_CREATE";
	public static final String TABLE_NODETYPE_PROPERTY__ON_PARENT_VERSION = "ON_PARENT_VERSION";
	public static final String TABLE_NODETYPE_PROPERTY__REQUIRED_TYPE = "REQUIRED_TYPE";
	public static final String TABLE_NODETYPE_PROPERTY__INDEXABLE = "INDEXABLE";
	public static final String TABLE_NODETYPE_PROPERTY__FTS = "FTS";

	public static final String TABLE_NODETYPE_CHILDS__NODE_TYPE = "NODETYPE_ID";
	public static final String TABLE_NODETYPE_CHILDS__ON_PARENT_VERSION = "ON_PARENT_VERSION";
	public static final String TABLE_NODETYPE_CHILDS__AUTO_CREATE = "AUTO_CREATE";
	public static final String TABLE_NODETYPE_CHILDS__MANDATORY = "MANDATORY";
	public static final String TABLE_NODETYPE_CHILDS__PROTECTED = "PROTECTED";
	public static final String TABLE_NODETYPE_CHILDS__SAMENAMESIBLING = "SAME_NAME_SIBLIMG";
	public static final String TABLE_NODETYPE_CHILDS__DEFAULT_NODE_TYPE = "DEFAULT_NODETYPE_ID";

	public static final String TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__CHILD_ID = "NODE_TYPE_CHILD_ID";
	public static final String TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__NODE_TYPE = "NODE_TYPE_ID";

	public static final String TABLE_WORKSPACE = "CM_WORKSPACE";
	public static final String TABLE_WORKSPACE__NAME = "NAME";
	public static final String TABLE_WORKSPACE__ROOT_NODE = "ROOT_NODE_ID";

	public static final String TABLE_NODE = "CM_NODE";
	public static final String TABLE_NODE__PARENT = "PARENT_ID";
	public static final String TABLE_NODE__VERSION_ = "VERSION";
	public static final String TABLE_NODE__NODE_TYPE = "NODE_TYPE_ID";
	public static final String TABLE_NODE__SECURITY_ID = "SECURITY_ID";
	public static final String TABLE_NODE__INDEX = "POS";
	public static final String TABLE_NODE__INDEX_MAX = "MAX_POS";
	public static final String TABLE_NODE__NODE_PATH = "NODE_PATH";
	public static final String TABLE_NODE__WORKSPACE_ID = "WORKSPACE_ID";
	public static final String TABLE_NODE__NODE_DEPTH = "DEPTH";
	public static final String TABLE_NODE__CONTENT_STORE_CONFIG_NODE = "STORE_CONFIG_NODE_ID";

	public static final String TABLE_NODE_LOCK = "CM_NODE_LOCK";

	public static final String _TABLE_NODE_LOCK_INFO = "CM_NODE_LOCK_INFO";
	public static final String TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID = "PARENT_LOCK_ID";
	public static final String TABLE_NODE_LOCK_INFO__LOCK_IS_DEEP = "LOCK_IS_DEEP";
	public static final String TABLE_NODE_LOCK_INFO__LOCK_OWNER = "LOCK_OWNER";
	// public static final String TABLE_NODE_LOCK_INFO__LOCK_EXPIRES =
	// "LOCK_EXPIRES";

	public static final String TABLE_SYSTEM_OBJECTS = "CM_SYSTEM_OBJECTS";
	public static final String TABLE_SYSTEM_OBJECTS__TYPE = "TYPE";
	public static final String TABLE_SYSTEM_OBJECTS__NAME = "NAME";
	public static final String TABLE_SYSTEM_OBJECTS__PRIVILEGED = "priv";

	public static final String TABLE_SYSTEM_PROPERTIES = "CM_SYSTEM_PROPERTIES";
	public static final String TABLE_SYSTEM_PROPERTIES__VALUE = "VALUE";
	public static final int TABLE_SYSTEM_PROPERTIES__VALUE_MAX_LEN = 254;

	public static final String TABLE_SYSTEM_PROPERTIES__VALUE__DB_VERSION = "DB_VERSION";
	public static final String TABLE_SYSTEM_PROPERTIES__VALUE__NT_VERSION = "NT_VERSION";
	public static final String TABLE_SYSTEM_PROPERTIES__VALUE__NT_HASH = "NT_HASH";
	public static final String TABLE_SYSTEM_PROPERTIES__VALUE__BUILD_VERSION = "BUILD_VERSION";
	public static final String TABLE_SYSTEM_PROPERTIES__VALUE__CREATED_FROM_ADDR = "CREATED_FROM_IPADDR";
	public static final String TABLE_SYSTEM_PROPERTIES__VALUE__CREATED_SYS_PROP = "CREATED_FROM_SYSPROPS";
	public static final String TABLE_SYSTEM_PROPERTIES__VALUE__CREATED_DATETIME = "CREATED_DATETIME";

	public static final String TABLE_ACE = "CM_ACE";
	public static final String TABLE_ACE___DIRECT_SUFFIX = "_DIRECT";

	public static final String TABLE_ACE__USER_ID = "USER_ID";
	public static final String TABLE_ACE__GROUP_ID = "GROUP_ID";
	public static final String TABLE_ACE__CONTEXT_ID = "CONTEXT_ID";

	public static final String TABLE_ACE2 = "CM_ACE2";
	public static final String TABLE_ACE2___PARENT_SUFFIX = "_PARENT";
	public static final String TABLE_ACE2___SEQUENCE_SUFFIX = "_SECQUENCE";
	public static final String TABLE_ACE2___FROM_SUFFIX = "_FROM";

	public static final String TABLE_ACE_RESTRICTION = "CM_ACE_RESTRICTION";
	public static final String TABLE_ACE_RESTRICTION__ACE_ID = "ACE_ID";

	public static final String TABLE_TYPE = "CM_TYPE";
	// public static final String TABLE_TYPE__NODE_ID = "NODE_ID";
	public static final String TABLE_TYPE__NODE_TYPE = "NODE_TYPE_ID";
	public static final String TABLE_TYPE__FROM_NODE_TYPE = "FROM_NODE_TYPE_ID";

	public static final String TABLE_NODE_PARENT = "CM_NODE_PARENTS";
	// public static final String TABLE_NODE_PARENT__NODE_ID = "NODE_ID";
	public static final String TABLE_NODE_PARENT__PARENT_ID = "PARENT_ID";
	public static final String TABLE_NODE_PARENT__LEVEL = "DEEP_LEVEL";

	public static final String TABLE_NODE_REFERENCE = "CM_NODE_REFERENCE";
	public static final String TABLE_NODE_REFERENCE__FROM = "FROM_ID";
	public static final String TABLE_NODE_REFERENCE__TO = "TO_ID";
	public static final String TABLE_NODE_REFERENCE__PROPERTY_NAME = "PROPERTY_NAME";
	public static final String TABLE_NODE_REFERENCE__PROPERTY_NAMESPACE = "PROPERTY_NAMESPACE_ID";
	public static final String TABLE_NODE_REFERENCE__PROPERTY_NODE_TYPE = "PROPERTY_NODE_TYPE_ID";
	public static final String TABLE_NODE_REFERENCE__UUID = "UUID";

	public static final String TABLE_NODE_EMBEDED = "CM_NODE_EMBEDED";
	// public static final String TABLE_NODE_EMBEDED__NODE_ID = "NODE_ID";

	public static final String TABLE_NODE_UNSTRUCTURED = "CM_NODE_UNSTRUCTURED";
	// public static final String TABLE_NODE_UNSTRUCTURED__NODE_ID = "NODE_ID";
	public static final String TABLE_NODE_UNSTRUCTURED__VERSION = "VERSION";
	public static final String TABLE_NODE_UNSTRUCTURED__TYPE = "TYPE";
	public static final String TABLE_NODE_UNSTRUCTURED__NODE_TYPE = "NODE_TYPE_ID";
	public static final String TABLE_NODE_UNSTRUCTURED__PROP_DEF = "PROP_DEF_ID";
	public static final String TABLE_NODE_UNSTRUCTURED__MULTIPLE = "MULTIPLE";
	public static final String TABLE_NODE_UNSTRUCTURED__LONG_VALUE = "LONG_VALUE";
	public static final String TABLE_NODE_UNSTRUCTURED__STRING_VALUE = "STRING_VALUE";
	public static final String TABLE_NODE_UNSTRUCTURED__DATE_VALUE = "DATE_VALUE";
	public static final String TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE = "DOUBLE_VALUE";
	public static final String TABLE_NODE_UNSTRUCTURED__BOOLEAN_VALUE = "BOOLEAN_VALUE";

	public static final String TABLE_NODE_UNSTRUCTURED_VALUES = "CM_NODE_UNSTRUCT_VALUES";
	public static final String TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY = "PROPERTY_ID";

	public static final String TABLE_NODETYPE_PROPERTY_CONSTRAINT__VALUE = "VALUE";
	public static final String TABLE_NODETYPE_PROPERTY_CONSTRAINT__PROPERTY_ID = "PROPERTY_ID";

	public static final String TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__VALUE = "PROP_VALUE";
	public static final String TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__TYPE = "PROP_TYPE";
	public static final String TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__PROPERTY_ID = "PROPERTY_ID";

	public static final char RIGHT_INDEX = '}';
	public static final char LEFT_INDEX = '{';
	public static final char PATH_DELIMITER = '/';

	// ********* FTS Related Constants *************

	// public static final String FIELD_FTS_DATA = "FTS_TEXT";
	public static final String FIELD_FTS_DATA_XYZ = "FTS_TEXT2";
	public static final String FTS_INDEX_NAME = "IDX_FTS";

	public static final String TABLE_OCR_DATA = "CM_OCR_DATA";
	public static final String TABLE_OCR_DATA__SERVER_ID = "CM_OCR_DATA";
	public static final String TABLE_OCR_DATA__OPERATION = "OPERATION";
	public static final String TABLE_OCR_DATA__COMPLETION_DATE = "COMPLETION_DATE";

	public static final String TABLE_OCR_ERROR = "CM_OCR_ERROR";
	public static final String TABLE_OCR_ERROR__ERROR_TYPE = "ERROR_TYPE";
	public static final String TABLE_OCR_ERROR__WORK_ID = "WORK_ID";
	public static final String TABLE_OCR_ERROR__ERROR_CODE = "ERROR_CODE";
	public static final String TABLE_OCR_ERROR__COMMENT = "ERROR_COMMENT";
	public static final String TABLE_OCR_ERROR__DATE = "ERROR_DATE";

	public static final String TABLE_INDEXABLE_DATA = "CM_INDEXABLE_DATA";
	public static final String TABLE_INDEXABLE_DATA__CONTENT_DATA = "CONTENT_DATA";
	public static final String TABLE_INDEXABLE_DATA__MIME_TYPE = "MIME_TYPE";
	public static final String TABLE_INDEXABLE_DATA__OPERATION = "OPERATION";
	public static final String TABLE_INDEXABLE_DATA__RESERVED = "RESERVED";
	public static final String TABLE_INDEXABLE_DATA__TIME = "START_TIME";
	public static final String TABLE_INDEXABLE_DATA__FTS_DATA_ID = "FTS_DATA_ID";
	public static final String TABLE_INDEXABLE_DATA__FTS_STAGE_ID = "FTS_STAGE_ID";
	public static final String TABLE_INDEXABLE_DATA__FINISH_TIME = "PLANNED_FINISH_TIME";

	public static final String TABLE_FTS_INDEXING_ERROR = "CM_FTS_INDEXING_ERROR";
	public static final String TABLE_FTS_INDEXING__ERROR_TYPE = "ERROR_TYPE";
	public static final String TABLE_FTS_INDEXING__ERROR_CODE = "ERROR_CODE";
	public static final String TABLE_FTS_INDEXING__COMMENT = "ERROR_COMMENT";

	public static final String TABLE_INDEX_ENTRY = "CM_INDEX_ENTRY";
	public static final String TABLE_INDEX_ENTRY__WORD = "WORD_ID";
	public static final String TABLE_INDEX_ENTRY__DATA_ID = TABLE_INDEXABLE_DATA__FTS_DATA_ID;

	public static final String TABLE_WORD = "CM_WORD";
	public static final String TABLE_WORD__DATA = "DATA";
	// public static final String TABLE_WORD__STATISTICS = "STAT";

	public static final String TABLE_STOPWORD = "CM_STOPWORD";
	public static final String TABLE_STOPWORD__DATA = "DATA";
	public static final String TABLE_INDEX_STOPWORD_TMP = "CM_TMP_STOPWORD";

	/**
	 * Defines maximal number of references word should have to be considered as
	 * "rare word"
	 */
	public static final int RARE_WORD_MAX_REFS = 1000;
	// ********* END of FTS Related Constants *************

	/*
	 * CM_INDEXABLE_DATA ID NODE_ID CONTENT_DATA MIME_TYPE OPERATION
	 * TEXT_CONTENT_ID TEXT_STORE_ID RESERVED TIME
	 * 
	 * CM_INDEX_ENTRY NODE_ID WORD_ID
	 * 
	 * CM_WORD ID DATA
	 * 
	 * CM_STOPWORD ID DATA
	 */

	// ************* Visiflow Types
	/*
	 * public static final String NS_EWF_NT = "http://www.exigen.com/jcr/nt";
	 * public static final String NS_EWF_MIX = "http://www.exigen.com/jcr/mix";
	 * public static final String NS_EWF = "http://www.exigen.com/jcr";
	 */

	public static final QName VF_DOCUMENT = new QName(ECR_NT_URI, "document");
	public static final QName VF_RESUORCE = new QName(ECR_NT_URI, "resource");

	public static final QName VF_RESOURCE_DATA = new QName(ECR_URI, "data");
	public static final QName VF_RESOURCE_MIMETYPE = new QName(ECR_URI, "mimeType");
	public static final QName JCR_RESOURCE_DATA = new QName(QName.NS_JCR_URI, "data");
	public static final QName JCR_RESOURCE_MIMETYPE = new QName(QName.NS_JCR_URI, "mimeType");

	public static final QName ECR_OCR_MIXIN = new QName(ECR_MIX_URI, "ocr_resource");
	public static final QName ECR_OCR_MIXIN__WORK_ID = new QName(ECR_URI, "ocr_workId");
	public static final QName ECR_OCR_MIXIN__BINARY_PROPERTY_NAME = new QName(ECR_URI, "ocr_binaryPropertyName");
	public static final QName ECR_OCR_MIXIN__CONTENT = new QName(ECR_URI, "ocr_content");
	public static final QName ECR_OCR_MIXIN__CONTENT_MIME_TYPE = new QName(ECR_URI, "ocr_content_mimeType");
	public static final QName ECR_OCR_MIXIN__COMPLETED = new QName(ECR_URI, "ocr_completed");
	public static final QName ECR_OCR_MIXIN__FAILED = new QName(ECR_URI, "ocr_failed");
	public static final QName ECR_OCR_MIXIN__COMPLETED_DATE = new QName(ECR_URI, "ocr_completedDate");
	public static final QName ECR_OCR_MIXIN__PROCESS_FTS = new QName(ECR_URI, "ocr_processFTS");
	public static final QName ECR_OCR_MIXIN__USER_ID = new QName(ECR_URI, "ocr_userId");

	/*
	 * public static final String TABLE_ACE__READ = "P_READ"; public static
	 * final String TABLE_ACE__GRANT = "P_GRANT"; public static final String
	 * TABLE_ACE__REMOVE = "P_REMOVE"; public static final String
	 * TABLE_ACE__ADD_NODE = "P_ADD_NODE"; public static final String
	 * TABLE_ACE__SET_PROPERTY = "P_SET_PROPERTY";
	 */

	// -------------- Content Store Configuration Constants
	// --------------------------------
	/**
	 * Table containing Content Store type default settings
	 */
	public static final String TABLE_STORE_TYPE = "CMCS_STORE_TYPE";
	public static final String TABLE_STORE_TYPE__NAME = "NAME";
	public static final String TABLE_STORE_TYPE__CONFIG = "CONFIG";

	/**
	 * Table containing Content Store settings
	 */
	public static final String TABLE_STORE = "CMCS_STORE";
	public static final String TABLE_STORE__NAME = "NAME";
	public static final String TABLE_STORE__CONFIG = "CONFIG";
	public static final String TABLE_STORE__TYPE = "TYPE";

	/**
	 * Table containing mapping from JCR Content ID to Content Store Content ID
	 */
	public static final String TABLE_CONTENT = "CMCS_CONTENT";
	public static final String TABLE_CONTENT__STORE_NAME = "STORE_NAME";
	public static final String TABLE_CONTENT__STORE_CONTENT_ID = "STORE_CONTENT_ID";
	public static final String TABLE_CONTENT__CONTENT_SIZE = "CONTENT_SIZE";

	/**
	 * Table containing upload schedule from JCR instance to Content Store
	 */
	public static final String TABLE_CONTENT_SCHEDULE__CONTENT_ID = "CONTENT_ID";
	public static final String TABLE_CONTENT_SCHEDULE = "CMCS_CONTENT_SCHEDULE";
	public static final String TABLE_CONTENT_SCHEDULE__STORE_NAME = "STORE_NAME";
	public static final String TABLE_CONTENT_SCHEDULE__INSTANCE_NAME = "INSTANCE_NAME";
	public static final String TABLE_CONTENT_SCHEDULE__OPERATION = "OPERATION";
	public static final String TABLE_CONTENT_SCHEDULE__STATUS = "STATUS";
	public static final String TABLE_CONTENT_SCHEDULE__SET_TIME = "STATUS_SET_TIME";
	public static final String TABLE_CONTENT_SCHEDULE__END_TIME = "STATUS_EXPECTED_END_TIME";
	public static final String TABLE_CONTENT_SCHEDULE__PARAMS = "PARAMS";
	public static final String TABLE_CONTENT_SCHEDULE__LENGTH = "CONTENT_SIZE";

	/*
	 * public static final QName SECURITY_MIXIN = new QName(QName.NS_REP_URI,
	 * "security"); public static final QName NODE_ACCESSCONTROL = new
	 * QName(QName.NS_JCR_URI, "accessControl"); public static final QName
	 * NODE_ACCESSCONTROL_TYPE = new QName(QName.NS_REP_URI, "accessControl");;
	 */

	public static final Long DEFAULT_ID_RANGE = 10000L;

	public static final String PROPERTY_CACHE_MANAGER = "cache";

	public static final char STORE_DELIMITER = '#';

	public static final String REPOSITORY_JNDI_NAME = "jndi_name";

	public static final String PROPERTY_NOT_FOUND = "Configuration property \"{0}\" not found.";

	// default fields for BLOB data storage
	public static final String FIELD_BLOB = "DATA";
	public static final String FIELD_BLOB_LENGTH = "LENGTH";

	public static final int DBOBJ_POS_AFTER_ALL = 0; // after all tables created
	// (default)
	public static final int DBOBJ_POS_BEFORE_TABLE = 1;
	public static final int DBOBJ_POS_AFTER_TABLE = 2;

	public static final int DBOBJ_ACTION_CREATE = 1;
	public static final int DBOBJ_ACTION_DELETE = 2;
	public static final int DBOBJ_ACTION_EXISTS = 4;
	public static final int DBOBJ_ACTION_STATUS = 8;
	public static final int DBOBJ_ACTION_COMPILE = 16;

	// Stored procedures
	public static final String STORED_PROC_ZIP_AND_MOVE = "ZIP_AND_MOVE";
	public static final String STORED_PROC_CONVERT_AND_MOVE = "CONVERT_AND_MOVE";

	// constants for FTS CONVERT_AND_MOVE/ZIP_AND_MOVE results

	public static final int RC_FTS_CONV_OK = 0;
	public static final int RC_FTS_CONV_NO_ROWS = 1;
	public static final int RC_FTS_CONV_TOO_MANY_ROWS = 2;
	public static final int RC_FTS_CONV_EXTRACT_ERR = 3;
	public static final int RC_FTS_CONV_COMPRESS_ERR = 4;
	public static final int RC_FTS_CONV_UPDATE_ERR = 5;
	public static final int RC_FTS_CONV_DELETE_ERR = 6;
	public static final int RC_FTS_CONV_ERR = 7;
	public static final int RC_FTS_CONV_MSSQL_OLE_ERR = 101;
	public static final int RC_FTS_CONV_MSSQL_MISSING_ZIP_FILTER = 102;
	public static final int RC_FTS_CONV_MSSQL_MISSING_DOC_FILTER = 103;
	public static final int RC_FTS_CONV_MSSQL_LOB_TO_FILE_ERR = 104;
	public static final int RC_FTS_CONV_MSSQL_FILE_TO_LOB_ERR = 105;
	public static final int RC_FTS_CONV_MSSQL_EXT_UTIL_ERR = 106;
	public static final int RC_FTS_CONV_MSSQL_BAD_FILE_EXT = 107;

	public static final QName EWF_TRAC_CREATION = new QName(ECR_URI, "trackCreation");

	public static final QName EWF_TRACKABLE__CREATED = new QName(ECR_URI, "created");
	public static final QName EWF_TRACKABLE__CREATEDBY = new QName(ECR_URI, "createdBy");
	public static final QName EWF_TRACKABLE__UPDATED = new QName(ECR_URI, "updated");
	public static final QName EWF_TRACKABLE__UPDATEDBY = new QName(ECR_URI, "updatedBy");

	public static final QName EWF_RESOURCE__STORAGE_NAME = new QName(ECR_URI, "storageName");

	public static final QName EWF_UNLOCKABLE = new QName(ECR_MIX_URI, "unlockable");
	public static final QName EWF_UNLOCKABLE__LOCK_TIME = new QName(ECR_URI, "unlockTime");

	public static final QName EWF_LOCKABLE = new QName(ECR_MIX_URI, "lockable");
	public static final QName EWF_LOCKABLE__LOCK_TIME = new QName(ECR_URI, "lockTime");

	public static final QName EWF_RESOURCE__SIZE = new QName(ECR_URI, "size");
	public static final QName EWF_RESOURCE__FILE_NAME = new QName(ECR_URI, "fileName");

	public static final String UNDEFINED_MIME_TYPE = "application/octet-stream";

	public static final QName EWF_STORE_CONFIGURATION = new QName(ECR_MIX_URI, "storeConfiguration");
	public static final QName EWF_STORE_CONFIGURATION__NAME = new QName(ECR_URI, "storeName");

	// 5 minutes
	public static final Long DEFAULT_COMMAND_EXECUTION_DELAY = 300L;

	public static final String PROPERTY_THREADS_ON = "threads.on";
	public static final String PROPERTY_THREADS_COUNT = "threads.count";

	public static final String PROPERTY_CMD_ENFORCE_UNLOCK_ON = "threads.cmd.enforceUnlock.on";
	public static final String PROPERTY_CMD_ENFORCE_UNLOCK_DELAY = "threads.cmd.enforceUnlock.delay";

	public static final String PROPERTY_CMD_EXTRACTOR_ON = "threads.cmd.extractor.on";
	public static final String PROPERTY_CMD_EXTRACTOR_DELAY = "threads.cmd.extractor.delay";

	public static final String PROPERTY_CMD_INDEXER_ON = "threads.cmd.indexer.on";
	public static final String PROPERTY_CMD_INDEXER_DELAY = "threads.cmd.indexer.delay";

	public static final String PROPERTY_CMD_MIMEDETECTOR_ON = "threads.cmd.mimedetector.on";
	public static final String PROPERTY_CMD_MIMEDETECTOR_DELAY = "threads.cmd.mimedetector.delay";

	public static final String PROPERTY_CMD_CLEAN_ON = "threads.cmd.delete.on";
	public static final String PROPERTY_CMD_CLEAN_DELAY = "threads.cmd.delete.delay";

	public static final String PROPERTY_CMD_FREE_RESERVED_ON = "threads.cmd.freeReserved.on";
	public static final String PROPERTY_CMD_FREE_RESERVED_DELAY = "threads.cmd.freeReserved.delay";

	public static final String PROPERTY_CMD_CACHE_CLEANER_ON = "threads.cmd.cacheCleaner.on";
	public static final String PROPERTY_CMD_CACHE_CLEANER_DELAY = "threads.cmd.cacheCleaner.delay";
	public static final String PROPERTY_CMD_CACHE_CLEANER_PARAMS = "threads.cmd.cacheCleaner.params";

	public static final String PROPERTY_CMD_ASYNC_UPDATE_ON = "threads.cmd.asynchUpdate.on";
	public static final String PROPERTY_CMD_ASYNC_UPDATE_DELAY = "threads.cmd.asynchUpdate.delay";
	public static final String PROPERTY_CMD_ASYNC_UPDATE_PARAMS = "threads.cmd.asynchUpdate.params";

	public static final String PROPERTY_CMD_OCR_SEND_ON = "threads.cmd.ocr.send.on";
	public static final String PROPERTY_CMD_OCR_SEND_DELAY = "threads.cmd.ocr.send.delay";

	public static final String PROPERTY_CMD_OCR_CHECK_ON = "threads.cmd.ocr.check.on";
	public static final String PROPERTY_CMD_OCR_CHECK_DELAY = "threads.cmd.ocr.check.delay";

	public static final String PROPERTY_CMD_OCR_RETRIVE_ON = "threads.cmd.ocr.retrive.on";
	public static final String PROPERTY_CMD_OCR_RETRIVE_DELAY = "threads.cmd.ocr.retrive.delay";

	/**
	 * batch size for FTS commands
	 */
	public static final String PROPERTY_CMD_FTS_BATCH_SIZE = "threads.cmd.fts.batchsize";

	public static final String PROPERTY_CMD_XML_FORMAT = "xml.format";

	public static final String EXTRACTED_TEXT_ENCODING = "UNICODE";

	/**
	 * Following flags define operation requested for indexable data record
	 */
	public static final Integer OPERATION_DELETE = 0;

	public static final Integer OPERATION_INSERT = 2;

	public static final Integer OPERATION_TEXT_EXTRACTED = 3;

	public static final String CONFIG_PROPERYT_BAM = "listener.bam";

	public static final String BUILTIN_NODETYPES_RESOURCE_PATH = "com/exigen/cm/jackrabbit/nodetype/builtin_nodetypes.cnd";
	public static final String ECR_NODETYPES_RESOURCE_NAME = "com/exigen/cm/jackrabbit/nodetype/ecr_nodetypes.cnd";

	public static final String DB_TRIGGER_UNSTRUCTURED = "CM_TRIGGER_UNSTR";

	public static final String DB_TRIGGER_UNSTRUCTURED_MULTIPLE = "CM_TRIGGER_UNSTR_MULTIPLE";

	public static final String CONFIGURATION_ATTRIBUTE__BEAN_FACTORY = "SPRING_BEAN_FACTORY";
	public static final String CONFIGURATION_ATTRIBUTE__CONFIGURATOR = "REPOSITORY_CONFIGURATOR";
	public static final String CONFIGURATION_ATTRIBUTE__INITIALIZER = "REPOSITORY_INITIALIZER";

	public static final String SESSION_SEQUENCE = "SESSION_SEQUENCE";

	public static final String TABLE_SESSION_MANAGER__DATE = "LAST_ACCESS_TIME";
	public static final String TABLE_NODE_LOCK_INFO__SESSION_ID = "SESSION_ID";
	public static final Long SESSION_MANAGER_COMMAND_DELAY = 60L;

	public static final String TABLE_ACE_PERMISSION = "CM_ACE_PERMISSIONS";
	public static final String TABLE_ACE_PERMISSION__COLUMN_NAME = "COLUMN_NAME";
	public static final String TABLE_ACE_PERMISSION__EXPORT_NAME = "EXPORT_NAME";
	public static final String TABLE_ACE_PERMISSION__DIRECT = "DIRECT";
	public static final String TABLE_ACE_PERMISSION__PERMISSION_NAME = "PERMISSION_NAME";
	public static final String TABLE_ACE_PERMISSION__SUB_PERMISSIONS = "SUB_PERMISSIONS";

	// // ------------------- QUERY VERSION SUPPORT --------------------
	// public static final String PROPERTY_QUERY_VERSION = "query.version";
}

/*
 * $Log: Constants.java,v $
 * Revision 1.29  2010/09/07 14:14:48  vsverlovs
 * EPB-198: code_review_EPB-105_2010-09-02
 * Revision 1.28 2010/08/27 10:17:51 abarancikovs JIRA:
 * EPB-105 - Can't upgrade JCR repository version 10 to 14 (EPB 7.0 to 7.1)
 * Added schema changes, to reflect requested DB version.
 * 
 * Revision 1.27 2009/03/23 15:23:36 dparhomenko *** empty log message ***
 * 
 * Revision 1.26 2009/02/23 14:30:19 dparhomenko *** empty log message ***
 * 
 * Revision 1.25 2009/02/13 15:36:46 dparhomenko *** empty log message ***
 * 
 * Revision 1.24 2009/02/09 15:13:17 dparhomenko *** empty log message ***
 * 
 * Revision 1.23 2009/02/09 15:02:08 dparhomenko *** empty log message ***
 * 
 * Revision 1.22 2009/02/05 10:00:40 dparhomenko *** empty log message ***
 * 
 * Revision 1.21 2009/02/03 12:28:59 dparhomenko *** empty log message ***
 * 
 * Revision 1.20 2009/02/03 12:28:43 dparhomenko *** empty log message ***
 * 
 * Revision 1.19 2009/01/23 07:21:39 dparhomenko *** empty log message ***
 * 
 * Revision 1.18 2008/09/03 10:25:14 dparhomenko *** empty log message ***
 * 
 * Revision 1.17 2008/07/22 09:06:26 dparhomenko *** empty log message ***
 * 
 * Revision 1.16 2008/07/16 11:42:51 dparhomenko *** empty log message ***
 * 
 * Revision 1.15 2008/07/15 11:27:18 dparhomenko *** empty log message ***
 * 
 * Revision 1.14 2008/06/13 09:35:29 dparhomenko *** empty log message ***
 * 
 * Revision 1.13 2008/06/11 10:07:15 dparhomenko *** empty log message ***
 * 
 * Revision 1.12 2008/06/09 12:36:17 dparhomenko *** empty log message ***
 * 
 * Revision 1.11 2008/06/02 11:36:12 dparhomenko *** empty log message ***
 * 
 * Revision 1.10 2008/05/07 09:14:10 dparhomenko *** empty log message ***
 * 
 * Revision 1.9 2008/04/29 10:55:59 dparhomenko *** empty log message ***
 * 
 * Revision 1.1 2006/02/10 15:50:23 dparhomenko PTR#0143252 start jdbc
 * implementation
 */