/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.objdef;

import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;

public interface DBObjectDef {

	public List<String> getCreateSQL()throws RepositoryException;
	public String getCheckExistsSQL() throws RepositoryException;
	public String getCheckStatusSQL() throws RepositoryException;
	public String getDeleteSQL() throws RepositoryException;
	public String getCompileSQL() throws RepositoryException;

	public void    create(DatabaseConnection conn)      throws RepositoryException;
	public boolean checkExists(DatabaseConnection conn) throws RepositoryException;
	public boolean checkStatus(DatabaseConnection conn) throws RepositoryException;
	public void    compile(DatabaseConnection conn)     throws RepositoryException;
	public void    delete(DatabaseConnection conn)      throws RepositoryException;
	
	public void setPositionInObjectList(int where);
	public int getPositionInObjectList();
	public void setLinkedObjectName(String o)  throws RepositoryException;
	public String getLinkedObjectName();
	public void setPositionInObjectList(int where,String obj) throws RepositoryException;

	public int getClassActions();
	public boolean isActionAvailable(int action);
	public void enableAction(int action) throws RepositoryException;
	public void disableAction(int action);
	
	public void setConnectionParameters(String url,String user,String pwd);
	
	public String getDescription();
	
}
