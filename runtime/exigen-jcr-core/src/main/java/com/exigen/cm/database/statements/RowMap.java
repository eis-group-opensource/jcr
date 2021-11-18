/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;

public class RowMap extends HashMap{

    public Long getLong(String field) {
        return (Long) get(field);
    }

    public String getString(String field) {
        return (String) get(field);
    }

    public Boolean getBoolean(String field) {
        return (Boolean) get(field);
    }
    
    public InputStream getStream(String field){
        return (InputStream)get(field);
    }

	public Calendar getDate(String field) {
		return (Calendar)get(field);
	}
}


/*
 * $Log: RowMap.java,v $
 * Revision 1.2  2008/04/29 10:56:00  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/08/04 12:33:35  maksims
 * #1802426 SQL Framework used to generate queries in DBContentStore
 *
 * Revision 1.1  2006/04/17 06:47:12  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/03/14 11:55:38  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.1  2006/03/03 10:33:19  dparhomenko
 * PTR#0144983 versioning support
 *
 */