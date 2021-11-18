/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import static com.exigen.cm.Constants.*;
import static com.exigen.cm.jackrabbit.name.QName.JCR_ROOT;
import static com.exigen.cm.jackrabbit.name.QName.JCR_SYSTEM;
import static com.exigen.cm.jackrabbit.name.QName.JCR_VERSIONSTORAGE;
import static com.exigen.cm.jackrabbit.name.QName.REP_ROOT;
import static com.exigen.cm.jackrabbit.name.QName.REP_SYSTEM;
import static com.exigen.cm.jackrabbit.name.QName.REP_VERSIONSTORAGE;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.Repository283;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.VersionException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributeListImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.cmd.Command;
import com.exigen.cm.cmd.CommandManager;
import com.exigen.cm.cmd.fts.DeleteCommand;
import com.exigen.cm.cmd.fts.FreeReservedCommand;
import com.exigen.cm.cmd.fts.IndexingCommand;
import com.exigen.cm.cmd.fts.MIMETypeDetectionCommand;
import com.exigen.cm.cmd.fts.TextExtractionCommand;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.ConnectionProvider;
import com.exigen.cm.database.ConnectionProviderImpl;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.IndexDefinition;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.dialect.DatabaseObject;
import com.exigen.cm.database.drop.DropSQLProvider;
import com.exigen.cm.database.objdef.DBObjectDef;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseCountStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.ValueChangeDatabaseStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.JCRTransactionManager;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.impl.cache.DummyCacheManager;
import com.exigen.cm.impl.command.EnforceUnlockCommand;
import com.exigen.cm.impl.command.SessionManagerCommand;
import com.exigen.cm.impl.nodetype.DBNodeTypeReader;
import com.exigen.cm.impl.observation.ObservationManagerFactory;
import com.exigen.cm.impl.observation.RepositoryObservationManagerFactory;
import com.exigen.cm.impl.observation.RepositoryObservationManagerImpl;
import com.exigen.cm.impl.security.RepositorySecurityManager;
import com.exigen.cm.impl.security.SecurityPermission;
import com.exigen.cm.impl.state2.RepositoryStateManager;
import com.exigen.cm.impl.state2._StandaloneStatemanager;
import com.exigen.cm.impl.taskmanager.TaskManager;
import com.exigen.cm.impl.tmp.NodeTypeConflictException;
import com.exigen.cm.impl.upgrade.UpgradeManager;
import com.exigen.cm.impl.upgrade.UpgradeToVersion10;
import com.exigen.cm.impl.upgrade.UpgradeToVersion14;
import com.exigen.cm.impl.xml.SecurityExport;
import com.exigen.cm.impl.xml.SecurityImport;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.EffectiveNodeType;
import com.exigen.cm.jackrabbit.nodetype.InvalidNodeTypeDefException;
import com.exigen.cm.jackrabbit.nodetype.NodeDef;
import com.exigen.cm.jackrabbit.nodetype.NodeDefImpl;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.uuid.UUID;
import com.exigen.cm.jackrabbit.uuid.VersionFourGenerator;
import com.exigen.cm.jackrabbit.value.ValueFactoryImpl;
import com.exigen.cm.jackrabbit.version.NodeStateEx;
import com.exigen.cm.jackrabbit.xml.NodeExportAcceptor;
import com.exigen.cm.jackrabbit.xml.RepositoryImportHandler;
import com.exigen.cm.jackrabbit.xml.RepositoryImporter;
import com.exigen.cm.security.JCRAuthenticator;
import com.exigen.cm.security.JCRPrincipals;
import com.exigen.cm.store.ContentStore;
import com.exigen.cm.store.ContentStoreConstants;
import com.exigen.cm.store.ContentStoreProvider;
import com.exigen.cm.store.ContentStoreProviderConfiguration;
import com.exigen.cm.store.StoreConfigurationImporter;
import com.exigen.cm.store.remote.client.AsynchronousContentStoreUpdater;
import com.exigen.cm.store.remote.client.CacheCleaner;
import com.exigen.vf.commons.logging.LogUtils;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * Repository default implementation 
 */
public class RepositoryImpl implements Repository {  

	/**
	 * Log instance
	 */
	private static Log log = LogFactory.getLog(RepositoryImpl.class);

	//_NodeTypeManagerImpl nodeTypeManager;

    //_NamespaceRegistryImpl namespaceRegistry;
	
	/**
	 * Node type helper
	 */
	private NodeTypeHelper nodeTypeHelper;
	
    /**
     * Options
     */
    private List<String> options = new ArrayList<String>();

    /**
     * Version generator
     */
    private VersionFourGenerator uuidGenerator;
    
    /**
     * Development mode, true if switch on
     */
    private boolean developmentMode = false;

    /**
     * Security copy type
     */
    private SecurityCopyType securityCopyType = SecurityCopyType.Inherit;
    
    /**
     * Drop/create flag
     */
    private boolean dropCreate = false;
    
    /**
     * Skip check flag
     */
    private boolean _skipCheck = false;
    
    /**
     * Reduced version check
     */
    private boolean reducedVersionCheck = true;
    
    /**
     * Deploy development constraints
     */
    private boolean deployDevelopmentConstraints = false;
    
//    private boolean startFTSThreads = false;
//    
//    private boolean ftsThreadsStarted = false;
    
    private JCRAuthenticator authenticator = null;

    private Long versionStorageNodeId;
    private String versionStoragePath;

    //TODO make private
    public Long systemRootId;

    private RepositorySecurityManager securityManager;

    private int queryVersion = 1;
    
    private Set<String> stopwords = new HashSet<String>();
    
    private DataImporter dataImporter = null; 
    
    private Map configuration = null;
    
    
    private NamespaceRegistryImpl namespaceRegistry = null;
    private DBNodeTypeReader nodeTypeReader = null;
    
    private ConnectionProvider connectionProvider;
    private ContentStoreProvider storeProvider;

    private NodeDef rootNodeDef;
    
    int sessionCount = 0;
    
    private CommandManager commandManager;
    
	private final HashMap<String, WorkspaceInfo> wspInfos = new HashMap<String, WorkspaceInfo>();

	private RepositoryObservationManagerFactory observationManagerFactory;
	
	private TaskManager taskManager;

	private RepositoryProviderImpl repositoryProvider;

	private String buildVersion;

	private boolean autoAddLockToken;

	private Long versionStorageDepth;

	private boolean supportFTS;
	
	private boolean supportsContextSecurity = false;
	
	private boolean ignoreLock;

	private Map<String, Object> configurationAttributes = new HashMap<String, Object>();

	private SessionManager sessionManager;

	private boolean allowUpgrade;

    private String repositoryName;

	private boolean supportSecurity;

	private boolean supportNodeTypeCheck;

	private boolean lockDisabled;

	private String ocrServer;

	private boolean startFTSThreads;

    RepositoryImpl() throws RepositoryException {
    	this.stateManager = new RepositoryStateManager(this);
        //
        buildVersion = "dev";
        String pathToThisClass = this.getClass().getResource("/"+this.getClass().getName().replace('.', '/')+".class").toString();
		try {
	        String manifestPath = pathToThisClass.substring(0, pathToThisClass.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
	        Manifest manifest = new Manifest(new URL(manifestPath).openStream());
	        Attributes v = manifest.getMainAttributes();
	        String value = v.getValue("Implementation-Version");
	        buildVersion = value;
		} catch (Exception e) {
		}    	
    }

    RepositoryImpl(RepositoryProviderImpl provider) throws RepositoryException {
    	this();
    	this.repositoryProvider = provider;
    	
        //
    }
    
    public RepositoryImpl(RepositoryProviderImpl provider, Map<String, Object> attributes, String name) throws RepositoryException {
		this(provider);
		this.configurationAttributes  = attributes;
		this.repositoryName = name;
	}

    public Object getConfigurationAttribute(String name){
    	return configurationAttributes.get(name);
    }
    
	public void initialize(Map<String, String> config, DataSource ds) throws RepositoryException {
        try {
            JCRTransactionManager trManager = TransactionHelper.getInstance().getTransactionManager();
            JCRTransaction tr = null;  
            if (trManager != null){
                try {
                	//if (trManager)
                    tr = trManager.startNewTransaction();
                } catch (Exception exc){
                    throw new RepositoryException(exc);
                }
            }
            
            
            initVariables(config, ds);            
            initOptions(); 
            boolean create = checkCreateTables(config);
            if(create){
              initDB(config);
            }
        	printCreatinInfo();

            securityManager.loadPermissions();
            
            sessionManager = new SessionManager(this);
            
            initFTS();
            initOCR();
            
            initCommands(config);

            if ("true".equals(config.get("ignoreLock"))){
            	this.ignoreLock = true;
            }

            initTaskExecutor(config);
            
            initContentStoreProvider(config, create);
            if (create){
            	if (!"true".equals(config.get("SKIP_INIT"))){
            		initDataAndSecurity();
            	}
            }
            
            if (supportFTS){
            	initStopWords();
            }
            initAuthenticator(config);
            
            if (trManager != null){
                try {
                    trManager.commitAndResore(tr);
                } catch (Exception exc){
                    throw new RepositoryException(exc);
                }
            }
            
            //initObservationManager();
            
            executeCustomConfigurator(config);
            
            if (supportOCR){
            	long delay = Long.parseLong(RepositoryConfiguration.DEFAULT_COMMAND_DELAY);
            	commandManager.registerCommand("com.exigen.cm.cmd.ocr.OCRFireEventCommand", delay);
            }

            
            int threadCount = Integer.parseInt(config.get(Constants.PROPERTY_THREADS_COUNT));
            commandManager.setThreadCount(threadCount);
            commandManager.setTaskManager(taskManager);
//            if ("true".equals(config.get(Constants.PROPERTY_THREADS_ON))){
            	commandManager.start();
//            }
            	
        	if(create){
        		 executeCustomInitializer(config);        		
        	}
        } catch (RepositoryException exc){
            throw exc;
        } catch (Exception ex){
            //LogUtils.error(log, ex.getMessage(), ex);
            throw new RepositoryException("Error initializing repository", ex);
        }        
    }
    
    private void initCommands(Map<String, String> config) throws RepositoryException {
    	if ("true".equals(config.get(Constants.PROPERTY_THREADS_ON))){
	        //configure commands
        	runCommand(config, PROPERTY_CMD_ENFORCE_UNLOCK_ON, PROPERTY_CMD_ENFORCE_UNLOCK_DELAY,EnforceUnlockCommand.class);	
	           
	        if (isSupportFTS()) {
	        	runCommand(config, PROPERTY_CMD_EXTRACTOR_ON, PROPERTY_CMD_EXTRACTOR_DELAY,TextExtractionCommand.class);	
	        	runCommand(config, PROPERTY_CMD_INDEXER_ON, PROPERTY_CMD_INDEXER_DELAY,IndexingCommand.class);	
	        	runCommand(config, PROPERTY_CMD_MIMEDETECTOR_ON, PROPERTY_CMD_MIMEDETECTOR_DELAY,MIMETypeDetectionCommand.class);	
	        }
	        
	        if (isSupportOCR()){
	        	runCommand(config, Constants.PROPERTY_CMD_OCR_SEND_ON, Constants.PROPERTY_CMD_OCR_SEND_DELAY,"com.exigen.cm.cmd.ocr.OCRSendCommand");	
	        	runCommand(config, Constants.PROPERTY_CMD_OCR_CHECK_ON, Constants.PROPERTY_CMD_OCR_CHECK_DELAY,"com.exigen.cm.cmd.ocr.OCRCheckCommand");	
	        	runCommand(config, Constants.PROPERTY_CMD_OCR_RETRIVE_ON, Constants.PROPERTY_CMD_OCR_RETRIVE_DELAY,"com.exigen.cm.cmd.ocr.OCRRetriveCommand");	
	        }
	            
        	runCommand(config, PROPERTY_CMD_CLEAN_ON, PROPERTY_CMD_CLEAN_DELAY, DeleteCommand.class);	
	
        	runCommand(config, PROPERTY_CMD_FREE_RESERVED_ON, PROPERTY_CMD_FREE_RESERVED_DELAY, FreeReservedCommand.class);	
	        
        	runCommand(config, PROPERTY_CMD_CACHE_CLEANER_ON, PROPERTY_CMD_CACHE_CLEANER_DELAY, new CacheCleaner(JCRHelper.getPropertiesByPrefix(PROPERTY_CMD_CACHE_CLEANER_PARAMS, config)));	
	
        	runCommand(config, PROPERTY_CMD_ASYNC_UPDATE_ON, PROPERTY_CMD_ASYNC_UPDATE_DELAY, new AsynchronousContentStoreUpdater(JCRHelper.getPropertiesByPrefix(PROPERTY_CMD_ASYNC_UPDATE_PARAMS, config)));	
        }
		
	}

	private void initFTS() throws RepositoryException{
    	DatabaseConnection conn = connectionProvider.createConnection();
		DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(TABLE_SYSTEM_PROPERTIES, FIELD_ID, PROPERTY_SUPPORT_FTS);
		try {
			st.execute(conn);
		} catch (RepositoryException exc){
			throw new RepositoryException("Error loading FTS settings from database.");
		}
		RowMap row = st.getRow();
		String supportFTSStr = row.getString(TABLE_SYSTEM_PROPERTIES__VALUE);
		this.supportFTS = Boolean.parseBoolean(supportFTSStr);
		st.close();
		conn.close();
		
	}
    
    private void initOCR() throws RepositoryException{
    	DatabaseConnection conn = connectionProvider.createConnection();
		DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(TABLE_SYSTEM_PROPERTIES, FIELD_ID, PROPERTY_SUPPORT_OCR);
		try {
			st.execute(conn);
		} catch (RepositoryException exc){
			throw new RepositoryException("Error loading OCR settings from database.");
		}
		RowMap row = st.getRow();
		String supportFTSStr = row.getString(TABLE_SYSTEM_PROPERTIES__VALUE);
		this.supportOCR = Boolean.parseBoolean(supportFTSStr);
		st.close();
		if (supportOCR){
			st = DatabaseTools.createSelectOneStatement(TABLE_SYSTEM_PROPERTIES, FIELD_ID, PROPERTY_SUPPORT_OCR_SERVER);
			try {
				st.execute(conn);
			} catch (RepositoryException exc){
				throw new RepositoryException("Error loading OCR settings from database.");
			}
			row = st.getRow();
			ocrServer = row.getString(TABLE_SYSTEM_PROPERTIES__VALUE);
			st.close();
		}
		conn.close();
    }

	private void initTaskExecutor(Map<String, String> configuration) throws RepositoryException{
		taskManager = TaskManager.getInstance(configuration);
    }

	//private void initObservationManager() throws RepositoryException{
		//this.observationManagerFactory = new RepositoryObservationManagerFactory(this);
		
		//RepositoryObservationManagerImpl oManager = this.observationManagerFactory.getObservationManager();
		/*oManager.addEventListener(new RepositoryTestEventListener(), 
				Event.NODE_ADDED
				, "/",
                true,
                null,
                null,
                false);*/
		
	//}

	protected void executeCustomConfigurator(Map<String, String> config) throws RepositoryException {
		if (configurationAttributes.containsKey(Constants.CONFIGURATION_ATTRIBUTE__CONFIGURATOR)){
			((RepositoryConfigurator)configurationAttributes.get(Constants.CONFIGURATION_ATTRIBUTE__CONFIGURATOR)).configure(this, config);
		} else if (config.containsKey(PROPERTY_CONFIGURATOR)){
            String className = (String) config.get(PROPERTY_CONFIGURATOR);
            RepositoryConfigurator configurator;
            try {
                configurator = (RepositoryConfigurator) Class.forName(className).newInstance();
            } catch (Exception e) {
                throw new RepositoryException("Error instantiating configurator class: "+className);
            } 
            configurator.configure(this, config);
        }
    }
    
	protected void executeCustomInitializer(Map<String, String> config) throws RepositoryException {
        DatabaseConnection conn = connectionProvider.createConnection();  
        try{

			if (configurationAttributes.containsKey(Constants.CONFIGURATION_ATTRIBUTE__INITIALIZER)){
				((RepositoryInitializer)configurationAttributes.get(Constants.CONFIGURATION_ATTRIBUTE__INITIALIZER)).initialize(this, config, conn);
			} else if (config.containsKey(PROPERTY_INITIALIZER)){
	            String className = (String) config.get(PROPERTY_INITIALIZER);
	            RepositoryInitializer configurator;
	            try {
	                configurator = (RepositoryInitializer) Class.forName(className).newInstance();
	            } catch (Exception e) {
	                throw new RepositoryException("Error instantiating configurator class: "+className, e);
	            } 
	            configurator.initialize(this, config, conn);
	        }
			conn.commit();
        } finally {
        	conn.close();
        }
        
    }
    
    private void initContentStoreProvider(Map<String, String> config, boolean createStores) throws RepositoryException {
        if(createStores){
            DatabaseConnection connection = getConnectionProvider().createConnection();
            boolean success = false;
            try{
                new StoreConfigurationImporter().importConfiguration(connection, config);
                connection.commit();
                success=true;
            }catch(RepositoryException ex){
                throw ex;
            }catch(RuntimeException ex){// runtime is already logged somewhere
                throw new RepositoryException(ex);
            }catch(Exception ex){
                String message = "Failed to import Content Stores definitions.";
                log.error(message, ex);
                throw new RepositoryException(message, ex);
            }finally{
                if(!success)
                    connection.rollback();
                connection.close();
            }
        }

        
        Map<String, String> storesConfig = JCRHelper.getPropertiesByPrefix(ContentStoreConstants.STORE_CONFIG_PREFIX, config);
        ContentStoreProviderConfiguration storeProviderConfig = new ContentStoreProviderConfiguration();
        storeProviderConfig.configure(storesConfig);
        String defaultStoreModeLocal = config.get(ContentStoreConstants.PROP_STORE_DEFAULT_MODE_LOCAL);
        if(defaultStoreModeLocal != null) // if not defined let's configuration keep default value
            storeProviderConfig.setDefaultStoreModeLocal(new Boolean(defaultStoreModeLocal));
        
        
        this.storeProvider.init(getConnectionProvider(), storeProviderConfig);
        

        if(createStores){
            Set<String> storeNames = storeProvider.getStoreNames();
//          first drop stores
            for(String storeName:storeNames) 
                storeProvider.getStore(storeName, false).drop();
//          then create            
            for(String storeName:storeNames)
                storeProvider.getStore(storeName, false).create();
        }else{
//            if (!_skipCheck){
                storeProvider.validateStores();
//            }
//            for(String storeName:storeNames)
//                storeProvider.getStore(storeName).validate();
        }
    }
    
    private void initAuthenticator(Map<String, String> config) throws RepositoryException {
        Map autheticatorConfig = JCRHelper.getPropertiesByPrefix("authenticator", config);
        this.authenticator.init(autheticatorConfig, this);
    }

    /**
     * Returns repository content store provider.
     * @return
     */
    public ContentStoreProvider getContentStoreProvider(){
        return storeProvider;
    }

    
    private void initDataAndSecurity() throws RepositoryException {
        getDataImporter().importData();
        getDataImporter().importSecurity();
    }
    
    public NamespaceRegistryImpl getNamespaceRegistry() throws RepositoryException {
        return namespaceRegistry;
    }
    
    public DBNodeTypeReader getNodeTypeReader() throws RepositoryException {
        return nodeTypeReader;
    }
    
    private DataImporter getDataImporter() throws RepositoryException {
        if (dataImporter == null) {
            Map<String, String> importConfig = JCRHelper.getPropertiesByPrefix("import", configuration);
            dataImporter = new DataImporter(importConfig, this);
        }
        return dataImporter;
    }
    
    
    private boolean checkCreateTables(Map config) throws RepositoryException, InvalidNodeTypeDefException {
        DatabaseConnection conn = connectionProvider.createConnection();  
        
        try {
        	if (!dropCreate){
        		try {
        			doVersionCheck();
        		} catch (EmptyDatabaseException exc){
        		    if (developmentMode){
        		        return true;
        		    } else {
        		        throw new EmptyDatabaseException("JCR schema not found");
        		    }
        		}
        	}

        	if (_skipCheck){
                namespaceRegistry = new NamespaceRegistryImpl(this);
                nodeTypeReader = new DBNodeTypeReader(getNamespaceRegistry());        
                namespaceRegistry.loadNamespaces(conn);
                nodeTypeReader.loadNodeTypes(conn); 
                configureSystemNodes(conn);
        		return false;
        	}
        	
            Map<String, TableDefinition> dbTables = DatabaseTools.getDatabaseTables(conn);
            List<String> missingTables = new LinkedList<String>();
            //checking that tables in database are the same with the model
            boolean isEmpty = false;
            try {
            	isEmpty = checkStaticTables(dbTables, missingTables, conn);
            } catch (RepositoryException exc){
            	if (!developmentMode || !dropCreate){
            		throw exc;
            	}
            }

        	if (developmentMode) {                                              
                
                    if (dropCreate && !isEmpty) {
                        dropRepository(config, conn);
                        dbTables = DatabaseTools.getDatabaseTables(conn);
                        missingTables.clear();
                        //checking that tables in database are the same with the model
                        isEmpty = checkStaticTables(dbTables, missingTables, conn);                
                    } else if (_skipCheck){
                    	try {
                    		doVersionCheck();
                		} catch (EmptyDatabaseException exc){
                			return true;
                		}
                        namespaceRegistry = new NamespaceRegistryImpl(this);
                        nodeTypeReader = new DBNodeTypeReader(getNamespaceRegistry());        
                        namespaceRegistry.loadNamespaces(conn);
                        nodeTypeReader.loadNodeTypes(conn); 
                        configureSystemNodes(conn);
                    	return false;
                    }
            } else {
                if (dropCreate) {
                    LogUtils.warn(log, "\"{0}\" is set to \"false\" and \"{1}\" is set to \"true\". \"{1}\" is ignored.", 
                                    PROPERTY_DEVELOPMENT_MODE, 
                                    PROPERTY_DATASOURCE_DROP_CREATE);
                }
            }
            
            
            
            if (isEmpty) {
                if (developmentMode){
//                    moved up to initilize();
//                    LogUtils.info(log, "Creating database...");
//                    conn = initDB(conn, config);
//                    dbTables = DatabaseTools.getDatabaseTables(conn);
//                    LogUtils.info(log, "Done.");
                    return true;
                } else {
                    String msg = LogUtils.error(log, "Repository not configured, please use CreateRepository utility.");
                    throw new RepositoryException(msg);
                }
            } else if (!missingTables.isEmpty()) {
                String msg = "The following tables are missing in database: {0}";
                msg = MessageFormat.format(msg, new Object[]{missingTables});
                LogUtils.error(log, msg);
                throw new RepositoryException(msg);
            } else {
                namespaceRegistry = new NamespaceRegistryImpl(this);
                nodeTypeReader = new DBNodeTypeReader(getNamespaceRegistry());        
                namespaceRegistry.loadNamespaces(conn);
                nodeTypeReader.loadNodeTypes(conn);                
            }
            
            missingTables.clear();
            
            //checking custom node types TabldeDefintions

            List nodeTypes = getNodeTypeReader().all();
            for (Iterator iter = nodeTypes.iterator(); iter.hasNext();) {
                NodeTypeDef nodeTypeDef = (NodeTypeDef) iter.next();
                TableDefinition tableDef = nodeTypeDef.getTableDefinition(new ArrayList<TableDefinition>(), conn);
                tableDef.setTableName(connectionProvider.getDialect().convertTableName(tableDef.getTableName()));
                if (dbTables.containsKey(tableDef.getTableName())) {                        
                    isEmpty = false;
                    TableDefinition dbTableDef = (TableDefinition)dbTables.get(tableDef.getTableName());
                    List<String> missingColumns = new LinkedList<String>();
                    for (Iterator columns = tableDef.getColumnIterator(); columns.hasNext();) {
                        ColumnDefinition column = (ColumnDefinition) columns.next();
                        if (!dbTableDef.containsColumn(column)) {
                            missingColumns.add(column.getColumnName());                            
                        }
                    }
                    if (!missingColumns.isEmpty()) {
                        String msg = null;
                        msg = "Table \"{0}\" is missing following columns: {1}";
                        msg = MessageFormat.format(msg, new Object[]{
                                        tableDef.getTableName(), 
                                        missingColumns});
                        LogUtils.error(log, msg);
                        throw new RepositoryException(msg);    
                    }
                } else {
                    missingTables.add(tableDef.getTableName());                                       
                }                                         
            }
            
            if (!missingTables.isEmpty()) {
                String msg = "The following tables are missing in database: {0}";
                msg = MessageFormat.format(msg, new Object[]{missingTables});
                LogUtils.error(log, msg);
                throw new RepositoryException(msg);
            }                
            //Right now, only OracleDialect does that check
            //by altering ID sequence, setting increment by to default value DEFAULT_ID_RANGE
            connectionProvider.getDialect().checkIdGeneratorInfrastracture(conn);
            
            
            // check other objects (stored procedures, specific grants,....)
            List<DBObjectDef> specObjs=connectionProvider.getDialect().getSpecificDBObjectDefs(conn,config);
        	for (int j=0;j<specObjs.size();j++){
        		DBObjectDef o=specObjs.get(j);
        		if (o.isActionAvailable(DBOBJ_ACTION_EXISTS) && o.checkExists(conn)!=true){
        				String msg="Missing database object: "+o.getDescription();
        				LogUtils.error(log,msg);
        				throw new RepositoryException(msg);
       			}
        		if (o.isActionAvailable(DBOBJ_ACTION_STATUS)){
        			if (o.checkStatus(conn)!=true){
        				String msg="Invalid database object: "+o.getDescription();
        				if (o.isActionAvailable(DBOBJ_ACTION_COMPILE)){
        					LogUtils.info(log,msg+". Trying to recompile");
        					o.compile(conn);
        					// check status again
        					if (o.checkStatus(conn)){
        						LogUtils.info(log,"Recompilation successful");
        					}else{
        						msg="Unsuccessful recompilation of: "+o.getDescription();
        						LogUtils.error(log,msg);
        						throw new RepositoryException(msg);
        					}	
        				}else{
        					LogUtils.error(log,msg);
        					throw new RepositoryException(msg);
        				}	
        			}	
        		}	
        	}	
            
//            validate stores -> moved to initContentStoreProvider()
//            Set<String> storeNames = storeProvider.getStoreNames();
//            for(String storeName:storeNames) {
//                ContentStore store = storeProvider.getStore(storeName);
//                store.validate();
//            }
            
            configureSystemNodes(conn);    
            conn.commit();

        } finally {
            conn.close();
        } 
        return false;
    }

    private void doVersionCheck() throws RepositoryException{
		DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(TABLE_SYSTEM_PROPERTIES, FIELD_ID, 
				TABLE_SYSTEM_PROPERTIES__VALUE__DB_VERSION);
		JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
		DatabaseConnection conn = connectionProvider.createConnection();
		try {
    		try {
    			//conn.lockTableRow(TABLE_SYSTEM_PROPERTIES, FIELD_ID, 
    			//		TABLE_SYSTEM_PROPERTIES__VALUE__DB_VERSION);

    			st.execute(conn);
    		} catch (RepositoryException exc){
    			if (!checkSystemTableExist(conn)){
    				throw new EmptyDatabaseException("Incorrect JCR tables structure, please fix(recreate) database.");
    			} else {
    				StringBuffer sb = new StringBuffer();
    				DatabaseSelectAllStatement st1 = DatabaseTools.createSelectAllStatement(TABLE_SYSTEM_PROPERTIES, true);
    				try {
						st1.execute(conn);
						while (st1.hasNext()){
							RowMap row = st1.nextRow();
							sb.append(row.getString(FIELD_ID));
							sb.append("=");
							sb.append(row.getString(TABLE_SYSTEM_PROPERTIES__VALUE));
							sb.append("\r\n");
						}
					} catch (Exception e) {
						sb.append("Table not found");
					} finally {
						st1.close();
					}
    				throw new RepositoryException(
							"Incorrect JCR tables structure, please fix(recreate) database.(DB version row is missed);\r\nSystem properties:\r\n"
									+ sb.toString(), exc);
    			}
    		}
    		RowMap row = st.getRow();
    		String activeVersion = row.getString(TABLE_SYSTEM_PROPERTIES__VALUE);
    		if (!DATABASE_VERSION.equals(activeVersion)){
    			st = DatabaseTools.createSelectOneStatement(TABLE_SYSTEM_PROPERTIES, FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__BUILD_VERSION);
    			String buildVersion = "unknown";
	    		try {
	    			st.execute(conn);
		    		RowMap row1 = st.getRow();
	    			buildVersion = row1.getString(TABLE_SYSTEM_PROPERTIES__VALUE);
	    		} catch (RepositoryException exc){
	    		}
	    		//TODO try to upgrade
	    		UpgradeManager manager = new UpgradeManager(this, conn);
	    		if (manager.upgrade(activeVersion, DATABASE_VERSION)){
	    			return;
	    		}
	    		
	    		TransactionHelper.getInstance().rollbackAndResore(tr);
	    		tr = null;
	    		conn.rollback();
    			throw new RepositoryException("Incorrect JCR tables structure, detected "+activeVersion+" version, but required "+DATABASE_VERSION+
    					" version, please fix(recreate) database. Database created with build "+buildVersion);
    		} else {
    			return;
    		}
		} finally {
			st.close();  
			if (tr != null){
			    TransactionHelper.getInstance().commitAndResore(tr);
	            conn.commit();
			}
			conn.close();
		}		
	}

	private boolean checkStaticTables(Map<String, TableDefinition> dbTables, List<String> missingTables, DatabaseConnection conn) throws RepositoryException {
        List<TableDefinition> staticTables = getStaticTableDefenitions(null);
        boolean isEmpty = true;
        for (TableDefinition tableDef: staticTables) {
            tableDef.setTableName(connectionProvider.getDialect().convertTableName(tableDef.getTableName()));
            if (dbTables.containsKey(tableDef.getTableName())) {                        
                isEmpty = false;
                TableDefinition dbTableDef = (TableDefinition)dbTables.get(tableDef.getTableName());
                List<String> missingColumns = new LinkedList<String>();
                for (Iterator columns = tableDef.getColumnIterator(); columns.hasNext();) {
                    ColumnDefinition column = (ColumnDefinition) columns.next();
                    if (!dbTableDef.containsColumn(column)) {
                        missingColumns.add(column.getColumnName());                            
                    }
                }
                if (!missingColumns.isEmpty()) {
                    String msg = null;
                    msg = "Table \"{0}\" is missing following columns: {1}";
                    msg = MessageFormat.format(msg, new Object[]{
                                    tableDef.getTableName(), 
                                    missingColumns});
                    LogUtils.error(log, msg);
                    throw new RepositoryException(msg);    
                }
            } else {
                if (!isEmpty) {
                    missingTables.add(tableDef.getTableName());
                }                                       
            }                    
        }
        if (!isEmpty){
        	validateDBVersion(conn);
        }
        return isEmpty;
    }

    private void dropRepository(Map config, DatabaseConnection _conn) throws RepositoryException {
        LogUtils.info(log, "Dropping database...");
        disableTransaction();
		TransactionHelper helper = TransactionHelper.getInstance();
		JCRTransaction tr = null;
		if (helper.getTransactionManager() != null) {
			tr = helper.getTransactionManager().startNewTransaction();
		}

		// Drop content stores registered for this repository.
		DatabaseConnection conn = connectionProvider.createConnection();
		ContentStoreProvider csProvider = getContentStoreProvider();
		if (csProvider != null) {
			for (String storeName : csProvider.getStoreNames()) {
				csProvider.getStore(storeName).drop();
			}
		}
		DropSQLProvider dropProvider = conn.getDialect().getDropProvider2(config);

		dropProvider.setConnection(conn);
		dropProvider.drop();
		conn.commit();
		conn.close();

		if (tr != null) {
			helper.getTransactionManager().commitAndResore(tr);
		}
		
		LogUtils.info(log, "Done.");
    }
 
    private void dropDatabase(Map config, DatabaseConnection conn) throws RepositoryException {
        LogUtils.info(log, "Dropping database...");
        disableTransaction();

//        //drop stores -> moved to initContentStoreProvider
//        Set<String> storeNames = storeProvider.getStoreNames();
//        for(String storeName:storeNames) {
//            ContentStore store = storeProvider.getStore(storeName);
//            store.drop();
//        }
        
        // first drop known specific DB objects (in reverse order)
        List<DBObjectDef> specObjs=conn.getDialect().getSpecificDBObjectDefs(conn,config);
       	for (int j=specObjs.size()-1;j>=0;j--){
       		DBObjectDef o=specObjs.get(j);
        	if (o.isActionAvailable(DBOBJ_ACTION_DELETE))
        		o.delete(conn);
       	}

        
        DropSQLProvider dropProvider = conn.getDialect().getDropProvider(config);
        dropProvider.setConnection(conn);
        dropProvider.drop();
        
        
        LogUtils.info(log, "Done.");
        conn.commit();

    }
 
    
    private void initOptions() {
        options.add(Repository.OPTION_LOCKING_SUPPORTED);
        options.add(Repository.OPTION_VERSIONING_SUPPORTED);
        options.add(Repository283.OPTION_NODE_TYPE_REG_SUPPORTED);
        options.add(Repository.LEVEL_2_SUPPORTED);

        options.add(Repository.SPEC_VERSION_DESC);
        options.add(Repository.LEVEL_1_SUPPORTED);
        options.add(Repository.SPEC_NAME_DESC);
        options.add(Repository.REP_VENDOR_DESC);
        options.add(Repository.REP_VENDOR_URL_DESC);
        options.add(Repository.REP_VERSION_DESC);
        options.add(Repository.REP_NAME_DESC);        
    }
    
    private void initVariables(Map<String, String> config, DataSource ds) throws RepositoryException {
// this methos expects that config is already filled with default values, if some properties are missed

    	String supportFTSStr = (String)config.get(PROPERTY_SUPPORT_FTS);
    	String supportOCRStr = (String)config.get(PROPERTY_SUPPORT_OCR);
    	String ocrServer = (String)config.get(PROPERTY_SUPPORT_OCR_SERVER);
    	String supportversionCheckStr = (String)config.get(Constants.PROPERTY_SUPPORT_VERSIONING_CHECK);
    	String supportLockDisableStr = (String)config.get(Constants.PROPERTY_SUPPORT_LOCK_DISABLE);
    	String supportSecurityStr = (String)config.get(Constants.PROPERTY_SUPPORT_SECURITY);
    	String supportNodeTypeCheckStr = (String)config.get(Constants.PROPERTY_SUPPORT_NODETYPE_CHECK);
        String developmentModeStr = (String)config.get(PROPERTY_DEVELOPMENT_MODE);
        String moveSecurityWithNodeStr = (String)config.get(PROPERTY_SECURITY_MOVE_WITH_NODE);
    	String autoAddLockTokenStr = (String)config.get(Constants.PROPERTY_AUTO_ADD_LOCK_TOKEN);
        String dropCreateStr = (String)config.get(PROPERTY_DATASOURCE_DROP_CREATE);
        String allowUpgradeStr = (String)config.get(PROPERTY_DATASOURCE_ALLOW_UPGRADE);
        String skipCheckStr = (String)config.get(PROPERTY_DATASOURCE_SKIP_CHECK);
        String reducedVersionCheckStr = (String)config.get(PROPERTY_DATASOURCE_REDUCED_VERSION_CHECK_CHECK);
        String authenticatorClassName = (String)config.get(PROPERTY_AUTHENTICATOR_CLASS_NAME);
        String startFTSThreadsStr = (String)config.get(Constants.PROPERTY_THREADS_ON);
        String rootUserName = (String)config.get(PROPERTY_ROOT_USER);
        String rootPassword = (String)config.get(PROPERTY_ROOT_PASSWORD);
        String cacheManagerClassName = (String) config.get(PROPERTY_CACHE_MANAGER);
        String ignonreCaseInSecurityStr = (String)config.get(PROPERTY_IGNORE_CASE_IN_SECURITY);
        cacheManagerClassName = DummyCacheManager.class.getName();

            
            // init simple variables
            
                      
        LogUtils.debug(log, "Start indexing and text extracting threads: {0}", startFTSThreadsStr);
        this.startFTSThreads = Boolean.valueOf(startFTSThreadsStr).booleanValue();

        LogUtils.debug(log, "Support FTS: {0}", supportFTSStr);
        this.supportFTS = Boolean.valueOf(supportFTSStr).booleanValue();
        
        LogUtils.debug(log, "Support OCR: {0}", supportOCRStr);
        this.supportOCR = Boolean.valueOf(supportOCRStr).booleanValue();
        
        if (supportOCR){
	        LogUtils.debug(log, "OCR Server: {0}", ocrServer);
	        this.ocrServer = ocrServer;
        }
        
        LogUtils.debug(log, "Support Simplified checkedout check: {0}", supportversionCheckStr);
        this.alwaysCheckCheckedOut = Boolean.valueOf(supportversionCheckStr).booleanValue();
        
        LogUtils.debug(log, "Support Lock Disable: {0}", supportLockDisableStr);
        this.lockDisabled = Boolean.valueOf(supportLockDisableStr).booleanValue();
        
        LogUtils.debug(log, "Support Security: {0}", supportSecurityStr);
        this.supportSecurity = Boolean.valueOf(supportSecurityStr).booleanValue();
        
        LogUtils.debug(log, "Support NodeType check: {0}", supportNodeTypeCheckStr);
        this.supportNodeTypeCheck = Boolean.valueOf(supportNodeTypeCheckStr).booleanValue();
        
        LogUtils.debug(log, "Ignore case in security: {0}", ignonreCaseInSecurityStr);
        this.ignoreCaseInSecurity = Boolean.valueOf(ignonreCaseInSecurityStr).booleanValue();
        
        LogUtils.debug(log, "Working in development mode: {0}", developmentModeStr);
        this.developmentMode = Boolean.valueOf(developmentModeStr).booleanValue();
            
        LogUtils.debug(log, "Auto add lock token to session: {0}", autoAddLockTokenStr);
        this.autoAddLockToken = Boolean.valueOf(autoAddLockTokenStr).booleanValue();
            
        LogUtils.debug(log, "Drop and create database: {0}", dropCreateStr);
        this.dropCreate = Boolean.valueOf(dropCreateStr).booleanValue();

        LogUtils.debug(log, "Allow upgrade database: {0}", allowUpgradeStr);
        this.allowUpgrade = Boolean.valueOf(allowUpgradeStr).booleanValue();

        LogUtils.debug(log, "Move ACE with node: {0}", moveSecurityWithNodeStr);
        this.securityCopyType = SecurityCopyType.valueOf(moveSecurityWithNodeStr);

        //if (developmentMode){
        LogUtils.debug(log, "Skip database validation: {0}", skipCheckStr);
        this._skipCheck = Boolean.valueOf(skipCheckStr).booleanValue();
        if (_skipCheck){
        	LogUtils.warn(log, "*** Skip database validation, please ensure that database are created and contains valid repository ***");
        }
        //}

        LogUtils.debug(log, "Reduced version validation: {0}", reducedVersionCheck);
        this.reducedVersionCheck = Boolean.valueOf(reducedVersionCheckStr).booleanValue();

        LogUtils.debug(log, "Root user name \"{0}\" .", rootUserName);
        LogUtils.debug(log, "Root user password \"{0}\" .", rootPassword.replaceAll("(.)", "*"));
            
            // init classes
        this.configuration = config;

        this.connectionProvider = new ConnectionProviderImpl();
        connectionProvider.configure(config, ds);

        this.storeProvider = new ContentStoreProvider();
//      this.storeProvider = new ContentStoreProvider(storesConfig, getConnectionProvider());
        

        LogUtils.debug(log, "Authenticator class name \"{0}\" .", authenticatorClassName);                 
        this.authenticator = (JCRAuthenticator)JCRHelper.loadAndInstantiateClass(authenticatorClassName);
        this.authenticator.setRepository(this);

/*        LogUtils.debug(log, "Cache Manager class name \"{0}\" .", cacheManagerClassName);                 
        this.cManager = (CacheManager)JCRHelper.loadAndInstantiateClass(cacheManagerClassName);
        this.cManager.configure(this, config);*/
           	
        this.uuidGenerator = new VersionFourGenerator();
            
        this.securityManager = new RepositorySecurityManager(this);
        
        this.commandManager = new CommandManager();
        commandManager.setRepository(this);
        commandManager.setConnectionProvider(connectionProvider);
        commandManager.setTaskManager(taskManager);
        commandManager.setStoreProvider(storeProvider);
        commandManager.setFTSBatchSize(Integer.parseInt(getConfigurationProperty(Constants.PROPERTY_CMD_FTS_BATCH_SIZE)));
        
        commandManager.registerCommand(SessionManagerCommand.class, Constants.SESSION_MANAGER_COMMAND_DELAY);
        
//        queryVersion = Integer.parseInt((String)config.get(Constants.PROPERTY_QUERY_VERSION));
    }
    
    private void runCommand(Map<String, String> config,
			String propertyCmdOn, String propertyCmdDelay,
			Class clazz) throws RepositoryException {
        if ("true".equals(config.get(propertyCmdOn))){
        	long delay = Long.parseLong(config.get(propertyCmdDelay));
        	commandManager.registerCommand(clazz, delay);
        }
	}

    private void runCommand(Map<String, String> config,
			String propertyCmdOn, String propertyCmdDelay,
			String clazz) throws RepositoryException {
        if ("true".equals(config.get(propertyCmdOn))){
        	long delay = Long.parseLong(config.get(propertyCmdDelay));
        	commandManager.registerCommand(clazz, delay);
        }
	}

    private void runCommand(Map<String, String> config,
			String propertyCmdOn, String propertyCmdDelay,
			Command command) throws RepositoryException {
        if ("true".equals(config.get(propertyCmdOn))){
        	long delay = Long.parseLong(config.get(propertyCmdDelay));
        	commandManager.registerCommand(command, delay);
        }
	}

	public boolean isLockDisabled() {
		return lockDisabled;
	}

	private void configureSystemNodes(DatabaseConnection conn) throws RepositoryException {
        //DatabaseConnection conn = connectionProvider.createConnection();
        boolean allowClose = conn.isAllowClose();
        conn.setAllowClose(false);
        try {
            //TODO optimize this
            NamespaceRegistryImpl nmRegistry = null;
            NodeTypeManagerImpl ntm = null;
            nmRegistry = getNamespaceRegistry();
            ntm = new NodeTypeManagerImpl(nmRegistry, getNodeTypeReader(), this);
            //NodeTypeRegistry ntRegistry = NodeTypeRegistry.create(nmRegistry,ntm.getReader());
            
//          find system root node
            DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_NODE, true);
            st.addCondition(Conditions.eq(FIELD_ID, Constants.SYSTEM_WORKSPACE_ROOT_ID));
            //st.addCondition(Conditions.isNull(TABLE_NODE__PARENT));
            //st.addCondition(Conditions.isNull(TABLE_NODE__WORKSPACE_ID));
            st.execute(conn);
            systemRootId = null;
            if (!st.hasNext()){
                //create system node
                systemRootId = Constants.SYSTEM_WORKSPACE_ROOT_ID;
                List<DatabaseInsertStatement> inserts = JCRHelper.createNodeStatement(systemRootId, JCR_ROOT, new Long(1),
                        ntm.findNodeTypeDef(REP_ROOT, null).getId(),
                        "", new Long(0),null, systemRootId, null, null, null, nmRegistry,(long) 1);
                for(DatabaseInsertStatement st11:inserts){
                	st11.execute(conn);
                }
                
                //add type
                NodeTypeImpl nt = ntm.getNodeType(REP_ROOT);
                //Long typeId = conn.nextId();
                DatabaseInsertStatement insert = JCRHelper.createNodeTypeStatement(systemRootId, nt.getSQLId(), nt.getSQLId());
                insert.execute(conn);

                insert = JCRHelper.createNodeTypeDetailsStatement(systemRootId, nt.getTableName());
                insert.execute(conn);
                
                conn.commit();
                
            } else {
                RowMap row = st.nextRow();
                systemRootId = row.getLong(FIELD_ID); 
            }

            
            _StandaloneStatemanager sm = createStandaloneStateManager(conn, systemRootId , null, false);
            NodeStateEx rootState = new NodeStateEx(sm.getNodeState(systemRootId, null), sm);
            
            NodeStateEx systemState;
            if (rootState.hasNode(JCR_SYSTEM)){
                systemState = (NodeStateEx) rootState.getNode(JCR_SYSTEM, 1, false);
            } else { 
                systemState = (NodeStateEx) rootState.addNode(JCR_SYSTEM, REP_SYSTEM, null);
                //rootState.save();
                //conn.commit();
            }
            
            // find system node
            /*st = DatabaseTools.createSelectAllStatement(TABLE_NODE, true);
            st.addCondition(Conditions.isNull(TABLE_NODE__PARENT));
            st.addCondition(Conditions.isNull(TABLE_NODE__WORKSPACE_ID));
            st.execute(conn);
            
            Long systemNodeId;
            if (!st.hasNext()){
                //create system node
                systemNodeId = conn.nextId();
                DatabaseInsertStatement insert = JCRHelper.createNodeStatement(systemNodeId, JCR_SYSTEM, new Long(1),
                        ntm.findNodeTypeDef(REP_SYSTEM).getId(),
                        "", new Long(1),null, null, null, null, nmRegistry);
                insert.execute(conn);
                
                //add type
                _NodeTypeImpl nt = ntm.getNodeType(REP_SYSTEM);
                Long typeId = conn.nextId();
                insert = JCRHelper.createNodeTypeStatement(systemNodeId, typeId, nt.getSQLId(), nt.getSQLId());
                insert.execute(conn);

                insert = JCRHelper.createNodeTypeDetailsStatement(systemNodeId, nt.getTableName());
                insert.execute(conn);
                
                conn.commit();
                
            } else {
                RowMap row = st.nextRow();
                systemNodeId = row.getLong(FIELD_ID); 
            }*/
           
            //TODO SESSION IS NULL
            //NodeStateEx state = new NodeStateEx(this, null, nmRegistry, ntm, conn, systemNodeId);
            
            NodeStateEx vs;
            if (systemState.hasNode(JCR_VERSIONSTORAGE)){
                vs = (NodeStateEx) systemState.getNode(JCR_VERSIONSTORAGE, 1, false);
            } else {
                vs = (NodeStateEx) systemState.addNode(JCR_VERSIONSTORAGE, REP_VERSIONSTORAGE, null);

                String hex = "0123456789abcdef";
                //add initial tree
                for(int i = 0 ; i < 16 ; i++){
                	for(int j = 0 ; j < 16 ; j++){
                		String _name = "h"+hex.substring(i,i+1)+hex.substring(j,j+1);
                		QName name = new QName(QName.NS_DEFAULT_URI, _name);
                		NodeStateEx n1 = (NodeStateEx) vs.addNode(name, QName.REP_VERSIONSTORAGE, null, false, false, true, true);
                		/*System.out.println(_name);
                        for(int i1 = 0 ; i1 < 16 ; i1++){
                        	for(int j1 = 0 ; j1 < 16 ; j1++){
                        		String _name1 = "h"+hex.substring(i1,i1+1)+hex.substring(j1,j1+1);
                        		QName name1 = new QName(QName.NS_DEFAULT_URI, _name1);
                        		n1.addNode(name1, QName.REP_VERSIONSTORAGE, null, false, false);
                        	}
                        }*/
                	}
                    //rootState.save();
                }
                
                //systemState.save();
                //conn.commit();
            }
            this.versionStorageNodeId = vs.getNodeId();
            this.versionStoragePath = vs.getNodeState().getInternalPath();
            this.versionStorageDepth = vs.getNodeState().getInternalDepthLong();
            
            
            rootState.save();
            conn.commit();

        } finally {
        	conn.setAllowClose(allowClose);
            //conn.close();
        }
        
    }    
    
    public _StandaloneStatemanager createStandaloneStateManager(DatabaseConnection conn, Long rootId, Long workspaceId) throws RepositoryException {
        return new _StandaloneStatemanager(this, conn, rootId, workspaceId, 
                null, workspaceId == null ? null:workspaceId.toString(), true, null);
    }

    _StandaloneStatemanager createStandaloneStateManager(DatabaseConnection conn, Long rootId, Long workspaceId, boolean securityAllowed) throws RepositoryException {
        return new _StandaloneStatemanager(this, conn, rootId, workspaceId, 
                null, workspaceId == null ? null:workspaceId.toString(), securityAllowed, null);
    }

	public JCRAuthenticator getAuthenticator() {
        return authenticator;
    }

    public Session login(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        if (credentials == null) {
            throw new LoginException("Empty credentials");
        }
        
        JCRPrincipals principals = getAuthenticator().authenticate(credentials, workspaceName);
        
        return login(principals, workspaceName);
    }

    public Session login(Credentials credentials) throws LoginException,
            RepositoryException {
        return login(credentials, null);
    }

    public Session login(String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return login((Credentials) null, workspaceName);
    }

    public Session login() throws LoginException, RepositoryException {
        return login((Credentials) null, null);
    }

    public String[] getDescriptorKeys() {
        return (String[]) options.toArray(new String[options.size()]);
    }

    public String getDescriptor(String key) {
        if (options.contains(key)) {
            return key;
        }
        return null;
    }

    public Session login(JCRPrincipals principals, String workspaceName)
            throws NamespaceException, UnsupportedRepositoryOperationException,
            AccessDeniedException, RepositoryException {
        if (principals == null) {
            throw new LoginException("Empty principals");
        }
        if (workspaceName == null) {
            workspaceName = DEFAULT_WORKSPACE;
        }

        // create workspace
        
        Workspace w = new WorkspaceImpl(this, workspaceName, principals, null);
        sessionCount++;
        return w.getSession();
    }

    public SessionImpl getSystemSession()throws NamespaceException, UnsupportedRepositoryOperationException,
    AccessDeniedException, RepositoryException {
        Workspace w = new WorkspaceImpl(this, null, null, null);
        return (SessionImpl)w.getSession();
    }
    
    public SessionImpl createTrustedSession(String workspaceName, String username, DatabaseConnection conn)throws NamespaceException, UnsupportedRepositoryOperationException,
    AccessDeniedException, RepositoryException {
    	JCRPrincipals jcrPrincipals = new JCRPrincipals(username, new ArrayList(), new ArrayList(), false);
        Workspace w = new WorkspaceImpl(this, workspaceName, jcrPrincipals, conn);
        
        SessionImpl session = (SessionImpl)w.getSession();
        session.setConnection(conn);
		return session;
    }
    
    
    /*public DatabaseConnection createConnection() throws RepositoryException {
        try {
            if (ds == null){
                InitialContext ctx = new InitialContext();
                ds = (DataSource) ctx.lookup(datasourceName);
            }
            Connection conn = ds.getConnection();
            conn.setAutoCommit(false);
            if (dialect.getDatabaseVendor().equals(DatabaseDialect.VENDOR_ORACLE)){
                Statement st = conn.createStatement();
                st.execute("alter session set hash_join_enabled=false");
                st.execute("alter session set optimizer_index_caching=25");
                st.close();
            }
            DatabaseConnection c = new DatabaseConnection(this, conn);
            return c;
        } catch (Exception exc){
            throw new RepositoryException("Error getting connection ", exc);
        }
    }*/
    

    public void registerNodeType(NodeTypeDef nodeTypeDef)
			throws RepositoryException {
    	JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
    	boolean error = false;
		DatabaseConnection conn = connectionProvider.createConnection();
		try {
			NamespaceRegistryImpl nmRegistry = getNamespaceRegistry();

			// check
			DBNodeTypeReader _nodeTypeReader = new DBNodeTypeReader(
					getNamespaceRegistry());
			_nodeTypeReader.loadNodeTypes(conn);
			_nodeTypeReader.addNodeDefinition(nodeTypeDef);
			
			NodeTypeManagerImpl ntm = new NodeTypeManagerImpl(nmRegistry,
					_nodeTypeReader, this);
			ntm.getNodeTypeRegistry();
			NodeTypeIterator allTypes = ntm.getAllNodeTypes();
            // gets all types and finds a type there having specified as super. 
            while (allTypes.hasNext()) {
                allTypes.nextNodeType();
                //NodeType[] superTypes = aType.getSupertypes();
            }
			
			
            ArrayList<NodeTypeDef> defs = new ArrayList<NodeTypeDef>();
            defs.add(nodeTypeDef);
            
			ntm = new NodeTypeManagerImpl(nmRegistry, getNodeTypeReader(), this);
			ntm.registerNodeDefs(conn, defs, false);
			conn.commit();
		} catch (RepositoryException exc){
			error = true;
			throw exc;
		} finally {
			conn.close();
		}
		reloadNodeTypeReader();
		
		if (tr!= null){
			if (error){
				TransactionHelper.getInstance().rollbackAndResore(tr);
			} else {
				TransactionHelper.getInstance().commitAndResore(tr);
			}
			//TransactionHelper.getInstance().resumeTransaction(tr);
		}
		
		increaseNodeTypeCounter();

	}
    
    public void alterNodeType(NodeTypeDef nodeTypeDef) throws RepositoryException, NodeTypeConflictException {
    	JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
    	boolean error = false;
    	try {
	    	DatabaseConnection conn = getConnectionProvider().createConnection();
	    	try {
				NamespaceRegistryImpl nmRegistry = getNamespaceRegistry();            
				NodeTypeManagerImpl ntm = new NodeTypeManagerImpl(nmRegistry, getNodeTypeReader(), this);
				ArrayList<NodeTypeDef> defs = new ArrayList<NodeTypeDef>();
	            defs.add(nodeTypeDef);
	            ntm.registerNodeDefs(conn, defs, true);
				//ntm.alterNodeType(conn, nodeTypeDef);
				conn.commit();
				
	
	    	} finally {
	    		conn.close();
	    	}
			
			reloadNodeTypeReader();
    	} catch (RepositoryException th){
    		error = true;
    		throw th;
    	}
		if (tr!= null){
			if (error){
				TransactionHelper.getInstance().rollbackAndResore(tr);
			} else {
				TransactionHelper.getInstance().commitAndResore(tr);
			}
			//TransactionHelper.getInstance().resumeTransaction(tr);
		}
		
		increaseNodeTypeCounter();
    }
    
    @Deprecated
    public void removeNodeType(String nodeType) throws RepositoryException {
    	JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
    	boolean error = false;
		NamespaceRegistryImpl nmRegistry = getNamespaceRegistry();            
		NodeTypeManagerImpl ntm = new NodeTypeManagerImpl(nmRegistry, getNodeTypeReader(), this);
    	DatabaseConnection conn = getConnectionProvider().createConnection();
    	try {
    		//ntm.removeNodeType(conn, nodeType);
    		ntm.unregisterNodeType(nodeType);
    	} catch (RepositoryException exc){
    		error = true;
    		throw exc;
    	} finally {
    		conn.close();
    	}
		
		reloadNodeTypeReader();
		if (tr!= null){
			if (error){
				TransactionHelper.getInstance().rollbackAndResore(tr);
			} else {
				TransactionHelper.getInstance().commitAndResore(tr);
			}
			//TransactionHelper.getInstance().resumeTransaction(tr);
		}
		increaseNodeTypeCounter();

    }
    
    String activeNodeTypeVersion = null;
    
    public void checkNodeTypeVersion(DatabaseConnection conn) throws RepositoryException{
    	DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(TABLE_SYSTEM_PROPERTIES, FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__NT_VERSION);
    	st.execute(conn);
    	RowMap row = st.getRow();
    	String ver = row.getString(TABLE_SYSTEM_PROPERTIES__VALUE);
    	st.close();
    	if (!ver.equals(activeNodeTypeVersion)){
    		reloadNodeTypeReader();
    	}
    	
    }
    
    synchronized void reloadNodeTypeReader() throws RepositoryException{
    	DatabaseConnection conn = getConnectionProvider().createConnection();
    	try {
	    	DBNodeTypeReader _nodeTypeReader = new DBNodeTypeReader(getNamespaceRegistry());        
	        namespaceRegistry.loadNamespaces(conn);
	        _nodeTypeReader.loadNodeTypes(conn);
	        this.nodeTypeReader = _nodeTypeReader;
            this.nodeTypeManager = null;
            this.stateManager.evictAll();

        	String ver = JCRHelper.getNodeTypeVersion(conn);
        	activeNodeTypeVersion = ver;
        	
        	mixReferenceableUUIDColumn = getNodeTypeManager().findColumnName(QName.MIX_REFERENCEABLE,QName.JCR_UUID);
        	nodeTypeHelper = null;
    	} finally {
    		conn.close();
    	}
		
	}

	public UUID generateUUID(){
		synchronized (uuidGenerator){
			return (UUID) uuidGenerator.nextIdentifier();
		}
    }

    public String getActiveNodeTypeVersion(){
    	return activeNodeTypeVersion;
    }

    
    public ContentStore createContentStore(String name){
        return getContentStoreProvider().getStore(name);//ContentStoreProvider.getInstance().getStore("file");
    }
   
    public void validateDBVersion(DatabaseConnection conn) throws RepositoryException{
    	boolean schemaExists = checkSystemTableExist(conn);
    	
    	if (schemaExists){
    		doVersionCheck();
    	}
    }

    public void printCreatinInfo() throws RepositoryException{
    	DatabaseConnection conn = connectionProvider.createConnection();
    	boolean schemaExists = checkSystemTableExist(conn);
    	
    	if (schemaExists){
    		try {
    			DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(TABLE_SYSTEM_PROPERTIES, FIELD_ID, 
    					Constants.TABLE_SYSTEM_PROPERTIES__VALUE__CREATED_FROM_ADDR);
    			st.execute(conn);
    			RowMap row = st.getRow();
        		String ip = row.getString(TABLE_SYSTEM_PROPERTIES__VALUE);
        		
    			st = DatabaseTools.createSelectOneStatement(TABLE_SYSTEM_PROPERTIES, FIELD_ID, 
				Constants.TABLE_SYSTEM_PROPERTIES__VALUE__CREATED_DATETIME);
				st.execute(conn);
				row = st.getRow();
				String date = row.getString(TABLE_SYSTEM_PROPERTIES__VALUE);

				String user="";
				try {
    			st = DatabaseTools.createSelectOneStatement(TABLE_SYSTEM_PROPERTIES, FIELD_ID, 
    					Constants.TABLE_SYSTEM_PROPERTIES__VALUE__CREATED_SYS_PROP + ".user.name");
				st.execute(conn);
				row = st.getRow();
					user = row.getString(TABLE_SYSTEM_PROPERTIES__VALUE);
				} catch (Throwable e) {
				
				}

				log.info("JCR schema created at "+date+" by "+user+" from "+ip);
			} catch (Throwable e) {
			}
    	}
    	conn.close();
    }

	private boolean checkSystemTableExist(DatabaseConnection conn) throws RepositoryException {
		boolean schemaExists = false;
		DatabaseMetaData metaData = conn.getConnectionMetaData();
    	String tableName = conn.getDialect().convertTableName(TABLE_SYSTEM_PROPERTIES);
    	try {
	        ResultSet rs = metaData.getTables(null, 
	        		conn.getDialect().getDatabaseVendor().equals(DatabaseDialect.VENDOR_ORACLE)?conn.getUserName():null
	        		, tableName, null);
	        try {
	            schemaExists = rs.next();
	        } finally {  
	            rs.close();
	        }  
    	} catch (SQLException exc){
    		
    	}
		return schemaExists;
	}
    
    public void increaseNodeTypeCounter() throws RepositoryException{
        JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
    	DatabaseConnection conn = connectionProvider.createConnection();
    	try {
			conn.lockTableRow(TABLE_SYSTEM_PROPERTIES, TABLE_SYSTEM_PROPERTIES__VALUE__NT_VERSION);
			DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(TABLE_SYSTEM_PROPERTIES, FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__NT_VERSION);
			try {	
				st.execute(conn);
			} catch (ItemNotFoundException exc){
		        DatabaseInsertStatement st1 = DatabaseTools.createInsertStatement(TABLE_SYSTEM_PROPERTIES);
		        st1.addValue(SQLParameter.create(FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__NT_VERSION));
		        st1.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE, "1"));
		        st1.execute(conn);
		        
		        st.execute(conn);

			}
			RowMap row = st.getRow();
			String _value = row.getString(TABLE_SYSTEM_PROPERTIES__VALUE);
			Long value = Long.parseLong(_value);
			value++;
			
			DatabaseUpdateStatement st2 = DatabaseTools.createUpdateStatement(TABLE_SYSTEM_PROPERTIES);
			st2.addCondition(Conditions.eq(FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__NT_VERSION));
			st2.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE, value.toString()));
			st2.execute(conn);
			
			conn.commit();
		} finally {
			conn.close();
		}
		if (tr!= null){
            TransactionHelper.getInstance().commitAndResore(tr);
        }
    	
    }
    
    
    
    public List<TableDefinition> getStaticTableDefenitions(TableDefinition seqTable) throws RepositoryException {
        
        //List<TableDefinition> tableDefs = new LinkedList<TableDefinition>();
    	List<TableDefinition> tableDefs = new ArrayList<TableDefinition>();
    	
    	boolean savedAutoIndexMode;  

        TableDefinition sysObjects = new TableDefinition(TABLE_SYSTEM_OBJECTS, false);
        sysObjects.addColumn(new ColumnDefinition(sysObjects, TABLE_SYSTEM_OBJECTS__TYPE,Types.VARCHAR));
        sysObjects.addColumn(new ColumnDefinition(sysObjects, TABLE_SYSTEM_OBJECTS__NAME,Types.VARCHAR)).setLength(254);
        sysObjects.addColumn(new ColumnDefinition(sysObjects, TABLE_SYSTEM_OBJECTS__PRIVILEGED,Types.BOOLEAN));
    	
        tableDefs.add(sysObjects);

        TableDefinition sysProps = new TableDefinition(TABLE_SYSTEM_PROPERTIES, false);
        sysProps.addColumn(new ColumnDefinition(sysProps, FIELD_ID,Types.VARCHAR, true));
        sysProps.addColumn(new ColumnDefinition(sysProps, TABLE_SYSTEM_PROPERTIES__VALUE,Types.VARCHAR)).setLength(TABLE_SYSTEM_PROPERTIES__VALUE_MAX_LEN);

        tableDefs.add(sysProps);
        
        
        tableDefs.addAll(connectionProvider.getDialect().getSpecificTableDefs(supportFTS));
        
        if (seqTable != null){
        	tableDefs.add(seqTable);
        }

        //SESSION MANAGER
        TableDefinition sessionManagerTable = new TableDefinition(TABLE_SESSION_MANAGER, true);
        sessionManagerTable.addColumn(new ColumnDefinition(sessionManagerTable, TABLE_SESSION_MANAGER__DATE,connectionProvider.getDialect().getColumnTypeTimeStampSQLType()));

        tableDefs.add(sessionManagerTable);

        
        //NODE METADATA TABLES
        
        //Namespace
        TableDefinition namespace = new TableDefinition(TABLE_NAMESPACE, true);
        namespace.addColumn(new ColumnDefinition(namespace, TABLE_NAMESPACE__PREFIX,Types.VARCHAR)).setLength(20);
        namespace.addColumn(new ColumnDefinition(namespace, TABLE_NAMESPACE__URI,Types.VARCHAR)).setLength(254);
        
        //NodeType
        TableDefinition nodeType = new TableDefinition(TABLE_NODETYPE, true);
        nodeType.addColumn(new ColumnDefinition(nodeType, FIELD_NAME,Types.VARCHAR)).setLength(256);
        // The only place where the index over NAMESPACE column can be required - after delete in CM_NAMESPACE
        // (not a typical operation for JCR) so we disable automatic index creation for this FK
        savedAutoIndexMode=nodeType.isAutoCreateIndex();
        if (savedAutoIndexMode!=false){
        	nodeType.setAutoCreateIndex(false);
        }	
        nodeType.addColumn(new ColumnDefinition(nodeType, FIELD_NAMESPACE,Types.INTEGER)).setForeignKey(namespace);
        if (savedAutoIndexMode!=false){
        	nodeType.setAutoCreateIndex(savedAutoIndexMode);
        }
        
        nodeType.addColumn(new ColumnDefinition(nodeType, TABLE_NODETYPE__MIXIN,Types.BOOLEAN));
        nodeType.addColumn(new ColumnDefinition(nodeType, TABLE_NODETYPE__ORDERABLE_CHILDS,Types.BOOLEAN));
        nodeType.addColumn(new ColumnDefinition(nodeType, TABLE_NODETYPE__PRIMARY_ITEM_NAME,Types.VARCHAR)).setLength(256);
        nodeType.addColumn(new ColumnDefinition(nodeType, TABLE_NODETYPE__PRIMARY_ITEM_NAMESPACE,Types.INTEGER));
        nodeType.addColumn(new ColumnDefinition(nodeType, TABLE_NODETYPE__TABLENAME,Types.VARCHAR)).setLength(256);
        nodeType.addColumn(new ColumnDefinition(nodeType, TABLE_NODETYPE__PRESENCECOLUMN,Types.VARCHAR)).setLength(256);
        //nodeType.addColumn(new ColumnDefinition(TABLE_NODETYPE__EMBEDED,Types.BOOLEAN));

        //NodeTypeSuperTypes
        TableDefinition nodeTypeSuperTypes = new TableDefinition(TABLE_NODETYPE_SUPERTYPES, true);
        nodeTypeSuperTypes.addColumn(new ColumnDefinition(nodeTypeSuperTypes, TABLE_NODETYPE_SUPERTYPES__PARENT,Types.INTEGER)).setForeignKey(nodeType);
        nodeTypeSuperTypes.addColumn(new ColumnDefinition(nodeTypeSuperTypes, TABLE_NODETYPE_SUPERTYPES__CHILD,Types.INTEGER)).setForeignKey(nodeType);
        
        //NodeTypeProperty
        TableDefinition nodeTypeProperty = new TableDefinition(TABLE_NODETYPE_PROPERTY, true);
        nodeTypeProperty.addColumn(new ColumnDefinition(nodeTypeProperty, TABLE_NODETYPE_PROPERTY__NODE_TYPE,Types.INTEGER)).setForeignKey(nodeType);
        // The only place where the index over NAMESPACE column can be required - after delete in CM_NAMESPACE
        // (not a typical operation for JCR) so we disable automatic index creation for this FK
        savedAutoIndexMode=nodeTypeProperty.isAutoCreateIndex();
        if (savedAutoIndexMode!=false){
        	nodeTypeProperty.setAutoCreateIndex(false);
        }
        nodeTypeProperty.addColumn(new ColumnDefinition(nodeTypeProperty, FIELD_NAMESPACE,Types.INTEGER)).setForeignKey(namespace);
        if (savedAutoIndexMode!=false){
        	nodeTypeProperty.setAutoCreateIndex(savedAutoIndexMode);
        }
        nodeTypeProperty.addColumn(new ColumnDefinition(nodeTypeProperty, FIELD_NAME,Types.VARCHAR)).setLength(256);
        nodeTypeProperty.addColumn(new ColumnDefinition(nodeTypeProperty, TABLE_NODETYPE_PROPERTY__ON_PARENT_VERSION,Types.INTEGER));
        nodeTypeProperty.addColumn(new ColumnDefinition(nodeTypeProperty, TABLE_NODETYPE_PROPERTY__AUTO_CREATE,Types.BOOLEAN));
        nodeTypeProperty.addColumn(new ColumnDefinition(nodeTypeProperty, TABLE_NODETYPE_PROPERTY__MANDATORY,Types.BOOLEAN));
        nodeTypeProperty.addColumn(new ColumnDefinition(nodeTypeProperty, TABLE_NODETYPE_PROPERTY__PROTECTED,Types.BOOLEAN));
        nodeTypeProperty.addColumn(new ColumnDefinition(nodeTypeProperty, TABLE_NODETYPE_PROPERTY__MILTIPLE,Types.BOOLEAN));
        nodeTypeProperty.addColumn(new ColumnDefinition(nodeTypeProperty, TABLE_NODETYPE_PROPERTY__COLUMN_NAME,Types.VARCHAR)).setLength(256);
        nodeTypeProperty.addColumn(new ColumnDefinition(nodeTypeProperty, TABLE_NODETYPE_PROPERTY__REQUIRED_TYPE,Types.INTEGER));
        nodeTypeProperty.addColumn(new ColumnDefinition(nodeTypeProperty, TABLE_NODETYPE_PROPERTY__INDEXABLE,Types.BOOLEAN));
        nodeTypeProperty.addColumn(new ColumnDefinition(nodeTypeProperty, TABLE_NODETYPE_PROPERTY__FTS,Types.BOOLEAN));
        
        
        //PropertyDefaultValue
        TableDefinition nodeTypePropertyDefaultValue = new TableDefinition(TABLE_NODETYPE_PROPERTY_DEFAULTVALUE,true);
        nodeTypePropertyDefaultValue.addColumn(new ColumnDefinition(nodeTypePropertyDefaultValue, Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__PROPERTY_ID,Types.INTEGER)).setForeignKey(nodeTypeProperty);
        nodeTypePropertyDefaultValue.addColumn(new ColumnDefinition(nodeTypePropertyDefaultValue, Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__TYPE,Types.INTEGER));
        nodeTypePropertyDefaultValue.addColumn(new ColumnDefinition(nodeTypePropertyDefaultValue, Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__VALUE,Types.VARCHAR)).setLength(256);
        
        //PropertyConstraint
        TableDefinition nodeTypePropertyConstraint = new TableDefinition(TABLE_NODETYPE_PROPERTY_CONSTRAINT,true);
        nodeTypePropertyConstraint.addColumn(new ColumnDefinition(nodeTypePropertyConstraint, TABLE_NODETYPE_PROPERTY_CONSTRAINT__PROPERTY_ID,Types.INTEGER)).setForeignKey(nodeTypeProperty);
        nodeTypePropertyConstraint.addColumn(new ColumnDefinition(nodeTypePropertyConstraint, TABLE_NODETYPE_PROPERTY_CONSTRAINT__VALUE,Types.VARCHAR)).setLength(256);
        
        //NodeTypeChilds
        TableDefinition nodeTypeChilds = new TableDefinition(TABLE_NODETYPE_CHILDS,true);
        nodeTypeChilds.addColumn(new ColumnDefinition(nodeTypeChilds, TABLE_NODETYPE_CHILDS__NODE_TYPE,Types.INTEGER)).setForeignKey(nodeType);
        // The only place where the index over NAMESPACE column can be required - after delete in CM_NAMESPACE
        // (not a typical operation for JCR) so we disable automatic index creation for this FK
        savedAutoIndexMode=nodeTypeChilds.isAutoCreateIndex();
        if (savedAutoIndexMode!=false){
        	nodeTypeChilds.setAutoCreateIndex(false);
        }
        nodeTypeChilds.addColumn(new ColumnDefinition(nodeTypeChilds, FIELD_NAMESPACE,Types.INTEGER)).setForeignKey(namespace);
        if (savedAutoIndexMode!=false){
        	nodeTypeChilds.setAutoCreateIndex(savedAutoIndexMode);
        }
        nodeTypeChilds.addColumn(new ColumnDefinition(nodeTypeChilds, FIELD_NAME,Types.VARCHAR)).setLength(256);
        nodeTypeChilds.addColumn(new ColumnDefinition(nodeTypeChilds, TABLE_NODETYPE_CHILDS__ON_PARENT_VERSION,Types.INTEGER));
        nodeTypeChilds.addColumn(new ColumnDefinition(nodeTypeChilds, TABLE_NODETYPE_CHILDS__AUTO_CREATE,Types.BOOLEAN));
        nodeTypeChilds.addColumn(new ColumnDefinition(nodeTypeChilds, TABLE_NODETYPE_CHILDS__MANDATORY,Types.BOOLEAN));
        nodeTypeChilds.addColumn(new ColumnDefinition(nodeTypeChilds, TABLE_NODETYPE_CHILDS__PROTECTED,Types.BOOLEAN));
        nodeTypeChilds.addColumn(new ColumnDefinition(nodeTypeChilds, TABLE_NODETYPE_CHILDS__SAMENAMESIBLING,Types.BOOLEAN));
        nodeTypeChilds.addColumn(new ColumnDefinition(nodeTypeChilds, TABLE_NODETYPE_CHILDS__DEFAULT_NODE_TYPE,Types.INTEGER)).setForeignKey(nodeType);
        
        //NodeTypeChildRequiredType
        TableDefinition nodeTypeChildsReqType = new TableDefinition(TABLE_NODETYPE_CHILDS_REQUIRED_TYPES,true);
        nodeTypeChildsReqType.addColumn(new ColumnDefinition(nodeTypeChildsReqType, TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__CHILD_ID,Types.INTEGER)).setForeignKey(nodeTypeChilds);
        nodeTypeChildsReqType.addColumn(new ColumnDefinition(nodeTypeChildsReqType, TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__NODE_TYPE,Types.INTEGER)).setForeignKey(nodeType); 
        
        
        //*******NODE DATA TABLES*******
               
        //ACE
        TableDefinition ace = new TableDefinition(TABLE_ACE,true);
        ace.addColumn(new ColumnDefinition(ace, TABLE_ACE__USER_ID,Types.VARCHAR).setLength(64));
        ace.addColumn(new ColumnDefinition(ace, TABLE_ACE__GROUP_ID,Types.VARCHAR).setLength(64));
        ace.addColumn(new ColumnDefinition(ace, TABLE_ACE__CONTEXT_ID,Types.VARCHAR)).setLength(64);
        for(SecurityPermission p: SecurityPermission.values()){
            ace.addColumn(new ColumnDefinition(ace, p.getColumnName(),Types.BOOLEAN));
            ace.addColumn(new ColumnDefinition(ace, p.getColumnName()+Constants.TABLE_ACE___DIRECT_SUFFIX, Types.BOOLEAN));
        }

        
        TableDefinition ace2 = new TableDefinition(TABLE_ACE2,false);
        ace2.setAutoCreateIndex(false);
        ace2.addColumn(new ColumnDefinition(ace2, FIELD_TYPE_ID,Types.INTEGER,true)).setForeignKey(ace);
        for(SecurityPermission p: SecurityPermission.values()){
            ace2.addColumn(new ColumnDefinition(ace2, p.getColumnName()+Constants.TABLE_ACE2___PARENT_SUFFIX, Types.INTEGER));
            ace2.addColumn(new ColumnDefinition(ace2, p.getColumnName()+Constants.TABLE_ACE2___SEQUENCE_SUFFIX, Types.INTEGER));
            ace2.addColumn(new ColumnDefinition(ace2, p.getColumnName()+Constants.TABLE_ACE2___FROM_SUFFIX, Types.VARCHAR));
        }

        TableDefinition aceRestrictions = new TableDefinition(TABLE_ACE_RESTRICTION,true);
        aceRestrictions.addColumn(new ColumnDefinition(aceRestrictions, TABLE_ACE_RESTRICTION__ACE_ID,Types.INTEGER,false)).setForeignKey(ace);
        aceRestrictions.addColumn(new ColumnDefinition(aceRestrictions, TABLE_ACE__USER_ID,Types.VARCHAR,false));
        aceRestrictions.addColumn(new ColumnDefinition(aceRestrictions, TABLE_ACE__GROUP_ID,Types.VARCHAR,false));

        
        
        TableDefinition acePermission = UpgradeToVersion10.getTableAcePermission(connectionProvider.getDialect());

        
        //Workspace -- 1
        TableDefinition workspace = new TableDefinition(TABLE_WORKSPACE,true);

        //Node
        TableDefinition node = new TableDefinition(TABLE_NODE,true);
        // no seperate index for FK in PARENT_ID columns - other indexes with 1st column=PARENT_ID can be used
        savedAutoIndexMode=node.isAutoCreateIndex();
        if (savedAutoIndexMode!=false){
        	node.setAutoCreateIndex(false);
        }
        node.addColumn(new ColumnDefinition(node, TABLE_NODE__PARENT,Types.INTEGER)).setForeignKey(node);
        if (savedAutoIndexMode!=false){
        	node.setAutoCreateIndex(savedAutoIndexMode);
        }
        node.addColumn(new ColumnDefinition(node, TABLE_NODE__VERSION_,Types.INTEGER));
        node.addColumn(new ColumnDefinition(node, TABLE_NODE__NODE_TYPE,Types.INTEGER)).setForeignKey(nodeType);
        node.addColumn(new ColumnDefinition(node, TABLE_NODE__SECURITY_ID,Types.INTEGER)); //TODO .setForeignKey(ace); to securityId field
        node.addColumn(new ColumnDefinition(node, FIELD_NAME,Types.VARCHAR)).setLength(254);
        // no index on FK to CM_NAMESPACE; required only in case of DELETE in CM_NAMESPACE (not typical)
        savedAutoIndexMode=node.isAutoCreateIndex();
        if (savedAutoIndexMode!=false){
        	node.setAutoCreateIndex(false);
        }
        node.addColumn(new ColumnDefinition(node, FIELD_NAMESPACE,Types.INTEGER)).setForeignKey(namespace);
        if (savedAutoIndexMode!=false){
        	node.setAutoCreateIndex(savedAutoIndexMode);
        }
        node.addColumn(new ColumnDefinition(node, TABLE_NODE__INDEX,Types.INTEGER));
        node.addColumn(new ColumnDefinition(node, TABLE_NODE__INDEX_MAX,Types.INTEGER));
        node.addColumn(new ColumnDefinition(node, TABLE_NODE__NODE_PATH,Types.VARCHAR)).setLength(3999);
        // no index on FK to CM_WORKSPACE; rquired only in case of delete in CM_WORKSPACE (not typical for JCR)
        savedAutoIndexMode=node.isAutoCreateIndex();
        if (savedAutoIndexMode!=false){
        	node.setAutoCreateIndex(false);
        }
        node.addColumn(new ColumnDefinition(node, TABLE_NODE__WORKSPACE_ID,Types.INTEGER)).setForeignKey(workspace);
        if (savedAutoIndexMode!=false){
        	node.setAutoCreateIndex(savedAutoIndexMode);
        }
        node.addColumn(new ColumnDefinition(node, TABLE_NODE__CONTENT_STORE_CONFIG_NODE,Types.INTEGER)).setForeignKey(node);
        node.addColumn(new ColumnDefinition(node, TABLE_NODE__NODE_DEPTH,Types.INTEGER)).setNotNull(true);
        
        //test purpose
        //node.addColumn(new ColumnDefinition(node, "XML",Types.CLOB));

        /* 
        IndexDefinition _nodeIndex1 = new IndexDefinition(node);
        _nodeIndex1.addColumn(TABLE_NODE__WORKSPACE_ID);
        _nodeIndex1.addColumn(TABLE_NODE__INDEX);
        node.addIndexDefinition(_nodeIndex1);
        
        
        
        IndexDefinition nodeIndex1 = new IndexDefinition(node);
        nodeIndex1.addColumn(TABLE_NODE__PARENT);
        nodeIndex1.addColumn(FIELD_NAME);
        nodeIndex1.addColumn(TABLE_NODE__INDEX);
        node.addIndexDefinition(nodeIndex1);
        
        IndexDefinition nodeIndex3 = new IndexDefinition(node);
        nodeIndex3.addColumn(TABLE_NODE__PARENT);
        nodeIndex3.addColumn(FIELD_NAME);
        nodeIndex3.addColumn(FIELD_NAMESPACE);
        nodeIndex3.addColumn(TABLE_NODE__INDEX);
        node.addIndexDefinition(nodeIndex3);
        */
                
//      intensively used in queries
        IndexDefinition nodeIndex5 = new IndexDefinition(node);
        nodeIndex5.addColumn(TABLE_NODE__PARENT);
        nodeIndex5.addColumn(FIELD_NAME);
        nodeIndex5.addColumn(FIELD_NAMESPACE);
        nodeIndex5.addColumn(TABLE_NODE__INDEX);
        nodeIndex5.addColumn(TABLE_NODE__SECURITY_ID);
        nodeIndex5.setUnique(true);
        nodeIndex5.addColumn(FIELD_ID);
        node.addIndexDefinition(nodeIndex5);
        
        /*
        IndexDefinition nodeIndex2 = new IndexDefinition(node);
        //TODO uncomment me
        //nodeIndex2.setUnique(true);
        nodeIndex2.addColumn(TABLE_NODE__PARENT);
        nodeIndex2.addColumn(FIELD_NAME);
        nodeIndex2.addColumn(TABLE_NODE__INDEX);
        nodeIndex2.addColumn(TABLE_NODE__WORKSPACE_ID);
        node.addIndexDefinition(nodeIndex2);
        */
        
        // intensively used
        IndexDefinition nodeIndex4 = new IndexDefinition(node);
        nodeIndex4.setUnique(true);
        nodeIndex4.addColumn(TABLE_NODE__PARENT);
        nodeIndex4.addColumn(FIELD_NAME);
        nodeIndex4.addColumn(FIELD_NAMESPACE);
        nodeIndex4.addColumn(TABLE_NODE__INDEX);
        nodeIndex4.addColumn(TABLE_NODE__WORKSPACE_ID);
        node.addIndexDefinition(nodeIndex4);
        
        IndexDefinition nodeIndex6 = new IndexDefinition(node);
        nodeIndex6.setUnique(true);
        nodeIndex6.addColumn(FIELD_ID);
        nodeIndex6.addColumn(TABLE_NODE__SECURITY_ID);
        node.addIndexDefinition(nodeIndex6);
        
        // specific queries by NODE_PATH
        IndexDefinition nodeIndex7 = new IndexDefinition(node);
        nodeIndex7.addColumn(TABLE_NODE__NODE_PATH);
        // for idx only access if versioning enabled
        nodeIndex7.addColumn(FIELD_ID);
        nodeIndex7.addColumn(TABLE_NODE__CONTENT_STORE_CONFIG_NODE);
        nodeIndex7.addColumn(TABLE_NODE__SECURITY_ID);
        nodeIndex7.setUnique(true);
        node.addIndexDefinition(nodeIndex7);
        
        // specific queries by node NAME
        IndexDefinition nodeIndex8 = new IndexDefinition(node);
        nodeIndex8.addColumn(FIELD_NAME);
        node.addIndexDefinition(nodeIndex8);
        
        // this allows in many queries (e.g. XPath queries) to use index-only access
        IndexDefinition nodeIndex9 = new IndexDefinition(node);
        nodeIndex9.setUnique(true);
        nodeIndex9.addColumn(FIELD_ID);
        nodeIndex9.addColumn(TABLE_NODE__PARENT);
        nodeIndex9.addColumn(TABLE_NODE__WORKSPACE_ID);
        nodeIndex9.addColumn(TABLE_NODE__NODE_DEPTH);
        nodeIndex9.addColumn(TABLE_NODE__SECURITY_ID);
        node.addIndexDefinition(nodeIndex9);
                
        TableDefinition ftsData = null;
	    if (supportFTS){
	        ftsData = new TableDefinition(TABLE_FTS_DATA,true);
	        ftsData.addColumn(new ColumnDefinition(ftsData, FIELD_TYPE_ID,Types.INTEGER)).setForeignKey(node);
	        ftsData.addColumn(new ColumnDefinition(ftsData, FIELD_NAME,Types.VARCHAR));
	        // no index for FK to CM_NAMESPACE - required only in case of DELETE FROM CM_NAMESPACE (nnot a typical operation)
	        savedAutoIndexMode=ftsData.isAutoCreateIndex();
	        if (savedAutoIndexMode!=false){
	        	ftsData.setAutoCreateIndex(false);
	        }
	        ftsData.addColumn(new ColumnDefinition(ftsData, FIELD_NAMESPACE,Types.INTEGER)).setForeignKey(namespace);
	        if (savedAutoIndexMode!=false){
	        	ftsData.setAutoCreateIndex(savedAutoIndexMode);
	        }
	        ftsData.addColumn(new ColumnDefinition(ftsData, TABLE_FTS_DATA__MIME_SOURCE,Types.VARCHAR));
	        ftsData.addColumn(new ColumnDefinition(ftsData, TABLE_FTS_DATA__TEXT_SOURCE,Types.VARCHAR));
	        // tmp coexistence of BLOB and CLOB
	        //ColumnDefinition column1 = new ColumnDefinition(ftsData, FIELD_FTS_DATA, Types.CLOB);
	        int ftsColType=Types.BLOB;
	        if (connectionProvider.getDialect().getDatabaseVendor().equals(
					DatabaseDialect.VENDOR_ORACLE)
					&& connectionProvider.getDialect().getDatabaseVersion()
							.startsWith("11")) {
				ftsColType = Types.CLOB;
			}
	        ColumnDefinition column2 = new ColumnDefinition(ftsData, FIELD_FTS_DATA_XYZ, ftsColType);
	        
	        //ftsData.addColumn(column1);
	        ftsData.addColumn(column2);
	        /*IndexDefinition idf = new IndexDefinition(ftsData);
	//        idf.setName(ftsData.getTableName().toUpperCase()+"_IDX_FTS");
	        idf.setName(ftsData.getTableName().toUpperCase()+FTS_INDEX_NAME_EXTENSION);
	        
	        idf.addColumn(column1);
	        idf.setFullTextSearch(true);
	        ftsData.addIndexDefinition(idf);*/
	        
	        // tmp - second OracleText index
	        IndexDefinition idf2 = new IndexDefinition(ftsData);
	//      idf.setName(ftsData.getTableName().toUpperCase()+"_IDX_FTS");
	        // TODO tmp coexistence of BLOB and CLOB
	      idf2.setName(Constants.FTS_INDEX_NAME);
	      
	      idf2.addColumn(column2);
	      idf2.setFullTextSearch(true);
	      ftsData.addIndexDefinition(idf2);
	    }

        
		//ACE -- 2
        ColumnDefinition aceSecurityId = new ColumnDefinition(ace, TABLE_NODE__SECURITY_ID,Types.INTEGER);
        ace.addColumn(aceSecurityId ).setForeignKey(node);
//        IndexDefinition aceIndex = new IndexDefinition(ace);
//        aceIndex.addColumn(ace.getColumn(FIELD_TYPE_ID));
//        aceIndex.addColumn(aceSecurityId);
//		ace.addIndexDefinition(aceIndex );

        //node lock
        
        TableDefinition nodeLock = new TableDefinition(TABLE_NODE_LOCK, false);
        ColumnDefinition nodeLockIdColumn = new ColumnDefinition(nodeLock, FIELD_TYPE_ID,Types.INTEGER,true);
    	nodeLock.setAutoCreateIndex(false);
    	if (deployDevelopmentConstraints){
    		nodeLockIdColumn.setForeignKey(node);
    	}
        nodeLock.addColumn(nodeLockIdColumn);
        
        TableDefinition nodeLockInfo = new TableDefinition(_TABLE_NODE_LOCK_INFO, false);
        ColumnDefinition nodeLockInfoIdColumn = new ColumnDefinition(nodeLockInfo, FIELD_TYPE_ID,Types.INTEGER,true);
    	nodeLockInfo.setAutoCreateIndex(false);
    	if (deployDevelopmentConstraints){
    		nodeLockInfoIdColumn.setForeignKey(node);
    	}
        nodeLockInfo.addColumn(nodeLockInfoIdColumn);
        ColumnDefinition nodeLockInfoIdColumn1 = new ColumnDefinition(nodeLockInfo, TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID,Types.INTEGER);
        if (deployDevelopmentConstraints){
        	nodeLockInfoIdColumn1.setForeignKey(node);
        }
        nodeLockInfo.addColumn(nodeLockInfoIdColumn1);
        nodeLockInfo.addColumn(new ColumnDefinition(nodeLockInfo, TABLE_NODE_LOCK_INFO__LOCK_IS_DEEP,Types.BOOLEAN));
        nodeLockInfo.addColumn(new ColumnDefinition(nodeLockInfo, TABLE_NODE_LOCK_INFO__LOCK_OWNER,Types.VARCHAR));
        //nodeLockInfo.addColumn(new ColumnDefinition(nodeLockInfo, TABLE_NODE_LOCK_INFO__LOCK_EXPIRES,connectionProvider.getDialect().getColumnTypeTimeStampSQLType()));
        nodeLockInfo.addColumn(new ColumnDefinition(nodeLockInfo, Constants.TABLE_NODE_LOCK_INFO__SESSION_ID,Types.INTEGER));

        /*IndexDefinition nodeLockInfoIndex = new IndexDefinition(nodeLockInfo);
        nodeLockInfoIndex.addColumn(nodeLockInfo.getColumn(FIELD_TYPE_ID));
		nodeLockInfo.addIndexDefinition(nodeLockInfoIndex);*/
        
        
        
        //Workspace -- 2
        workspace.addColumn(new ColumnDefinition(workspace, TABLE_WORKSPACE__NAME,Types.VARCHAR)).setLength(254);
        workspace.addColumn(new ColumnDefinition(workspace, TABLE_WORKSPACE__ROOT_NODE,Types.INTEGER)).setForeignKey(node);
        
        //Type
        ColumnDefinition typeIdColumn;
        ColumnDefinition typeNodeTypeColumn;
        TableDefinition type = new TableDefinition(TABLE_TYPE, false);
        if (connectionProvider.getDialect().getDatabaseVendor().equals(DatabaseDialect.VENDOR_ORACLE)){
        	type.setIndexOrganized(true);
        	typeIdColumn = new ColumnDefinition(type, FIELD_TYPE_ID,Types.INTEGER,true);
        	typeNodeTypeColumn = new ColumnDefinition(type, TABLE_TYPE__NODE_TYPE,Types.INTEGER,true);
        }else{
        	typeIdColumn = new ColumnDefinition(type, FIELD_TYPE_ID,Types.INTEGER);
        	typeNodeTypeColumn = new ColumnDefinition(type, TABLE_TYPE__NODE_TYPE,Types.INTEGER);
        }
        
        // another composite index with 1st column=typeIdColumn (or IOT/PK in case of Oracle) will be used for FK
        savedAutoIndexMode=type.isAutoCreateIndex();
        if (savedAutoIndexMode!=false){
        	type.setAutoCreateIndex(false);
        }
        typeIdColumn.setForeignKey(node);
        if (savedAutoIndexMode!=false){
        	type.setAutoCreateIndex(savedAutoIndexMode);
        }
        type.addColumn(typeIdColumn);
        typeNodeTypeColumn.setForeignKey(nodeType);
        type.addColumn(typeNodeTypeColumn);
        ColumnDefinition typeFromNodeTypeColumn=new ColumnDefinition(type, TABLE_TYPE__FROM_NODE_TYPE,Types.INTEGER);
        type.addColumn(typeFromNodeTypeColumn).setForeignKey(nodeType);
        {
        	Iterator<ColumnDefinition> i=type.getColumnIterator();
        	while(i.hasNext()){
        		i.next().setNotNull(true);
        	}
        }

        if (!connectionProvider.getDialect().getDatabaseVendor().equals(DatabaseDialect.VENDOR_ORACLE)){
        	IndexDefinition typeIndex1 = new IndexDefinition(type);
        	typeIndex1.addColumn(typeIdColumn);
        	typeIndex1.addColumn(typeNodeTypeColumn);
        	typeIndex1.addColumn(typeFromNodeTypeColumn);
        	typeIndex1.setUnique(true);
        	type.addIndexDefinition(typeIndex1);
        }
        
        
        //NodeParents
        //2 unique indexes: NODE_ID,PARENT_ID(PK) and PARENT_ID,NODE_ID
        TableDefinition nodeParents = new TableDefinition(TABLE_NODE_PARENT,false);
        nodeParents.setAutoCreateIndex(false);
        if (connectionProvider.getDialect().getDatabaseVendor().equals(DatabaseDialect.VENDOR_ORACLE)){
        	nodeParents.setIndexOrganized(true);
        }
        nodeParents.addColumn(new ColumnDefinition(nodeParents, FIELD_TYPE_ID,Types.INTEGER,true)).setForeignKey(node);
        nodeParents.addColumn(new ColumnDefinition(nodeParents, TABLE_NODE_PARENT__PARENT_ID,Types.INTEGER,true)).setForeignKey(node);
        nodeParents.addColumn(new ColumnDefinition(nodeParents, TABLE_NODE_PARENT__LEVEL,Types.INTEGER));
        {
        	Iterator<ColumnDefinition> i=nodeParents.getColumnIterator();
        	while(i.hasNext()){
        		i.next().setNotNull(true);
        	}
        }	
                
        IndexDefinition nodeParentIndex2 = new IndexDefinition(nodeParents);
        nodeParentIndex2.addColumn(TABLE_NODE_PARENT__PARENT_ID);
        nodeParentIndex2.addColumn(FIELD_TYPE_ID);        
        nodeParentIndex2.setUnique(true);
        nodeParents.addIndexDefinition(nodeParentIndex2);
        
        //NodeReferences
        TableDefinition nodeRefs = new TableDefinition(TABLE_NODE_REFERENCE,true);
        nodeRefs.addColumn(new ColumnDefinition(nodeRefs, TABLE_NODE_REFERENCE__FROM,Types.INTEGER)).setForeignKey(node);
        nodeRefs.addColumn(new ColumnDefinition(nodeRefs, TABLE_NODE_REFERENCE__TO,Types.INTEGER)).setForeignKey(node);
        nodeRefs.addColumn(new ColumnDefinition(nodeRefs, TABLE_NODE_REFERENCE__PROPERTY_NAME,Types.VARCHAR)).setLength(254);
        // The only place where the index over NAMESPACE column can be required - after delete in CM_NAMESPACE
        // (not a typical operation for JCR) so we disable automatic index creation for this FK
        savedAutoIndexMode=nodeRefs.isAutoCreateIndex();
        if (savedAutoIndexMode!=false){
        	nodeRefs.setAutoCreateIndex(false);
        }
        nodeRefs.addColumn(new ColumnDefinition(nodeRefs, TABLE_NODE_REFERENCE__PROPERTY_NAMESPACE,Types.INTEGER)).setForeignKey(namespace);
        if (savedAutoIndexMode!=false){
        	nodeRefs.setAutoCreateIndex(savedAutoIndexMode);
        }
        nodeRefs.addColumn(new ColumnDefinition(nodeRefs, TABLE_NODE_REFERENCE__PROPERTY_NODE_TYPE,Types.INTEGER)).setForeignKey(nodeType);
        nodeRefs.addColumn(new ColumnDefinition(nodeRefs, TABLE_NODE_REFERENCE__UUID,Types.VARCHAR));
        
        //NodeEmbededProperties
        //TableDefinition nodeEmbeded = new TableDefinition(TABLE_NODE_EMBEDED,true);
        //nodeEmbeded.addColumn(new ColumnDefinition(nodeEmbeded, FIELD_TYPE_ID,Types.INTEGER)).setForeignKey(node);
        
        //NodeUnstructuredProperty
        TableDefinition nodeUnstruct = new TableDefinition(TABLE_NODE_UNSTRUCTURED,true);
        // For FK in column FIELD_TYPE_ID the composite index will be used. The only place where the index
        // over NAMESPACE column can be required - after delete in CM_NAMESPACE (not a typical operation for
        // JCR) - so we disable automatic index creation for these FK
        savedAutoIndexMode=nodeUnstruct.isAutoCreateIndex();
        if (savedAutoIndexMode!=false){
        	nodeUnstruct.setAutoCreateIndex(false);
        }
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, FIELD_TYPE_ID,Types.INTEGER)).setForeignKey(node);
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, TABLE_NODE_UNSTRUCTURED__VERSION,Types.INTEGER));
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, FIELD_NAME,Types.VARCHAR));
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, FIELD_NAMESPACE,Types.INTEGER)).setForeignKey(namespace);
        if (savedAutoIndexMode!=false){
        	nodeUnstruct.setAutoCreateIndex(savedAutoIndexMode);
        }
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, TABLE_NODE_UNSTRUCTURED__TYPE,Types.INTEGER));
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, TABLE_NODE_UNSTRUCTURED__NODE_TYPE,Types.INTEGER)).setForeignKey(nodeType);
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, TABLE_NODE_UNSTRUCTURED__PROP_DEF,Types.INTEGER)).setForeignKey(nodeTypeProperty);
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, TABLE_NODE_UNSTRUCTURED__MULTIPLE,Types.BOOLEAN));
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, TABLE_NODE_UNSTRUCTURED__LONG_VALUE,Types.INTEGER));
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, TABLE_NODE_UNSTRUCTURED__STRING_VALUE,Types.VARCHAR)).setLength(254);
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, TABLE_NODE_UNSTRUCTURED__DATE_VALUE,Types.TIMESTAMP));
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE,Types.FLOAT));
        nodeUnstruct.addColumn(new ColumnDefinition(nodeUnstruct, TABLE_NODE_UNSTRUCTURED__BOOLEAN_VALUE,Types.BOOLEAN));
        
/*
      Index to optimize queries for IS NULL on unstructured properties
      
      Query example:
      Query selects nodes which have no unstructured property with namespace 2 and name 'upT'
      
        select node.ID
        from CM_NODE node
        where not exists (select prop.node_id 
                          from CM_NODE_UNSTRUCTURED prop 
                          where prop.NODE_ID=node.ID and prop.NAMESPACE=2 and prop.NAME ='upT')
*/
        IndexDefinition nodeUnstructIndex = new IndexDefinition(nodeUnstruct);
        nodeUnstructIndex.addColumn(FIELD_TYPE_ID);
        nodeUnstructIndex.addColumn(FIELD_NAME);
        nodeUnstructIndex.addColumn(FIELD_NAMESPACE);        
        nodeUnstruct.addIndexDefinition(nodeUnstructIndex);

        
        
        
        //NodeUnstructuredPropertyValues
        TableDefinition nodeUnstructValues = new TableDefinition(TABLE_NODE_UNSTRUCTURED_VALUES,true);
        nodeUnstructValues.addColumn(new ColumnDefinition(nodeUnstructValues, TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY,Types.INTEGER)).setForeignKey(nodeUnstruct);
        nodeUnstructValues.addColumn(new ColumnDefinition(nodeUnstructValues, TABLE_NODE_UNSTRUCTURED__LONG_VALUE,Types.INTEGER));
        nodeUnstructValues.addColumn(new ColumnDefinition(nodeUnstructValues, TABLE_NODE_UNSTRUCTURED__STRING_VALUE,Types.VARCHAR)).setLength(254);
        nodeUnstructValues.addColumn(new ColumnDefinition(nodeUnstructValues, TABLE_NODE_UNSTRUCTURED__DATE_VALUE,Types.TIMESTAMP));
        nodeUnstructValues.addColumn(new ColumnDefinition(nodeUnstructValues, TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE,Types.FLOAT));
        nodeUnstructValues.addColumn(new ColumnDefinition(nodeUnstructValues, TABLE_NODE_UNSTRUCTURED__BOOLEAN_VALUE,Types.BOOLEAN));
        nodeUnstructValues.addColumn(new ColumnDefinition(nodeUnstructValues, TABLE_NODE_UNSTRUCTURED__PROP_DEF,Types.INTEGER)).setForeignKey(nodeTypeProperty);
        nodeUnstructValues.addColumn(new ColumnDefinition(nodeUnstructValues, FIELD_TYPE_ID,Types.INTEGER)).setForeignKey(node);
        
        
        //FTS tables
        TableDefinition indexableData = new TableDefinition(TABLE_INDEXABLE_DATA, true);
        //indexableData.addColumn(new ColumnDefinition(indexableData, FIELD_TYPE_ID, Types.INTEGER));
        indexableData.addColumn(new ColumnDefinition(indexableData, TABLE_INDEXABLE_DATA__CONTENT_DATA, Types.VARCHAR));
        indexableData.addColumn(new ColumnDefinition(indexableData, TABLE_INDEXABLE_DATA__MIME_TYPE, Types.VARCHAR));
        indexableData.addColumn(new ColumnDefinition(indexableData, TABLE_INDEXABLE_DATA__OPERATION, Types.INTEGER));
        indexableData.addColumn(new ColumnDefinition(indexableData, TABLE_INDEXABLE_DATA__RESERVED, Types.BOOLEAN)).setNotNull(true);
        indexableData.addColumn(new ColumnDefinition(indexableData, TABLE_INDEXABLE_DATA__TIME, Types.TIMESTAMP));
        indexableData.addColumn(new ColumnDefinition(indexableData, TABLE_INDEXABLE_DATA__FINISH_TIME, Types.TIMESTAMP));
        indexableData.addColumn(new ColumnDefinition(indexableData, TABLE_INDEXABLE_DATA__FTS_DATA_ID, Types.INTEGER));
        indexableData.addColumn(new ColumnDefinition(indexableData, TABLE_INDEXABLE_DATA__FTS_STAGE_ID, Types.INTEGER));
        //indexableData.addColumn(new ColumnDefinition(nodeType, FIELD_NAME,Types.VARCHAR)).setLength(256);
        //indexableData.addColumn(new ColumnDefinition(nodeType, FIELD_NAMESPACE,Types.INTEGER)).setForeignKey(namespace);
        
        TableDefinition ocrData = UpgradeToVersion14.getTableOCRWork(connectionProvider.getDialect());
        TableDefinition ocrError = UpgradeToVersion14.getTableOCRError(connectionProvider.getDialect());
        
        // for load of big number of docs
        IndexDefinition indexableDataContentIdx=new IndexDefinition(indexableData);
        indexableDataContentIdx.addColumn(TABLE_INDEXABLE_DATA__CONTENT_DATA);
        indexableData.addIndexDefinition(indexableDataContentIdx);

        /*TableDefinition stage = new TableDefinition(TABLE_FTS_STAGE, true);
        stage.addColumn(new ColumnDefinition(stage, FIELD_BLOB, Types.BLOB));
        stage.addColumn(new ColumnDefinition(stage, TABLE_FTS_STAGE__FILENAME, Types.VARCHAR));
*/
        
        //FTS tables
        
        // it is possible to hav multiple error records per one id
        // TableDefinition indexableError = new TableDefinition(TABLE_FTS_INDEXING_ERROR, true);
        TableDefinition indexableError = new TableDefinition(TABLE_FTS_INDEXING_ERROR, false);
        indexableError.addColumn(new ColumnDefinition(indexableError, FIELD_ID,Types.INTEGER));
        indexableError.addColumn(new ColumnDefinition(indexableError, TABLE_FTS_INDEXING__ERROR_CODE, Types.VARCHAR));
        indexableError.addColumn(new ColumnDefinition(indexableError, TABLE_FTS_INDEXING__ERROR_TYPE, Types.VARCHAR));
        indexableError.addColumn(new ColumnDefinition(indexableError, TABLE_FTS_INDEXING__COMMENT, Types.VARCHAR));

        
        TableDefinition word = new TableDefinition(TABLE_WORD, true);
        ColumnDefinition wordDataColumn = new ColumnDefinition(word, TABLE_WORD__DATA, Types.VARCHAR); 
        word.addColumn(wordDataColumn);        
//        word.addColumn(new ColumnDefinition(word, TABLE_WORD__STATISTICS, Types.INTEGER));
        IndexDefinition wordDataIndex = new IndexDefinition(word);
        wordDataIndex.addColumn(wordDataColumn);
        wordDataIndex.setUnique(true);
        word.addIndexDefinition(wordDataIndex);
        
//        TableDefinition stopWord = new TableDefinition(TABLE_INDEX_STOPWORD, true);
        TableDefinition stopWord = new TableDefinition(TABLE_STOPWORD, false);
        stopWord.addColumn(new ColumnDefinition(stopWord, TABLE_STOPWORD__DATA, Types.VARCHAR));
        
//        TableDefinition indexEntry = new TableDefinition(TABLE_INDEX_ENTRY, true);
        TableDefinition indexEntry = new TableDefinition(TABLE_INDEX_ENTRY, false);
        indexEntry.addColumn(new ColumnDefinition(indexEntry, TABLE_INDEX_ENTRY__DATA_ID, Types.INTEGER));//.setForeignKey(node);
        indexEntry.addColumn(new ColumnDefinition(indexEntry, TABLE_INDEX_ENTRY__WORD, Types.INTEGER)).setForeignKey(word);

        IndexDefinition indexEntryIndex1 = new IndexDefinition(indexEntry);
        indexEntryIndex1.addColumn(TABLE_INDEX_ENTRY__DATA_ID);
        indexEntry.addIndexDefinition(indexEntryIndex1);
        
        IndexDefinition indexEntryIndex2 = new IndexDefinition(indexEntry);
        indexEntryIndex2.addColumn(TABLE_INDEX_ENTRY__DATA_ID);
        indexEntryIndex2.addColumn(TABLE_INDEX_ENTRY__WORD);
        indexEntryIndex2.setUnique(true);
        indexEntry.addIndexDefinition(indexEntryIndex2);
        //FTS tables end
        
        
//      --------- Content Stores Related Tables ---------------------------------------------
//        Content Store Type configuration
        TableDefinition storeType = new TableDefinition(TABLE_STORE_TYPE, true);
        storeType.addColumn(new ColumnDefinition(storeType, TABLE_STORE_TYPE__NAME, Types.VARCHAR));
        

        ColumnDefinition configST = new ColumnDefinition(storeType, TABLE_STORE_TYPE__CONFIG, Types.VARCHAR);
        configST.setLength(3999);
        storeType.addColumn(configST);

//      Content Store configuration
        TableDefinition store = new TableDefinition(TABLE_STORE, true);
        store.addColumn(new ColumnDefinition(store, TABLE_STORE__NAME, Types.VARCHAR));

        ColumnDefinition configS = new ColumnDefinition(store, TABLE_STORE__CONFIG, Types.VARCHAR);
        configS.setLength(3999);
        store.addColumn(configS);
        store.addColumn(new ColumnDefinition(store, TABLE_STORE__TYPE, Types.INTEGER)).setForeignKey(storeType);

        
        
//      Content JCR ID to Content Store Content ID mapping
        TableDefinition content = new TableDefinition(TABLE_CONTENT, true);
        content.addColumn(new ColumnDefinition(content, TABLE_CONTENT__STORE_NAME, Types.VARCHAR));
        content.addColumn(new ColumnDefinition(content, TABLE_CONTENT__STORE_CONTENT_ID, Types.VARCHAR));
        content.addColumn(new ColumnDefinition(content, TABLE_CONTENT__CONTENT_SIZE, Types.INTEGER));        
        
//      Content upload schedule data
        TableDefinition contentSchedule = new TableDefinition(TABLE_CONTENT_SCHEDULE, true);
        contentSchedule.addColumn(new ColumnDefinition(contentSchedule, TABLE_CONTENT_SCHEDULE__CONTENT_ID, Types.INTEGER));
        contentSchedule.addColumn(new ColumnDefinition(contentSchedule, TABLE_CONTENT_SCHEDULE__INSTANCE_NAME, Types.VARCHAR));
        contentSchedule.addColumn(new ColumnDefinition(contentSchedule, TABLE_CONTENT_SCHEDULE__STORE_NAME, Types.VARCHAR));        
        contentSchedule.addColumn(new ColumnDefinition(contentSchedule, TABLE_CONTENT_SCHEDULE__OPERATION, Types.INTEGER));
        contentSchedule.addColumn(new ColumnDefinition(contentSchedule, TABLE_CONTENT_SCHEDULE__STATUS, Types.INTEGER));
        contentSchedule.addColumn(new ColumnDefinition(contentSchedule, TABLE_CONTENT_SCHEDULE__SET_TIME, Types.TIMESTAMP));
        contentSchedule.addColumn(new ColumnDefinition(contentSchedule, TABLE_CONTENT_SCHEDULE__END_TIME, Types.TIMESTAMP));
        contentSchedule.addColumn(new ColumnDefinition(contentSchedule, TABLE_CONTENT_SCHEDULE__LENGTH, Types.INTEGER));        
        ColumnDefinition params = new ColumnDefinition(contentSchedule, TABLE_CONTENT_SCHEDULE__PARAMS, Types.VARCHAR);
        params.setLength(3999);
        contentSchedule.addColumn(params);        
//      --------- Content Stores Related Tables END -------------------------
        
        
        tableDefs.add(namespace);
        tableDefs.add(nodeType);
        tableDefs.add(nodeTypeSuperTypes);
        tableDefs.add(nodeTypeProperty);
        tableDefs.add(nodeTypePropertyDefaultValue);
        tableDefs.add(nodeTypePropertyConstraint);
        tableDefs.add(nodeTypeChilds);
        tableDefs.add(nodeTypeChildsReqType);
        tableDefs.add(ace);
        tableDefs.add(ace2);
        tableDefs.add(aceRestrictions);
        tableDefs.add(acePermission);
        tableDefs.add(workspace);
        tableDefs.add(node);
        tableDefs.add(nodeLock);
        tableDefs.add(nodeLockInfo);
        tableDefs.add(type);
        tableDefs.add(nodeParents);
        tableDefs.add(nodeRefs);
        //tableDefs.add(nodeEmbeded);
        tableDefs.add(nodeUnstruct);
        tableDefs.add(nodeUnstructValues);        
        tableDefs.add(indexableData);
        tableDefs.add(ocrData);
        tableDefs.add(ocrError);
        if (supportFTS){
            tableDefs.add(indexableError);
            //tableDefs.add(stage);
            tableDefs.add(indexEntry);
            tableDefs.add(word);
	        tableDefs.add(ftsData);
	        tableDefs.add(stopWord);
        }
        
        tableDefs.add(storeType);
        tableDefs.add(store);
        tableDefs.add(content);
        tableDefs.add(contentSchedule);
        
        return tableDefs;
                
    }

    
    private void initDB(Map config) throws RepositoryException, InvalidNodeTypeDefException {
        LogUtils.info(log, "Creating database...");
        DatabaseConnection conn = connectionProvider.createConnection();  
        try{
            conn = initDB(conn, config);
            conn.commit();
        }finally{
            conn.close();
        }
        
        LogUtils.info(log, "Done.");
    }
    
    private void createAdditionalNodeTypeIndex(DatabaseConnection conn, QName nodeType,QName property, String indexName, boolean includeNodeId) throws RepositoryException{
    	NodeTypeDef ntd=getNodeTypeManager().getNodeTypeDef(nodeType);
    	if (ntd==null){
    		String msg = MessageFormat.format("Cannot nodetype \"{0}\"  for indexing", new Object[]{nodeType});
            LogUtils.error(log, msg);
            throw new RepositoryException(msg);
    	}
    	PropDef props[]=ntd.getPropertyDefs();
    	for(int i=0;i<props.length ;i++){
			if (props[i].getName().equals(property)){
				TableDefinition td=ntd.getTableDefinition(new ArrayList<TableDefinition>(),conn);
				// add additional index
				IndexDefinition idx=new IndexDefinition(td);
				idx.setName(indexName);
				idx.addColumn(props[i].getColumnName());
				if (includeNodeId){
					idx.addColumn(Constants.FIELD_TYPE_ID);
				}
				td.addIndexDefinition(idx);
				// execute SQL for added index (last in returned array)
				String[][] sqls=conn.getDialect().buildCreateIndexStatements(td);
				if (sqls.length>0 && sqls[sqls.length-1] != null && sqls[sqls.length-1][1].length() > 0){
                	conn.registerSysObject(DatabaseObject.INDEX, sqls[sqls.length-1][0], false);
                	conn.execute(sqls[sqls.length-1][1]);
				}else{
					String msg = MessageFormat.format("Error generating index for column \"{0}\" in table \"{1}\"",
			        		new Object[]{props[i].getColumnName(),td.getTableName()});
			        LogUtils.error(log, msg);
			        throw new RepositoryException(msg);
				}
				return;
			}
		}
    	String msg = MessageFormat.format("Cannot find column of property \"{0}\"  for indexing",
        		new Object[]{property});
        LogUtils.error(log, msg);
        throw new RepositoryException(msg);
    }
    
    private void createAdditionalNodeTypeIndexes(DatabaseConnection conn) throws RepositoryException{
        /* if versioning is used then 2 indexes are required for these SQLs:
        select * from CM_TYPE_BASE where X_JCR_UUID_24=:1
        and:
        select THIS_.ID as ID from CM_NODE THIS_, CM_TYPE_NT_VERSIONHISTORY vh_ where
		 ( vh_.NODE_ID = THIS_.ID   )  AND  ( vh_.X_JCR_VERSIONABLEUUID_18 = :1 )
        */

    	createAdditionalNodeTypeIndex(conn,QName.NT_VERSIONHISTORY,QName.JCR_VERSIONABLEUUID,"ADDTNLIDX_001",true);
    	createAdditionalNodeTypeIndex(conn,QName.MIX_REFERENCEABLE,QName.JCR_UUID,"ADDTNLIDX_002", false);
    }
    
    private DatabaseConnection initDB(DatabaseConnection conn, Map config) throws RepositoryException, InvalidNodeTypeDefException {
    	
        disableTransaction();

        JCRTransaction tr1 = null;
        if (TransactionHelper.getInstance().getTransactionManager() != null){
        	conn.commit();
        	conn.close();
        	tr1 = TransactionHelper.getInstance().getTransactionManager().startNewTransaction();
        	conn = connectionProvider.createConnection();
        }
        
    	
    	DatabaseDialect dialect = conn.getDialect();
    	dialect.checkInitDBConfiguration(config, supportFTS);
    	dialect.beforeInitializeDatabase(conn);
        
        TableDefinition seqTable = dialect.createIdGeneratorInfrastracture(conn);
        List<TableDefinition> staticTables = getStaticTableDefenitions(seqTable);        
        TableDefinition[] tables = staticTables.toArray(new TableDefinition[staticTables.size()]);
        List<DBObjectDef> specObjs=dialect.getSpecificDBObjectDefs(conn,config);
        
        // Create tables
        for(int i = 0; i < tables.length ; i++){
           	// Specific object(s) before table
        	for (int j=0;j<specObjs.size();j++){
        		DBObjectDef o=specObjs.get(j);
        		if (o.getPositionInObjectList()==DBOBJ_POS_BEFORE_TABLE &&
        				o.getLinkedObjectName().equalsIgnoreCase(tables[i].getTableName()) &&
        				o.isActionAvailable(DBOBJ_ACTION_CREATE))
        			o.create(conn);
        	}
        	// Table 
        	if (!tables[i].getTableName().equals(Constants.TABLE_SYSTEM_OBJECTS)){
        		conn.registerSysObject(DatabaseObject.TABLE, dialect.convertTableName(tables[i].getTableName()), false);
        	}
            String sql = conn.getDialect().buildCreateStatement(tables[i]);
            conn.execute(sql);
        	if (tables[i].getTableName().equals(Constants.TABLE_SYSTEM_OBJECTS)){
        		conn.registerSysObject(DatabaseObject.TABLE, dialect.convertTableName(tables[i].getTableName()), false);
        	}
        	//conn.createTables(new TableDefinition[]{tables[i]});
            // Specific objects(s) after table
        	conn.commit();
        	for (int j=0;j<specObjs.size();j++){
        		DBObjectDef o=specObjs.get(j);
        		if (o.getPositionInObjectList()==DBOBJ_POS_AFTER_TABLE &&
        				o.getLinkedObjectName().equalsIgnoreCase(tables[i].getTableName()) &&
        				o.isActionAvailable(DBOBJ_ACTION_CREATE))
        			o.create(conn);
         	}	
        }
        // Add indexes
        for(int i = 0; i < tables.length ; i++){
            String[][] sqls = dialect.buildCreateIndexStatements(tables[i]);
            for (int j = 0; j < sqls.length; j++) {
                String[] sqlIndex = sqls[j];
                if (sqlIndex != null && sqlIndex[1].length() > 0){
                	conn.registerSysObject(DatabaseObject.INDEX, sqlIndex[0], false);
                    conn.execute(sqlIndex[1]);
                }     
            }       
        }        
        // Add PK-s
        for(int i = 0; i < tables.length ; i++){
            if (tables[i].getPKColumnIterator().hasNext() && !tables[i].isIndexOrganized()){
                String sql = dialect.buildPKAlterStatement(tables[i]);
                conn.execute(sql);
            }
        }
        // Add FK-s
        for(int i = 0; i < tables.length ; i++){
            String sql = dialect.buildFKAlterStatement(tables[i], conn);
            if (sql != null && sql.length() > 0){
                conn.execute(sql);
            }
        }

        // specific objects after all static tables
    	for (int j=0;j<specObjs.size();j++){
    		DBObjectDef o=specObjs.get(j);
    		if (o.getPositionInObjectList()==DBOBJ_POS_AFTER_ALL &&
    				o.isActionAvailable(DBOBJ_ACTION_CREATE))
    			o.create(conn);
    	}
    	conn.commit();

        namespaceRegistry = new NamespaceRegistryImpl(this);
        nodeTypeReader = new DBNodeTypeReader(getNamespaceRegistry());        
        NodeTypeManagerImpl ntm = new NodeTypeManagerImpl(getNamespaceRegistry(), nodeTypeReader, this);

//        create stores -> Moved to initContentStoreProvider()
//        Set<String> storeNames = storeProvider.getStoreNames();
//        for(String storeName:storeNames) {
//            ContentStore store = storeProvider.getStore(storeName);
//            store.create();
//        }
        
        /*if (TransactionHelper.getInstance().getTransactionManager() != null){
        	conn.commit();
        	conn.close();
            JCRTransactionManager trManager = TransactionHelper.getInstance().getTransactionManager();
            try {
                trManager.commit(trManager.getTransaction());
                trManager.begin();
            } catch (Exception e) {
                throw new RepositoryException();
            } 
            conn = connectionProvider.createConnection();
        }*/
        
        /*JCRTransaction tr = TransactionHelper.getInstance().getTransactionManager().getTransaction();
        if (tr != null){
        	TransactionHelper.getInstance().getTransactionManager().commit(tr);
        }*/
        if (tr1 != null){
        	conn.commit();
        	conn.close();
        	TransactionHelper.getInstance().getTransactionManager().commitAndResore(tr1);
        	tr1 = TransactionHelper.getInstance().getTransactionManager().startNewTransaction();
        	conn = connectionProvider.createConnection();
        } /*else {
        	conn.commit();
        	conn.close();
        	conn = connectionProvider.createConnection();
        }*/
        getDataImporter().importBuiltinNodeTypes(conn, getNamespaceRegistry(), ntm);
        getDataImporter().importNodeTypes(conn, getNamespaceRegistry(), ntm);
        namespaceRegistry.loadNamespaces(conn);
        nodeTypeReader.loadNodeTypes(conn);

        
        
                
        createAdditionalNodeTypeIndexes(conn);
        
        if (!"true".equals(config.get("SKIP_INIT"))){
	        createWorkspace(conn, getNamespaceRegistry(), ntm, DEFAULT_WORKSPACE);
	        //createWorkspace(conn, getNamespaceRegistry(), ntm, "test");
	        conn.commit();
	        configureSystemNodes(conn);

	        //initDataAndSecurity();                        
        }
        conn = connectionProvider.getDialect().afterInitializeDatabase(conn, config);

        
        DatabaseInsertStatement st = DatabaseTools.createInsertStatement(TABLE_SYSTEM_PROPERTIES);
        
        st.addValue(SQLParameter.create(FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__DB_VERSION));
        st.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE, Constants.DATABASE_VERSION));
        st.addBatch();
        
        st.addValue(SQLParameter.create(FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__BUILD_VERSION));
        st.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE, buildVersion));
        st.addBatch();
        
        st.addValue(SQLParameter.create(FIELD_ID, PROPERTY_SUPPORT_FTS));
        st.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE, Boolean.toString(supportFTS)));
        st.addBatch();
        
        st.addValue(SQLParameter.create(FIELD_ID, PROPERTY_SUPPORT_OCR));
        st.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE, Boolean.toString(supportOCR)));
        st.addBatch();
        
        st.addValue(SQLParameter.create(FIELD_ID, PROPERTY_SUPPORT_OCR_SERVER));
        st.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE, ocrServer));
        st.addBatch();
        
        st.addValue(SQLParameter.create(FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__CREATED_DATETIME));
        st.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE,(
        		new java.util.Date(System.currentTimeMillis())).toString()));
        st.addBatch();
        
        Properties props=System.getProperties();
        Enumeration propNames = props.propertyNames();
        while ( propNames.hasMoreElements() ) {
        	String propName=(String) propNames.nextElement();
        	st.addValue(SQLParameter.create(FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__CREATED_SYS_PROP+"."+propName));
        	String v=props.getProperty(propName);
        	if (v.length()>TABLE_SYSTEM_PROPERTIES__VALUE_MAX_LEN-1){
        		v=v.substring(0, TABLE_SYSTEM_PROPERTIES__VALUE_MAX_LEN-1);
        	}
            st.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE, v));
        	st.addBatch();
        }
        
        InetAddress iadr=null;
        try{
        	iadr=InetAddress.getLocalHost();
        }catch(UnknownHostException e){
        	
        }
        if (iadr!=null){
        	st.addValue(SQLParameter.create(FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__CREATED_FROM_ADDR));
        	st.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE,iadr.getHostAddress())); 
        	st.addBatch();
        }	
        
        st.executeBatch(conn);
        
       
        
        /*st = DatabaseTools.createInsertStatement(TABLE_SYSTEM_PROPERTIES);
        st.addValue(SQLParameter.create(FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__NT_VERSION));
        st.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE, "1"));
        st.execute(conn);
        */
        
        if (tr1 != null){
        	TransactionHelper.getInstance().getTransactionManager().commitAndResore(tr1);
        }
        
        return conn;
        
    }

    private void disableTransaction() throws RepositoryException{
		if (TransactionHelper.getInstance().isTransactionActive() && TransactionHelper.getInstance().getType() != TransactionHelper.APPLICATION_SERVER_SPRING){
			//throw new RepositoryException("Cannot drop or create repository within transaction");
		}
		
	}

	public void createWorkspace(String name) throws RepositoryException{
    	DatabaseConnection conn = connectionProvider.createConnection();
    	try {
    		createWorkspace(conn, getOrCreateNamespaceRegistry(), getNodeTypeManager(), name);
    	} catch (RepositoryException re){
    		throw re;
		} catch (Exception e) {
			throw new RepositoryException(e);
		} finally{
			conn.close();
		}
    }

    
    private NamespaceRegistryImpl getOrCreateNamespaceRegistry() throws RepositoryException {
		if (getNamespaceRegistry() == null){
			namespaceRegistry = new NamespaceRegistryImpl(this);
			DatabaseConnection conn = getConnectionProvider().createConnection(); 
            namespaceRegistry.loadNamespaces(conn);
            conn.close();
		}
		return getNamespaceRegistry();
	}

	public Long createWorkspace(DatabaseConnection conn, NamespaceRegistryImpl nmRegistry, NodeTypeManagerImpl ntm, String name) throws RepositoryException, NoSuchNodeTypeException {
        log.info("Create workspace: "+name);
        //check workspace
        if (nmRegistry == null){
            nmRegistry = getNamespaceRegistry();
        }
        if (ntm == null){
            ntm = new NodeTypeManagerImpl(nmRegistry, getNodeTypeReader(), this);        
        }
        boolean worksapceExist = checkWorkspace(conn, name);
        if (worksapceExist){
            throw new RepositoryException("Workspace "+name+" already exists");
        }
        
        
        Long workspaceId = conn.nextId();
        Long rootNodeId = conn.nextId();

        //create Workspace
        DatabaseInsertStatement st = DatabaseTools.createInsertStatement(TABLE_WORKSPACE);
        st.addValue(SQLParameter.create(FIELD_ID, workspaceId));
        st.addValue(SQLParameter.create(TABLE_WORKSPACE__NAME, name));
        st.execute(conn);
        st.close();
        
        //create rootNode
        st = DatabaseTools.createInsertStatement(TABLE_NODE);
        st.addValue(SQLParameter.create(FIELD_ID, rootNodeId));
        //st.addValue(SQLParameter.create(TABLE_NODE__PARENT, null));
        st.addValue(SQLParameter.create(TABLE_NODE__VERSION_, new Long(1)));
        st.addValue(SQLParameter.create(TABLE_NODE__NODE_TYPE, ntm.findNodeTypeDef(REP_ROOT, null).getId()));
        st.addValue(SQLParameter.create(TABLE_NODE__SECURITY_ID, rootNodeId));
        st.addValue(SQLParameter.create(FIELD_NAME, JCR_ROOT.getLocalName()));
        st.addValue(SQLParameter.create(FIELD_NAMESPACE, nmRegistry._getByURI(JCR_ROOT.getNamespaceURI()).getId()));
        st.addValue(SQLParameter.create(TABLE_NODE__INDEX,  new Long(1)));
        st.addValue(SQLParameter.create(TABLE_NODE__INDEX_MAX,  new Long(1)));
        st.addValue(SQLParameter.create(TABLE_NODE__NODE_PATH, ""));
        st.addValue(SQLParameter.create(TABLE_NODE__WORKSPACE_ID, workspaceId));
        st.addValue(SQLParameter.create(TABLE_NODE__NODE_DEPTH, new Long(0)));
        st.execute(conn);
        st.close();

        st = DatabaseTools.createInsertStatement(TABLE_NODE_LOCK);
        st.addValue(SQLParameter.create(FIELD_TYPE_ID, rootNodeId));
        st.execute(conn);
        st.close();

        st = DatabaseTools.createInsertStatement(_TABLE_NODE_LOCK_INFO);
        st.addValue(SQLParameter.create(FIELD_TYPE_ID, rootNodeId));
        st.execute(conn);
        st.close();

        
        //create type definition for root node
        NodeTypeDef nd = ntm.findNodeTypeDef(REP_ROOT, null);
        //try {
        	ArrayList<String> tableNames = new ArrayList<String>();
			EffectiveNodeType et = ntm.getNodeTypeRegistry().getEffectiveNodeType(REP_ROOT);
			for(QName typeName:et.getAllNodeTypes()){
				NodeTypeImpl nt = ntm.getNodeType(typeName);
				DatabaseInsertStatement insert = JCRHelper.createNodeTypeStatement(rootNodeId, nt.getSQLId(), nd.getId());
                insert.execute(conn);
                
                boolean tablePresent = false;
                String tableName = nt.getTableName();
                if (tableNames.contains(tableName)){
                	tablePresent = true;
                } else {
                	tableNames.add(tableName);
                }
                
                ValueChangeDatabaseStatement _st;
            	if (tablePresent){
                	_st = DatabaseTools.createUpdateStatement(tableName,FIELD_TYPE_ID,rootNodeId);
            	} else {
                    _st = JCRHelper.createNodeTypeDetailsStatement(rootNodeId, tableName);
            	}
            	
                ((ValueChangeDatabaseStatement)_st).addValue(SQLParameter.create(nt.getPresenceColumn(), true));
                _st.execute(conn);
			}
		/*} catch (NodeTypeConflictException e) {
			throw new RepositoryException("Error initializing root node");
		}*/
        
        
        
        DatabaseUpdateStatement st2 = DatabaseTools.createUpdateStatement(TABLE_WORKSPACE, FIELD_ID, workspaceId);
        st2.addValue(SQLParameter.create(TABLE_WORKSPACE__ROOT_NODE, rootNodeId));
        st2.execute(conn);
        st2.close();
        
        
        conn.commit();
        return workspaceId;
    }    
    
    boolean checkWorkspace(DatabaseConnection conn, String name) throws RepositoryException {
        DatabaseSelectAllStatement stCheck = DatabaseTools.createSelectAllStatement(TABLE_WORKSPACE, true);
        try {
            stCheck.addCondition(Conditions.eq(TABLE_WORKSPACE__NAME, name));
            stCheck.execute(conn);
            if (stCheck.hasNext()){
                return true;
            } else {
                return false;
            }
        } finally {
            stCheck.close();
        }
       
    }

    public ValueFactoryImpl getValueFactory() {
        return new ValueFactoryImpl();
    }

    public Long nextId() throws RepositoryException {
        return connectionProvider.nextId(null);
    }
    
    public Long getSequenceNextId(String sequenceName) throws RepositoryException{
    	return nextId();
    }

    public Long getVersionStorageNodeId() {
        return versionStorageNodeId;
    }
    
    public Long getVersionStorageDepth() {
        return versionStorageDepth;
    }
    
    public String getVersionStoragePath() {
        return versionStoragePath;
    }

    public Long getSystemRootId() {
        return systemRootId;
    }

    public RepositorySecurityManager getSecurityManager() {
        return securityManager;
    } 
    

    
    
    
    /**
     * Loads collection of stopwords from FTS index.
     */
    void initStopWords() throws Exception{
        
        
        
        stopwords = new HashSet<String>();
        DatabaseConnection connection = connectionProvider.createConnection();
        connection.getDialect().initStopWords(connection, configuration);
        
        DatabaseSelectAllStatement swStatement = DatabaseTools.createSelectAllStatement(TABLE_STOPWORD, true);
        swStatement.addResultColumn(TABLE_STOPWORD__DATA);
        swStatement.execute(connection);
        while(swStatement.hasNext())
            stopwords.add(swStatement.nextRow().getString(TABLE_STOPWORD__DATA));
        
        swStatement.close();
        connection.close();
    }

    /**
     * Returns stopwords.
     * @return stopwords set.
     */
    public Set getStopWords(){
        return stopwords;
    }
    
    public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    public void importSecurity(InputStream inputStream) throws RepositoryException {
        SecurityImport secImport = new SecurityImport(null, this);
        secImport.doImport(inputStream);        
    }

    public String getConfigurationProperty(String propName) {
        return (String) configuration.get(propName);
    }

    public NodeDef getRootNodeDef() {
        if (rootNodeDef == null){
            rootNodeDef = new NodeDefImpl();
            rootNodeDef.configure(REP_ROOT ,false, true, false, OnParentVersionAction.ABORT, false);
        }
        return rootNodeDef;
        
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public long getUnprocessedFTSCount() throws RepositoryException{
        DatabaseConnection conn = getConnectionProvider().createConnection();
        try {
            DatabaseCountStatement cs = DatabaseTools.createCountStatement(TABLE_INDEXABLE_DATA);
            cs.execute(conn);
            long count = cs.getCount().longValue();
            cs.close();
            return count;
        } finally {
            conn.close();
        }
    }
    
    public long getUnprocessedOCRCount() throws RepositoryException{
        DatabaseConnection conn = getConnectionProvider().createConnection();
        try {
            DatabaseCountStatement cs = DatabaseTools.createCountStatement(Constants.TABLE_OCR_DATA);
            cs.addCondition(Conditions.gte(Constants.TABLE_OCR_DATA__OPERATION, 0L));
            cs.execute(conn);
            long count = cs.getCount().longValue();
            cs.close();
            return count;
        } finally {
            conn.close();
        }
    }
    
    public ObservationManagerFactory getObservationManagerFactory(
            String workspaceName) throws IllegalStateException, RepositoryException {
        // check sanity of this instance
        // TODO sanityCheck();

        return getWorkspaceInfo(workspaceName).getObservationManagerFactory();
        //throw new UnsupportedOperationException();
    }
    
    private String syncObj = new String("");
    
	public RepositoryObservationManagerFactory getObservationManagerFactory() throws RepositoryException {
		if (observationManagerFactory == null){
			synchronized (syncObj){
				if (observationManagerFactory == null){
					this.observationManagerFactory = new RepositoryObservationManagerFactory(this);
				}
			}
			//commandManager.registerCommand(observationManagerFactory);
		}
		return this.observationManagerFactory;
	}
	
	public RepositoryObservationManagerImpl getObservationManager() throws RepositoryException{
		return getObservationManagerFactory().getObservationManager();
	}

    
    protected WorkspaceInfo getWorkspaceInfo(String workspaceName)
            throws IllegalStateException, RepositoryException {
        // check sanity of this instance
        //TODO sanityCheck();

        WorkspaceInfo wspInfo = wspInfos.get(workspaceName);
        if (wspInfo == null) {
            //TODO temp solution, need read workspaces frow db
            //throw new NoSuchWorkspaceException(workspaceName);
            WorkspaceConfig cfg = new WorkspaceConfig(workspaceName);
            wspInfo = new WorkspaceInfo(this, cfg);
            //wspInfo.initialize();
            wspInfos.put(workspaceName, wspInfo);
        }

        synchronized (wspInfo) {
            if (!wspInfo.isInitialized()) {
                try {
                    initWorkspace(wspInfo);
                } catch (RepositoryException e) {
                    log.error("Unable to initialize workspace '"
                            + workspaceName + "'", e);
                    throw new NoSuchWorkspaceException(workspaceName);
                }
            }
        }
        return wspInfo;
    }
    
    
    private void initWorkspace(WorkspaceInfo wspInfo) throws RepositoryException {
        // first initialize workspace info
        wspInfo.initialize();
        // get system session and Workspace instance
        /*SessionImpl sysSession = wspInfo.getSystemSession();
        WorkspaceImpl wsp = (WorkspaceImpl) sysSession.getWorkspace();
*/
        /**
         * todo implement 'System' workspace
         * FIXME
         * - the should be one 'System' workspace per repository
         * - the 'System' workspace should have the /jcr:system node
         * - versions, version history and node types should be reflected in
         *   this system workspace as content under /jcr:system
         * - all other workspaces should be dynamic workspaces based on
         *   this 'read-only' system workspace
         *
         * for now, we just create a /jcr:system node in every workspace
         */
/*        NodeImpl rootNode = (NodeImpl) sysSession.getRootNode();
        if (!rootNode.hasNode(JCR_SYSTEM)) {
            NodeTypeImpl nt = sysSession.getNodeTypeManager().getNodeType(REP_SYSTEM);
            NodeImpl sysRoot = rootNode.internalAddChildNode(JCR_SYSTEM, nt, SYSTEM_ROOT_NODE_ID);
            // add version storage
            nt = sysSession.getNodeTypeManager().getNodeType(REP_VERSIONSTORAGE);
            sysRoot.internalAddChildNode(JCR_VERSIONSTORAGE, nt, VERSION_STORAGE_NODE_ID);
            // add node types
            nt = sysSession.getNodeTypeManager().getNodeType(REP_NODETYPES);
            sysRoot.internalAddChildNode(JCR_NODETYPES, nt, NODETYPES_NODE_ID);
            rootNode.save();
        }

        // register the repository as event listener for keeping repository statistics
        wsp.getObservationManager().addEventListener(this,
                Event.NODE_ADDED | Event.NODE_REMOVED
                | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED,
                "/", true, null, null, false);

        // register SearchManager as event listener
        SearchManager searchMgr = wspInfo.getSearchManager();
        if (searchMgr != null) {
            wsp.getObservationManager().addEventListener(searchMgr,
                    Event.NODE_ADDED | Event.NODE_REMOVED
                    | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED
                    | Event.PROPERTY_CHANGED,
                    "/", true, null, null, false);
        }

        // register the observation factory of that workspace
        delegatingDispatcher.addDispatcher(wspInfo.getObservationManagerFactory());
        */
        //throw new UnsupportedOperationException();
    }

    private SoftHashMap<Long, NodeId> ids = new SoftHashMap<Long, NodeId>(10000);

	private RepositoryStateManager stateManager;

	private NodeTypeManagerImpl nodeTypeManager;
	
	private String mixReferenceableUUIDColumn = null;

	private Long versionStorageTypeId;

    private boolean ignoreCaseInSecurity = true;

    private String[] workspaceNames;

	private boolean alwaysCheckCheckedOut;

	private boolean supportOCR = false;

    
	public void registerNodeId(NodeId nodeId){
		synchronized (ids){
			ids.put(nodeId.getId(), nodeId);
		}
	}
	
	public NodeId buildNodeId(Long id, DatabaseConnection conn) throws RepositoryException{
    	if (id == null){
    		return null;
    	}
    	NodeId nodeId = null;
    	synchronized (ids) {
    		nodeId = ids.get(id);	
		}
		
		if (nodeId == null){
			//TODO optimize this
			//1.find uuis
			String uuid = null;
			//TODO use type definition for table and column name
			String columnName = mixReferenceableUUIDColumn;
			if (columnName == null) {
				columnName = getNodeTypeManager().findColumnName(QName.MIX_REFERENCEABLE,QName.JCR_UUID);
			}
			DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(getNodeTypeManager().getNodeType(QName.MIX_REFERENCEABLE).getTableName(), true);
			st.addResultColumn(columnName);
			st.addCondition(Conditions.eq(Constants.FIELD_TYPE_ID, id));
			st.addCondition(Conditions.eq(getNodeTypeManager().getNodeType(QName.MIX_REFERENCEABLE).getPresenceColumn(), true));
			st.execute(conn);
			if (st.hasNext()){
				RowMap row = st.nextRow();
				uuid = row.getString(columnName);
			}
			
			nodeId = new NodeId(id, uuid);
			synchronized (ids) {
				ids.put(id, nodeId);
			}
		}
		return nodeId;
	}

	public RepositoryStateManager getStateManager() {
		return stateManager;
	}

	public NodeTypeManagerImpl getNodeTypeManager() throws RepositoryException {
		NodeTypeManagerImpl result = nodeTypeManager; 
		if (result == null){
			result = new NodeTypeManagerImpl(getNamespaceRegistry(), getOrCreateNodeTypeReader(), this);
			this.nodeTypeManager = result;
		}
		return result;
	}

	private synchronized DBNodeTypeReader getOrCreateNodeTypeReader() throws RepositoryException {
		if (nodeTypeReader == null){
			nodeTypeReader = new DBNodeTypeReader(getOrCreateNamespaceRegistry());
			DatabaseConnection conn = getConnectionProvider().createConnection(); 
			nodeTypeReader.loadNodeTypes(conn);
			conn.close();
		}
		return nodeTypeReader;
	}
	public void backup(OutputStream out, OutputStream out2, boolean indenting) throws SAXException, RepositoryException, NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException, IOException {
	    backup(out, out2, indenting, null);
	}
	
	public void backup(OutputStream out, OutputStream out2, boolean indenting, NodeExportAcceptor exportAcceptor) throws SAXException, RepositoryException, NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException, IOException {
        //boolean indenting = false;
        OutputFormat format = new OutputFormat("xml", "UTF-8", indenting);
        XMLSerializer serializer = new XMLSerializer(out, format);
        SessionImpl systemSession = getSystemSession(); 
        //export nodetypes
        systemSession.exportNodeTypes(out2);
        //export data
        
        serializer.startDocument();
        serializer.startElement("repository", new AttributeListImpl());

        
        
        serializer.startElement("system", new AttributeListImpl());
        systemSession.exportSystemView("/", serializer.asContentHandler(), false, null, false, false);
        serializer.endElement("system");
        
        String[] workspaces = systemSession.getWorkspace().getAccessibleWorkspaceNames();
        
        systemSession.logout();

        for(String workspace:workspaces){
        	Workspace w = new WorkspaceImpl(this, workspace, null, null);
            SessionImpl s = (SessionImpl) w.getSession();
            AttributeListImpl attrs = new AttributeListImpl();
            attrs.addAttribute("name", "string", workspace);
			//AttributeListImpl attrs = new AttributeListImpl();
            serializer.startElement("workspace", attrs);
            s.exportSystemView("/", serializer.asContentHandler(), false, null, false, true, exportAcceptor);
            serializer.endElement("workspace");
            
            serializer.startElement("workspace-security", attrs);
            SecurityExport se = new SecurityExport(serializer.asContentHandler(), s.getRootNode(), false, true);
            se.serialize(false);
            serializer.endElement("workspace-security");
            s.logout();
        }
        
        serializer.endElement("repository");
        
        serializer.endDocument();

		
	}

	/**
	 * Restore repository from snapshot.
	 * 
	 * @param data input stream
	 */
	public void restore(InputStream data) throws RepositoryException{
		try {
			
			RepositoryImportHandler handler = (RepositoryImportHandler) getRestoreContentHandler(null);

	            XMLReader parser =
	                    XMLReaderFactory.createXMLReader();
	            parser.setContentHandler(handler);
	            parser.setErrorHandler(handler);
	            // being paranoid...
	            parser.setFeature("http://xml.org/sax/features/namespaces", true);
	            parser.setFeature("http://xml.org/sax/features/namespace-prefixes",
	                    false);

	            parser.parse(new InputSource(data));
			
		} catch (FileNotFoundException e) {
			throw new RepositoryException(e);
        } catch (IOException e) {
			throw new RepositoryException(e);
        } catch (SAXException se) {
            // check for wrapped repository exception
            Exception e = se.getException();
            if (e != null && e instanceof RepositoryException) {
                throw (RepositoryException) e;
            } else {
                String msg = "failed to parse XML stream";
                log.debug(msg);
                throw new InvalidSerializedDataException(msg, se);
            }
        }
		
		
		
	}
	
    public ContentHandler getRestoreContentHandler(ZipFile zipFile) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
        RepositoryImporter importer = new RepositoryImporter(this, zipFile);
        return new RepositoryImportHandler(importer, getNamespaceRegistry(), getNamespaceRegistry());  
    }

    /**
     * Returns active query version.
     * @return
     */
    int getQueryVersion(){
        return queryVersion;
    }

	public int getBatchSize() {
		return 501;
		//return 20;
	}

	public boolean isIgnoreCaseInSecurity(){
		return ignoreCaseInSecurity ;
	}

	public void switchToSingleMode() throws RepositoryException{
		// TODO Auto-generated method stub
	}
	
	public void switchToNormalOperation()  throws RepositoryException{
		
	}

	public TaskManager getTaskManager() {
		return taskManager;
	}

	public void shutdown() {
		getTaskManager().shutdown();
		repositoryProvider.unregister(this);
	}

	public boolean allowAutoLockToken() {
		return autoAddLockToken;
	}

	public Long getVersionStorageTypeId() throws RepositoryException {
		if (versionStorageTypeId == null){
			versionStorageTypeId = getNodeTypeManager().getNodeType(QName.REP_VERSIONSTORAGE).getNodeTypeDef().getId();
		}
		return versionStorageTypeId;
	}

	public boolean isSupportFTS() {
		return supportFTS;
	}
	
	public boolean isSupportOCR() {
		return supportOCR ;
	}

	public synchronized void reloadNamespaceRegistry(DatabaseConnection conn) throws RepositoryException{
		namespaceRegistry = new NamespaceRegistryImpl(this);
		namespaceRegistry.loadNamespaces(conn);
		reloadNodeTypeReader();
	}

	public void evictAllFromCache() throws RepositoryException {
		this.stateManager.evictAll();
		
	}

	public boolean isIgnoreLock() {
		return ignoreLock;
	}

	public SessionManager getSessionManager() {
		return sessionManager;
	}

	public boolean isAllowUpgrade() {
		return allowUpgrade;
	}

    public SecurityCopyType getSecurityCopyType() {
        return securityCopyType;
    }
    
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        if (workspaceNames == null){
            DatabaseConnection conn = getConnectionProvider().createConnection();
            try {
                DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_WORKSPACE, true);
                st.addResultColumn(Constants.TABLE_WORKSPACE__NAME);
                st.execute(conn);
                ArrayList<String> result = new ArrayList<String>();
                while(st.hasNext()){
                    RowMap row = st.nextRow();
                    result.add(row.getString(Constants.TABLE_WORKSPACE__NAME));
                }
                workspaceNames = result.toArray(new String[result.size()]);
                st.close();
            } finally {
                conn.close();
            }
        }
        return workspaceNames;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public boolean isReducedVersionCheck() {
        return reducedVersionCheck;
    }
    
    public CommandManager getCommandManager() {
		return commandManager;
	}

	public boolean isSupportSecurity() {
		return supportSecurity;
	}

	public boolean isSupportNodeTypeCheck() {
		return supportNodeTypeCheck;
	}

	public boolean isAlwaysCheckCheckedOut() {
		return alwaysCheckCheckedOut;
	}

	public NodeTypeHelper getNodeTypeHelper() throws RepositoryException{
		if (nodeTypeHelper == null){
			nodeTypeHelper = new NodeTypeHelper(getNodeTypeReader());
		}
		return nodeTypeHelper;
	}

	public boolean isContextSecuritySupported() {
		return supportsContextSecurity;
	}
	
}

/*
 * $Log: RepositoryImpl.java,v $
 * Revision 1.74  2011/02/22 09:36:42  vsverlovs
 * EPB-187 - Copy JCR model build process to Bamboo build server and enable Sonar quality management.
 * dropRepository code clean up.
 *
 * Revision 1.73  2010/09/27 15:49:43  vsverlovs
 * EPB-198: code_review_EPB-105_2010-09-02
 *
 * Revision 1.72  2010/08/27 10:16:41  abarancikovs
 * JIRA: EPB-105 - Can't upgrade JCR repository version 10 to 14 (EPB 7.0 to 7.1)
 * Added schema changes, to reflect requested DB version.
 *
 * Revision 1.71  2009/03/23 15:23:36  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.70  2009/03/23 11:20:43  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.69  2009/03/18 09:11:14  vpukis
 * EPBJCR-22: Oracle 11g (11.1.0.7) dialect
 *
 * Revision 1.68  2009/03/16 12:13:20  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.67  2009/03/12 10:57:00  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.66  2009/02/26 06:56:22  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.65  2009/02/23 14:30:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.64  2009/02/13 15:36:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.63  2009/02/09 15:56:05  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.62  2009/02/09 14:26:16  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.61  2009/02/09 12:52:02  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.60  2009/02/06 08:29:54  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.59  2009/02/05 11:52:08  maksims
 * *** empty log message ***
 *
 * Revision 1.58  2009/02/04 12:16:38  maksims
 * *** empty log message ***
 *
 * Revision 1.57  2009/01/27 14:07:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.56  2009/01/23 07:21:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.55  2008/12/22 14:07:31  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.54  2008/12/22 14:07:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.53  2008/11/24 10:04:01  maksims
 * node export acceptor added to support custom filtering of exported nodes
 *
 * Revision 1.52  2008/11/06 10:23:53  maksims
 * export stop types support added
 *
 * Revision 1.51  2008/09/11 09:43:23  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.50  2008/09/03 10:25:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.49  2008/07/23 09:56:30  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.48  2008/07/22 09:06:26  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.47  2008/07/17 06:57:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.46  2008/07/16 11:42:52  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.45  2008/07/16 08:38:11  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.44  2008/07/15 11:27:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.43  2008/07/09 06:40:24  dparhomenko
 * *** empty log message ***
 *
 * 
 */