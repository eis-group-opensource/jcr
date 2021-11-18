/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle.objdef;

import static com.exigen.cm.Constants.DBOBJ_ACTION_COMPILE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_CREATE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_DELETE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_EXISTS;
import static com.exigen.cm.Constants.DBOBJ_ACTION_STATUS;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.objdef.AbstractDBSourceObjectDef;
import com.exigen.vf.commons.logging.LogUtils;


public abstract class OracleSourceObjectDef extends AbstractDBSourceObjectDef {
	
	public OracleSourceObjectDef(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	private Log log = LogFactory.getLog(OracleSourceObjectDef.class);
	
	public int getClassActions(){
		return DBOBJ_ACTION_CREATE+DBOBJ_ACTION_DELETE+DBOBJ_ACTION_STATUS+
			DBOBJ_ACTION_EXISTS+DBOBJ_ACTION_COMPILE;
	}
	
	public List<String> getCreateSQL() {
		ArrayList<String> result = new ArrayList<String>();
	    try {
	    	if (this.createSQL != null){
	    		String str = this.createSQL.replace('\r',' ').replace('\n',' ');
	    		StringTokenizer st = new StringTokenizer(str, "/");
	    		while (st.hasMoreTokens())
	    			result.add(this.substitute(st.nextToken()));
	    	}
	    } catch (Exception e){
	    	String msg="Error splitting Oracle SQL source in statement list: "+e.getMessage();
	    	LogUtils.error(log, msg);
	    }
	    return result;
	}

}
