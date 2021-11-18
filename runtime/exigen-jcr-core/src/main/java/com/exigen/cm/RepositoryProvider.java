/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.imageio.spi.ServiceRegistry;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.transaction.JCRTransactionManager;
import com.exigen.vf.commons.logging.LogUtils;

public abstract class RepositoryProvider {

    protected static RepositoryProvider instance;
    private static Log log = LogFactory.getLog(RepositoryProvider.class);

    public final static RepositoryProvider getInstance() throws RepositoryException {
        if (instance != null) {
            return instance;
        }
        for (Iterator it = ServiceRegistry.lookupProviders(RepositoryProvider.class); it.hasNext();) {
            instance = (RepositoryProvider) it.next();
            if (it.hasNext()) {
                String msg = "RepositoryProvider configuration file contains more then one provider implementaiton. Using \"{0}\".";
                msg = MessageFormat.format(msg, new Object[]{instance.getClass().getName()});
                LogUtils.warn(log, msg);
            }
            return instance;
        }
        String msg = "RepositoryProvider configuration not found. Please add \"META-INF\\{0}\" file, with provider implmentation class name in it, to classpath.";
        msg = MessageFormat.format(msg, new Object[]{RepositoryProvider.class.getName()});
        LogUtils.error(log, msg);
        throw new RepositoryException(msg);
    }

    public synchronized Repository getRepository() throws RepositoryException {
        return getRepository(Constants.DEFAULT_REPOSITORY_NAME);
    }
    
    public abstract Repository getRepository(String repositoryName) throws RepositoryException;
    
    
    /**
     * @deprecated
     * Configure multiple repositories
     * @param configuration
     * @throws RepositoryException
     */
    public abstract void configure(Map<String,String> configuration) throws RepositoryException;

    /**
     * Configure multiple repositories from resource (file)
     * @param resource
     * @throws RepositoryException
     */
    public abstract void configure(String resource) throws RepositoryException;
    
    /**
     * Configure individual repository
     * @param repositoryName
     * @param configuration
     * @throws RepositoryException
     */
    public abstract void configure(String repositoryName, Map<String,String> configuration) throws RepositoryException;
    
    /**
     * Configure individual repository datasource
     * @param repositoryName
     * @param ds datasource
     * @throws RepositoryException
     */
    public abstract void configure(String repositoryName, DataSource ds) throws RepositoryException;    
    
    /**
     * Configure individual repository from resource (file)
     * @param repositoryName
     * @param resource
     * @throws RepositoryException
     */
    public abstract  void configure(String repositoryName, String resource) throws RepositoryException;

    
    /**
     * Configure individual transaction manager
     * @param repositoryName
     * @param transaction manager
     * @throws RepositoryException
     */
	public abstract void configure(String name, JCRTransactionManager manager) throws RepositoryException;

	public abstract void setAttribute(String repositoryName, String attrName, Object attrValue);
    
	public abstract Set<String> getActiveRepositories();
	
}

/*
 * $Log: RepositoryProvider.java,v $
 * Revision 1.5  2008/07/15 11:27:18  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2007/09/06 11:17:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2007/08/29 12:55:30  dparhomenko
 * fix drop procedure for version without fts
 *
 * Revision 1.2  2007/05/31 08:54:08  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.1  2007/04/26 08:59:29  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/10/13 09:20:27  dparhomenko
 * PTR#0148476 fix exception text
 *
 * Revision 1.5  2006/07/18 12:51:11  zahars
 * PTR#0144986 INFO log level improved
 *
 * Revision 1.4  2006/06/02 09:23:52  zahars
 * PTR#0144983 Configure(Map hashmap) deprecated
 *
 * Revision 1.3  2006/05/17 09:09:08  zahars
 * PTR#0144983 new configure methods added
 *
 * Revision 1.2  2006/04/20 11:42:55  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:47:17  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.7  2006/04/13 10:03:58  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.6  2006/03/09 11:02:53  ivgirts
 * PTR #1801059 added error message in case if configuration not found
 *
 * Revision 1.5  2006/03/03 10:33:14  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.4  2006/03/03 09:39:28  ivgirts
 * PTR #1801059 Database startup checking added
 *
 * Revision 1.3  2006/02/27 16:22:20  ivgirts
 * PTR#1801059 Configuration file added.
 *
 * Revision 1.2  2006/02/15 13:58:27  zahars
 * PTR #0144980. javax.imageio.spi.ServiceRegistry should be used instead of sun.misc.Service
 *
 * Revision 1.1  2006/02/10 15:50:32  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */