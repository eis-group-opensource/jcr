/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle;

import java.util.ArrayList;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.vf.commons.logging.LogUtils;


/**
 * implementaion of abstract <code>DropSQLObjectsBase</code> class customized
 * for Oracle10
 * 
 */
public class DropOracle10SQLObjects extends DropOracleSQLObjects {

	private Log log = LogFactory.getLog(DropOracle10SQLObjects.class);
	
    public DropOracle10SQLObjects() {
        super();
    }
 
    public void checkSchemaIsEmpty() throws RepositoryException {
		StringBuffer sb = new StringBuffer();
		// alternative (but not safe) - execute PURGE RECYCLEBIN
        sb.append("SELECT object_name||' ('||object_type||')' AS item FROM user_objects "+
        		" WHERE object_name NOT IN (SELECT object_name FROM user_recyclebin)");
    	ArrayList<String> data=getSQLObjects(sb.toString(), "ITEM", null);
		for (String str:data)
			LogUtils.error(log,"After cleaning remains in DB: "+str);
		if (data.size()>0){
			String msg="Num of objects in DB schema after cleanup: "+data.size();
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
     }
    
}
