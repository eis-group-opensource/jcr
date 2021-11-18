/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.xml;

import java.io.InputStream;
import java.util.ArrayList;

import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.DatabaseCondition;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.security.SecurityPermission;

/**
 * TODO Put class description here
 * 
 */
public class SecurityImport extends DefaultHandler  implements SecurityXMLConstants {

    private NodeImpl currentNode = null;
    private SessionImpl session = null;
    private RepositoryImpl repository = null;
    private DatabaseConnection conn;
    private ArrayList<DatabaseStatement> statements = new ArrayList<DatabaseStatement>();

    
    public SecurityImport(SessionImpl session, RepositoryImpl repository) throws RepositoryException {
        this.session = session;
        this.repository = repository;
    }
    
    public void doImport(InputStream inputStream) throws RepositoryException {
        try {
            prepare();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            SAXResult result = new SAXResult(this);
            transformer.transform(new StreamSource(inputStream), result);            
            //if (repository != null){
            
            executeStatements();
                conn.close();
            //}
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

	private void executeStatements() throws RepositoryException {
		for(DatabaseStatement st:statements){
			st.execute(conn);
		}
		
		conn.commit();
		statements.clear();
	}

	private void prepare() throws RepositoryException {
		if (repository != null){
		    this.conn = repository.getConnectionProvider().createConnection(); 
		} else {
		    this.conn = session.getConnection();
		}
	}



    public void startElement (String uri, String name, String qName, Attributes atts) throws SAXException {
    	if (conn == null){
    		try {
				prepare();
			} catch (RepositoryException e) {
				throw new SAXException(e);
			}
    	}
        try {
            if (NODE_ELEMENT.equalsIgnoreCase(name)) {
                String currentNodePath = atts.getValue(NODE_PATH_ATTR);
                if (session == null){
                    session = (SessionImpl) repository.login(new SimpleCredentials(Constants.DEFAULT_ROOT_USER_NAME, Constants.DEFAULT_ROOT_PASSWORD.toCharArray()));
                }
                currentNode = (NodeImpl)session.getItem(currentNodePath);
                //String fromPath = atts.getValue(NODE_PATH_FROM_ATTR);
                
                DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(Constants.TABLE_NODE);
                DatabaseCondition c1 = Conditions.eq(Constants.FIELD_ID, currentNode.getNodeId());
                //DatabaseSelectAllStatement st1 = RepositorySecurityManager.createSelectAllStatement(currentNode.getNodeId());
                
                DatabaseSelectAllStatement st2 = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, false);
                    st2.addResultColumn(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);
                    st2.addJoin(Constants.TABLE_NODE_PARENT, "parents", Constants.FIELD_ID, Constants.FIELD_TYPE_ID );
                    st2.addCondition(Conditions.eq("parents."+Constants.TABLE_NODE_PARENT__PARENT_ID, currentNode.getNodeId()));
                    //st2.addCondition(Conditions.eq(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID, securityId));
                    st2.addGroup(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);                
                
                DatabaseCondition c2 = Conditions.in(Constants.FIELD_ID, st2);
                st.addCondition(Conditions.or(c1, c2));
                //if (fromPath != null){
                    //update security id for node
                 //   NodeImpl otherNode = (NodeImpl) session.getItem(fromPath);
                 //   currentNode.setSecurityId(otherNode.getNodeId());
                 //   st.addValue(SQLParameter.create(Constants.TABLE_NODE__SECURITY_ID, otherNode.getNodeId()));
                //} else {
                    currentNode.setSecurityId(currentNode.getNodeId());
                    st.addValue(SQLParameter.create(Constants.TABLE_NODE__SECURITY_ID, currentNode.getNodeId()));
                    st.addValue(SQLParameter.createSQL(Constants.TABLE_NODE__VERSION_, Constants.TABLE_NODE__VERSION_+"+1"));                    
                //}
                //st.execute(conn);
                statements.add(st);
                    
                //allow superuser access
                /*Long aceId = null;
                aceId = grant(atts, session.getUserID(), SecurityPermission.X_GRANT.getExportName(), SecurityPermission.X_GRANT, aceId, currentNode.getSecurityId());
                aceId = grant(atts, session.getUserID(), SecurityPermission.READ.getExportName(), SecurityPermission.READ, aceId, currentNode.getSecurityId());
                for(SecurityPermission p : SecurityPermission.values()){
                	aceId = grant(atts, session.getUserID(), p.getExportName(), p, aceId, currentNode.getSecurityId());
                }*/
                //conn.commit();
                
            } else if(USER_ELEMENT.equalsIgnoreCase(name) || GROUP_ELEMENT.equalsIgnoreCase(name)) {
                
                Long aceId = null;
                for(SecurityPermission p : SecurityPermission.values()){
                	aceId = grant(atts, name, p.getExportName(), p, aceId, currentNode.getSecurityId());
                }
                
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SAXException(e);
        }
        
        try {
			conn.commit();
		} catch (RepositoryException e) {
			throw new SAXException(e);
		}
        
    }  
    



    private Long grant(Attributes atts, String name, String attrName, SecurityPermission p, Long aceId, Long securityId) throws RepositoryException {
        String principal = atts.getValue(ID_ATTR);
        String value = atts.getValue(attrName);
        if (value != null){
            String userId = null;
            String groupId = null;
            if (USER_ELEMENT.equalsIgnoreCase(name)){
                userId = principal;
            } else {
                groupId = principal;
            }
            if (aceId == null){
                DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE, true);
                st.addCondition(Conditions.eq(Constants.TABLE_NODE__SECURITY_ID, securityId));
                if (userId != null){
                    st.addCondition(Conditions.eq(Constants.TABLE_ACE__USER_ID, userId));
                }
                if (groupId != null){
                    st.addCondition(Conditions.eq(Constants.TABLE_ACE__GROUP_ID, groupId));
                }

                st.execute(conn);
                if (st.hasNext()){
                    //read aceId
                    aceId = st.nextRow().getLong(Constants.FIELD_ID);
                } else {
                    //create ace 
                    aceId = conn.nextId();
                    DatabaseInsertStatement ist  = DatabaseTools.createInsertStatement(Constants.TABLE_ACE);
                    ist.addValue(SQLParameter.create(Constants.FIELD_ID, aceId));
                    ist.addValue(SQLParameter.create(Constants.TABLE_NODE__SECURITY_ID, securityId));
                    if (userId != null){
                        ist.addValue(SQLParameter.create(Constants.TABLE_ACE__USER_ID, userId));
                    }
                    if (groupId != null){
                        ist.addValue(SQLParameter.create(Constants.TABLE_ACE__GROUP_ID, groupId));
                    }
                    //ist.execute(conn);
                    //ist.close();
                    statements.add(ist);
                }
                st.close();
                //conn.commit();
            }
            
            //System.out.println(principal+" "+attrName +" "+value);
            DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(Constants.TABLE_ACE, Constants.FIELD_ID, aceId);
            st.addValue(SQLParameter.create(p.getColumnName(), Boolean.parseBoolean(value)));
            String parentPath = atts.getValue(attrName+"Parent");
            NodeImpl n = (NodeImpl)session.getItem(parentPath);
            Long parentNodeId = n.getNodeId();
            st.addValue(SQLParameter.create(p.getColumnName()+"_PARENT", parentNodeId));
            //st.execute(conn);
            //st.close();
            //conn.commit();
            statements.add(st);
            
        }
        
        return aceId;
        
    }

    /*private void grant(NodeImpl node, String principal, String name, SecurityPermission permission, String value) throws RepositoryException {
        
        if (session != null){*/
/*            if (USER_ELEMENT.equalsIgnoreCase(name)){
                session.getSecurityManager().setUserPermission(node,principal, permission, Boolean.valueOf(value));
            } else {
                session.getSecurityManager().setGroupPermission(node,principal, permission, Boolean.valueOf(value));
            }*/
/*            throw new UnsupportedOperationException("FIX ME");
        } else {
            String userId = null;
            String groupId = null;
            if (USER_ELEMENT.equalsIgnoreCase(name)){
                userId = principal;
            } else {
                groupId = principal;
            }
            //repository.getSecurityManager().setPermission(conn, userId, groupId, permission, node.getSecurityId(), Boolean.valueOf(value));
            throw new UnsupportedOperationException();
        }
    }
*/
    public void endElement (String uri, String name, String qName) throws SAXException{
    }

	@Override
	public void endDocument() throws SAXException {
        try {
        	executeStatements();
			//conn.commit();
		} catch (RepositoryException e) {
			throw new SAXException(e);
		}
		super.endDocument();
	}

	public void closeSession() {
		session.logout();
		try {
			conn.close();
		} catch (RepositoryException e) {
		}
	}    
    
}


/*
 * $Log: SecurityImport.java,v $
 * Revision 1.1  2007/04/26 09:00:44  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.7  2006/10/12 08:56:53  dparhomenko
 * PTR#0148476 fix exception text
 *
 * Revision 1.6  2006/09/26 12:31:45  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.5  2006/08/10 10:26:05  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.4  2006/07/06 09:29:40  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/06/02 07:21:44  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.2  2006/04/24 11:37:44  dparhomenko
 * PTR#0144983 build procedure
 *
 * Revision 1.1  2006/04/17 06:47:06  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.4  2006/04/13 10:03:50  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/12 12:49:02  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/10 11:30:17  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.1  2006/04/06 14:34:22  ivgirts
 * PTR #1800998 added Security export/import
 *
 */
