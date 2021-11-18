/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.state2._NodeState;

public class NodeIteratorImpl extends RangeIteratorImpl implements NodeIterator{

    //private NodeManager nodeManager;

    public NodeIteratorImpl(SessionImpl session, Iterator iterator) {
        super(session, iterator);
        //this.nodeManager = session.getNodeManager();
    }

    public NodeIteratorImpl(SessionImpl session, List data) {
        super(session, data);
        //this.nodeManager = session.getNodeManager();
    }


    public Node nextNode() {
        return (Node) next();
    }

    protected Object buildObject(Object source) {
        try {
            if (source instanceof Node){
                return source;
            } else {
            	_NodeState state = null;
            	if (source instanceof Long){
            		//TODO optimize read ahead
            		state = session.getStateManager().getNodeState((Long) source, getNextIds());
            	} else {
            		state = (_NodeState) source;
            	}
                return new NodeImpl(state, session.getStateManager());
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
            //TODO change to repository runtime exception
            throw new RuntimeException("Error building Node for id "+source);
        } 
    }
    
    
    protected void cache() {
        ArrayList<Long> ids = new ArrayList<Long>();
        for(long i = cacheStart ; i < cacheEnd ; i++){
            Object obj = data.get((int) i);
            if (obj instanceof Long){
                ids.add((Long)obj);
            } else {
                return;
            }
        }
        
        //TODO check for already loaded nodes
        /*if (ids.size() > 1){
            DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, true);
            st.addCondition(Conditions.in(Constants.FIELD_ID, ids));
            DatabaseConnection conn = null;
            try {
                conn = session.getConnection();
                st.execute(conn);
                session._getRepository().getCacheManager().putAll(Constants.TABLE_NODE, st);
            } catch (Exception exc){
                exc.printStackTrace();
            } finally {
                if (conn != null){
                    try {
                        conn.close();
                    } catch (RepositoryException e) {
                    }
                }
            }
        }*/
    }


    
}


/*
 * $Log: NodeIteratorImpl.java,v $
 * Revision 1.5  2008/07/17 06:57:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2008/07/16 08:45:05  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/06/02 11:36:11  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2008/05/19 11:09:02  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 09:01:27  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2007/03/02 09:32:25  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.4  2006/11/14 07:37:25  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.3  2006/09/07 10:37:08  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.2  2006/04/20 11:43:09  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:47:00  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/06 09:16:24  dparhomenko
 * PTR#0144983 MS FTS
 *
 * Revision 1.2  2006/04/05 14:30:51  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.1  2006/03/27 14:57:33  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.4  2006/03/27 14:27:21  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.3  2006/03/01 13:57:02  dparhomenko
 * PTR#0144983 iterator support
 *
 * Revision 1.2  2006/02/27 15:02:49  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/17 13:03:40  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */