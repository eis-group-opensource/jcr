/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.order;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.BuildingContextHolder;
import com.exigen.cm.query.order.OrderDefinition.ORDER;

/**
 * Produces Order defininitions.
 */
public class OrderDefinitionProducer extends BuildingContextHolder {
    private final static Log log = LogFactory.getLog(OrderDefinitionProducer.class);
    
    public OrderDefinitionProducer(BuildingContext context) {
        super(context);
    }
    
    /**
     * Produces OrderDefinition instance corresponding to provided property name.
     * @param propertyName
     * @param order
     * @return
     */
    public OrderDefinition produce(String propertyName, ORDER order){
        
        PROPERTY_TYPE pt = getPropertyType(propertyName);
        switch(pt){
            case JCR_ID:
                return new OrderNodeId(order);

            case JCR_NAME:
                return new OrderJCRName(order);

            case JCR_PRIMARY_TYPE:
                return new OrderPrimaryType(order);
                
            case GENERAL:
                return new OrderGeneralProperty(propertyName, order);
                
            default :
                    String message = MessageFormat.format("Ordering by property {0} of type {1} is not supported",
                            propertyName, pt);
                    log.error(message);
                    throw new UnsupportedOperationException(message);
        }
    }

}

/*
 * $Log: OrderDefinitionProducer.java,v $
 * Revision 1.1  2007/04/26 09:01:08  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/12/15 13:13:25  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:09  maksims
 * #1801897 Query2 addition
 *
 */