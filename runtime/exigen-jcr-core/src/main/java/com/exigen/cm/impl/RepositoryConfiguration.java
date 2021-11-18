/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import static com.exigen.cm.Constants.DEFAULT_JNDI_DATASOURCE_NAME;
import static com.exigen.cm.Constants.DEFAULT_JNDI_PREFIX;
import static com.exigen.cm.Constants.DEFAULT_ROOT_PASSWORD;
import static com.exigen.cm.Constants.DEFAULT_ROOT_USER_NAME;
import static com.exigen.cm.Constants.IMPORT_ROOT_PASSWORD;
import static com.exigen.cm.Constants.IMPORT_ROOT_USER;
import static com.exigen.cm.Constants.PROPERTY_AUTHENTICATOR_CLASS_NAME;
import static com.exigen.cm.Constants.PROPERTY_AUTHENTICATOR_ROOT_PASSWORD;
import static com.exigen.cm.Constants.PROPERTY_AUTHENTICATOR_ROOT_USER;
import static com.exigen.cm.Constants.PROPERTY_AUTO_ADD_LOCK_TOKEN;
import static com.exigen.cm.Constants.PROPERTY_CACHE_MANAGER;
import static com.exigen.cm.Constants.PROPERTY_CMD_CLEAN_DELAY;
import static com.exigen.cm.Constants.PROPERTY_CMD_CLEAN_ON;
import static com.exigen.cm.Constants.PROPERTY_CMD_ENFORCE_UNLOCK_DELAY;
import static com.exigen.cm.Constants.PROPERTY_CMD_ENFORCE_UNLOCK_ON;
import static com.exigen.cm.Constants.PROPERTY_CMD_EXTRACTOR_DELAY;
import static com.exigen.cm.Constants.PROPERTY_CMD_EXTRACTOR_ON;
import static com.exigen.cm.Constants.PROPERTY_CMD_FREE_RESERVED_DELAY;
import static com.exigen.cm.Constants.PROPERTY_CMD_FREE_RESERVED_ON;
import static com.exigen.cm.Constants.PROPERTY_CMD_FTS_BATCH_SIZE;
import static com.exigen.cm.Constants.PROPERTY_CMD_INDEXER_DELAY;
import static com.exigen.cm.Constants.PROPERTY_CMD_INDEXER_ON;
import static com.exigen.cm.Constants.PROPERTY_CMD_MIMEDETECTOR_DELAY;
import static com.exigen.cm.Constants.PROPERTY_CMD_MIMEDETECTOR_ON;
import static com.exigen.cm.Constants.PROPERTY_CONFIGURATOR;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_ALLOW_UPGRADE;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_DIALECT_CLASSNAME;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_DRIVER_CLASSNAME;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_DROP_CREATE;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_JNDI_NAME;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_PASSWORD;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_REDUCED_VERSION_CHECK_CHECK;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_SKIP_CHECK;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_URL;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_USER;
import static com.exigen.cm.Constants.PROPERTY_DEVELOPMENT_MODE;
import static com.exigen.cm.Constants.PROPERTY_IGNORE_CASE_IN_SECURITY;
import static com.exigen.cm.Constants.PROPERTY_ROOT_PASSWORD;
import static com.exigen.cm.Constants.PROPERTY_ROOT_USER;
import static com.exigen.cm.Constants.PROPERTY_SECURITY_MOVE_WITH_NODE;
import static com.exigen.cm.Constants.PROPERTY_SUPPORT_FTS;
import static com.exigen.cm.Constants.PROPERTY_SUPPORT_OCR;
import static com.exigen.cm.Constants.PROPERTY_SUPPORT_OCR_SERVER;
import static com.exigen.cm.Constants.PROPERTY_THREADS_COUNT;
import static com.exigen.cm.Constants.PROPERTY_THREADS_ON;
import static com.exigen.cm.Constants.REPOSITORY_JNDI_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.ConnectionProviderImpl;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.transaction.JCRTransactionManager;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.impl.cache.JCSCacheManager;
import com.exigen.cm.impl.ewf.EWFRepositoryConfigurator;
import com.exigen.cm.security.authenticator.TrustedAuthenticator;
import com.exigen.vf.commons.logging.LogUtils;

/**
 * Provides repository configuration.
 * Takes care about default values
 * 
 * Repository configuration could be provided in a several ways, which are listed below 
 * by decreasing priority:
 * 
 * 1. Explicit configuration for the repository (Map or file)
 * 2. Default file for the repository named "repository-<repository name>.properties"
 * 3. Explicit configuration for several repositories (Map or file)
 * 
 * If configuration is provided for several repositories, keys are prefixed with repository
 * name
 */
public class RepositoryConfiguration {
    
	private Map <String, String> configuration = null;
    private HashMap <String, Map <String, String>>individualConfiguration = new HashMap<String, Map<String,String>>();
    private HashMap <String, DataSource> datasources = new HashMap<String, DataSource>();
    private HashMap <String, JCRTransactionManager> transactionManagers = new HashMap<String, JCRTransactionManager>();
    private static Log log = LogFactory.getLog(RepositoryConfiguration.class);    

    public static final String DEFAULT_COMMAND_DELAY = "60";
    
    /**
     * Configuration for several repositories as Map, where keys are property names.
     * @param config
     * @throws RepositoryException 
     */
    public void configure(Map<String,String>  config ) throws RepositoryException {
        configuration = config;
    }

    /**
     * Configuration for several repositories as resource (file). File should be in classpath
     * @param resource file name
     * @throws RepositoryException
     */
    public void configure(String resource) throws RepositoryException{
        configuration = configureFromResource(resource);
    }
    
    
    /**
     * Configuration for single repository as Map
     * @param repositoryName
     * @param config
     * @throws RepositoryException
     */
    public void configure(String repositoryName, Map<String, String> config) throws RepositoryException {
        individualConfiguration.put(repositoryName, config);
    }

    /**
     * Sets datasource for given repository
     * @param repositoryName
     * @param ds datasource
     * @throws RepositoryException
     */
    public void configure(String repositoryName, DataSource ds) throws RepositoryException {
    	if (ds != null){
    		datasources.put(repositoryName, ds);
    	}
    }
    
    /**
     * Sets transaction manager for given repository
     * @param repositoryName
     * @param manager transaction manager
     * @throws RepositoryException
     */
	public void configure(String repositoryName, JCRTransactionManager manager) {
		transactionManagers.put(repositoryName, manager);
		
	}

    /**
     * Configuration for single repository as resource (File). File should be in classpath
     * @param repositoryName
     * @param resource file name
     * @throws RepositoryException
     */
    public void configure(String repositoryName, String resource)throws RepositoryException {
        individualConfiguration.put(repositoryName, configureFromResource(resource));
    }
    
    
    /**
     * Loads configuration from resource (file). Accepts data as "name=value" pairs
     * @param resource resource name
     * @return Configuration
     * @throws RepositoryException 
     */
    @SuppressWarnings("unchecked") // Properties object assignment to Map<String,String>
    protected Map<String,String> configureFromResource(String resource) throws RepositoryException {
        Map<String, String> config = new HashMap<String, String>();
        InputStream is = null;
        try {
            URL resourceURL = getClass().getClassLoader().getResource(resource);
            if (resourceURL == null){
                String msg = "Can't find configuration file in classpath: " + resource;
                log.error("Can't find configuration file in classpath: " + resource);
                throw new RepositoryException(msg);
            }
            LogUtils.info(log,"Loading configuration from " + resourceURL);
            is = getClass().getClassLoader().getResourceAsStream(resource);
            if (is == null) {
                String msg = "Configuration configuration file " + resource +" not found in classpath.";
                LogUtils.error(log, msg);
                throw new RepositoryException(msg);
            }
            Properties props = new Properties();
            try {
                props.load(is);
                config = (Map)props; // warning, because Properties are <String, Object>             
            } catch (IOException e) {
                LogUtils.error(log, e.getMessage(), e);
                throw new RepositoryException(e.getMessage(), e);
            }                
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {/*empty*/}
            }
        }
        return config;
    }
    
    
    /**
     * Returns repository configuration
     * @param repositoryName
     * @return configuration
     * @throws RepositoryException
     */
    public Map<String, String> getRepositoryConfiguration(String repositoryName) throws RepositoryException{
        
        // try individual configuration
        Map <String, String> config = individualConfiguration.get(repositoryName);
        if (config == null) { // try from resource
            String resource = "repository-"+repositoryName + ".properties";
            if ( getClass().getClassLoader().getResource(resource) != null) { // file exists
                config = configureFromResource(resource);
                individualConfiguration.put(repositoryName, config);
            }
        }
        
        if (config == null) { // try configuration for multiple repositories
            /*if (configuration == null) {
                configuration = getReposConfiguration();
            }*/
            if (configuration == null){
                throw new RepositoryException("Configuration for repository is not provided. Name of repository: " + repositoryName+". Please put file repository-"+repositoryName+".properties into classpath");
            }
            config = JCRHelper.getStringPropertiesByPrefix(repositoryName, configuration);
        }
        checkFillConfiguration(repositoryName, config);
        return config;
    }
    
    /**
     * Returns list of repository names. First search individual repository configuration
     * than multiple configuration
     * @return Repository names
     * @throws RepositoryException
     */
    /*public String[] getRepositoryNames() throws RepositoryException{
        Set <String> repNames = new HashSet <String>();
        // all repositories with configuration provided
        Set <String> keys = individualConfiguration.keySet();
        for (String key: keys){
            repNames.add(key);
        }
        // repositories with multiple configuration
        Map <String, String> c = getReposConfiguration();
        if (c != null){
            keys = c.keySet();
            for (String key: keys){
                int index = key.indexOf(".");
                if (index > 0){
                    repNames.add(key.substring(0, index));
                }
            }
        }
        if (repNames.size() == 0)
            return new String[]{DEFAULT_REPOSITORY_NAME};
        return repNames.toArray(new String[repNames.size()]);
        
    }*/
    
    
    /*private Map <String,String> getReposConfiguration() throws RepositoryException {
        if (configuration == null){
            if (getClass().getClassLoader().getResource("repositories.properties") != null){
                return configureFromResource("repositories.properties");
            }
        }
        return configuration;
    }*/
    
    /**
     * Verifies repository configuration and set default values
     * @param repositoryName
     * @param config
     * @throws RepositoryException
     */
    public void checkFillConfiguration(String repositoryName, Map <String, String> config) throws RepositoryException {
        
    	DataSource ds = datasources.get(repositoryName);
    	if (ds == null){
	        // data source JNDI name or Connection
	        String jndiName = (String)config.get(PROPERTY_DATASOURCE_JNDI_NAME);
	        //check jndi datasource
	        boolean jndiOk = false;
	        try {
	            InitialContext ctx = new InitialContext();
	            ctx.lookup(jndiName);
	            jndiOk = true;
	        } catch (Exception exc){
	            if (TransactionHelper.getInstance().getType() == TransactionHelper.APPLICATION_SERVER_JBOSS){
		        	if (jndiName != null && jndiName.length() > 0){
		                if (!jndiName.startsWith("java:")){
		                	String _jndiName = "java:"+jndiName;
				        	try {
					            InitialContext ctx = new InitialContext();
					            ctx.lookup(_jndiName);
					            jndiName = _jndiName;
					            jndiOk = true;
					            config.put(PROPERTY_DATASOURCE_JNDI_NAME,jndiName);
				            } catch (Exception exc1){
				            	//datasource not found, exception is not neccesary
				            }
				            
		                }
		        	}
	            } else {
	                if (jndiName != null && jndiName.length() > 0){
                        if (ds == null){
                            try {
                                InitialContext ctx = new InitialContext();
                                ctx.lookup("java:comp/env/"+jndiName);
                                jndiName = "java:comp/env/"+jndiName;
                                jndiOk = true;
                                config.put(PROPERTY_DATASOURCE_JNDI_NAME,jndiName);
                            } catch (Exception e1) {
                                
                            }
                        }	                    
	                }
	            }
	            if (!jndiOk){
	            	log.warn("Datasource "+jndiName+" not found");
	            }
	        	
	        }
	        
	        
	        if (!jndiOk){
		        String connectionURL = (String)config.get(PROPERTY_DATASOURCE_URL);
		        String connectionUser = (String)config.get(PROPERTY_DATASOURCE_USER);
		        String connectionPassword = (String)config.get(PROPERTY_DATASOURCE_PASSWORD);
		        String driverName = (String)config.get(PROPERTY_DATASOURCE_DRIVER_CLASSNAME);
		        String dialectClassName = (String)config.get(PROPERTY_DATASOURCE_DIALECT_CLASSNAME);
		        
		        boolean validConnectionInfo = !( JCRHelper.isEmpty(connectionURL) || 
		        		JCRHelper.isEmpty(connectionUser)  || ( JCRHelper.isEmpty(driverName) && JCRHelper.isEmpty(dialectClassName) ));
	
		        /*if ( JCRHelper.isEmpty(dialectClassName) ) {
		            String msg = MessageFormat.format(PROPERTY_NOT_FOUND, new Object[]{PROPERTY_DATASOURCE_DIALECT_CLASSNAME});
		            LogUtils.error(log, msg);
		            throw new RepositoryException(msg);
		        }*/
	
		        
		        if (!validConnectionInfo){
		            String msg = "Configuration does not contain enough information for DB connection. Either datasource or connection url and user should be provided\n";
		            msg = msg.concat(PROPERTY_DATASOURCE_JNDI_NAME + " = " + jndiName + "\n");
		            msg = msg.concat(PROPERTY_DATASOURCE_URL + " = " + connectionURL + "\n");
		            msg = msg.concat(PROPERTY_DATASOURCE_USER + " = " + connectionUser + "\n");
		            LogUtils.error(log, msg);
		            throw new RepositoryException(msg);
		        }
		        
		        if ( JCRHelper.isEmpty(jndiName) ) {
		            // access via connection
		            config.put(PROPERTY_DATASOURCE_JNDI_NAME,DEFAULT_JNDI_DATASOURCE_NAME);
		            if (JCRHelper.isEmpty(connectionPassword)){
		                config.put(PROPERTY_DATASOURCE_PASSWORD, ""); // no password assumed as empty 
		            }
		        }
	        }
    	}
        ConnectionProviderImpl cp = new ConnectionProviderImpl();
        cp.configure(config, ds);
        DatabaseDialect dialect = cp.getDialect();
        

        // check mandatory params: dialect and (datasource or connection)
        //  Dialect
        
        // dialect checks/fills mandatory field for dialect
        //DatabaseDialect dialect = (DatabaseDialect)JCRHelper.loadAndInstantiateClass(dialectClassName);
        dialect.checkConfiguration(config);
        
        
        
        
        String supportFTSStr = (String)config.get(PROPERTY_SUPPORT_FTS);   
        String supportOCRStr = (String)config.get(PROPERTY_SUPPORT_OCR);   
        String ocrServer = (String)config.get(PROPERTY_SUPPORT_OCR_SERVER);   
        String supportSecurityStr = (String)config.get(Constants.PROPERTY_SUPPORT_SECURITY);   
        String supportNodeTypeStr = (String)config.get(Constants.PROPERTY_SUPPORT_NODETYPE_CHECK);   
        String _supportVersionCheckStr = (String)config.get(Constants.PROPERTY_SUPPORT_VERSIONING_CHECK);   
        String _lockDisableStr = (String)config.get(Constants.PROPERTY_SUPPORT_LOCK_DISABLE);   
        String developmentModeStr = (String)config.get(PROPERTY_DEVELOPMENT_MODE);   
        String moveSecurityWithNodeStr = (String)config.get(PROPERTY_SECURITY_MOVE_WITH_NODE);   
        String dropCreateStr = (String)config.get(PROPERTY_DATASOURCE_DROP_CREATE);
        String allowUpgradeStr = (String)config.get(PROPERTY_DATASOURCE_ALLOW_UPGRADE);
        String skipCheckStr = (String)config.get(PROPERTY_DATASOURCE_SKIP_CHECK);
        String reducedVersionCheck = (String)config.get(PROPERTY_DATASOURCE_REDUCED_VERSION_CHECK_CHECK);
        String authenticatorClassName = (String)config.get(PROPERTY_AUTHENTICATOR_CLASS_NAME);
        String startThreadsStr = (String)config.get(PROPERTY_THREADS_ON);
        String startThreadsCountStr = (String)config.get(PROPERTY_THREADS_COUNT);
        String rootUserName = (String)config.get(PROPERTY_ROOT_USER);
        String rootPassword = (String)config.get(PROPERTY_ROOT_PASSWORD);
        String repositoryJNDIName = (String)config.get(REPOSITORY_JNDI_NAME);
        String cacheManagerClassName = (String)config.get(PROPERTY_CACHE_MANAGER);
        String autoAddLockToken = (String)config.get(PROPERTY_AUTO_ADD_LOCK_TOKEN);   
        String ignoreCaseInSecurity = (String)config.get(PROPERTY_IGNORE_CASE_IN_SECURITY);   
        
        
        String cmdEnforceUnlockOn = config.get(PROPERTY_CMD_ENFORCE_UNLOCK_ON);
        String cmdEnforceUnlockDelay = config.get(PROPERTY_CMD_ENFORCE_UNLOCK_DELAY);
        
        String cmdExtractorOn = config.get(PROPERTY_CMD_EXTRACTOR_ON);
        String cmdExtractorDelay = config.get(PROPERTY_CMD_EXTRACTOR_DELAY);
        
        
        String cmdMimeOn = config.get(PROPERTY_CMD_MIMEDETECTOR_ON);
        String cmdMimeDelay = config.get(PROPERTY_CMD_MIMEDETECTOR_DELAY);
        
        String cmdDeleteOn = config.get(PROPERTY_CMD_CLEAN_ON);
        String cmdDeleteDelay = config.get(PROPERTY_CMD_CLEAN_DELAY);
        
        String cmdFreeReserved = config.get(PROPERTY_CMD_FREE_RESERVED_ON);
        String cmdFreeReservedDelay = config.get(PROPERTY_CMD_FREE_RESERVED_DELAY);
        
        String cmdFTSBatchSize = config.get(PROPERTY_CMD_FTS_BATCH_SIZE);
        if (JCRHelper.isEmpty(cmdFTSBatchSize)){
            config.put(PROPERTY_CMD_FTS_BATCH_SIZE,DEFAULT_COMMAND_DELAY);
        }
        
        //TODO replace this with checkValue(config, key, value, defaultValue)
        if ( JCRHelper.isEmpty(authenticatorClassName)) {
            authenticatorClassName = TrustedAuthenticator.class.getName();
            config.put(PROPERTY_AUTHENTICATOR_CLASS_NAME, authenticatorClassName);
        }
        
        if ( JCRHelper.isEmpty(startThreadsStr) ) {
            startThreadsStr = "true";
            config.put(PROPERTY_THREADS_ON, startThreadsStr);                
        }
        
        if ( JCRHelper.isEmpty(startThreadsCountStr) ) {
        	startThreadsCountStr = "3";
            config.put(PROPERTY_THREADS_COUNT, startThreadsCountStr);                
        }

        if ( JCRHelper.isEmpty(cmdEnforceUnlockOn) ) {
        	cmdEnforceUnlockOn = "true";
            config.put(PROPERTY_CMD_ENFORCE_UNLOCK_ON, cmdEnforceUnlockOn);                
        }
        if ( JCRHelper.isEmpty(cmdEnforceUnlockDelay) ) {
        	cmdEnforceUnlockDelay = "60";
            config.put(PROPERTY_CMD_ENFORCE_UNLOCK_DELAY, cmdEnforceUnlockDelay);                
        }
        
        if ( JCRHelper.isEmpty(cmdExtractorOn) ) {
        	cmdExtractorOn = "true";
            config.put(PROPERTY_CMD_EXTRACTOR_ON, cmdExtractorOn);                
        }
        if ( JCRHelper.isEmpty(cmdExtractorDelay) ) {
        	cmdExtractorDelay = DEFAULT_COMMAND_DELAY;
            config.put(PROPERTY_CMD_EXTRACTOR_DELAY, cmdExtractorDelay);                
        }
        
        
        
        if ( JCRHelper.isEmpty(cmdMimeOn) ) {
        	cmdMimeOn = "true";
            config.put(PROPERTY_CMD_MIMEDETECTOR_ON, cmdMimeOn);                
        }
        if ( JCRHelper.isEmpty(cmdMimeDelay) ) {
        	cmdMimeDelay = DEFAULT_COMMAND_DELAY;
            config.put(PROPERTY_CMD_MIMEDETECTOR_DELAY, cmdMimeDelay);                
        }
        
        if ( JCRHelper.isEmpty(cmdDeleteOn) ) {
        	cmdDeleteOn = "true";
            config.put(PROPERTY_CMD_CLEAN_ON, cmdDeleteOn);                
        }
        if ( JCRHelper.isEmpty(cmdDeleteDelay) ) {
        	cmdDeleteDelay = "60";
            config.put(PROPERTY_CMD_CLEAN_DELAY, cmdDeleteDelay);                
        }
        
        if ( JCRHelper.isEmpty(cmdFreeReserved) ) {
        	cmdFreeReserved = "true";
            config.put(PROPERTY_CMD_FREE_RESERVED_ON, cmdFreeReserved);                
        }
        if ( JCRHelper.isEmpty(cmdFreeReservedDelay) ) {
        	cmdFreeReservedDelay = "60";
            config.put(PROPERTY_CMD_FREE_RESERVED_DELAY, cmdFreeReservedDelay);                
        }
        
        if ( JCRHelper.isEmpty(supportFTSStr) ) {
        	supportFTSStr = "false";
            config.put(PROPERTY_SUPPORT_FTS, supportFTSStr);                
        }

        if ( JCRHelper.isEmpty(supportOCRStr) ) {
        	supportOCRStr = "false";
            config.put(PROPERTY_SUPPORT_OCR, supportOCRStr);                
        }
        
        if ("true".equals(supportOCRStr)){
        	if (ocrServer == null) {
	            String msg = "Configuration does not contain enough for OCR processing, please specify OCR server in proeprty "+PROPERTY_SUPPORT_OCR_SERVER;
	            LogUtils.error(log, msg);
	            throw new RepositoryException(msg);
        	}
        	StringTokenizer st = new StringTokenizer(ocrServer,":");
        	boolean  ocrError = false;
        	try {
            	st.nextToken();
            	st.nextToken();
            	if (st.hasMoreElements()){
            		ocrError = true;
            	}
			} catch (Exception e) {
				ocrError = true;
			}
			if (ocrError){
	            String msg = "incorrect OCR server URl format. Should be \"server:port\"";
	            LogUtils.error(log, msg);
	            throw new RepositoryException(msg);

			}
        }

        if ( JCRHelper.isEmpty(supportSecurityStr) ) {
        	supportSecurityStr = "true";
            config.put(Constants.PROPERTY_SUPPORT_SECURITY, supportSecurityStr);                
        }

        if ( JCRHelper.isEmpty(supportNodeTypeStr) ) {
        	supportNodeTypeStr = "true";
            config.put(Constants.PROPERTY_SUPPORT_NODETYPE_CHECK, supportNodeTypeStr);                
        }

        if ( JCRHelper.isEmpty(_lockDisableStr) ) {
        	_lockDisableStr = "false";
            config.put(Constants.PROPERTY_SUPPORT_LOCK_DISABLE, _lockDisableStr);                
        }

        if ( JCRHelper.isEmpty(_supportVersionCheckStr) ) {
        	_supportVersionCheckStr = "true";
            config.put(Constants.PROPERTY_SUPPORT_VERSIONING_CHECK, _supportVersionCheckStr);                
        }

        if ( JCRHelper.isEmpty(developmentModeStr) ) {
            developmentModeStr = "false";
            config.put(PROPERTY_DEVELOPMENT_MODE, developmentModeStr);                
        }

        if ( JCRHelper.isEmpty(autoAddLockToken) ) {
            autoAddLockToken = "true";
            config.put(PROPERTY_AUTO_ADD_LOCK_TOKEN, developmentModeStr);                
        }

        if ( JCRHelper.isEmpty(ignoreCaseInSecurity) ) {
            ignoreCaseInSecurity = "true";
            config.put(PROPERTY_IGNORE_CASE_IN_SECURITY, ignoreCaseInSecurity);                
        }

        if ( JCRHelper.isEmpty(dropCreateStr) ) {
            dropCreateStr  = "false";
            config.put(PROPERTY_DATASOURCE_DROP_CREATE, dropCreateStr);
        }
        
        if ( JCRHelper.isEmpty(allowUpgradeStr) ) {
            allowUpgradeStr  = "true";
            config.put(PROPERTY_DATASOURCE_ALLOW_UPGRADE, allowUpgradeStr);
        }
        
        if ( JCRHelper.isEmpty(moveSecurityWithNodeStr) ) {
            moveSecurityWithNodeStr  = SecurityCopyType.Inherit.toString();
            config.put(PROPERTY_SECURITY_MOVE_WITH_NODE, moveSecurityWithNodeStr);
        }
        
        if ( JCRHelper.isEmpty(skipCheckStr) ) {
            skipCheckStr  = "false";
            config.put(PROPERTY_DATASOURCE_SKIP_CHECK, skipCheckStr);
        }
        
        if ( JCRHelper.isEmpty(reducedVersionCheck) ) {
            reducedVersionCheck  = "true";
            config.put(PROPERTY_DATASOURCE_REDUCED_VERSION_CHECK_CHECK, reducedVersionCheck);
        }
        

        checkCommand(config, PROPERTY_CMD_INDEXER_ON, PROPERTY_CMD_INDEXER_DELAY, "true", DEFAULT_COMMAND_DELAY);
        
        //OCR
        checkCommand(config, Constants.PROPERTY_CMD_OCR_SEND_ON, Constants.PROPERTY_CMD_OCR_SEND_DELAY, "true", DEFAULT_COMMAND_DELAY);
        checkCommand(config, Constants.PROPERTY_CMD_OCR_CHECK_ON, Constants.PROPERTY_CMD_OCR_CHECK_DELAY, "true", DEFAULT_COMMAND_DELAY);
        checkCommand(config, Constants.PROPERTY_CMD_OCR_RETRIVE_ON, Constants.PROPERTY_CMD_OCR_RETRIVE_DELAY, "true", DEFAULT_COMMAND_DELAY);
        
        if ( JCRHelper.isEmpty(rootUserName) ) {
            rootUserName = DEFAULT_ROOT_USER_NAME;
            rootPassword = DEFAULT_ROOT_PASSWORD;
            config.put(PROPERTY_ROOT_USER, rootUserName);
            config.put(PROPERTY_ROOT_PASSWORD, rootPassword);
        }
        
        config.put(PROPERTY_AUTHENTICATOR_ROOT_USER, rootUserName);
        config.put(PROPERTY_AUTHENTICATOR_ROOT_PASSWORD, rootPassword);
        config.put(IMPORT_ROOT_USER, rootUserName);
        config.put(IMPORT_ROOT_PASSWORD, rootPassword);                 
        
        if ( JCRHelper.isEmpty(repositoryJNDIName) ){
            repositoryJNDIName = DEFAULT_JNDI_PREFIX+"/"+repositoryName;
            config.put(REPOSITORY_JNDI_NAME,repositoryJNDIName);
        }
        
        if ( JCRHelper.isEmpty(cacheManagerClassName) ){
            cacheManagerClassName = JCSCacheManager.class.getName();
            config.put(PROPERTY_CACHE_MANAGER,cacheManagerClassName);
        }
        
//      Set default value for Query Version if needed
//        String queryVersion = config.get(PROPERTY_QUERY_VERSION);
//        if ( JCRHelper.isEmpty(queryVersion) )
//            config.put(PROPERTY_QUERY_VERSION,"2");
        
        
        
        // stores configuration validation moved to Content Store Configuration of specific store types
        /*
        String defaultStoreType = (String)config.get(ContentStorePROPERTY_DEFAULT_STORE_TYPE);
        String textStoreType = (String)config.get(ContentStorePROPERTY_TEXT_STORE_TYPE);
        String defaultStoreRootDir = (String)config.get(ContentStorePROPERTY_DEFAULT_STORE_ROOTDIR); 
        String textStoreRootDir = (String)config.get(ContentStorePROPERTY_TEXT_STORE_ROOTDIR); 
        //* /
        
        if (JCRHelper.isEmpty(defaultStoreType)) {
            defaultStoreType = DBContentStoreBuilder.TYPE;
            config.put(ContentStorePROPERTY_DEFAULT_STORE_TYPE, defaultStoreType);
        }
        
        if (JCRHelper.isEmpty(textStoreType)) {
            textStoreType = DBContentStoreBuilder.TYPE;
            config.put(ContentStorePROPERTY_TEXT_STORE_TYPE, textStoreType);
        }
        
        // check "file" or "db"
        if ( !defaultStoreType.equals(FileContentStoreBuilder.TYPE) && 
             !defaultStoreType.equals(DBContentStoreBuilder.TYPE) ) {
            String msg = "Content store type for " + ContentStorePROPERTY_DEFAULT_STORE_TYPE + "is neither " + FileContentStoreBuilder.TYPE  + " nor " + DBContentStoreBuilder.TYPE;
            LogUtils.error(log, msg);
            throw new RepositoryException(msg);
        }

        if ( !textStoreType.equals(FileContentStoreBuilder.TYPE) && 
             !textStoreType.equals(DBContentStoreBuilder.TYPE) ) {
               String msg = "Content store type for " + ContentStorePROPERTY_TEXT_STORE_TYPE + "is neither " + FileContentStoreBuilder.TYPE  + " nor " + DBContentStoreBuilder.TYPE;
               LogUtils.error(log, msg);
               throw new RepositoryException(msg);
           }
        
        // check root dir for file store
        if (defaultStoreType.equals(FileContentStoreBuilder.TYPE) &&
                JCRHelper.isEmpty(defaultStoreRootDir)){
            String msg = "For content store of type " + FileContentStoreBuilder.TYPE + " " + ContentStorePROPERTY_DEFAULT_STORE_ROOTDIR + " should be provided";
            LogUtils.error(log, msg);
            throw new RepositoryException(msg);
        }
        
        if (textStoreType.equals(FileContentStoreBuilder.TYPE) &&
                JCRHelper.isEmpty(textStoreRootDir)){
            String msg = "For content store of type " + FileContentStoreBuilder.TYPE + " " + ContentStorePROPERTY_TEXT_STORE_ROOTDIR + " should be provided";
            LogUtils.error(log, msg);
            throw new RepositoryException(msg);
        }
        //*/
        
        
        if (!config.containsKey(PROPERTY_CONFIGURATOR)){
            config.put(PROPERTY_CONFIGURATOR,EWFRepositoryConfigurator.class.getName());
        }
        
        
    }

	private void checkCommand(Map<String, String> config, String commandOn,
			String commandDelay, String comandValue, String commandDelayValue) {
        String cmdIndexerOn = config.get(commandOn);
        String cmdIndexerDelay = config.get(commandDelay);
        if ( JCRHelper.isEmpty(cmdIndexerOn) ) {
        	cmdIndexerOn = comandValue;
            config.put(commandOn, cmdIndexerOn);                
        }
        if ( JCRHelper.isEmpty(cmdIndexerDelay) ) {
        	cmdIndexerDelay = commandDelayValue;
            config.put(commandDelay, cmdIndexerDelay);                
        }		
	}

	public DataSource getDatasource(String name) {
		return datasources.get(name);
	}
	
	public JCRTransactionManager getTransactionManager(String name) {
		return transactionManagers.get(name);
	}
	
	

}


/*
 * $Log: RepositoryConfiguration.java,v $
 * Revision 1.24  2009/03/24 07:54:44  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.23  2009/03/23 15:23:36  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.22  2009/02/26 12:18:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.21  2009/02/26 06:56:22  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.20  2009/02/23 14:30:18  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.19  2009/02/13 15:36:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.18  2009/02/05 10:00:40  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.17  2009/01/27 14:07:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.16  2008/09/11 09:42:38  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.15  2008/09/10 09:23:52  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.14  2008/09/03 10:25:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.13  2008/07/17 06:57:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.12  2008/07/15 11:27:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.11  2008/07/03 14:14:41  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.10  2008/07/02 07:19:59  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.9  2008/06/13 09:35:30  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2008/04/29 10:55:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/03/28 13:45:57  dparhomenko
 * Autmaticaly unlock orphaned locks(session scoped)
 *
 */
