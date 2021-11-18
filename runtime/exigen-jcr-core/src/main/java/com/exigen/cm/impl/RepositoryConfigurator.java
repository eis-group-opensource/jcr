/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.Map;

import javax.jcr.RepositoryException;

public interface RepositoryConfigurator {

    void configure(RepositoryImpl repository, Map<String, String> config) throws RepositoryException;

}


/*
 * $Log: RepositoryConfigurator.java,v $
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/07/12 11:51:05  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/06/27 11:51:05  dparhomenko
 * PTR#1802276 implement exigen autocreated properties
 *
 */