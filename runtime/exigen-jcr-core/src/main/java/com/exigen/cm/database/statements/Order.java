/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

public class Order {

    
    private String field;
    private boolean desc;

    private Order(String field, boolean desc){
        this.field = field;
        this.desc = desc;
        
    }
    
    public static final Order asc(String fieldName){
        return new Order(fieldName, false);
    }

    public static final Order desc(String fieldName){
        return new Order(fieldName, true);
    }

    public boolean getDesc() {
        return desc;
    }


    public String getField() {
        return field;
    }

    
}


/*
 * $Log: Order.java,v $
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/06/30 14:32:41  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 */