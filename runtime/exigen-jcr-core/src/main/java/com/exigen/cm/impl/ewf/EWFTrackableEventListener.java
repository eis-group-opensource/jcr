/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.ewf;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.ItemId;
import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.PropertyId;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.observation.BeforeSaveEventListener;
import com.exigen.cm.impl.observation.EventImpl;
import com.exigen.cm.impl.state2.ItemStatus;
import com.exigen.cm.impl.state2._AbstractsStateManager;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class EWFTrackableEventListener implements BeforeSaveEventListener{
    
    private Log log = LogFactory.getLog(RepositoryImpl.class);
    
    public void onEvent(EventIterator events) {
        HashSet<Long> processedNodes = new HashSet<Long>();
        SessionImpl session= null;
        _AbstractsStateManager stateManager = null;
        while(events.hasNext()){
            Event e = events.nextEvent();
            if (session == null){
            	session = ((EventImpl)e).getSession();
            	stateManager = session.getStateManager();
            }
            try {
            	if (log.isDebugEnabled()){
            		log.debug("EWFTrackableEventListener : "+e.getPath()+" event type: "+e.getType());
            	}
                //ItemImpl pi = ((EventImpl) e).getEventItem();
                //NodeImpl n = (NodeImpl) pi.getParent();
            	EventImpl eImpl = (EventImpl) e;
                ItemId id = (ItemId) eImpl.getItemid();
                Long nId = null;
                if (id instanceof NodeId){
                	nId = ((NodeId)eImpl.getParentId()).getId();
                } else {
                	nId = ((PropertyId)id).getParentId();
                }
                if (!processedNodes.contains(nId)){
                	_NodeState nState = stateManager.getNodeState(nId, null, false, nId.toString());
                	if (!nState.getStatus().equals(ItemStatus.Invalidated) && !nState.getStatus().equals(ItemStatus.Destroyed)){
	                    Calendar time = new GregorianCalendar();
	                    String user = session.getUserID();
	                    if (!nState.hasProperty(getCreatedByProperty()) || 
	                    		nState.getProperty(getCreatedByProperty(),true).getString().equals("")){
	                        //InternalValue value = InternalValue.create(time);
	                        //nState.internalSetProperty(getCtreatedDateProperty(), value, true, false);
	                    	InternalValue value = InternalValue.create(user);
	                        nState.internalSetProperty(getCreatedByProperty(), value, true, false, true);
	                    }
	                    InternalValue value = InternalValue.create(time);
	                    nState.internalSetProperty(getUpdatedDateProperty(), value, true, false, true);
	                    value = InternalValue.create(user);
	                    nState.internalSetProperty(getUpdatedByProperty(), value, true, false, true);
                	}
                    processedNodes.add(nId);
                }
            } catch (RepositoryException e1) {
                e1.printStackTrace();
                throw new RuntimeException("Error processing event :"+e1.getMessage());
            }
        }
    }

	protected QName getUpdatedByProperty() {
		return Constants.EWF_TRACKABLE__UPDATEDBY;
	}

	protected QName getUpdatedDateProperty() {
		return Constants.EWF_TRACKABLE__UPDATED;
	}

	protected QName getCreatedByProperty() {
		return Constants.EWF_TRACKABLE__CREATEDBY;
	}

}


/*
 * $Log: EWFTrackableEventListener.java,v $
 * Revision 1.4  2008/07/09 06:40:24  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2007/10/19 13:45:18  dparhomenko
 * migrate to ECR types
 *
 * Revision 1.2  2007/08/21 09:26:40  dparhomenko
 * optimize for ipb
 *
 * Revision 1.1  2007/04/26 08:59:11  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.11  2006/12/06 12:59:58  dparhomenko
 * PTR#0149529 fix listener
 *
 * Revision 1.10  2006/11/14 07:37:40  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.9  2006/10/17 10:46:56  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.8  2006/10/11 13:08:56  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.7  2006/10/09 11:22:46  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.6  2006/10/05 14:13:12  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.5  2006/09/28 12:23:29  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.4  2006/09/14 09:49:44  dparhomenko
 * PTR#0148153
 *
 * Revision 1.3  2006/09/11 09:06:08  dparhomenko
 * PTR#0148025 fix unknown user default value
 *
 * Revision 1.2  2006/09/07 14:38:04  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.1  2006/06/27 11:51:06  dparhomenko
 * PTR#1802276 implement exigen autocreated properties
 *
 */