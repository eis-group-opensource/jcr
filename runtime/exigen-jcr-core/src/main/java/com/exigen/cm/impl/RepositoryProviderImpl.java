/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.RepositoryProvider;
import com.exigen.cm.database.transaction.JCRTransactionManager;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.vf.commons.logging.LogUtils;

public class RepositoryProviderImpl extends RepositoryProvider {

    private static Log log = LogFactory.getLog(RepositoryProviderImpl.class);    
    private Map<String, RepositoryImpl> repositories = new HashMap<String, RepositoryImpl>();
    protected RepositoryConfiguration repConfig = new RepositoryConfiguration();
    private Map<String, Map<String,Object>> attributes= new HashMap<String, Map<String,Object>>(); 
    
    public synchronized Repository getRepository(String name) throws RepositoryException {
        
        LogUtils.debug(log, "Trying to get \"{0}\" repository from cache...", name);
        RepositoryImpl repository = (RepositoryImpl)repositories.get(name);        
        if (repository == null){
            LogUtils.debug(log, "Repository not found, creating...");
            LogUtils.info(log, "Creating \"{0}\" repository", name);
            
           DataSource ds = repConfig.getDatasource(name); 
           JCRTransactionManager trManager = repConfig.getTransactionManager(name);
           repository = createRepository(repConfig.getRepositoryConfiguration(name), name, ds, trManager);
           repositories.put(name, repository);
        } else {
            LogUtils.debug(log, "\"{0}\" repository successfully retrieved from cache.", name);
        }
        return repository;
    }    
    
    /**
     * Configure multiple repositores
     * @see com.exigen.cm.RepositoryProvider#configure(java.util.Map)
     */
    public void configure(Map<String, String> configuration) throws RepositoryException {
        repConfig.configure(configuration);
    }
    
    /**
     * Configure multiple repositories from resource (file)
     * @param resource
     * @throws RepositoryException
     */
    public void configure(String resource) throws RepositoryException {
        repConfig.configure(resource);
    }
    
    /**
     * Configure individual repository
     * @param repositoryName
     * @param configuration
     * @throws RepositoryException
     */
    public void configure(String repositoryName, Map<String, String> configuration) throws RepositoryException{
        repConfig.configure(repositoryName, configuration);
    }
    
    /**
     * Configure individual repository datasource
     * @param repositoryName
     * @param ds datasource
     * @throws RepositoryException
     */
    public void configure(String repositoryName, DataSource ds) throws RepositoryException{
        repConfig.configure(repositoryName, ds);
    }
    
	@Override
    /**
     * Configure individual transaction manager
     * @param repositoryName
     * @param transaction manager
     * @throws RepositoryException
     */
	public void configure(String repositoryName, JCRTransactionManager manager) throws RepositoryException {
        repConfig.configure(repositoryName, manager);
	}

	
    /**
     * Configure individual repository from resource (file)
     * @param repositoryName
     * @param resource
     * @throws RepositoryException
     */
    public void configure(String repositoryName, String resource) throws RepositoryException {
        repConfig.configure(repositoryName, resource);
    }
    
    private synchronized RepositoryImpl createRepository(Map<String,String> config, String name, DataSource ds, JCRTransactionManager trManager) throws RepositoryException {
        if (log.isInfoEnabled()) {
            LogUtils.info(log, "The following repository configuration is used:");
            Set<String> keySet = config.keySet();
            Set<String> orderedKeySet = new TreeSet<String> (keySet);
            LogUtils.info(log, "**************************************************");
            for (String key: orderedKeySet) {
                String value = config.get(key);
                if (key.endsWith("password") && value != null) {                   
                    value = ((String)value).replaceAll("(.)", "*");
                }
                LogUtils.info(log, "{0}={1}", key, value);
            }
            LogUtils.info(log, "**************************************************");
        }
        
        RepositoryImpl _repository = null;

        try {
        	
        	// check configuration, fill default values
//        	checkFillConfiguration(config, name);

        	
        	/*String jndiName = (String)config.get(Constants.DATASOURCE_JNDI_NAME);
            String dialectClassName = (String)config.get(Constants.DATASOURCE_DIALECT_CLASSNAME);
            
            // check if data source really binded
            InitialContext ctx = new InitialContext();            
            Object ds = null;
            try {
                ds = ctx.lookup(jndiName);
            } catch (javax.naming.NameNotFoundException e) {
                LogUtils.info(log, "DataSource with name \"{0}\" not found in JNDI. Creating and binding...", jndiName);                
            }
            if (ds == null) {
                bindDatasource(config, jndiName, dialectClassName);    
            }*/                       
        	if (trManager != null){
        		TransactionHelper.getInstance().configureTransactionManager(TransactionHelper.APPLICATION_SERVER_SPRING ,trManager);
        	}
            _repository = new RepositoryImpl(this, findOrCreateAttributes(name), name);
            _repository.initialize(config, ds);
            
        } catch (RepositoryException e){
        	throw e;
        } catch (Exception ex){
            LogUtils.error(log, ex.getMessage(), ex);
            throw new RepositoryException("Error configuring Datasource", ex);
        }
        return _repository;
    }

	public void unregister(RepositoryImpl rep) {
		String key = null;
		for(String name:repositories.keySet()){
			RepositoryImpl r = repositories.get(name);
			if (r.equals(rep)){
				key = name;
				break;
			}
		}
		if (key != null){
			repositories.remove(key);
		}
		
	}

	@Override
	public void setAttribute(String repositoryName, String attrName, Object attrValue) {
		Map<String, Object> attrs = findOrCreateAttributes(repositoryName);
		attrs.put(attrName, attrValue);
		
	}

	synchronized Map<String, Object> findOrCreateAttributes(String repositoryName) {
		if (!attributes.containsKey(repositoryName)){
			attributes.put(repositoryName, new HashMap<String, Object>());
		}
		return attributes.get(repositoryName);
	}

    @Override
    public Set<String> getActiveRepositories() {
        return repositories.keySet();
    }
}

/*
 * $Log: RepositoryProviderImpl.java,v $
 * Revision 1.7  2008/07/17 06:57:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2008/07/16 08:38:11  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2008/07/15 11:27:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2007/09/06 11:17:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2007/08/29 12:55:30  dparhomenko
 * fix drop procedure for version without fts
 *
 * Revision 1.2  2007/05/31 08:54:15  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.31  2007/02/22 09:24:16  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.30  2006/12/11 09:29:29  dparhomenko
 * PTR#1803217 fix errors
 *
 * Revision 1.29  2006/10/13 09:20:28  dparhomenko
 * PTR#0148476 fix exception text
 *
 * Revision 1.28  2006/09/27 16:31:44  MZaharenkovs
 * #1801897 Added checking for property value != null, because it may result NPE if property is not defined
 *
 * Revision 1.27  2006/08/15 08:37:58  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.26  2006/07/19 10:31:19  zahars
 * PTR#0144986 Oracle index refresh interval introduced
 *
 * Revision 1.25  2006/07/18 12:51:15  zahars
 * PTR#0144986 INFO log level improved
 *
 * Revision 1.24  2006/06/02 07:21:28  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.23  2006/05/17 14:53:20  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 * Revision 1.22  2006/05/16 15:49:13  zahars
 * PTR#0144983 added option to read properties from <repository name>.properties
 *
 * Revision 1.21  2006/05/15 13:29:45  zahars
 * PTR#0144983 Constants from RepositoryProviderImpl are moved to Constants
 *
 * Revision 1.20  2006/05/10 08:04:12  dparhomenko
 * PTR#0144983 build 004
 *
 * Revision 1.19  2006/05/05 13:15:45  maksims
 * #0144986 JCRHelper.getPropertieByPrefix result changed to Map<String, Object>
 *
 * Revision 1.18  2006/05/03 13:11:43  zahars
 * PTR#0144983 Store properties introduced
 *
 * Revision 1.17  2006/04/27 08:24:20  dparhomenko
 * PTR#0144983 organize imports
 *
 * Revision 1.16  2006/04/21 12:11:34  dparhomenko
 * PTR#0144983 build procedure
 *
 * Revision 1.15  2006/04/20 15:02:16  zahars
 * PTR#0144983 properties cleanup
 *
 * Revision 1.14  2006/04/20 14:07:00  dparhomenko
 * PTR#0144983 bild procedure
 *
 * Revision 1.13  2006/04/20 12:38:17  dparhomenko
 * PTR#0144983 tests
 *
 * Revision 1.12  2006/04/20 12:33:36  zahars
 * PTR#0144983 Check stopword path for MSSQL
 *
 * Revision 1.11  2006/04/20 11:50:20  zahars
 * PTR#0144983 Refactoring
 *
 * Revision 1.10  2006/04/20 11:42:46  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.9  2006/04/19 13:21:24  zahars
 * PTR#0144983 Configuration properties are systematized
 *
 * Revision 1.8  2006/04/19 13:13:34  dparhomenko
 * PTR#0144983 tests
 *
 * Revision 1.7  2006/04/19 11:40:10  ivgirts
 * PTR #1801731 added support for WebDAV
 *
 * Revision 1.6  2006/04/19 08:19:05  zahars
 * PTR#0144983 JCSCacheManager set as default one
 *
 * Revision 1.5  2006/04/19 08:06:46  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.4  2006/04/18 14:44:21  zahars
 * PTR#0144983 initialization process streamlined
 *
 * Revision 1.3  2006/04/18 12:53:47  zahars
 * PTR#0144983 default value for repository JNDI name moved to RepositoryProviderImpl
 *
 * Revision 1.2  2006/04/18 11:45:04  zahars
 * PTR#0144983 configuration fill/check moved from RepositoryImpl to RepositoryProviderImpl
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.20  2006/04/13 10:03:44  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.19  2006/04/12 12:03:02  zahars
 * PTR#0144983 Check for dialect added
 *
 * Revision 1.18  2006/04/11 15:47:11  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.17  2006/03/27 15:05:10  dparhomenko
 * PTR#0144983 remove _JCRHelper
 *
 * Revision 1.16  2006/03/24 08:54:37  ivgirts
 * PTR #1801059 RepositoryImpl.initalize(Map config) method introduced
 *
 * Revision 1.15  2006/03/23 15:44:57  ivgirts
 * PTR #1801413 added configuration property for starting FTS threads
 *
 * Revision 1.14  2006/03/21 17:03:14  ivgirts
 * PTR #1801413 Two threads(indexing and text extracting) a launched during repository creation
 *
 * Revision 1.13  2006/03/03 12:43:11  ivgirts
 * PTR #1801059 system out removed
 *
 * Revision 1.12  2006/03/03 12:10:07  ivgirts
 * PTR #1801059 added password masking before logging password
 *
 * Revision 1.11  2006/03/03 11:07:49  ivgirts
 * PTR #1801059 thorws SQLException replaced with throws RepositoryException
 *
 * Revision 1.10  2006/03/03 10:33:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.9  2006/03/03 09:39:24  ivgirts
 * PTR #1801059 Database startup checking added
 *
 * Revision 1.8  2006/02/27 16:22:19  ivgirts
 * PTR#1801059 Configuration file added.
 *
 * Revision 1.5  2006/02/20 15:32:25  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.4  2006/02/17 13:46:47  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 * Revision 1.3  2006/02/16 13:53:04  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.2  2006/02/13 12:40:40  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 * Revision 1.1  2006/02/10 15:50:23  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */