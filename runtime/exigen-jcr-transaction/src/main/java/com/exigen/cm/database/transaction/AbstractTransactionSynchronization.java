/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.transaction;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class AbstractTransactionSynchronization implements
    TrabsactionSynchronization{

    private Log log = LogFactory.getLog(TrabsactionSynchronization.class);

    protected Map<RowLockInfo, Throwable> locks = Collections.synchronizedMap(new HashMap<RowLockInfo, Throwable>());

    protected static List<WeakReference<AbstractTransactionSynchronization>> managers = Collections.synchronizedList(new ArrayList<WeakReference<AbstractTransactionSynchronization>>());
    
    protected static int counter = 0;
    
    public AbstractTransactionSynchronization(){
        registerList();
    }
    
    private void registerList() {
        if (log.isDebugEnabled()){
            synchronized (managers) {
                managers.add(new WeakReference(this));
            }
        }
    }

    
    private void cleanUp(){
        if (log.isDebugEnabled()){
            synchronized (managers) {
                counter ++;
                if (counter > 1000 && managers.size() > 1000){
                    ArrayList<WeakReference<AbstractTransactionSynchronization>> forRemove = new ArrayList<WeakReference<AbstractTransactionSynchronization>>();
                    for(WeakReference<AbstractTransactionSynchronization> r:managers){
                        if (r.get() == null){
                            forRemove.add(r);
                        }
                    }
                    managers.removeAll(forRemove);
                    counter = 0;
                }
            }
        }
    }

    protected void clearLocks(){
        locks.clear();
    }
    
    public void registerLock(Long connectionId, String tableName, String pkColumn, Object id){
        if (log.isDebugEnabled()){
            if (id instanceof Collection){
                for(Object o:(Collection)id){
                    registerLock(connectionId, tableName, pkColumn, o);
                }
            } else {
                cleanUp();
                RowLockInfo lockInfo = new RowLockInfo(connectionId, tableName, pkColumn, id);
                checkThatLockAlreadyExists(lockInfo);
                Throwable stackTrace  = new Throwable();
                if (log.isTraceEnabled()){
                    //log.trace("Connection "+connectionId+" lock row in table "+tableName+ " with pk "+pkColumn+"="+id, stackTrace);
                    log.trace("Connection "+connectionId+" lock row in table "+tableName+ " with pk "+pkColumn+"="+id);
                } else {
                    log.debug("Connection "+connectionId+" lock row in table "+tableName+ " with pk "+pkColumn+"="+id);
                }
                
                locks.put(lockInfo, stackTrace);
            }
        }
    }

    private void checkThatLockAlreadyExists(RowLockInfo lockInfo) {
        if (log.isDebugEnabled()){
            synchronized (managers){
                for(WeakReference<AbstractTransactionSynchronization> r:managers){
                    AbstractTransactionSynchronization manager = r.get();
                    if (manager != null){
                        for(RowLockInfo info:manager.locks.keySet()){
                            if (lockInfo.equals(info) && lockInfo.connectionId != info.connectionId){
                                log.debug("Possible concurent lock between connections :" + info.connectionId+" and "+lockInfo.connectionId);
                                //log.error(info.connectionId,manager.locks.get(info));
                                //log.error(lockInfo.connectionId,new Throwable());
                            }
                        }
                    }
                }
            }
        }
        
    }
}


class RowLockInfo{

    Long connectionId;
    String tableName;
    String pkColumn;
    Object id;

    public RowLockInfo(Long connectionId, String tableName, String pkColumn, Object id) {
        this.connectionId = connectionId;
        this.tableName =tableName;
        this.pkColumn = pkColumn;
        this.id = id;
    }
	
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RowLockInfo){
            EqualsBuilder builder = new EqualsBuilder();
            RowLockInfo other = (RowLockInfo) obj;
            builder.append(tableName, other.tableName);
            builder.append(pkColumn, other.pkColumn);
            builder.append(id, other.id);
            return builder.isEquals();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(tableName);
        builder.append(pkColumn);
        builder.append(id);
        return builder.toHashCode();
    }


   
    
}