/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.jackrabbit.name.QName;

/**
 * Holds query building context. 
 */
public abstract class BuildingContextHolder {
    
    private static final Log log = LogFactory.getLog(BuildingContextHolder.class);
    
    
//  Declares attribute types which can be set for comparison
    public enum PROPERTY_TYPE {GENERAL, JCR_NAME, JCR_PRIMARY_TYPE, JCR_MIXINS, JCR_ID, POSITION}

    private final BuildingContext context;
    
    protected BuildingContextHolder(BuildingContext context){
        this.context=context;
    }
    
    /**
     * Returns building context instance.
     * @return
     */
    public BuildingContext getBuildingContext(){
        return context;
    }
    
    /**
     * Returns attribute type by its string name.
     * @param qNameStr
     * @return
     */
    protected PROPERTY_TYPE getPropertyType(String qNameStr){
        try{

            if(qNameStr == null)
                return PROPERTY_TYPE.GENERAL;
                
            QName qName = QName.fromJCRName(qNameStr, getBuildingContext().getSession().getNamespaceResolver());            
            
            if(QName.JCR_NAME.equals(qName))
                return PROPERTY_TYPE.JCR_NAME;

            if(QName.JCR_PRIMARYTYPE.equals(qName))
                return PROPERTY_TYPE.JCR_PRIMARY_TYPE;

            if(QName.JCR_MIXINTYPES.equals(qName))
                return PROPERTY_TYPE.JCR_MIXINS;

            if(Constants.FIELD_INTERNAL_ID.equals(qName))
                return PROPERTY_TYPE.JCR_ID;
    
            return PROPERTY_TYPE.GENERAL;
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to parse name {0}", new Object[]{qNameStr});
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }    
}


/*
 * $Log: BuildingContextHolder.java,v $
 * Revision 1.1  2007/04/26 08:59:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/20 16:19:18  maksims
 * #1803635 javadocs added
 *
 * Revision 1.1  2006/12/15 13:13:29  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:06  maksims
 * #1801897 Query2 addition
 *
 */