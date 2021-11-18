/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.ewf;

import java.util.HashSet;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.DatabaseCondition;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.observation.BeforeSaveEventListener;
import com.exigen.cm.impl.observation.EventImpl;

public class EWFStoreConfigurationEventListener implements BeforeSaveEventListener{
    
    private Log log = LogFactory.getLog(RepositoryImpl.class);
    
    public void onEvent(EventIterator events) {
        HashSet<NodeImpl> processedNodes = new HashSet<NodeImpl>();
        while(events.hasNext()){
            Event e = events.nextEvent();
            try {
            	if (log.isDebugEnabled()){
            		log.debug("EWFStoreConfigurationEventListener : "+e.getPath()+" event type: "+e.getType());
            	}
                NodeImpl n = (NodeImpl) ((EventImpl) e).getEventParentItem();
                if (!processedNodes.contains(n)){
                    if (e.getPath().endsWith("ecr:storeName")){
                        //PropertyImpl p = (PropertyImpl) ((EventImpl) e).getEventItem();
                        
                        if (e.getType() == Event.PROPERTY_ADDED || e.getType() == Event.PROPERTY_CHANGED){
                        	DatabaseConnection conn = n._getWorkspace().getConnection();
                        	
                        	
                            DatabaseSelectAllStatement stAll = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, false);
                            stAll.addResultColumn(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);
                            stAll.addJoin(Constants.TABLE_NODE_PARENT, "parents", Constants.FIELD_ID, Constants.FIELD_TYPE_ID );
                            stAll.addCondition(Conditions.eq("parents."+Constants.TABLE_NODE_PARENT__PARENT_ID, n.getNodeState().getNodeId()));
                            stAll.addGroup(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);

                            /*stAll.execute(conn);
                            while (stAll.hasNext()){
                            	RowMap w = stAll.nextRow();
                            	
                            }*/
                            
                        	
                        	DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(Constants.TABLE_NODE);
                        	DatabaseCondition c1 = Conditions.in(Constants.FIELD_ID, stAll);
                        	DatabaseCondition c2 = Conditions.eq(Constants.FIELD_ID, n.getNodeState().getNodeId());
                        	st.addCondition(Conditions.or(c1,c2));
                        	st.addValue(SQLParameter.create(Constants.TABLE_NODE__CONTENT_STORE_CONFIG_NODE, n.getNodeState().getNodeId()));
                        	st.execute(conn);
                        	st.close();
                        } else {
                        	//change store conf id to parent value
                        }
                        
                        processedNodes.add(n);
                    }
                }
            } catch (RepositoryException e1) {
                e1.printStackTrace();
                throw new RuntimeException("Error processing event :"+e1.getMessage());
            }
        }
    }

}


/*
 * $Log: EWFStoreConfigurationEventListener.java,v $
 * Revision 1.2  2007/10/19 13:45:18  dparhomenko
 * migrate to ECR types
 *
 * Revision 1.1  2007/04/26 08:59:11  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.8  2006/10/30 15:03:33  dparhomenko
 * PTR#1803272 a lot of fixes
 *
 * Revision 1.7  2006/10/11 13:08:56  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.6  2006/10/09 11:22:46  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.5  2006/10/05 14:13:12  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.4  2006/09/28 12:23:29  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.3  2006/09/07 10:36:58  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.2  2006/07/12 10:10:13  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/11 10:26:00  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/07/10 12:06:26  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/06/27 11:51:06  dparhomenko
 * PTR#1802276 implement exigen autocreated properties
 *
 */