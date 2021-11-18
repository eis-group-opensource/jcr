/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.transaction;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.vf.commons.logging.LogUtils;

public abstract class TransactionHelper {

	static Log log = LogFactory.getLog(TransactionHelper.class);
	private static final String transactionalInstanceClass = "com.exigen.cm.database.transaction.jta.TransactionHelperTransactionImpl";

    /**
     * Application Server not detected.
     */
    public static final int APPLICATION_SERVER_NONE         = 0;
    /**
     * WebSphere application server.
     */
    public static final int APPLICATION_SERVER_WEBSPHERE     = 1;
    /**
     * WebLogic application server.
     */
    public static final int APPLICATION_SERVER_WEBLOGIC     = 2;
    
    /**
     * JBoss application server.
     */
    public static final int APPLICATION_SERVER_JBOSS         = 3;
    
    /**
     * Spring
     */
    public static final int APPLICATION_SERVER_SPRING         = 5;
    
    /**
     * Exigen Local EJB Container.
     */
    public static final int APPLICATION_SERVER_LOCAL_CONTAINER = 4;
    
    
	public static final String JBOSS_TRANSACTION_MANAGER = "java:/TransactionManager";

    
    
    private static TransactionHelper instance = null;
    
    public static TransactionHelper getInstance() throws RepositoryException{
    	if (instance == null){
    		int type = getApplicationServerType();
    		switch (type) {
			case APPLICATION_SERVER_WEBSPHERE:
				instance = instantiateTransactionHelperTransactionImpl(type);
				break;

			case APPLICATION_SERVER_WEBLOGIC:
				instance = instantiateTransactionHelperTransactionImpl(type);
				break;

			case APPLICATION_SERVER_JBOSS:
				instance = instantiateTransactionHelperTransactionImpl(type);
				break;

			case APPLICATION_SERVER_LOCAL_CONTAINER:
				instance = instantiateTransactionHelperTransactionImpl(type);
				break;

			default:
				instance = new TransactionHelperNoTransactionImpl();
				break;
			}
    	}
    	return instance;
    }
      
	abstract protected void init(int type);

	private static int applicatonServerType = -1;
    
    /**
     * Gets application server type (WebLogic,WebShere,...).
     * 
     * @return integer, which represent application server.
     */
    public static int getApplicationServerType(){
        if (applicatonServerType == -1){
            try {
                InitialContext ctx = new InitialContext();
                String factoryName = (String) ctx.getEnvironment().get(Context.INITIAL_CONTEXT_FACTORY);
                if (factoryName != null) {
                    factoryName = factoryName.toLowerCase();
                    //System.out.println(factoryName);
                    if (factoryName.indexOf("weblogic") > -1){
                        applicatonServerType = APPLICATION_SERVER_WEBLOGIC;
                    } else  if (factoryName.indexOf("websphere") > -1){ 
                        applicatonServerType = APPLICATION_SERVER_WEBSPHERE;
                    } else if (factoryName.equalsIgnoreCase("org.jnp.interfaces.NamingContextFactory") ||
                            factoryName.equalsIgnoreCase("org.jboss.naming.JBossRemotingContextFactory")){ //TODO may be danger
                        return APPLICATION_SERVER_JBOSS;
                        //throw new VFRuntimeException("JBoss not supported yet");
                    } else {
                    	try {
                            Object obj = ctx.lookup(JBOSS_TRANSACTION_MANAGER);
                            if (obj != null){
                    		applicatonServerType = APPLICATION_SERVER_LOCAL_CONTAINER;
                            } else {
                        		applicatonServerType = APPLICATION_SERVER_NONE;
                            }
                    	} catch (Exception exc){
                    		applicatonServerType = APPLICATION_SERVER_NONE;
                    	}
                    	
                        
                    }
                } else {
                    applicatonServerType = APPLICATION_SERVER_NONE;
                }
            } catch (Exception e){
                return APPLICATION_SERVER_NONE;
            }
        }
        return applicatonServerType;
    }
    
    /**
     * Suspends JTA Transaction if exist.
     * @return suspended transaction or null
     *
     * @throws VFHibernatorRuntimeException if error occured during transaction suspending
     */
    //public abstract JCRTransaction suspendTransaction() throws RepositoryException;
    
    /**
     * Resume JTA Transaction.
     *
     * @param transaction transaction for resuming
     * @throws RepositoryException 
     * 
     * @throws VFHibernatorRuntimeException if error occured during transaction resuming
     */
    //public abstract void resumeTransaction(JCRTransaction transaction) throws RepositoryException;
    
    
    /**
     * Gets transaction manager if exists.
     * 
     * @return transaction manager or null.
     * @throws VFHibernatorRuntimeException if error occurred during detecting application server
     */
    public abstract JCRTransactionManager getTransactionManager() throws RepositoryException;

	public abstract JCRTransaction startNewTransaction() throws RepositoryException ;

	public abstract void commitAndResore(JCRTransaction tr) throws RepositoryException ;
	
	public abstract  void rollbackAndResore(JCRTransaction tr) throws RepositoryException ;

    protected static Object getStaticMethodValue(String className, String methodName) throws RepositoryException {
        Class clazz;
        try {
            clazz = Class.forName(className);
            Method m = MethodUtils.getAccessibleMethod(clazz, methodName, new Class[]{});
            return m.invoke(null, new Object[]{});
        } catch (ClassNotFoundException e) {
            String msg = LogUtils.error(log, "Error executing static method {0} for class {1}", new Object[]{methodName, className}, e);
            throw new RepositoryException(msg, e);
        } catch (IllegalArgumentException e) {
            String msg = LogUtils.error(log, "Error executing static method {0} for class {1}", new Object[]{methodName, className}, e);
            throw new RepositoryException(msg, e);
        } catch (IllegalAccessException e) {
            String msg = LogUtils.error(log, "Error executing static method {0} for class {1}", new Object[]{methodName, className}, e);
            throw new RepositoryException(msg, e);
        } catch (InvocationTargetException e) {
            String msg = LogUtils.error(log, "Error executing static method {0} for class {1}", new Object[]{methodName, className}, e);
            throw new RepositoryException(msg, e);
        }
    }

	public abstract int getType();

	public boolean isManagementEnvironment() {
		return getApplicationServerType() != APPLICATION_SERVER_NONE && 
		getApplicationServerType() != APPLICATION_SERVER_LOCAL_CONTAINER;
	}

	public abstract boolean isTransactionActive() ;

	public void configureTransactionManager(int type, JCRTransactionManager trManager) throws RepositoryException{
		//if (instance == null || instance instanceof TransactionHelperNoTransactionImpl){
			
			instance = instantiateTransactionHelperTransactionImpl(type, trManager);
		
		//}
	}

    private TransactionHelper instantiateTransactionHelperTransactionImpl(int type, JCRTransactionManager trManager)  throws RepositoryException{
    	try {
			Class c = Class.forName(transactionalInstanceClass);
			Constructor constructor = c.getConstructor(new Class[]{});
			TransactionHelper instance = (TransactionHelper) constructor.newInstance(new Class[]{});
			instance.init(type, trManager);
			return instance;
    	} catch (Exception exc){
    		throw new RepositoryException(exc);
    	}
	}

	abstract protected void init(int type, JCRTransactionManager trManager);

	private static TransactionHelper instantiateTransactionHelperTransactionImpl(int type) throws RepositoryException {
    	try {
			Class c = Class.forName(transactionalInstanceClass);
			Constructor constructor = c.getConstructor(new Class[]{});
			TransactionHelper instance = (TransactionHelper) constructor.newInstance(new Class[]{});
			instance.init(type);
			return instance;
    	} catch (Exception exc){
    		throw new RepositoryException(exc);
    	}
	}

	public static JCRTransaction getCurrentTransaction() throws RepositoryException {
		JCRTransactionManager trManager = getInstance().getTransactionManager();
		if (trManager == null){
			return null;
		}
		return trManager.getTransaction();
	}

    public abstract TrabsactionSynchronization createTransactionSynchranization() throws RepositoryException;





}


/*
 * $Log: TransactionHelper.java,v $
 * Revision 1.1  2009/02/03 12:02:57  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/07/03 11:13:06  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2008/05/07 09:14:10  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2007/11/30 07:47:49  dparhomenko
 * Fix lock problem
 *
 * Revision 1.4  2007/10/16 13:07:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2007/09/03 14:09:49  dparhomenko
 * remove geronimo dependency
 *
 * Revision 1.2  2007/08/29 12:55:29  dparhomenko
 * fix drop procedure for version without fts
 *
 * Revision 1.1  2007/04/26 09:01:10  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.8  2007/03/02 10:32:21  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.7  2007/03/02 09:32:13  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.6  2007/02/22 09:24:30  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.5  2006/11/30 10:59:58  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.4  2006/11/14 07:37:37  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.3  2006/05/25 14:49:15  dparhomenko
 * PTR#1801955 add JBOSS support
 *
 * Revision 1.2  2006/05/08 14:45:09  dparhomenko
 * PTR#0144983 fixes
 *
 * Revision 1.1  2006/04/17 06:46:58  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/13 10:04:01  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/03/23 14:26:56  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.1  2006/02/10 15:50:34  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */