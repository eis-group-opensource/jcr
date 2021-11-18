/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.objdef;

import static com.exigen.cm.Constants.DBOBJ_ACTION_COMPILE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_CREATE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_DELETE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_EXISTS;
import static com.exigen.cm.Constants.DBOBJ_ACTION_STATUS;
import static com.exigen.cm.Constants.DBOBJ_POS_AFTER_ALL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.vf.commons.logging.LogUtils;

public abstract class AbstractDBObjectDef implements
		DBObjectDef {
	
	private Log log = LogFactory.getLog(AbstractDBObjectDef.class);
	private int   positionInObjectList=DBOBJ_POS_AFTER_ALL;
	private String linkedObject=null;
	private String connectURL=null;
	private String connectUser=null;
	private String connectPass=null;
	protected int supportedActions=0;
	private String name;
	
	public AbstractDBObjectDef(String name){
		this.supportedActions=this.getClassActions();
		this.name = name;
	}
	
	public int getClassActions(){
		return 0;
	}
	
	public final void create(DatabaseConnection conn) throws RepositoryException{
		if (! this.isActionAvailable(DBOBJ_ACTION_CREATE)){
			String msg="DB object("+this.getDescription()+") does not support create action";
			LogUtils.error(log,msg);
			throw new RepositoryException(msg);
		}	
		List<String> sql=getCreateSQL();
		if (sql==null || sql.size()==0){
			String msg="DB object("+this.getDescription()+
				") existence check SQL statement list is null or empty";
			LogUtils.error(log,msg);
			throw new RepositoryException(msg);
		}	
		for (String s:sql){
			if (s==null || s.length()==0){
				String msg="DB object("+this.getDescription()+
					") create SQL statement is null or empty";
				LogUtils.error(log,msg);
				throw new RepositoryException(msg);
			}	
		}
		if (this.connectURL==null){
			for( String s:sql) { // SQL logged in DatabaseConnection
				register(conn, false);
				conn.execute(s);
			}	
		}else{
			register(conn, true);
			Connection tmpConn;
	    	try {
	    		tmpConn = DriverManager.getConnection(this.connectURL,this.connectUser,this.connectPass);
	    		Statement st=tmpConn.createStatement();
				for( String s:sql){
					LogUtils.debug(log,"SQL (URL="+this.connectURL+" USER="+this.connectUser+"): "+s);
					st.execute(s);
				}	
				st.close();
	    		tmpConn.close();
	    	} catch(SQLException e){
	    		String msg=e.getMessage();
	    		LogUtils.error(log,msg);
				throw new RepositoryException(msg);
	    	}
		}
	}
	
	abstract protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException;

	public boolean checkExists(DatabaseConnection conn) throws RepositoryException{
		if (! this.isActionAvailable(DBOBJ_ACTION_EXISTS)){
			String msg="DB object("+this.getDescription()+") does not support existence check";
			LogUtils.error(log,msg);
			throw new RepositoryException(msg);
		}	
		boolean exists=false;
		String sql=this.getCheckExistsSQL();
		if (sql==null || sql.length()==0){
			String msg="DB object("+this.getDescription()+") existence check SQL is null or empty";
			LogUtils.error(log,msg);
			throw new RepositoryException(msg);
		}	
/*		if (this.connectURL==null){
			try{
				LogUtils.debug(log,"Check Exists SQL : "+sql);
				PreparedStatement st=conn.prepareStatement(sql,false);
				ResultSet rs=st.executeQuery();
				if (rs.next())
					exists=true;
				rs.close();
				st.close();
			}catch(SQLException e){
				String msg=e.getMessage();
				LogUtils.error(log,msg);
				throw new RepositoryException(msg);
			}
		}else{
	    	Connection tmpConn;
	    	try {
	    		tmpConn = DriverManager.getConnection(this.connectURL,this.connectUser,this.connectPass);
	    		Statement st=tmpConn.createStatement();
				LogUtils.debug(log,"Check Exists SQL (URL="+this.connectURL+" USER="+this.connectUser+"): "+sql);
				ResultSet rs=st.executeQuery(sql);
				if (rs.next())
					exists=true;
				rs.close();
				st.close();
	    		tmpConn.close();
	    	} catch(SQLException e){
	    		String msg=e.getMessage();
	    		LogUtils.error(log,msg);
				throw new RepositoryException(msg);
	    	}
		}*/
		
    	DatabaseConnection tmpConn;
    	try {
    		if (this.connectUser != null){
    			if (this.connectPass == null){
    				log.info("Skip checking for "+getName());
    				return true;
    			}
    			tmpConn = conn.getConnectionProvider().createConnection(this.connectUser, this.connectPass);
    		} else {
    			tmpConn = conn;
    		}
    		Statement st=tmpConn.createStatement();
			LogUtils.debug(log,"Check Exists SQL (URL="+this.connectURL+" USER="+this.connectUser+"): "+sql);
			ResultSet rs=st.executeQuery(sql);
			if (rs.next())
				exists=true;
			rs.close();
			st.close();
    		if (this.connectUser != null){
    			tmpConn.close();
    		}
    	} catch(SQLException e){
    		String msg=e.getMessage();
    		LogUtils.error(log,msg);
			throw new RepositoryException(msg);
    	}
		
		return exists;
	}
	
	protected String getName() {
		return name;
	}

	public boolean checkStatus(DatabaseConnection conn) throws RepositoryException{
		if (! this.isActionAvailable(DBOBJ_ACTION_STATUS)){
			String msg="DB object("+this.getDescription()+") does not support status check";
			LogUtils.error(log,msg);
			throw new RepositoryException(msg);
		}	
		boolean valid=false;
		String sql=this.getCheckStatusSQL();
		if (sql==null || sql.length()==0){
			String msg="DB object("+this.getDescription()+") status check SQL is null or empty";
			LogUtils.error(log,msg);
			throw new RepositoryException(msg);
		}	
		if (this.connectURL==null){
			try{
				LogUtils.debug(log,"Check Status SQL : "+sql);
				PreparedStatement st=conn.prepareStatement(sql,false);
				ResultSet rs=st.executeQuery();
				if (rs.next() && rs.getString(1).equalsIgnoreCase("VALID"))
					valid=true;
				rs.close();
				st.close();
			}catch(SQLException e){
				String msg=e.getMessage();
				LogUtils.error(log,msg);
				throw new RepositoryException(msg);
			}
		}else{
	    	Connection tmpConn;
	    	try {
	    		if (this.connectPass == null){
	    			log.info("Skip checking for "+getName());
	    			return true;
	    		}
	    		tmpConn = DriverManager.getConnection(this.connectURL,this.connectUser,this.connectPass);
	    		Statement st=tmpConn.createStatement();
				LogUtils.debug(log,"Check Status SQL [URL="+this.connectURL+" USER="+this.connectUser+"]: "+sql);
				ResultSet rs=st.executeQuery(sql);
				if (rs.next() && rs.getString(1).equalsIgnoreCase("VALID"))
					valid=true;
				rs.close();
				st.close();
	    		tmpConn.close();
	    	} catch(SQLException e){
	    		String msg=e.getMessage();
	    		LogUtils.error(log,msg);
				throw new RepositoryException(msg);
	    	}
		}
		return valid;
	}
	
	public void compile(DatabaseConnection conn) throws RepositoryException{
		if (! this.isActionAvailable(DBOBJ_ACTION_COMPILE)){
			String msg="DB object("+this.getDescription()+") does not support recompilation";
			LogUtils.error(log,msg);
			throw new RepositoryException(msg);
		}	
		String sql=this.getCompileSQL();
		if (sql==null || sql.length()==0){
			String msg="DB object("+this.getDescription()+") compile SQL is null or empty";
			LogUtils.error(log,msg);
			throw new RepositoryException(msg);
		}	
		if (this.connectURL==null){
			LogUtils.debug(log,"Compile SQL : "+sql);
			try{
				conn.execute(sql);
			}catch(RepositoryException e){
				LogUtils.info(log,"Compilation error: "+this.getDescription()+". "+e.getMessage());
			}
		}else{
	    	Connection tmpConn;
	    	try {
	    		tmpConn = DriverManager.getConnection(this.connectURL,this.connectUser,this.connectPass);
	    		Statement st=tmpConn.createStatement();
				LogUtils.debug(log,"Compile SQL [URL="+this.connectURL+" USER="+this.connectUser+"]: "+sql);
				st.execute(sql);
				st.close();
	    		tmpConn.close();
	    	} catch(SQLException e){
	    		LogUtils.info(log,"Compilation error: "+this.getDescription()+". "+e.getMessage());
	    	}
		}
	}

	public void delete(DatabaseConnection conn) throws RepositoryException{
		if (! this.isActionAvailable(DBOBJ_ACTION_DELETE)){
			String msg="DB object("+this.getDescription()+") does not support delete action";
			LogUtils.error(log,msg);
			throw new RepositoryException(msg);
		}	
		String sql=this.getDeleteSQL();
		if (sql==null || sql.length()==0){
			String msg="DB object("+this.getDescription()+") delete SQL is null or empty";
			LogUtils.error(log,msg);
			throw new RepositoryException(msg);
		}	
		if (this.connectURL==null){
			LogUtils.debug(log,"Delete SQL : "+sql);
			try{
				conn.execute(sql);
			}catch(RepositoryException e){
				LogUtils.info(log,"Delete error: "+this.getDescription()+". "+e.getMessage());
			}
		}else{
	    	Connection tmpConn;
	    	try {
	    		tmpConn = DriverManager.getConnection(this.connectURL,this.connectUser,this.connectPass);
	    		Statement st=tmpConn.createStatement();
				LogUtils.debug(log,"Delete SQL [URL="+this.connectURL+" USER="+this.connectUser+"]: "+sql);
				st.execute(sql);
				st.close();
	    		tmpConn.close();
	    	} catch(SQLException e){
	    		LogUtils.info(log,"Delete error: "+this.getDescription()+". "+e.getMessage());
	    	}
		}
	}
	
	public void setPositionInObjectList(int where,String obj)  throws RepositoryException{
		this.setPositionInObjectList(where);
		this.setLinkedObjectName(obj);
	}

	public void setPositionInObjectList(int where) {
		this.positionInObjectList=where;
	}

	public int getPositionInObjectList() {
		return this.positionInObjectList;
	}

	public void setLinkedObjectName(String o) throws RepositoryException{
		this.linkedObject=formatName(o);
		if (this.linkedObject==null){
			String msg="Invalid or empty linked DB object name '"+o+"'";
			LogUtils.error(log,msg);
			throw new RepositoryException(msg);
		}	
	}

	public String getLinkedObjectName() {
		return this.linkedObject;
	}
	
	public boolean isActionAvailable(int action){
		return((this.supportedActions & action) == action);
	}
	
	public void enableAction(int action) throws RepositoryException{
		if ((action & this.getClassActions()) == action) // is action at the class level allowed ? 
			this.supportedActions|=action;
		else{
			String msg="Cannot enable action ("+action+") for object - " +
				"not enabled at class level";
			LogUtils.error(log,msg);
			throw new RepositoryException(msg);
		}			
	}
	
	public void disableAction(int action){
		if ((action & this.supportedActions)==action)
			this.supportedActions^=action;
	}
	
	public String getCheckExistsSQL() throws RepositoryException{
		String msg="The method getCheckExistsSQL() is not defined for "+this.getDescription();
		LogUtils.error(log,msg);
		throw new RepositoryException(msg);
	}
	
	public String getCheckStatusSQL() throws RepositoryException{
		String msg="The method getCheckStatusSQL() is not defined for "+this.getDescription();
		LogUtils.error(log,msg);
		throw new RepositoryException(msg);
	}
	
	public String getDeleteSQL() throws RepositoryException{
		String msg="The method getDeleteSQL() is not defined for "+this.getDescription();
		LogUtils.error(log,msg);
		throw new RepositoryException(msg);
	}
	
	public String getCompileSQL() throws RepositoryException{
		String msg="The method getCompileSQL() is not defined for "+this.getDescription();
		LogUtils.error(log,msg);
		throw new RepositoryException(msg);
	}
	
	public void setConnectionParameters(String url,String user,String pwd){
		this.connectURL=url;
		this.connectUser=user;
		this.connectPass=pwd;
	}
	
	protected String formatName(String name,String regexpr){
		String x=name.trim();
		if (x==null || x.length()==0)
			return null;
		Pattern p;
		try{
			p=Pattern.compile(regexpr);
		}catch(PatternSyntaxException e){
			LogUtils.error(log,"RegExp compile error: "+e.getMessage());
			return null;
		}	
		Matcher m=p.matcher(x);
		if (!m.find())
			x=null;
		return x;
	}
	
	protected String formatNameUpper(String name,String regexpr){
		String x=formatName(name,regexpr);
		if (x!=null)
			return x.toUpperCase();
		return null;
	}
	
	protected String formatName(String name,int maxLength, boolean forceUpperCase){
		if (name==null || name.length()==0)
			return null;
		String x=(forceUpperCase ? name.toUpperCase() : name);
		String regexp="^[A-Z"+(forceUpperCase ? "": "a-z")+"]"+
			"[A-Z"+(forceUpperCase ? "" : "a-z")+"0-9\\_]{0,"+(maxLength-1)+"}$";
		return formatName(x,regexp);
	}
	
	protected String formatName(String name,int maxLength){
		return formatName(name,maxLength,true);
	}

	protected String formatName(String name){
		return formatName(name,30,true);
	}

}
