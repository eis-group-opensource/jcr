/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.cmd.fts.extract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.cmd.fts.TextExtractor;
import com.exigen.cm.store.StoreHelper;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.bridge.UnoUrlResolver;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * Asbtract class for OpenOfiice based text extractors.
 * To enable OpenOffice extraction functionality OO instance must be started
 * with the following command:
 * <OO_HOME>/staroffice/soffice "-accept=socket,host=localhost,port=8100;urp;StarOffice.ServiceManager"
 * 
 * @author Maksims
 *
 */
public abstract class OODocumentTextExtractor implements TextExtractor {
    
    private Log log = LogFactory.getLog(OODocumentTextExtractor.class);
    

    /**
     * OpenOffice UNO PORT property name
     */
    public static final String PNAME_OO_PORT = "oo.port";

    /**
     * OpenOffice UNO HOST property name
     */
    public static final String PNAME_OO_HOST = "oo.host";    
    
    
    public static final String PNAME_TMP_DIR = "oo.tmpDir";    
    
    
    public static final String OO_DEFAULT_PORT = "8100";
    public static final String OO_DEFAULT_HOST = "localhost";
    
//  default OO URL for OO started as: 
//  <OO_HOME>/program/soffice "-accept=socket,host=localhost,port=8100;urp;StarOffice.ServiceManager"
    private static final String DEFAULT_CONNECTION_STRING = "uno:socket,host="+OO_DEFAULT_HOST+",port="+OO_DEFAULT_PORT+";urp;StarOffice.ServiceManager";;

    
//    private String ooPort = OO_DEFAULT_PORT;
//    private String ooHost = OO_DEFAULT_HOST;    
    private String ooConnectionString = DEFAULT_CONNECTION_STRING;
    
    /**
     * Object used to load document. May become invalid in case network connection
     * failed. In this case refrech might help.
     */
    private XComponentLoader componentLoader;
    
//  Name of Tmp dir used when performing text extraction
//    private String tempDirPath = "ootextextr";
//    private File tempDir;
    
    
    protected OODocumentTextExtractor(){
        this(null);
    }
    
    protected OODocumentTextExtractor(Map properties){
        if(properties != null){
            String ooPort = (String)properties.get(PNAME_OO_PORT);
            ooPort = ooPort == null ? OO_DEFAULT_PORT : ooPort;
            
            String ooHost = (String)properties.get(PNAME_OO_HOST);            
            ooHost = ooHost == null ? OO_DEFAULT_HOST : ooHost;
            ooConnectionString = "uno:socket,host="+ooHost+",port="+ooPort+";urp;StarOffice.ServiceManager";
            
            /*
//          It looks that OO dislike URLs with spaces ...
//          encoding doesn't help ...
            tempDirPath = properties.getProperty(PNAME_TMP_DIR, tempDirPath);
            File tf = new File(tempDirPath);
            if(!tf.exists())
                tf.mkdir();
            else
                if(!tf.isDirectory())
                    throw new RuntimeException("Wrong name provided for temp dir: " + tf.getAbsolutePath());
            tempDir = tf;//*/
        }
        
        log.debug("OpenOffice connection string is " + ooConnectionString);
    }
    
    
    /**
     * Returns text from document.
     * @param document which text must be accessed.
     * @return document's text as string.
     * @throws Exception
     */
    protected abstract String getText(XComponent document) throws Exception;
    
    /**
     * It appears that OO despite declared ability 
     * execute whole bunch of TypeDetectors
     * relies on file extension to get file type infor ...
     * @return
     */
    protected abstract String getFileExtension();
    
    /**
     * @inheritDoc
     */
    public void extract(String mimeType, InputStream source, Writer target ) throws Exception{
        
        boolean isSourceClosed = false;
        try {
            if(componentLoader == null){
                log.debug("Accessing OpenOffice instance for the first time.");
                componentLoader = refreshComponentLoader();
                log.info("OpenOffice instance accessed at " + ooConnectionString);            
            }

        
            File tmpFile = File.createTempFile("ftsoo", getFileExtension());      
        
            OutputStream tmpOs = new FileOutputStream(tmpFile);
            StoreHelper.transfer(source, tmpOs, -1);
            isSourceClosed = true;
            String fileURL= "file:///" + tmpFile.getCanonicalPath();//"file:///"; // place to take file from 

            XComponent document = null;
            try{
                document = getComponent(fileURL, componentLoader);
            }catch(Exception ex){
                log.warn("Failed to re-use existing OO document loader. Will try to refresh ...", ex);
                componentLoader = refreshComponentLoader();
                document = getComponent(fileURL, componentLoader);
            }
            finally{
//          Clean up tmp file
                if(!tmpFile.delete())
                    tmpFile.deleteOnExit();
            }
        
                
            if(document == null)
                throw new Exception("Failed to get text using OpenOffice from " + ooConnectionString);
            
            String text = getText(document);
        
            if(text == null)
                throw new Exception("Failed to get text from document");

            document.dispose(); // dispose document
        
            target.write(text);
        }
        finally {
            target.close();
            if (!isSourceClosed)
                source.close();
        }

    }    

    /**
     * Returns document loaded from temporary URL by provided component loader.
     * @param url
     * @param loader
     * @return
     * @throws Exception
     */
    protected XComponent getComponent(String url, XComponentLoader loader) throws Exception{
        // Loading the wanted document
        PropertyValue propertyValues[] = new PropertyValue[]{
                    new com.sun.star.beans.PropertyValue()};

        propertyValues[0].Name = "Hidden";
        propertyValues[0].Value = new Boolean(true);
        
//        url = URLEncoder.encode(url, "ASCII");
        XComponent component = loader.loadComponentFromURL(url, "_blank", 0, propertyValues);
        return component;
    }
    
    
    
    
    /**
     * Returns Component Loader for documents (Document, Spreadsheet or anything loadable from URL) 
     * @return OO component loader.
     * @throws Exception
     */
    protected XComponentLoader refreshComponentLoader() throws Exception{
        
        log.info("Trying to reach OpenOffice instance at " + ooConnectionString);
        
        try{
    //      creates initial (local) component filter
            XComponentContext localContext = Bootstrap.createInitialComponentContext(null);
            
            
            XUnoUrlResolver connector = UnoUrlResolver.create(localContext);
            
    //      Get remote service manager
            Object serviceManagerRef = connector.resolve(ooConnectionString);
    //      Cast it to component factory
            XMultiComponentFactory  ooComponentFactory = (XMultiComponentFactory)UnoRuntime.queryInterface(XMultiComponentFactory.class, serviceManagerRef);
    
            
    //      Factory properties accessible via returned xPropertySet
            XPropertySet xProperySet = (XPropertySet) UnoRuntime.queryInterface(  
                    XPropertySet.class, ooComponentFactory);
    
            
    //      Get the default filter ref for service manager (ooComponentFactory). 
            Object remoteContextRef = xProperySet.getPropertyValue("DefaultContext");        
    //      Cast it to component filter 
            XComponentContext remoteContext 
                             = (XComponentContext) UnoRuntime.queryInterface( 
                                                                 XComponentContext.class, 
                                                                 remoteContextRef);        
            
    //      Get Desktop service reference        
            Object ooDesktopRef
                = ooComponentFactory.createInstanceWithContext("com.sun.star.frame.Desktop", 
                        remoteContext);
    
    //      Cast it to component loader        
            XComponentLoader xCompLoader = 
                    (XComponentLoader) UnoRuntime.queryInterface(XComponentLoader.class, ooDesktopRef);        
    
            log.info("Reference to OpenOffice instance successfully obtained");
            return xCompLoader;

        }catch(Exception ex){
            String message = MessageFormat.format("Failed to obtain reference to OpenOffice instance at {0}. Be sure URL is correct and OpenOffice instance is running in server mode e.g. with {1}."
                    , new Object[]{ooConnectionString,
                            "<path to OpenOffice>/soffice.exe \"-accept=socket,host=localhost,port=8100;urp;StarOffice.ServiceManager\""});
            log.error(message, ex);
            throw new Exception(message, ex);
        }

    }    
    

    /**
     * Helper method. Print out services supported by target
     * @param target
     */
    protected void printSupportedServices(Object target){
        XServiceInfo serviceInfo = (XServiceInfo)UnoRuntime.queryInterface(XServiceInfo.class, target);
        StringBuffer sinfo = new StringBuffer("Services supported by ");
        sinfo.append(serviceInfo.getImplementationName()).append(": ");
        
        if(serviceInfo != null){
            String[] services = serviceInfo.getSupportedServiceNames();
            
            for(int u=0; u<services.length; u++){
                if(u>0) sinfo.append(',');
                sinfo.append(services[u]);
            }
        }else        
            sinfo.append("NONE");

        
        log.debug(sinfo);
    }
}
/*
 * $$Log: OODocumentTextExtractor.java,v $
 * $Revision 1.1  2007/04/26 08:58:50  dparhomenko
 * $PTR#1804279 migrate JCR to maven from B302 directory
 * $
 * $Revision 1.9  2006/07/06 09:33:17  dparhomenko
 * $PTR#1802310 Add new features to DatabaseConnection
 * $
 * $Revision 1.8  2006/06/27 11:48:58  zahars
 * $PTR#0144986 Extractor interface changed
 * $
 * $Revision 1.7  2006/06/27 08:48:53  maksims
 * $#1801897 File header/footer added
 * $$
 */