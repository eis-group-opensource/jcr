/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Represents single operation over content stored in Content Store.
 * @author Maksims
 *
 */
public class BatchUnit {

//  Batch Unit fields
    protected final Long jcrContentId;
    protected ContentDataSource dataSource;
    protected BatchOperation operation;
    protected Map<String, String> params;

    protected int length = 0;

//  Flag defining if batch unit is processed  
    private boolean isProcessed=false;

    public BatchUnit(Long jcrContentId){
        this.jcrContentId= jcrContentId;
    }
    
    @SuppressWarnings("unchecked") // Warning that Properties cannot be copied to Map<String,String>
    protected BatchUnit(Long jcrContentId, BatchOperation operation, Properties params, ContentDataSource source){
        this(jcrContentId);
        
        if(params != null){
            Map<String, String> pmap = new HashMap<String, String>();
            pmap.putAll((Map)params);
            this.params = pmap;
        }
        
        dataSource = source;
        this.operation = operation;

    }

    
    protected BatchUnit(Long jcrContentId, BatchOperation operation, Map<String, String> params, ContentDataSource source){
        this(jcrContentId);
        dataSource = source;
        this.operation = operation;
        this.params = params;
    }
    
    public Long getJCRContentId(){
        return jcrContentId;
    }
    
    public BatchOperation getOperation(){
        return operation;
    }
    public void setOperation(BatchOperation op){
        operation=op;
    }
    

    public ContentDataSource getDataSource(){
        return dataSource;
    }

    public void setDataSource(ContentDataSource src){
        dataSource=src;
    }

    
    public Map<String,String> getParams(){
        return params;
    }

    public void setParams(Map<String,String> params){
        this.params=params;
    }
    
    
    
    public boolean isProcessed(){
        return isProcessed;
    }
    
    public void setProcessed(boolean processed){
        isProcessed = processed;
    }
    
    public void setLength(int length){
        this.length = length;
    }
    
    public int getLength(){
        return length;
    }
}

/*
 * $Log: BatchUnit.java,v $
 * Revision 1.1  2007/04/26 09:01:01  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2006/08/08 13:10:37  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.3  2006/07/28 15:49:06  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.2  2006/07/06 16:43:07  maksims
 * #1802356 Cache and maintenance processes implementation added
 *
 * Revision 1.1  2006/07/04 14:04:39  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */