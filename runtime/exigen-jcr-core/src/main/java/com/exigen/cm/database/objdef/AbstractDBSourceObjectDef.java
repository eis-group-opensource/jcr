/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.objdef;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.impl.StreamUtils;
import com.exigen.vf.commons.logging.LogUtils;

public abstract class AbstractDBSourceObjectDef extends
		AbstractDBObjectDef {
	
	public AbstractDBSourceObjectDef(String name) {
		super(name);
	}

	protected String createSQL=null;
	private HashMap substMap=null;
	private Log log = LogFactory.getLog(AbstractDBObjectDef.class);

	public void setCreateStatement(String sql) throws RepositoryException{
		if (sql==null || sql.length()==0){
			String msg="Null or empty SQL source";
			LogUtils.error(log, msg);
            throw new RepositoryException(msg);
		}   
		this.createSQL=sql;
	}
	
	public void setCreateStatement(InputStream in) throws RepositoryException {
		if (in!=null)
			try{
				this.setCreateStatement(new String(StreamUtils.getBytes(in)));
			}catch (IOException e){
				String msg="IO error setting create SQL: "+e.getMessage();
				LogUtils.error(log, msg);
                throw new RepositoryException(msg);
			}
		else{
			String msg="Null InputStream for SQL source, please check you build, some reasources are missed";
			LogUtils.error(log, msg);
            throw new RepositoryException(msg);
		}    
	}
	
	public void setSubstitutionMap(HashMap subst){
		this.substMap=subst;
	}
	
	public HashMap<String,String> getSubstitutionMap(){
		HashMap<String,String> newMap=new HashMap<String,String>();
		if (this.substMap != null) {
			Object[] keys = this.substMap.keySet().toArray();
			for (int i = 0; i < keys.length; i++)
				newMap.put((String)keys[i],(String)this.substMap.get(keys[i]));
		}
		return newMap;
	}
	
	protected String substitute(String in){
		String out=in;
		if (this.substMap != null && out != null ) {
			Object[] keys = this.substMap.keySet().toArray();
			for (int i = 0; i < keys.length; i++)
				out=out.replace((CharSequence)("%%" + (String) keys[i] + "%%"),
						(CharSequence)this.substMap.get(keys[i]));
//				out = out.replaceAll("%%" + (String) keys[i] + "%%",
//						(String) this.substMap.get(keys[i]));
		}
		return out;
	}
	
	public List<String> getCreateSQL() {
		ArrayList<String> result = new ArrayList<String>();
		if (this.createSQL != null)
				result.add(this.substitute(this.createSQL));
		return result;
	}
	
	public List<String> getCreateSQLWithoutSubst() {
		ArrayList<String> result = new ArrayList<String>();
		if (this.createSQL != null)
				result.add(this.createSQL);
		return result;
	}

}
