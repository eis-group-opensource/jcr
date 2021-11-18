/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.ewf;

import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.RepositoryConfigurator;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.observation.RepositoryObservationManagerImpl;

public class EWFRepositoryConfigurator implements RepositoryConfigurator{

    private static final Log log = LogFactory.getLog(EWFRepositoryConfigurator.class);
    private static final String BAM_LISTENER_CLASS_NAME = "com.exigen.cm.impl.ewf.bam.EWFBAMEventListener";
    private static final String BAM_AUDIT_FACTORY_CLASS_NAME = "com.exigen.audit.AuditFactory";

    
    public void configure(RepositoryImpl repository, Map<String, String> config) throws RepositoryException {
        RepositoryObservationManagerImpl repositoryObsMgr = repository.getObservationManagerFactory().getObservationManager();
        configureBAM(repositoryObsMgr, config);

        repositoryObsMgr.addEventListener(new NTResourceEventListener(),
        		Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED,
                "/",
                true,
                null,
                new String[]{"nt:resource"},
                false);

        
        try {
        	repository.getNamespaceRegistry().getURI("ecr_mix");
        	repository.getNamespaceRegistry().getURI("ecr_nt");
        } catch (NamespaceException exc){
        	return;
        }
        
        //TODO do we need Event.PROPERTY_REMOVED ??
        try {
        	int events;
        	if (repository.isReducedVersionCheck()){
        		events= Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED;
        	} else {
        		events= Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED;
        	}
	        repositoryObsMgr.addEventListener(new EWFTrackableEventListener(),
	                events,
	                "/",
	                true,
	                null,
	                new String[]{"ecr_mix:trackable"},
	                false);
        } catch (NoSuchNodeTypeException exc){
        	
        }
        try {
	        repositoryObsMgr.addEventListener(new EWFTrackCreationEventListener(),
	                Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED,
	                "/",
	                true,
	                null,
	                new String[]{"ecr_mix:trackCreation"},
	                false);
	    } catch (NoSuchNodeTypeException exc){
	    	
	    }

	    try {
	        repositoryObsMgr.addEventListener(new EWFUnlockableEventListener(),
	                Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED,
	                "/",
	                true,
	                null,
	                new String[]{"ecr_mix:lockable"},
	                false);
        } catch (NoSuchNodeTypeException exc){
        	
        }
	        
        try {
	        repositoryObsMgr.addEventListener(new EWFResourceEventListener(),
	                Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED,
	                "/",
	                true,
	                null,
	                new String[]{"ecr_mix:resourceExt"},
	                false);
        } catch (NoSuchNodeTypeException exc){
        	
        }
	        
        
        try {
	        if (repository.getNodeTypeManager().hasNodeType("ecr_mix:storeConfiguration")){
		        repositoryObsMgr.addEventListener(new EWFStoreConfigurationEventListener(),
		                Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED  |Event.PROPERTY_REMOVED,
		                "/",
		                true,
		                null,
		                new String[]{"ecr_mix:storeConfiguration"},
		                false);
		        
	        } else {
	        	log.error("Error adding EWFStoreConfigurationEventListener, nodetype ewf_mix:storeConfiguration not found");
	        }
        } catch (NoSuchNodeTypeException exc){
        	
        }
    }
    
	private void configureBAM(RepositoryObservationManagerImpl repositoryObsMgr, Map<String, String> config) throws RepositoryException {
		String value = config.get(Constants.CONFIG_PROPERYT_BAM);
		if ("false".equals(value)){
			return;
		}
		Class bamListener =null ;
		Class auditFactory = null;
		EventListener listener = null;
		try {
			bamListener = Class.forName(BAM_LISTENER_CLASS_NAME);
			auditFactory = Class.forName(BAM_AUDIT_FACTORY_CLASS_NAME);
			listener = (EventListener) bamListener.newInstance();
		} catch (Exception e) {
			
		}
		
		if (bamListener != null && auditFactory != null && listener != null){
			repositoryObsMgr.addEventListener(listener,
	        		Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED| Event.PROPERTY_REMOVED ,
	                "/",
	                true,
	                null,
	                null,
	                false);
		} else {
			log.warn("BAM listener disabled");
		}
	}


}


/*
 * $Log: EWFRepositoryConfigurator.java,v $
 * Revision 1.9  2009/01/23 07:21:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2008/09/11 06:06:16  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/05/07 09:14:10  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2008/03/28 13:45:58  dparhomenko
 * Autmaticaly unlock orphaned locks(session scoped)
 *
 * Revision 1.5  2007/10/19 13:45:18  dparhomenko
 * migrate to ECR types
 *
 * Revision 1.4  2007/08/29 12:55:30  dparhomenko
 * fix drop procedure for version without fts
 *
 * Revision 1.3  2007/08/21 09:26:40  dparhomenko
 * optimize for ipb
 *
 * Revision 1.2  2007/06/13 12:26:48  dparhomenko
 * PTR#1804802 fix EWFRepository configurator
 *
 * Revision 1.1  2007/04/26 08:59:11  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.13  2007/03/12 08:24:10  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.12  2007/02/26 09:45:58  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.11  2006/12/15 12:54:47  dparhomenko
 * PTR#1803217 code reorganization
 *
 * Revision 1.10  2006/12/06 11:10:46  dparhomenko
 * PTR#1803525 fix ewf nodetypes
 *
 * Revision 1.9  2006/10/09 11:22:46  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.8  2006/10/09 08:59:44  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.7  2006/09/28 12:23:29  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.6  2006/09/27 12:32:55  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.5  2006/09/26 10:11:03  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.4  2006/07/12 11:51:24  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/07/11 10:26:00  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/07/06 07:54:27  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/06/27 11:51:06  dparhomenko
 * PTR#1802276 implement exigen autocreated properties
 *
 */