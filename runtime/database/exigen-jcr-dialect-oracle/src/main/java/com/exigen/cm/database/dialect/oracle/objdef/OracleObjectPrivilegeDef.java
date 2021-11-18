/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle.objdef;

import static com.exigen.cm.Constants.DBOBJ_ACTION_CREATE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_DELETE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_EXISTS;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.objdef.AbstractDBObjectDef;
import com.exigen.vf.commons.logging.LogUtils;

public class OracleObjectPrivilegeDef extends AbstractDBObjectDef {
	
	private String grantee;
	private String privilege;
	private String objectGranted;
	private Log log = LogFactory.getLog(OracleObjectPrivilegeDef.class);
	
	public List<String> getCreateSQL() {
		ArrayList<String> result = new ArrayList<String>();
		result.add(	"GRANT "+this.getPrivilege()+
					" ON "+this.getObjectGranted()+
					" TO "+this.getGrantee());
		return result;
	}
	
	public int getClassActions(){
		return DBOBJ_ACTION_CREATE+DBOBJ_ACTION_DELETE+DBOBJ_ACTION_EXISTS;
	}

	
	public OracleObjectPrivilegeDef(String obj, String user,String priv) throws RepositoryException{
		super(obj);
		this.setObjectGranted(obj);
		this.setGrantee(user);
		this.setPrivilege(priv);
	}
	
	public String getCheckExistsSQL(){
		return "SELECT 1 FROM user_tab_privs_made WHERE grantee='"+this.getGrantee()+
			"' and table_name='"+this.getObjectGranted()+"' AND privilege='"+
			this.getPrivilege()+"'";
	}
	
	public String getDeleteSQL(){
		return "REVOKE "+this.getPrivilege()+
			" ON "+this.getObjectGranted()+
			" FROM "+this.getGrantee();
	}

	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
		//conn.registerSysObject(DatabaseObject.PROCEDURE, this.getProcName());
	}

	
	public String getGrantee() {
		return grantee;
	}

	public void setGrantee(String grantee)  throws RepositoryException{
		this.grantee = formatName(grantee);
		if (this.grantee==null){
			String msg="Empty or invalid grantee name '"+grantee+"'";
			LogUtils.error(log, msg);
            throw new RepositoryException(msg);
		}    
	}

	public String getObjectGranted() {
		return objectGranted;
	}

	public void setObjectGranted(String objectGranted)  throws RepositoryException{
		this.objectGranted = formatName(objectGranted);
		if (this.objectGranted==null){
			String msg="Empty or invalid granted object name '"+objectGranted+"'";
			LogUtils.error(log, msg);
            throw new RepositoryException(msg);
		}    
	}

	public String getPrivilege() {
		return privilege;
	}

	public void setPrivilege(String privilege)  throws RepositoryException{
		this.privilege = formatNameUpper(privilege,"^(?i)(INSERT|UPDATE|DELETE|SELECT|EXECUTE)$"); 
		if (this.privilege==null){
			String msg="Empty or invalid privilege name '"+privilege+"'";
			LogUtils.error(log, msg);
            throw new RepositoryException(msg);
		}    
	}
	
	public String getDescription(){
		return "'"+this.getPrivilege()+ "' privilege on Oracle object '"+this.getObjectGranted()+
			"' for '"+this.getGrantee()+"'";
	}

	
}
