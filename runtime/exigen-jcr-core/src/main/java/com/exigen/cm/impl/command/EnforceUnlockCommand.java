/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.command;

import java.util.Calendar;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.AbstractRepositoryCommand;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.PropDef;

public class EnforceUnlockCommand extends AbstractRepositoryCommand{


    private String tableName;
    private String columnName;
    private boolean executable;

    public boolean execute(DatabaseConnection conn) throws RepositoryException {
        DatabaseSelectAllStatement st1 = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, false);
        st1.addJoin(Constants.TABLE_WORKSPACE, "w", Constants.TABLE_NODE__WORKSPACE_ID, Constants.FIELD_ID);
        st1.addJoin(tableName, "u", Constants.FIELD_ID, Constants.FIELD_TYPE_ID);
        st1.addCondition(Conditions.lt("u."+columnName, Calendar.getInstance()));
        st1.setLockForUpdate(true);
        
        st1.addResultColumn("w."+Constants.TABLE_WORKSPACE__NAME);
        st1.addResultColumn(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);
        st1.execute(conn);
        while (st1.hasNext()){
            RowMap row = st1.nextRow();
            Long nodeId = row.getLong(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);
            String wName = row.getString("w."+Constants.TABLE_WORKSPACE__NAME);
            SessionImpl s = repository.createTrustedSession(wName, "DSUSER", conn);
            NodeImpl nn = s.getNodeById(repository.buildNodeId(nodeId, conn));
            if (nn.isLocked()){
                s.addLockToken(nn.getNodeId().toString());
                nn.unlock();
            }
            s.logout();
        }
        return false;
    }

    @Override
    public boolean init() throws RepositoryException {
        super.init();
        List<NodeTypeDef> ntypes = repository.getNodeTypeReader().all();
        for(NodeTypeDef def:ntypes){
            if (def.getName().equals(Constants.EWF_UNLOCKABLE)){
                tableName = def.getTableName();
                PropDef[] props = def.getPropertyDefs();
                for(PropDef prop: props){
                    if (prop.getName().equals(Constants.EWF_UNLOCKABLE__LOCK_TIME)){
                        columnName = prop.getColumnName();
                    }
                }
            }
        }
        if (tableName == null){
        	return false;
        }
        if (columnName == null){
            throw new RepositoryException("Can't find property "+Constants.EWF_UNLOCKABLE__LOCK_TIME+" in type "+Constants.EWF_UNLOCKABLE);
        }
        if (tableName == null){
            throw new RepositoryException("Type "+Constants.EWF_UNLOCKABLE+" not found");
        }
        return true;
    }

    public String getDisplayableName(){
        return "Enforce unlock";
    }
    

}


/*
 * $Log: EnforceUnlockCommand.java,v $
 * Revision 1.2  2009/01/23 07:21:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 09:02:20  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.12  2006/11/14 07:37:35  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.11  2006/11/09 13:44:58  zahars
 * PTR #1803381  FTS batch size could be configured
 *
 * Revision 1.10  2006/10/17 10:46:52  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.9  2006/09/28 12:39:49  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.8  2006/08/22 11:50:56  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.7  2006/08/18 08:18:04  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.6  2006/08/15 08:38:12  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.5  2006/08/07 14:26:04  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.4  2006/07/18 12:51:16  zahars
 * PTR#0144986 INFO log level improved
 *
 * Revision 1.3  2006/07/14 08:21:28  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.2  2006/07/06 07:54:45  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/04 09:27:19  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 */