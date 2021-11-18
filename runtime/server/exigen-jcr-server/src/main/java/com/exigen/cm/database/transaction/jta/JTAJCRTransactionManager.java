/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.transaction.jta;

import javax.jcr.RepositoryException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.JCRTransactionManager;
import com.exigen.cm.database.transaction.TrabsactionSynchronization;

public class JTAJCRTransactionManager implements JCRTransactionManager{

	private TransactionManager manager;

	public JTAJCRTransactionManager(TransactionManager manager) {
		this.manager = manager;
	}

    public void registerSyncronization(JTATransactionSynchronization syncronization) throws RepositoryException{
        try {
            Transaction tr = manager.getTransaction(); 
            if (tr != null){
                tr.registerSynchronization(syncronization);
            }
        } catch (IllegalStateException e) {
            throw new RepositoryException(e);
        } catch (RollbackException e) {
            throw new RepositoryException(e);
        } catch (SystemException e) {
            throw new RepositoryException(e);
        }
    }
    
	public JCRTransaction begin() throws RepositoryException {
		try {
			manager.begin();
			return getTransaction();
		} catch (NotSupportedException e) {
			throw new RepositoryException(e);
		} catch (SystemException e) {
			throw new RepositoryException(e);
		}
	}

	public void commit(JCRTransaction tr)  throws RepositoryException {
			try {
				//((JTAJCRTRansaction)tr).getTransaction().commit();
				//System.out.println("Before Commit"+manager.getTransaction());
				//((JTAJCRTRansaction)getTransaction()).getTransaction().commit();
				manager.commit();
				//System.out.println("After commit "+manager.getTransaction());
			} catch (IllegalStateException e) {
				throw new RepositoryException(e);
			} catch (SecurityException e) {
				throw new RepositoryException(e);
			} catch (HeuristicMixedException e) {
				throw new RepositoryException(e);
			} catch (HeuristicRollbackException e) {
				throw new RepositoryException(e);
			} catch (RollbackException e) {
				throw new RepositoryException(e);
			} catch (SystemException e) {
				throw new RepositoryException(e);
			}
		
	}

	public JCRTransaction getTransaction() throws RepositoryException {
		try {
//			System.out.println("Manager "+manager);
//			System.out.println("Transaction "+manager.getTransaction());
			return new JTAJCRTRansaction(manager.getTransaction(), this);
		} catch (SystemException e) {
			throw new RepositoryException(e);
		}
	}

	public void resume(JCRTransaction tr)  throws RepositoryException {
		try {
			//System.out.println("Resume "+((JTAJCRTRansaction)tr).getTransaction());
			if (((JTAJCRTRansaction)tr).getTransaction() != null) {
//			    manager.getTransaction()
//			    ((JTAJCRTRansaction)tr).getTransaction().
				manager.resume(((JTAJCRTRansaction)tr).getTransaction());
			}
		} catch (InvalidTransactionException e) {
			throw new RepositoryException(e);
		} catch (IllegalStateException e) {
			throw new RepositoryException(e);
		} catch (SystemException e) {
			throw new RepositoryException(e);
		}
		
	}

	public void rollback(JCRTransaction tr)  throws RepositoryException {
		try {
			//((JTAJCRTRansaction)getTransaction()).getTransaction().rollback();
		    manager.rollback(); //INFO required by jboss 4.2.2
		} catch (IllegalStateException e) {
			throw new RepositoryException(e);
		} catch (SecurityException e) {
			throw new RepositoryException(e);
		} catch (SystemException e) {
			throw new RepositoryException(e);
		}
		
	}

	public JCRTransaction suspend()  throws RepositoryException {
		try {
			return new JTAJCRTRansaction(manager.suspend(), this);
		} catch (SystemException e) {
			throw new RepositoryException(e);
		}
	}

	public void commitAndResore(JCRTransaction tr) throws RepositoryException {
		commit(tr);
		resume(tr);
		
	}

	public void rollbackAndResore(JCRTransaction tr)throws RepositoryException  {
	   // Transaction current = ((JTAJCRTRansaction)getTransaction()).getTransaction();
	    //System.out.println("11"+getTransaction());
		rollback(tr);
        //System.out.println("12"+getTransaction());
		// TODO in JBOSS 4.2.2 this cause exception resume(tr);
		//Transaction  now = ((JTAJCRTRansaction)getTransaction()).getTransaction();

		//if (now != null && !now.equals(current)){
		    resume(tr);
		   // manager.resume(current);
		   // manager.rollback();
		   // manager.resume(((JTAJCRTRansaction)tr).getTransaction())
		//}
		
	}

	public JCRTransaction startNewTransaction() throws RepositoryException {
		JCRTransaction tr = suspend();
		begin();
		return tr;
	}

    public TrabsactionSynchronization createTransactionSynchranization() throws RepositoryException {
        JTATransactionSynchronization result = new JTATransactionSynchronization();
        this.registerSyncronization(result);
        return result;
    }

}
