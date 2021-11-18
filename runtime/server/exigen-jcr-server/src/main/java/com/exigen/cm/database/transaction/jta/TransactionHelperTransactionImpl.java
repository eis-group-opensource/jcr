/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.transaction.jta;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.JCRTransactionManager;
import com.exigen.cm.database.transaction.TrabsactionSynchronization;
import com.exigen.cm.database.transaction.TrabsactionSynchronizationNoTransaction;
import com.exigen.cm.database.transaction.TransactionHelper;


public class TransactionHelperTransactionImpl extends TransactionHelper {

	private int type;
    
	private static JCRTransactionManager transactionManager= null;
    
	private static boolean transactionManagerEvaluated= false;

	private Log log = LogFactory.getLog(TransactionHelperTransactionImpl.class);
	
	@Override
	public void commitAndResore(JCRTransaction tr) throws RepositoryException {
		JCRTransactionManager mgr = getTransactionManager();
		if (mgr != null) {
			mgr.commitAndResore(tr);
		} 
	}

	@Override
	public JCRTransactionManager getTransactionManager()
			throws RepositoryException {
        if (!transactionManagerEvaluated){
            int serverType = type;
            if (serverType == APPLICATION_SERVER_JBOSS || serverType == APPLICATION_SERVER_LOCAL_CONTAINER){
                try {
                    InitialContext ctx = new InitialContext();
                    TransactionManager _transactionManager = (TransactionManager) ctx.lookup(JBOSS_TRANSACTION_MANAGER);
                    transactionManager = new JTAJCRTransactionManager(_transactionManager);
                } catch (Exception exc){
                	exc.printStackTrace();
                    throw new RepositoryException("Error getting TransactionManager");
                }
                transactionManagerEvaluated = true;
            } else if (serverType == APPLICATION_SERVER_WEBLOGIC){
                //transactionManager = (TransactionManager) TransactionHelper.getStaticMethodValue("weblogic.transaction.TxHelper", "getTransactionManager");
                //transactionManagerEvaluated = true;
                try {
                    InitialContext ctx = new InitialContext();
                    TransactionManager _transactionManager = (TransactionManager) ctx.lookup("javax.transaction.TransactionManager");
                    transactionManager = new JTAJCRTransactionManager(_transactionManager);
                } catch (Exception exc){
                	exc.printStackTrace();
                    throw new RepositoryException("Error getting TransactionManager");
                }
                transactionManagerEvaluated = true;
            } else if (serverType == APPLICATION_SERVER_WEBSPHERE){
            	TransactionManager _transactionManager = (TransactionManager) TransactionHelper.getStaticMethodValue("com.ibm.ws.Transaction.TransactionManagerFactory", "getTransactionManager");
                transactionManager = new JTAJCRTransactionManager(_transactionManager);
                transactionManagerEvaluated = true;
            }
        }
        return transactionManager;
	}

	/*@Override
	public void resumeTransaction(JCRTransaction transaction)
			throws RepositoryException {
        if (transaction == null){
            return;
        }
        try {
            getTransactionManager().resume(transaction);
        }catch (IllegalStateException e) {
            String msg = LogUtils.error(log, "Error suspending transaction", e);
            throw new RepositoryException(msg, e);
        } catch (Exception e) {
            String msg = LogUtils.error(log, "Error resuming transaction", e);
            throw new RepositoryException(msg, e);
        }
	}*/

	@Override
	public void rollbackAndResore(JCRTransaction tr) throws RepositoryException {
		JCRTransactionManager mgr = getTransactionManager();
		if (mgr != null) {
			mgr.rollbackAndResore(tr);
		} 
	}

	@Override
	public JCRTransaction startNewTransaction() throws RepositoryException {
		JCRTransactionManager mgr = getTransactionManager();
		if (mgr != null) {
			/*JCRTransaction tr = mgr.suspend();			
			mgr.begin();
			return tr;*/
			return mgr.startNewTransaction();
		} 
		return null;
	}

	/*@Override
	public JCRTransaction suspendTransaction() throws RepositoryException {
        JCRTransactionManager tm = getTransactionManager();
        if (tm == null){
            return null;
        }
        try {
            
            return tm.suspend();
        } catch (Exception e) {
            String msg = LogUtils.error(log, "Error suspending transaction", e);
            throw new RepositoryException(msg, e);
        }
	}*/

	@Override
	public int getType() {
		return type;
	}

	@Override
	public boolean isTransactionActive() {
		try {
		    if (getTransactionManager() != null){
		        return getTransactionManager().getTransaction() != null;
		    } else {
		        log.debug("null value for getTransactionManager()");
		    }
		} catch (RepositoryException e) {
		}
		return false;
	}

	@Override
	protected void init(int type) {
		this.type = type;
	}

	@Override
	protected void init(int type, JCRTransactionManager trManager) {
		transactionManager = trManager;
		transactionManagerEvaluated = true;
		this.type = type;
	}

    @Override
    public TrabsactionSynchronization createTransactionSynchranization() throws RepositoryException {
       if (isTransactionActive()){
            /*JTATrabsactionSynchronization result = new JTATrabsactionSynchronization();
            JTAJCRTransactionManager tm = (JTAJCRTransactionManager) getTransactionManager();
            tm.registerSyncronization(result);
            return result;*/
           return getTransactionManager().createTransactionSynchranization();
        } else {
            return new TrabsactionSynchronizationNoTransaction();
        }
        
    }
}
