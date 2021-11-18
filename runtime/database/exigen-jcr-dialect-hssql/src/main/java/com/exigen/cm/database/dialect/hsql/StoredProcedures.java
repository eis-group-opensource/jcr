/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.hsql;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.ResultSetMetadata;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.impl.security.JCRSecurityHelper;
import com.exigen.cm.impl.security.SecurityPermission;
import com.exigen.cm.security.JCRPrincipals;
import com.exigen.cm.store.StoreHelper;
import com.exigen.cm.store.StoreHelper.FileBackedOutputStream;

/**
 * Contains stored procedures for HSQL
 * 
 */
public class StoredProcedures {
    private static final Log log = LogFactory.getLog(StoredProcedures.class);

    public static Boolean checkPermissionRead(Connection c1, Long nodeId, Long securityId, String userId,
            String groupList, String contextList, Boolean validateBrowse) throws SQLException, RepositoryException {
        if (securityId == null){
            return true;
        }
        //parse groups
        ArrayList<String> groups = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(groupList, ",");
        while (st.hasMoreElements()) {
            String group = st.nextToken().trim();
            if (group.length() > 0) {
                groups.add(group);
            }
        }
        
        //parse contexts
        ArrayList<String> contexts = new ArrayList<String>();
        st = new StringTokenizer(contextList, ",");
        while (st.hasMoreElements()) {
            String context = st.nextToken().trim();
            if (context.length() > 0) {
                contexts.add(context);
            }
        }
        
        StringBuffer sql = new StringBuffer("select this_.* from CM_ACE this_ where  SECURITY_ID = ?  AND  ( USER_ID = ? ");
        for (String group : groups) {
            sql.append(" OR GROUP_ID = ? ");
        }
        sql.append(") AND ( CONTEXT_ID is null" );
        for (String context : contexts) {
            sql.append(" OR CONTEXT_ID = ? ");
        }
        
        sql.append(")");
        

        PreparedStatement stmt = c1.prepareStatement(sql.toString());
        ResultSet rs = null;
        try {
            stmt.setLong(1, securityId);
            stmt.setString(2, userId);
            int pos = 3;
            for (String group : groups) {
                stmt.setString(pos++, group);
            }
            for (String context : contexts) {
                stmt.setString(pos++, context);
            }
            stmt.execute();
            rs = stmt.getResultSet();
            
            HyperSonicSQLDatabaseDialect dialect = new HyperSonicSQLDatabaseDialect();
            ArrayList<RowMap> rows = new ArrayList<RowMap>();
   			ResultSetMetadata md = new ResultSetMetadata(rs, true, new HashMap<String, String>(), false);
   			while (rs.next()){
                RowMap row = DatabaseTools.assembleRow(rs, dialect, md);
                rows.add(row);
            }
            
   			JCRPrincipals  principals = new JCRPrincipals(userId, groups, contexts, false);
   			
            Boolean result = JCRSecurityHelper.validateSecurityPermission(nodeId, rows, principals, SecurityPermission.READ);
            if (result != null && result == true){
                return true;
            } else if (validateBrowse){
                result = JCRSecurityHelper.validateSecurityPermission(nodeId, rows, principals, SecurityPermission.BROWSE);
            }
            if (result == null){
                rs.close();
                stmt.close();
                stmt = c1.prepareStatement("select this_.* from CM_ACE this_ where  SECURITY_ID = ?");
                stmt.setLong(1, securityId);
                stmt.execute();
                rs = stmt.getResultSet();
                if (rs.next()){
                    return false;
                }
            } else if (!result){
                return false;
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            stmt.close();
        }

        return true;
    }
    
    /**
     * Stored procedure that moves text from FTS_STAGE to FTS_DA
     * this procedure does not ZIP the text (it is called zip for similarity
     * with oracle and ms sql
     * @param conn 
     * @param id - FTS_STAGE id (equals to FTS_DATA id)
     * @return 0 if OK, error code otherwise
     * @throws SQLException 
     */
    public static Integer ZIP_AND_MOVE(Connection conn, Long id) throws SQLException {
        // SELECT DATA FROM CM_FTS_STAGE WHERE ID=?
        final String query = "SELECT " + Constants.TABLE_FTS_STAGE__DATA + " FROM " + Constants.TABLE_FTS_STAGE + " WHERE " + Constants.FIELD_ID + " =?";
        
        // UPDATE CM_FTS_DATA SET FTS_TEXT2=? WHERE ID = ?
        final String update = "UPDATE " + Constants.TABLE_FTS_DATA + " SET " + Constants.TABLE_FTS_DATA__TEXT + " =? WHERE " + Constants.FIELD_ID + " =?";
        
        // DELETE FROM CM_FTS_STAGE WHERE ID=?
        final String delete = "DELETE FROM " + Constants.TABLE_FTS_STAGE + " WHERE " + Constants.FIELD_ID + " =?"; 
        
        Integer result = Constants.RC_FTS_CONV_OK;
        PreparedStatement pSelect = null;
        PreparedStatement pUpdate = null;
        PreparedStatement pDelete = null;
        ResultSet rs = null;
        log.debug("HSQL ZIP_AND_MOVE");
        try {
            log.debug("Query: " + query);
            pSelect = conn.prepareStatement(query);
            pSelect.setLong(1, id);
            pSelect.execute();
            rs = pSelect.getResultSet();
            if ( ! rs.next()){
                // no rows found
                result = Constants.RC_FTS_CONV_NO_ROWS;
                log.error("Failed to find record with text");
            } else {
                InputStream is = rs.getBinaryStream(1);
                // read strea
                FileBackedOutputStream fos = new FileBackedOutputStream();
                StoreHelper.transfer(is, fos);

                // copy to CM_FTS_DATA
                log.debug("Update: " + update);
                pUpdate = conn.prepareStatement(update);
                int length = (int)fos.getLength();
                log.debug("ZIP_AND_MOVE read bytes: " + length);
                pUpdate.setBinaryStream(1,fos.toInputStream(),length);
                pUpdate.setLong(2,id);
                int count = pUpdate.executeUpdate();
                if (count !=1){
                    result = Constants.RC_FTS_CONV_UPDATE_ERR;
                    log.error("Updated record count not match 1: "+ count );
                } else {
                    if ( rs.next() ){
                        result = Constants.RC_FTS_CONV_TOO_MANY_ROWS;
                        log.error("Too many rows in STAGE table");
                    }
                }
                log.debug("Delete: " + delete);
                pDelete = conn.prepareStatement(delete);
                pDelete.setLong(1,id);
                pDelete.execute();
                
                
            }     
            
        } catch (Exception e){
            result = Constants.RC_FTS_CONV_ERR;
            log.error("ZIP_AND_MOVE Failed: ", e);
        }
        finally {
            if (rs != null)
                rs.close();
            if (pSelect != null)
                pSelect.close();
            if (pUpdate != null)
                pUpdate.close();
            if (pDelete != null)
                pDelete.close();
        }
        
        return result;
        
    }

}


/*
 * $Log: StoredProcedures.java,v $
 * Revision 1.6  2009/01/26 10:53:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2008/07/17 06:35:03  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2008/07/16 11:42:52  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/06/02 11:36:11  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2008/04/29 10:56:00  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 09:00:20  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.11  2006/11/20 12:05:52  zahars
 * PTR #1803381 ZIP and move fixed for HSQL
 *
 * Revision 1.10  2006/11/14 07:37:18  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.9  2006/08/11 09:04:44  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.8  2006/08/04 12:33:34  maksims
 * #1802426 SQL Framework used to generate queries in DBContentStore
 *
 * Revision 1.7  2006/07/17 09:07:03  zahars
 * PTR#0144986 Comments added
 *
 * Revision 1.6  2006/07/12 14:44:22  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 * Revision 1.5  2006/07/12 12:34:03  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 * Revision 1.4  2006/07/12 10:10:16  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/07/11 13:04:15  zahars
 * PTR#0144986 Update statement fixed
 *
 * Revision 1.2  2006/07/11 12:17:07  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 * Revision 1.1  2006/04/20 08:20:33  dparhomenko
 * PTR#0144983 stored procedure for Hypersonic check security
 *
 */