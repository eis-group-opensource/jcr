/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.InvalidNodeTypeDefException;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.compact.CompactNodeTypeDefReader;
import com.exigen.cm.jackrabbit.nodetype.compact.ParseException;
import com.exigen.vf.commons.logging.LogUtils;

public class DataImporter {
    
    private static Log log = LogFactory.getLog(DataImporter.class);
    
    public static final String IMPORT_PREFIX_DELIMITER = Constants.IMPORT_PREFIX + Constants.PROPERTY_DELIMITER;
    
    
    private HashMap<String, DataFiles> data = new HashMap<String, DataFiles>();
    private String nodetypeFilePath = null;
    private RepositoryImpl repository = null;

	private boolean skipBuiltin = false;
    
    public DataImporter(Map<String, String> config, RepositoryImpl repository) throws RepositoryException {
        this.repository = repository;
        this.nodetypeFilePath = (String)config.get(Constants.NODETYPE_FILE_PATH);

        if (config.get(Constants.NODETYPE_SKIP_BUILTIN) != null){
        	if (Boolean.parseBoolean(config.get(Constants.NODETYPE_SKIP_BUILTIN))){
        		this.skipBuiltin  = true;
        	}
        }
        
        parseImportConfig(Constants.DEFAULT_WORKSPACE, config);
        
        //parse other workspaces
        ArrayList<String> workspaces = new ArrayList<String>();
        for(Iterator it = config.keySet().iterator();it.hasNext() ;){
            String key = (String) it.next();
            if (key.indexOf(".") > 0 && !Constants.PROPERTY_ROOT_PASSWORD.equals(key) && !Constants.PROPERTY_ROOT_USER.equals(key)){ //&& key.indexOf(".") < key.indexOf("=")
                String workspaceName = key.substring(0,key.indexOf("."));
                if (!workspaces.contains(workspaceName)){
                    workspaces.add(workspaceName);
                }
            }
        }
        for(String workspaceName:workspaces){
            Map<String, String> importConfig = JCRHelper.getPropertiesByPrefix(workspaceName, config);
            parseImportConfig(workspaceName, importConfig);
        }
    }
    
    private void parseImportConfig(String workspaceName, Map<String, String> importConfig) {
        DataFiles workspace = new DataFiles(workspaceName);
        workspace.setDataFilePath((String)importConfig.get(Constants.DATA_FILE_PATH));
        workspace.setSecurityFilePath((String)importConfig.get(Constants.SECURITY_FILE_PATH));
        workspace.setBinaryZipFilePath((String)importConfig.get(Constants.BINARY_ZIP_FILE_PATH));
        workspace.setUsername((String)importConfig.get(Constants.PROPERTY_ROOT_USER));
        workspace.setPassword((String)importConfig.get(Constants.PROPERTY_ROOT_PASSWORD));
        if (!workspace.isEmpty()){
        	data.put(workspace.getWorkspaceName(), workspace);
        }
    }

    
    private void importNodeTypes(DatabaseConnection conn, NamespaceRegistryImpl nmRegistry, NodeTypeManagerImpl ntm, InputStream in, String name) throws RepositoryException, InvalidNodeTypeDefException {
        
        JCRTransaction tr1 = null;
        if (TransactionHelper.getInstance().getTransactionManager() != null){
        	//conn.commit();
        	//conn.close();
        	tr1 = TransactionHelper.getInstance().getTransactionManager().startNewTransaction();
        	conn = repository.getConnectionProvider().createConnection();
        }
    	try {
	        List<NodeTypeDef> defs;
	        Map namespaces;
	        try {
	        	if (name.endsWith(".cnd")){
	        		try {
						CompactNodeTypeDefReader reader = new CompactNodeTypeDefReader(new InputStreamReader(in),name);
			            defs = reader.getNodeTypeDefs();
			            namespaces = reader.getNamespaceMapping().getPrefixToURIMapping();
					} catch (ParseException e) {
						throw new RepositoryException("Error parsing nodetypes from "+name+" :"+e.getMessage());
					}
	        	} else if (name.endsWith(".xml")){
		            com.exigen.cm.jackrabbit.nodetype.xml.NodeTypeReader reader = new com.exigen.cm.jackrabbit.nodetype.xml.NodeTypeReader(in, repository);
		            defs = Arrays.asList(reader.getNodeTypeDefs());
		            namespaces = reader.getNamespaces();
	        	} else {
	        		throw new RepositoryException("Unsupported nodetype file format: "+name);
	        	}
	        } catch (IllegalNameException e) {
	            throw new InvalidNodeTypeDefException(
	                    "Invalid namespace reference in a node type definition", e);
	        } catch (UnknownPrefixException e) {
	            throw new InvalidNodeTypeDefException(
	                    "Invalid namespace reference in a node type definition", e);
	        } catch (IOException e) {
	            throw new RepositoryException(e);
	        } finally {
	            if (in != null) {
	                try {
	                    in.close();
	                } catch (IOException e) {/**/}
	            }
	        }
	        
	        ArrayList<String> exists = new ArrayList<String>();
	        exists.addAll(Arrays.asList(nmRegistry.getPrefixes()));
	        for(Iterator it = namespaces.keySet().iterator(); it.hasNext();){
	            String prefix = (String) it.next();
	            String uri = (String) namespaces.get(prefix);
	            if (!exists.contains(prefix)){
	                nmRegistry.registerNamespace(prefix, uri, conn);
	            }
	        }
	        
	        ntm.registerNodeDefs(conn, defs, false);
    	} finally {
    		conn.commit();
    		
            if (tr1 != null){
            	conn.commit();
            	conn.close();
            	TransactionHelper.getInstance().getTransactionManager().commitAndResore(tr1);
            	//tr1 = TransactionHelper.getInstance().getTransactionManager().startNewTransaction();
            	conn = repository.getConnectionProvider().createConnection();
            }    		
    	}
        if (TransactionHelper.getInstance().getTransactionManager() != null){
        	conn.commit();
        	conn.close();
        	tr1 = TransactionHelper.getInstance().getTransactionManager().startNewTransaction();
        	conn = repository.getConnectionProvider().createConnection();
        }
        try {
	        repository.reloadNodeTypeReader();
	        repository.increaseNodeTypeCounter();
        } finally {
    		conn.commit();
    		
            if (tr1 != null){
            	conn.commit();
            	conn.close();
            	TransactionHelper.getInstance().getTransactionManager().commitAndResore(tr1);
            	//tr1 = TransactionHelper.getInstance().getTransactionManager().startNewTransaction();
            	//conn = repository.getConnectionProvider().createConnection();
            }    		
        }
    }
    
    
    public void importNodeTypes(DatabaseConnection conn, NamespaceRegistryImpl nmRegistry, NodeTypeManagerImpl ntm) throws RepositoryException, InvalidNodeTypeDefException {
        InputStream in = null;
        if (nodetypeFilePath != null) {
        	StringTokenizer st = new StringTokenizer(nodetypeFilePath,",");
        	while (st.hasMoreTokens()){
        		String fileName = st.nextToken();
        		fileName = fileName.trim();
	            in = JCRHelper.getInputStream(fileName, true);            
	            LogUtils.info(log, "Using \"{0}\" file for creating node types.", fileName);
	            importNodeTypes(conn, nmRegistry, ntm, in, fileName);
        	}
        } else {
	        LogUtils.info(log, "Using \"{0}\" file for creating node types.", Constants.ECR_NODETYPES_RESOURCE_NAME);
	        in = getClass().getClassLoader().getResourceAsStream(Constants.ECR_NODETYPES_RESOURCE_NAME);
	        importNodeTypes(conn, nmRegistry, ntm, in, Constants.ECR_NODETYPES_RESOURCE_NAME);
        }
    }

    public void importBuiltinNodeTypes(DatabaseConnection conn, NamespaceRegistryImpl nmRegistry, NodeTypeManagerImpl ntm) throws RepositoryException, InvalidNodeTypeDefException {
    	if (!skipBuiltin){
	        LogUtils.info(log, "Using \"{0}\" file for creating node types.", Constants.BUILTIN_NODETYPES_RESOURCE_PATH);
	        InputStream in = getClass().getClassLoader().getResourceAsStream(Constants.BUILTIN_NODETYPES_RESOURCE_PATH);
	        importNodeTypes(conn, nmRegistry, ntm, in, Constants.BUILTIN_NODETYPES_RESOURCE_PATH);
    	}
    }


    
    public void importData() throws RepositoryException {
        for(DataFiles df:data.values()){
            checkWorkspace(df);
            String dataFilePath = df.getDataFilePath();
            if (dataFilePath != null && !dataFilePath.equals("")) {
            	dataFilePath = dataFilePath.trim();
                log.info("Import data from "+dataFilePath+ " to workspace "+df.getWorkspaceName());
                String binaryZipFilePath = df.getBinaryZipFilePath();
                if (binaryZipFilePath != null){
                	binaryZipFilePath = binaryZipFilePath.trim();
                }
                if ("".equals(binaryZipFilePath)){
                    binaryZipFilePath = null;
                }
                if (binaryZipFilePath != null) {
                    log.info("Import binary from " + binaryZipFilePath);
                }
                
                String user = df.getUsername() != null ? df.getUsername():Constants.DEFAULT_ROOT_USER_NAME;
                String password = df.getPassword() != null ? df.getPassword():Constants.DEFAULT_ROOT_PASSWORD;
                SimpleCredentials credentials = new SimpleCredentials(user, password.toCharArray());
                SessionImpl session  = (SessionImpl)repository.login(credentials, df.getWorkspaceName());   
                InputStream fis = null;
                try {
                    
                    fis = JCRHelper.getInputStream(dataFilePath, true);
                    LogUtils.debug(log, "Importing data from file \"{0}\"", dataFilePath);

                    ZipFile zipFile = null;
                    if (binaryZipFilePath != null) {
                        zipFile = new ZipFile(binaryZipFilePath);
                    }    
                    /*NodeTypeManagerImpl ntm = session.getNodeTypeManager();
                    NodeTypeIterator nti = ntm.getAllNodeTypes();
                    while (nti.hasNext()){
                    	NodeTypeImpl nt = (NodeTypeImpl) nti.nextNodeType();
                    	System.out.println(nt.getName());
                    	
                    }*/
                    
                    session.importXML("/", fis, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW, zipFile);
                    session.save();
                } catch (RepositoryException exc){
                    throw exc;
                } catch (Exception e) {
                    //LogUtils.error(log, e.getMessage(), e);
                    throw new RepositoryException(e);
                } finally {
                    session.logout();
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {/**/}
                    }
                }
            }
        }
    }
    
    private void checkWorkspace(DataFiles df) throws RepositoryException {
        DatabaseConnection conn = repository.getConnectionProvider().createConnection();
        try {
            if (!repository.checkWorkspace(conn, df.getWorkspaceName())) {
                NodeTypeManagerImpl ntm = new NodeTypeManagerImpl(repository.getNamespaceRegistry(), repository.getNodeTypeReader(), repository);
                repository.createWorkspace(conn, repository.getNamespaceRegistry(), 
                        ntm, df.getWorkspaceName());
                conn.commit();
            }
        } finally {
            conn.close();
        }
        
    }

    public void importSecurity() throws RepositoryException {
        for(DataFiles df:data.values()){
            checkWorkspace(df);
            String securityFilePath = df.getSecurityFilePath();
            if (securityFilePath != null && !securityFilePath.equals("")) {
            	securityFilePath = securityFilePath.trim();
                log.info("Import security from "+securityFilePath+ " to workspace "+df.getWorkspaceName());
                //File securityFile = new File(securityFilePath);
                //SimpleCredentials credentials = new SimpleCredentials(df.getUsername(), df.getPassword().toCharArray());
                //SessionImpl session  = (SessionImpl)repository.login(credentials);
                InputStream fis = null;
                try {
                    fis = JCRHelper.getInputStream(securityFilePath, true);
                    LogUtils.debug(log, "Importing security from file \"{0}\"", securityFilePath);
                    repository.importSecurity(fis);
                } catch (Exception e) {
                    LogUtils.error(log, e.getMessage(), e);
                    throw new RepositoryException(e);
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {/**/}
                    }
                }
            }
        }
    }    
}
class DataFiles{
    
    private String workspaceName;
    
    private String dataFilePath;
    private String securityFilePath;
    private String binaryZipFilePath;
    
    private String username;
    private String password;
    
    public DataFiles(String workspaceName){
        this.workspaceName = workspaceName;
    }

    public boolean isEmpty() {
		if (notEmpty(dataFilePath) || notEmpty(securityFilePath) || notEmpty(binaryZipFilePath)){
			return false;
		}
		return true;
	}

	private boolean notEmpty(String value) {
		if (value != null && value.length() > 0){
			return true;
		} else {
			return false;
		}
	}

	public String getDataFilePath() {
        return dataFilePath;
    }

    public void setDataFilePath(String dataFilePath) {
        this.dataFilePath = dataFilePath;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSecurityFilePath() {
        return securityFilePath;
    }

    public void setSecurityFilePath(String securityFilePath) {
        this.securityFilePath = securityFilePath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public String getBinaryZipFilePath() {
        return binaryZipFilePath;
    }

    public void setBinaryZipFilePath(String binaryZipFilePath) {
    this.binaryZipFilePath = binaryZipFilePath;}
    
    
    
}

/*
 * $Log: DataImporter.java,v $
 * Revision 1.5  2007/10/22 10:58:49  dparhomenko
 * Fix locks in Spring + MSSQL environment
 *
 * Revision 1.4  2007/10/19 13:45:18  dparhomenko
 * migrate to ECR types
 *
 * Revision 1.3  2007/07/05 08:52:12  dparhomenko
 * PTR#0152003 fix insert statemnt for oracle blobs
 *
 * Revision 1.2  2007/05/21 10:58:02  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.24  2007/02/26 13:14:39  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.23  2007/02/22 09:24:16  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.22  2007/02/05 13:14:03  dparhomenko
 * PTR#1803853 fix session scope lock
 *
 * Revision 1.21  2007/01/24 08:46:25  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.20  2006/12/11 12:17:58  dparhomenko
 * PTR#1803217 fix errors
 *
 * Revision 1.19  2006/12/04 13:42:51  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.18  2006/10/30 15:03:35  dparhomenko
 * PTR#1803272 a lot of fixes
 *
 * Revision 1.17  2006/10/23 14:38:15  dparhomenko
 * PTR#0148641 fix data import
 *
 * Revision 1.16  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.15  2006/10/03 06:20:42  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.14  2006/09/26 10:11:07  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.13  2006/09/13 08:27:40  dparhomenko
 * PTR#0148153 fix FTS
 *
 * Revision 1.12  2006/08/25 13:26:13  zahars
 * PTR#0144986 import updated to support import binary from zip
 *
 * Revision 1.11  2006/07/12 11:51:05  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.10  2006/06/26 14:01:41  dparhomenko
 * PTR#0146672 move operations
 *
 * Revision 1.9  2006/05/18 14:53:54  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 * Revision 1.8  2006/05/12 08:58:44  dparhomenko
 * PTR#0144983 Fix import export parameter description
 *
 * Revision 1.7  2006/05/05 13:15:44  maksims
 * #0144986 JCRHelper.getPropertieByPrefix result changed to Map<String, Object>
 *
 * Revision 1.6  2006/04/27 08:24:20  dparhomenko
 * PTR#0144983 organize imports
 *
 * Revision 1.5  2006/04/25 14:30:52  dparhomenko
 * PTR#0144983 fix security
 *
 * Revision 1.4  2006/04/24 12:52:34  dparhomenko
 * PTR#0144983 add additional log
 *
 * Revision 1.3  2006/04/24 12:47:46  dparhomenko
 * PTR#0144983 add session.logout()
 *
 * Revision 1.2  2006/04/20 11:42:46  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.9  2006/04/13 10:03:44  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.8  2006/04/12 13:45:08  dparhomenko
 * PTR#0144983 MS FTS
 *
 * Revision 1.7  2006/04/12 13:22:11  ivgirts
 * PTR #1801676 added implmentation of the user management and RepositoryAuthenticator
 *
 * Revision 1.6  2006/04/12 12:48:59  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.5  2006/04/12 08:30:49  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.4  2006/04/11 15:47:11  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.3  2006/04/10 11:30:12  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.2  2006/04/07 14:43:01  ivgirts
 * PTR #1801059 Authenticator is used for user authentication
 *
 * Revision 1.1  2006/04/06 14:38:31  ivgirts
 * PTR #1800998 added importer class
 *
 */

