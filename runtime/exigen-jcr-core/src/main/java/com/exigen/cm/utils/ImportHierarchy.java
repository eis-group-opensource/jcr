/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.RepositoryProvider;
import com.exigen.cm.impl.RepositoryImpl;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

/**
 * Imports files hierarchy either zipped or not into repository
 * @author Maksims
 *
 */
public class ImportHierarchy {
    
    private static Log log = LogFactory.getLog(ImportHierarchy.class);
    
    private static final String OPTION_SOURCE="source";
    private static final String OPTION_ROOT="importRootPath";
    
    private static String DEFAULT_ENCODING="UTF-8";
//    private static String DEFAULT_MIME_TYPE="application/octet-stream";
    
    
    private Repository repository;
    private Session session;
    private Node importRoot;
    
//  Holds mapping from file extension to mime-type. Defaults to application/octet-stream
    private static Properties extensionToMimeType = null;
    
    
    private ImportHierarchy(Map<String, String> configuration, 
            String wspName, String user,
            String pwd, String rootPath) throws Exception{
        
        repository = getRepository(configuration);
        session = repository.login(new SimpleCredentials(user, pwd.toCharArray()), wspName);
        importRoot = session.getRootNode();
        
        if(rootPath != null) {
        	if (rootPath.startsWith("/")){
        		rootPath = rootPath.substring(1);
        	}
    		if (!importRoot.hasNode(rootPath)){
    			importRoot.addNode(rootPath);
    			importRoot.save();
    		}
    		importRoot = importRoot.getNode(rootPath);
        	
        }
    }
    
    private RepositoryImpl getRepository(Map<String, String> configuration) throws RepositoryException {
        RepositoryProvider provider = RepositoryProvider.getInstance();
        provider.configure(Constants.DEFAULT_REPOSITORY_NAME , configuration);
        RepositoryImpl repository = (RepositoryImpl)provider.getRepository();
        return repository;
    }    
    

    /**
     * Imports hierarchy from zip.
     * @param zip
     * @throws Exception
     */
    private void importData(ZipFile zip) throws Exception{
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while(entries.hasMoreElements()){
            ZipEntry entry = entries.nextElement();
            
            String path = entry.getName();
            if(entry.isDirectory()){
                getFolderNode(path, importRoot);
            }else{
                int fileIdx = path.lastIndexOf('/');//File.separatorChar); // ZIP separates names using /
                
                String docName;
                Node folder;
                
                if(fileIdx < 0){
                    docName = path;
                    folder = importRoot;
                }else{
                    folder = getFolderNode(path.substring(0, fileIdx), importRoot);
                    docName = path.substring(fileIdx+1);
                }
                
                addDocument(folder, docName, 
                        zip.getInputStream(entry), extensionToMimeType(getExtension(docName)),
                        entry.getSize(), DEFAULT_ENCODING);
                
            }
        }
    }
    
    
    /**
     * Imports hierarchy from folder.
     * @param zip
     * @throws Exception
     */
    private void importData(File file) throws Exception{
        importData(file.isDirectory() ? file.listFiles() : new File[]{file}, importRoot);
    }
    
    private void importData(File[] files, Node context) throws Exception{

        for(File file : files){
            if(file.isDirectory()){
                Node res = getFolderNode(file.getName(), context);
                importData(file.listFiles(), res);
            }else{
                String fileName = file.getName();
                String message = MessageFormat.format("Adding file {0}", new Object[]{fileName});
                log.info(message);
                
                String mimeType = URLConnection.guessContentTypeFromName(fileName);
                if(mimeType == null)
                    mimeType = Constants.UNDEFINED_MIME_TYPE;
                
//                String mimeType = extensionToMimeType(getExtension(fileName));
                    
                addDocument(context, fileName, new FileInputStream(file), 
                        mimeType,
                        file.length(), DEFAULT_ENCODING);
            }
        }
    }
    
    
    
    private Node getFolderNode(String path, Node current) throws Exception{
        if(current.hasNode(path)) // check by full path
            return current.getNode(path);
    
        StringTokenizer segments = new StringTokenizer(path, File.separator);
        while(segments.hasMoreTokens()){
            String name = segments.nextToken();
            if(current.hasNode(name))
                current = current.getNode(name);
            else{
                Node next = current.addNode(name, "ecr_nt:folder");
                current.save();
                current = next;
            }
        }
        
        return current;
    }

    
    private void addDocument(Node folder, String name, InputStream data, String mimeType, long size, String encoding) throws Exception{
        Node doc = folder.addNode(name, "ecr_nt:document");
        Node content = doc.addNode("jcr:content", "ecr_nt:resource");
        content.setProperty("jcr:data", data);
        content.setProperty("jcr:encoding", encoding);
        content.setProperty("ecr:fileName", name);
        content.setProperty("jcr:mimeType", mimeType);
        //content.setProperty("ewf:size", size);

        folder.save();
        data.close();
    }


    /**
     * Returns file extension or <code>null</code> if file have no extension e.g
     * file started or ended with dot or it has no dot in file name.
     * @param fileName
     * @return
     */
    private static String getExtension(String fileName){
        int idx = fileName.lastIndexOf('.');
//      If fileName starts or ends from . assuming file have no extension
        return idx > 0 && idx+1 < fileName.length() ? fileName.substring(idx+1) : null;
    }
    

    /**
     * Returns mime-type associated with passed file extension.
     * Defaults to application/octet-stream mime-type if no such file extension is registered.
     * @param extension
     * @return
     * @throws Exception
     */
    private static String extensionToMimeType(String extension) throws Exception{
        if(extensionToMimeType == null){
            InputStream data = ImportHierarchy.class.getResourceAsStream("extensionsMapping.properties");
            extensionToMimeType = new Properties();
            extensionToMimeType.load(data);
        }

        return extensionToMimeType.getProperty(extension, Constants.UNDEFINED_MIME_TYPE);
    }
    

    /**
     * Params are:
     * - ZIP file name or Folder path (MANDATORY)
     * - Repository name (OPTIONAL)
     * - Workspace name (OPTIONAL)
     * - User (OPTIONAL)
     * - password
     * - Path in repository to import into ...
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception{
        SimpleJSAP jsap = new SimpleJSAP(
                "ImportHierarchy",
                "Import file structure into repository",
                new Parameter[] {
                        new FlaggedOption(OPTION_SOURCE, JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 's', "source","Import source. Can be either zip or folder"),
                        new FlaggedOption(OPTION_ROOT, JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'p', "repositoryPath","Node to import files into"),                        
                    });

        UtilsHelper.addRepositoryPropertiesParameter(jsap);

        JSAPResult config = jsap.parse(args);
        if(!config.success())
            reportError(jsap, config);
        
        String sourceName = config.getString(OPTION_SOURCE);
        File srcFile = new File(sourceName);
        if(!srcFile.exists()){
            String message = MessageFormat.format("Cannot import data from non-existing source {0}"
                    , new Object[]{srcFile.getAbsolutePath()});
            log.error(message);
            System.exit(-1);
        }
        String root = config.getString(OPTION_ROOT);
        
        
        String wspName = config.getString(UtilsHelper.OPTION_REPOSITORY_WORKSPACE);
        String user = config.getString(UtilsHelper.OPTION_REPOSITORY_USER);
        String pwd = config.getString(UtilsHelper.OPTION_REPOSITORY_PASSWORD);

        if (wspName == null){
        	wspName = Constants.DEFAULT_WORKSPACE;
        }
        if (user == null){
        	user = Constants.DEFAULT_ROOT_USER_NAME;
        	pwd = Constants.DEFAULT_ROOT_PASSWORD;
        } else {
        	if (pwd == null){
        		throw new RepositoryException("password can't be empty");
        	}
        }
        
        Map<String, String> configuration = UtilsHelper.getRepositoryConfiguration(null,config.getString(UtilsHelper.OPTION_REPOSITORY_PROPERTIES), true, false); 
        
        ImportHierarchy cmd = new ImportHierarchy(configuration, wspName, user, pwd, root);
        
        String message = MessageFormat.format("Preparing to import data from {0}"
                , new Object[]{srcFile.getAbsolutePath()});
        log.info(message);

        if(srcFile.isDirectory())
            cmd.importData(srcFile);
        else
            cmd.importData(new ZipFile(srcFile));
            

    }
    
    private static void reportError(SimpleJSAP jsap, JSAPResult config){
        System.err.println();

        for (java.util.Iterator errs = config.getErrorMessageIterator(); errs
                .hasNext();) {
            System.err.println("Error: " + errs.next());
        }

        System.err.println();
        System.err.println("Usage: java " + ImportHierarchy.class.getName());
        System.err.println("                " + jsap.getUsage());
        System.err.println();
        System.err.println(jsap.getHelp());
        System.exit(1);
    }
}

/*
 * $Log: ImportHierarchy.java,v $
 * Revision 1.4  2007/11/06 12:42:48  dparhomenko
 * PTR#1805691
 *
 * Revision 1.3  2007/10/19 13:45:19  dparhomenko
 * migrate to ECR types
 *
 * Revision 1.2  2007/05/21 13:31:44  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.1  2007/04/26 09:00:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.18  2006/08/21 11:03:21  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.17  2006/07/12 11:51:20  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.16  2006/07/04 15:37:27  maksims
 * #1802356 Remove fixed
 *
 * Revision 1.15  2006/05/19 11:41:55  zahars
 * PTR#0144983 Configuration for utils updated
 *
 * Revision 1.14  2006/05/19 08:45:56  zahars
 * PTR#0144983 Short option names introduced
 *
 * Revision 1.13  2006/05/18 15:14:47  zahars
 * PTR#0144983 Added ability to read default properties from configuration file
 *
 * Revision 1.12  2006/05/18 14:53:50  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 * Revision 1.11  2006/05/05 13:17:24  maksims
 * #0144986 JCRHelper.getPropertieByPrefix result changed to Map<String, Object> so method signature is changed correspondingly
 *
 * Revision 1.10  2006/05/03 13:11:40  zahars
 * PTR#0144983 Store properties introduced
 *
 * Revision 1.9  2006/04/28 15:35:00  ivgirts
 * PTR #1801730 utilities modified
 *
 * Revision 1.8  2006/04/27 10:25:57  zahars
 * PTR#0144983 parameters fixed
 *
 * Revision 1.7  2006/04/26 15:25:00  maksims
 * #0144986 UtilsHelper used to configure repository
 *
 * Revision 1.4  2006/04/25 13:13:59  ivgirts
 * PTR #1801730 parameters for jdbc connection added
 *
 * Revision 1.3  2006/04/25 12:06:28  maksims
 * #0144986 stopwords default initialization added for HSQL
 *
 * Revision 1.2  2006/04/20 11:42:57  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/18 13:40:44  maksims
 * #0144986 command tool to import hierarchy from file system
 *
 */