/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.xml;

/**
 * TODO Put class description here
 * 
 */
public interface SecurityXMLConstants {

    public static final String ROOT_ELEMENT = "jcr-security-export";
    
    public static final String NODE_ELEMENT = "node";
    public static final String NODE_PATH_ATTR = "path";
    //public static final String NODE_PATH_FROM_ATTR = "from";
    
    public static final String ACL_ELEMENT = "acl";    
    public static final String USER_ELEMENT = "user";
    public static final String GROUP_ELEMENT = "group";;
    
    public static final String ID_ATTR = "ID";
    //public static final String READ_ATTR = "read";
    //public static final String ADD_NODE_ATTR = "addNode";
    //public static final String SET_PROPERTY_ATTR = "setProperty";    
    //public static final String REMOVE_ATTR = "remove";
    //public static final String GRANT_ATTR = "grant";
}


/*
 * $Log: SecurityXMLConstants.java,v $
 * Revision 1.1  2007/04/26 09:00:44  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2007/02/22 09:24:46  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.3  2006/08/10 10:26:05  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.2  2006/06/02 07:21:44  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.1  2006/04/17 06:47:06  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/06 14:34:22  ivgirts
 * PTR #1800998 added Security export/import
 *
 */

