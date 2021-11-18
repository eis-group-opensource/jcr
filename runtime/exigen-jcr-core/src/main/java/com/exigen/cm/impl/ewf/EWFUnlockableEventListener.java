/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.ewf;

import java.util.Calendar;
import java.util.HashSet;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.observation.BeforeSaveEventListener;
import com.exigen.cm.impl.observation.EventImpl;
import com.exigen.cm.impl.state2.ItemStatus;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class EWFUnlockableEventListener implements BeforeSaveEventListener{
    
    private Log log = LogFactory.getLog(RepositoryImpl.class);
    
    public void onEvent(EventIterator events) {
        HashSet<NodeImpl> processedNodes = new HashSet<NodeImpl>();
        while(events.hasNext()){
            Event e = events.nextEvent();
            try {
            	if (log.isDebugEnabled()){
            		log.debug("EWFUnlockableEventListener : "+e.getPath()+" event type: "+e.getType());
            	}
                NodeImpl n = (NodeImpl) ((EventImpl) e).getEventParentItem();
                if (n.getNodeState().getStatus() != ItemStatus.Destroyed && n.getNodeState().getStatus() != ItemStatus.Invalidated){
	                if (!processedNodes.contains(n)){
	                    if (e.getType() == Event.PROPERTY_ADDED && e.getPath().endsWith("jcr:lockOwner")){
	                        Calendar time = Calendar.getInstance();
	                        //time.add(Calendar.WEEK_OF_YEAR, 1);
	                        InternalValue value = InternalValue.create(time);
	                        n.internalSetProperty(Constants.EWF_UNLOCKABLE__LOCK_TIME, value, true, false);
	                        processedNodes.add(n);
	                    } else if (e.getType() == Event.PROPERTY_REMOVED && e.getPath().endsWith("jcr:lockOwner")){
	                        n.internalSetProperty(Constants.EWF_UNLOCKABLE__LOCK_TIME, null, true, false);
	                        processedNodes.add(n);
	                    }
	                }
                }
            } catch (RepositoryException e1) {
                e1.printStackTrace();
                throw new RuntimeException("Error processing event :"+e1.getMessage());
            }
        }    }

}


/*
 * $Log: EWFUnlockableEventListener.java,v $
 * Revision 1.2  2009/01/23 07:21:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:11  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.10  2006/11/30 11:00:06  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.9  2006/11/22 08:45:09  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.8  2006/10/17 10:46:56  dparhomenko
 * PTR#0148641 fix default values
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
 * Revision 1.3  2006/08/22 11:50:55  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.2  2006/06/27 12:50:48  dparhomenko
 * PTR#1802276 implement exigen autocreated properties
 *
 * Revision 1.1  2006/06/27 11:51:06  dparhomenko
 * PTR#1802276 implement exigen autocreated properties
 *
 */