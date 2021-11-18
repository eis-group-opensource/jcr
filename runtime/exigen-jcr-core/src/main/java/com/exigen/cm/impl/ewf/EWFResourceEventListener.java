/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.ewf;

import java.util.HashSet;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.StorableInputStream;
import com.exigen.cm.impl.PropertyDefinitionImpl;
import com.exigen.cm.impl.PropertyId;
import com.exigen.cm.impl.PropertyImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.observation.BeforeSaveEventListener;
import com.exigen.cm.impl.observation.EventImpl;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.PropDefImpl;
import com.exigen.cm.jackrabbit.value.BLOBFileValue;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class EWFResourceEventListener implements BeforeSaveEventListener{
    
    private Log log = LogFactory.getLog(RepositoryImpl.class);
    
    
    public void onEvent(EventIterator events) {
        HashSet<_NodeState> processedNodes = new HashSet<_NodeState>();
        while(events.hasNext()){
            Event e = events.nextEvent();
            try {
            	if (log.isDebugEnabled()){
            		log.debug("EWFResourceEventListener : "+e.getPath()+" event type: "+e.getType());
            	}
            	_NodeState n = ((EventImpl) e).getEventParentItemState();
                if (!processedNodes.contains(n)){
                	//e.getPath().endsWith("ewf:data")
                    if (((PropertyId)((EventImpl) e).getItemid()).getName().equals(QName.JCR_DATA)){
                        PropertyImpl p = (PropertyImpl) ((EventImpl) e).getEventItem();
                        InternalValue[] vv = ((_PropertyState)p.getItemState()).getValues();
                        BLOBFileValue blob = (BLOBFileValue) vv[0].internalValue();
                        long size = blob.getLength();
                        setProperty(n, Constants.EWF_RESOURCE__SIZE, InternalValue.create(size));
                        //n.internalSetProperty(Constants.EWF_RESOURCE__SIZE, InternalValue.create(size), true, false, false, false);
                        
                        String contentId = ((_PropertyState)p.getItemState()).getValues()[0].getContentId();
                    	String storeName = "";
                    	if (contentId != null){
	                    	int pos = contentId.indexOf(Constants.STORE_DELIMITER);
	                        if (pos > 0){
	                        	storeName = contentId.substring(0, pos);
	                        } else {
	                        	storeName = "";
	                        }
                    	} else {
                    		BLOBFileValue value = (BLOBFileValue) ((_PropertyState)p.getItemState()).getValues()[0].internalValue();
                            if (value instanceof StorableInputStream) {
								StorableInputStream sIn = (StorableInputStream) value;
								storeName = sIn.getStoreName();
								if (storeName == null){
									storeName = "";
								}
							} else {
								storeName = value.getStoreId();
							}
                            
                    	}
						if (storeName == null){
							storeName = "";
						}
                        InternalValue value = InternalValue.create(storeName);
                        setProperty(n, Constants.EWF_RESOURCE__STORAGE_NAME, value);
                        //n.internalSetProperty(Constants.EWF_RESOURCE__STORAGE_NAME, value, true, false, false, false);
                        
                        processedNodes.add(n);
                        
                        
                        
                        
                    }
                }
            } catch (RepositoryException e1) {
                e1.printStackTrace();
                throw new RuntimeException("Error processing event :"+e1.getMessage());
            }
        }
    }


	private void setProperty(_NodeState n, QName name, InternalValue value) throws ConstraintViolationException, RepositoryException {
		_PropertyState st = n.getProperty(name, false);
		int type;
        if (value == null) {
            type = PropertyType.UNDEFINED;
        } else {
            type = value.getType();
        }
		if (st == null){
			PropertyDefinitionImpl def = n.getApplicablePropertyDefinition(name, type, false);
	        st = new _PropertyState(n.getRepository(), n,
					name, type, def.getRequiredType(), def.isMultiple(),
					(PropDefImpl) def.unwrap(), null);
			n.addProperty(st);
		}
		st.internalSetValue(new InternalValue[]{value}, type, true, false);
		
	}

}


/*
 * $Log: EWFResourceEventListener.java,v $
 * Revision 1.2  2008/05/07 09:14:10  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:11  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.14  2007/02/26 13:14:49  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.13  2007/02/02 08:48:59  dparhomenko
 * PTR#1803853 fix session scope lock
 *
 * Revision 1.12  2006/10/17 10:46:56  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.11  2006/10/11 13:08:56  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.10  2006/10/09 11:22:46  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.9  2006/10/05 14:13:12  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.8  2006/09/27 12:32:55  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.7  2006/09/11 08:37:28  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.6  2006/09/07 14:38:04  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.5  2006/09/07 10:36:58  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.4  2006/07/14 11:28:19  zahars
 * PTR#0144986 In StorableInputStream skipFTSProcessing changed
 *
 * Revision 1.3  2006/07/11 10:26:00  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/07/10 12:06:26  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/06/27 11:51:06  dparhomenko
 * PTR#1802276 implement exigen autocreated properties
 *
 */