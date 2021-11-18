/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_JNDI_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.RepositoryProvider;
import com.exigen.cm.database.ConnectionProviderImpl;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.InvalidNodeTypeDefException;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.compact.CompactNodeTypeDefReader;
import com.exigen.cm.jackrabbit.nodetype.compact.ParseException;
import com.exigen.cm.jackrabbit.nodetype.xml.NodeTypeReader;
import com.exigen.cm.jackrabbit.util.name.NamespaceMapping;
import com.exigen.cm.utils.UtilsHelper;

public class CreateRepositoryHelper {

//	private static RepositoryImpl repository;
    private static Log log = LogFactory.getLog(CreateRepositoryHelper.class);


	public static void createRepositoryTables(DataSource dataSource)  throws RepositoryException {
		createRepositoryTables(dataSource, new HashMap<String, String>());
	}
	
	public static void createRepositoryTables(DataSource dataSource, HashMap<String, String> config)  throws RepositoryException {
		config = new HashMap<String, String>(config);
		RepositoryImpl repository = initRepository(config, dataSource);
		DatabaseConnection conn = repository.getConnectionProvider().createConnection();
        try {
            Method method;
			method = ReflectHelper.getMethod(repository.getClass(), "initialize", new Class[]{config.getClass(),DataSource.class});
            try {
                method.invoke(repository, new Object[]{config, dataSource});
            } catch (java.lang.reflect.InvocationTargetException exc){
                throw new RepositoryException("Error creating repository", exc.getTargetException());
            } catch (IllegalArgumentException exc) {
            	throw new RepositoryException("Error creating repository", exc);
    		} catch (IllegalAccessException exc) {
    			throw new RepositoryException("Error creating repository", exc);
    		}
            conn.commit();
        } finally {
            conn.close();
        }
        log.info("Repository Created.");
	}

	public static void dropRepositoryTables(DataSource dataSource) throws RepositoryException{
		dropRepositoryTables(dataSource, new HashMap<String, String>());
	}
	
	public static void dropRepositoryTables(DataSource dataSource, HashMap<String, String> config) throws RepositoryException{
		config = new HashMap<String, String>(config);
		RepositoryImpl repository = initRepository(config, dataSource);
        DatabaseConnection conn = repository.getConnectionProvider().createConnection();
        try {
        	String methodName = "dropRepository";
            Method method = ReflectHelper.getMethod(repository.getClass(), methodName, new Class[]{Map.class, DatabaseConnection.class});
            try {
                method.invoke(repository, new Object[]{config, conn});
            } catch (java.lang.reflect.InvocationTargetException exc){
                throw new RepositoryException("Error dropping repository", exc.getTargetException());
            } catch (IllegalArgumentException exc) {
            	throw new RepositoryException("Error dropping repository", exc);
			} catch (IllegalAccessException exc) {
				throw new RepositoryException("Error dropping repository", exc);
			}
            conn.commit();
        } finally {
            conn.close();
        }
        log.info("Repository Dropped.");
	}

	public static void importNodeTypes(DataSource dataSource, String[] nodeTypeFiles) throws RepositoryException{
		importNodeTypes(dataSource, nodeTypeFiles, new HashMap<String, String>());
	}

	private static void importNodeTypes(DataSource dataSource, String[] nodeTypeFiles, HashMap<String, String> config) throws RepositoryException{
		config = new HashMap<String, String>(config);
		RepositoryImpl repository = createRepository(dataSource, config);
		SessionImpl session = repository.getSystemSession();

		for(String fileName:nodeTypeFiles){
			importNodeTypes(session, fileName);
		}
		session.getConnection().commit();
		session.logout();
	}
	

	private static RepositoryImpl createRepository(DataSource dataSource,
			HashMap<String, String> config) throws RepositoryException {
		String name="importNodeType_"+System.currentTimeMillis();
        config.put(Constants.PROPERTY_DEVELOPMENT_MODE, "false");
        config.put(Constants.PROPERTY_DATASOURCE_DROP_CREATE, "false");
        config.put(Constants.PROPERTY_THREADS_ON, "false");
        config.put(Constants.PROPERTY_CMD_EXTRACTOR_ON, "false");


		RepositoryProvider provider = RepositoryProvider.getInstance();
		provider.configure(name, dataSource);
		provider.configure(name, config);
		
		RepositoryImpl repository = (RepositoryImpl) provider.getRepository(name); 
		return repository;
	}


	private static void importNodeTypes(SessionImpl session, String nodetypes) throws RepositoryException{
		InputStream in = JCRHelper.getInputStream(nodetypes, true);

		List<NodeTypeDef> defs;
		RepositoryImpl repository = session._getRepository();

		Reader r = new InputStreamReader(in);
		Properties namespaces = null;
		NamespaceMapping nsMapping = null;
		if (nodetypes.endsWith(".cnd")){
			//1.try cnd format
			try {
				CompactNodeTypeDefReader reader = new CompactNodeTypeDefReader(r, nodetypes);
				defs = reader.getNodeTypeDefs();
				nsMapping = reader.getNamespaceMapping();
			} catch (ParseException e) {
				throw new RepositoryException("Error reading nodetype definitions. ", e);
			}
		} else if (nodetypes.endsWith(".xml")){
			//2. try xml format
			try {
				NodeTypeReader reader = new NodeTypeReader(in, repository);
				
				defs = Arrays.asList(reader.getNodeTypeDefs());
				namespaces = reader.getAddedNamespaces();
			} catch (IOException e) {
				throw new RepositoryException("Error reading nodetype definitions. ", e);
			} catch (InvalidNodeTypeDefException e) {
				throw new RepositoryException("Error reading nodetype definitions. ", e);
			} catch (IllegalNameException e) {
				throw new RepositoryException("Error reading nodetype definitions. ", e);
			} catch (UnknownPrefixException e) {
				throw new RepositoryException("Error reading nodetype definitions. ", e);
			}
		} else {
			throw new RepositoryException("Unknown file format");
		}
		
		
		if (namespaces != null){
			NamespaceRegistryImpl nr = repository.getNamespaceRegistry();
			for(Object _prefix:namespaces.keySet()){
				String prefix = (String) _prefix;
				String uri = namespaces.getProperty(prefix);
				if (!nr.hasPrefix(prefix)){
					nr.registerNamespace(prefix, uri);
				}
			}
		}
		
		if (nsMapping != null){
			NamespaceRegistryImpl nr = repository.getNamespaceRegistry();
			for(Object _prefix:nsMapping.getPrefixes()){
				String prefix = (String) _prefix;
				String uri = nsMapping.getURI(prefix);
				if (!nr.hasPrefix(prefix)){
					nr.registerNamespace(prefix, uri);
				}
			}
			
		}
				
		importNodeTypes(defs, session);
		log.info("Nodetypes "+nodetypes+" imported.");

		
	}
	
	private static void importNodeTypes(List<NodeTypeDef> defs, SessionImpl session) throws RepositoryException{
		NodeTypeManagerImpl ntManager = session.getNodeTypeManager();
		ntManager.registerNodeDefs(session.getConnection(), defs, true);
	}


	private static RepositoryImpl initRepository(Map<String, String> config, DataSource datasource) throws RepositoryException {
		String repname="importNodeType_"+System.currentTimeMillis();
		if (config.get(Constants.CONFIG_MS_FTS_STOPWORD) == null){
        	config.put(Constants.CONFIG_MS_FTS_STOPWORD, "skip");
        }
        UtilsHelper.configureRepository(config, true, false);
        config.put(Constants.PROPERTY_DATASOURCE_SKIP_CHECK, "false");        

        RepositoryConfiguration c = new RepositoryConfiguration();
        c.configure(repname, datasource);
        c.checkFillConfiguration(repname, config );
        
        Method method;
        if (datasource == null){
	        String dialectName = (String) config.get(Constants.PROPERTY_DATASOURCE_DIALECT_CLASSNAME);
	        //bind datasource
	        method = ReflectHelper.getMethod(ConnectionProviderImpl.class, "bindDatasource", new Class[]{Map.class, String.class, String.class, InitialContext.class});
	        try {
	            method.invoke(null, new Object[]{config, "tempDS", dialectName, new InitialContext()});
	            config.put(PROPERTY_DATASOURCE_JNDI_NAME, "tempDS");
	        } catch (java.lang.reflect.InvocationTargetException exc){
	            throw new RepositoryException("Error configuring datasource", exc.getTargetException());
	        } catch (IllegalArgumentException exc) {
	        	throw new RepositoryException("Error configuring datasource", exc);
			} catch (IllegalAccessException exc) {
				throw new RepositoryException("Error configuring datasource", exc);
			} catch (NamingException e) {
				throw new RepositoryException("Error configuring datasource", e);
			}
        }
        
        RepositoryImpl repository = ReflectHelper.createRepositoryObject();
        
        method = ReflectHelper.getMethod(repository.getClass(), "initVariables", new Class[]{Map.class, DataSource.class});
        try {
            method.invoke(repository, new Object[]{config, datasource});
        } catch (java.lang.reflect.InvocationTargetException exc){
            throw new RepositoryException("Error configuring Repository instance", exc.getTargetException());
        } catch (IllegalArgumentException exc) {
        	throw new RepositoryException("Error configuring Repository instance", exc);
		} catch (IllegalAccessException exc) {
			throw new RepositoryException("Error configuring Repository instance", exc);		
        } catch (Exception exc){
            throw new RepositoryException("Error configuring Repository instance", exc);
        }
        return repository;
	}


}
