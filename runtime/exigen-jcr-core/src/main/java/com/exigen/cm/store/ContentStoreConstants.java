/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store;

import static com.exigen.cm.Constants.PROPERTY_DELIMITER;

/**
 * Holds Content Store related Constants.
 */
public class ContentStoreConstants {

 // Prefix for content store type definition in configuration
    public static final String TYPE_DEF_PREFIX = "storeTypeDef";
    
//  Prefix for content store definition in configuration
    public static final String STORE_DEF_PREFIX = "storeDef";    

    /**
     * Declares file store type name
     */
    public static final String STORE_TYPE_FILE = "FILE";
    
    /**
     * Declares DB store type name
     */
    public static final String STORE_TYPE_DB = "DB";

    /**
     * Declares centera store type name
     */
    public static final String STORE_TYPE_CENTERA = "CENTERA";

    /**
     * Declares remote store type name
     */
    public static final String STORE_TYPE_REMOTE = "REMOTE";    
    
    
    public static final String PROP_STORE_DEFAULT_MODE_LOCAL = "defaultStoreModeLocal";
    public static final String PROP_STORE_LOCAL_MODE = "localMode";
    public static final String PROP_STORE_REMOTE_PREFIX = "remote";
    public static final String PROP_STORE_LOCAL_PREFIX = "local";  
    public static final String PROP_STORE_TYPE = "type";
    public static final String PROP_STORE_LOCAL_TYPE  = PROP_STORE_LOCAL_PREFIX + "." + PROP_STORE_TYPE;     
    public static final String PROP_STORE_VALIDATION_ON = "validate";
    
    
    public static final String PROP_FILE_STORE_ROOT = "rootDir";
    /**
     * Name of property declaring size of buffer in bytes to be used by store to
     * write files.
     */
    public static final String PROP_FILE_STORE_BUFFER_SIZE="bufferSize";
    
    /**
     * Number of attempts store will perform to store file on a disk.
     */
    public static final String PROP_FILE_STORE_ATTEMPTS="attempts";

    
    /**
     * Name of property containing number of seconds should pass to item expiration.
     * Defaults to "never expired"
     */
    public static final String PROP_CACHE_EXPIRATION="expirationSeconds";

    /**
     * Name of property containing buffer size in bytes used to store content on disk.
     * Defaults to 1024
     */
    public static final String PROP_CACHE_BUFFER_SIZE="bufferSize";

    /**
     * Name of proprty containing path to cache root directory.
     * Defaults to returned by <code>System.getProperty("java.io.tmpdir")</code>.
     */
    public static final String PROP_CACHE_ROOT="rootDir";
    
    
    public static final String PROP_DB_STORE_MAY_CREATE_TABLE = "mayCreateIfNotExists";
    
    public static final String PROP_DB_STORE_TABLE = "table";    

    
    public static final String DEFAULT_STORE_NAME = "DEFAULT";

    public static final String DEFAULT_TEXT_STORE_NAME = "TEXT";

    public static final String PROPERTY_TEXT_STORE_NAME = "textStoreName";

    public static final String STORE_CONFIG_PREFIX = "store";

    
    public static final String PROPERTY_DEFAULT_STORE_TYPE = ContentStoreConstants.STORE_CONFIG_PREFIX+PROPERTY_DELIMITER+ContentStoreConstants.DEFAULT_STORE_NAME+PROPERTY_DELIMITER+PROP_STORE_TYPE;
    public static final String PROPERTY_TEXT_STORE_TYPE = ContentStoreConstants.STORE_CONFIG_PREFIX+PROPERTY_DELIMITER+ContentStoreConstants.DEFAULT_TEXT_STORE_NAME+PROPERTY_DELIMITER+PROP_STORE_TYPE;

    //* Used in UtilsHellper!
    public static final String PROPERTY_TEXT_STORE_TABLE = STORE_CONFIG_PREFIX
        +PROPERTY_DELIMITER + DEFAULT_TEXT_STORE_NAME
        +PROPERTY_DELIMITER + PROP_STORE_LOCAL_PREFIX 
        +PROPERTY_DELIMITER + PROP_DB_STORE_TABLE;
    
    public static final String PROPERTY_DEFAULT_STORE_TABLE = STORE_CONFIG_PREFIX
        +PROPERTY_DELIMITER + DEFAULT_STORE_NAME
        +PROPERTY_DELIMITER + PROP_STORE_LOCAL_PREFIX 
        +PROPERTY_DELIMITER + PROP_DB_STORE_TABLE;

    
    public static final String PROPERTY_TEXT_STORE_ROOTDIR = STORE_CONFIG_PREFIX
        +PROPERTY_DELIMITER + DEFAULT_TEXT_STORE_NAME
        +PROPERTY_DELIMITER + PROP_STORE_LOCAL_PREFIX 
        +PROPERTY_DELIMITER + PROP_FILE_STORE_ROOT;
    
    public static final String PROPERTY_DEFAULT_STORE_ROOTDIR = STORE_CONFIG_PREFIX
        +PROPERTY_DELIMITER + DEFAULT_STORE_NAME
        +PROPERTY_DELIMITER + PROP_STORE_LOCAL_PREFIX 
        +PROPERTY_DELIMITER + PROP_FILE_STORE_ROOT;    
    //*/
    
    
//  Centera Property names
    public static final String PROP_CENTERA_POOL_ADDRESS = "centeraPool.address";
//  Values provided within this property will be set as FPPool global options. Optional.
    public static final String PROP_CENTERA_POOL_OPTIONS = "centeraPool.options";
    
// Prefix for Connections Pool configuration
    public static final String PROP_CENTERA_CP_CONFIG = "connectionsPool.config";
    public static final String PROP_CENTERA_CP_CONFIG_MAX_WAIT_TIME = "maxWait";
    public static final String PROP_CENTERA_CP_CONFIG_MAX_ACTIVE_SIZE = "maxActiveSize";
    public static final String PROP_CENTERA_CP_CONFIG_MAX_SIZE = "maxSize";
    public static final String PROP_CENTERA_CP_CONFIG_MIN_SIZE = "minSize";
    public static final String PROP_CENTERA_CP_CONFIG_EVICT_EXEC_INTERVAL = "evictExecInterval";
    public static final String PROP_CENTERA_CP_CONFIG_IDLE_TIME_TILL_EVICTION = "idleTimeTillEviction";
    
//  Centera Run-time Property names. 
//  Can be passed within parameters Map along with stored content into 
//  CenteraContentStore.put(InputStream, int, Map<String,String>)
    
    /**
     * CClip name to be written. In case not provided default value
     * will be used.
     */
    public static final String PROP_CENTERA_RT_CLIP_NAME="clipName";
    
    /**
     * CClip Tag name to be used. If not provided default value
     * will be used.
     */
    public static final String PROP_CENTERA_RT_TAG_NAME="tagName";   
    public static final String PROP_CENTERA_RT_TAG_NAME_DEFAULT_VALUE = "jcrContentTag";
    

    /**
     * Prefix for Centera Description Attributes. 
     * Parameters can be passed in a form:
     *  descriptionAttribute.name1=value
     *  descriptionAttribute.name2=value ...
     *  
     * Parameters will be set as CClip.setDescriptionAttribute(name, value)
    */
    public static final String PROP_CENTERA_RT_DESCRIPTION = "descriptionAttribute"; 
    
   /**
    * This property value will be converted to long and passed to Centera AS IS. 
    * So check out Centera docs for proper values for retentionPeriod
    * Default value for this property is 0
    */
    public static final String PROP_CENTERA_RT_RETENTION="retentionPeriod";
    public static final String PROP_CENTERA_RETENTION = PROP_CENTERA_RT_RETENTION;    
    

}

/*
 * $Log: ContentStoreConstants.java,v $
 * Revision 1.3  2009/02/26 15:17:32  maksims
 * added support for property store.<store name>.validate=true|false
 *
 * Revision 1.2  2007/12/07 15:04:52  maksims
 * added capability to create content table if not existing opn initialization
 *
 * Revision 1.1  2007/04/26 08:59:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.10  2006/12/20 12:42:01  maksims
 * #1803657 added default CClip retentionTime support in store instance configuration
 *
 * Revision 1.9  2006/11/30 14:54:32  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.8  2006/11/10 16:29:18  maksims
 * #1801897 Centera pools pool added
 *
 * Revision 1.7  2006/11/09 15:44:23  maksims
 * #1801897 Centera Content Store added
 *
 * Revision 1.6  2006/08/21 11:20:12  maksims
 * #1801897 Default store mode local property added
 *
 * Revision 1.5  2006/08/14 16:18:36  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.4  2006/08/08 13:10:35  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.3  2006/07/28 15:49:07  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.2  2006/07/06 08:22:12  maksims
 * #1802356 Content Store configuration import added
 *
 * Revision 1.1  2006/07/04 14:01:33  maksims
 * #1802356 Content Store Constants moved to ContentStoreConstants
 *
 */