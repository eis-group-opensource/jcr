/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.Iterator;
import java.util.Properties;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.jackrabbit.util.name.NamespaceMapping;

public class NamespaceRegistryImpl extends BaseNamespaceRegistryImpl {

    private RepositoryImpl repository;
    
    public NamespaceRegistryImpl(RepositoryImpl repository, NamespaceRegistryImpl namespaceRegistry) throws RepositoryException {
        this.repository = repository;
        if (namespaceRegistry != null) {
            for (Iterator iter = namespaceRegistry.getNamespaces().iterator(); iter.hasNext();) {
                Namespace namespace = (Namespace) iter.next();                
                addNamespace(namespace.getId(), namespace.getPrefix(), namespace.getUri());
            }
        }
        addNamespace(null, "", "");

    }

    public NamespaceRegistryImpl(RepositoryImpl repository) throws RepositoryException {
        this(repository, (NamespaceRegistryImpl)null);

    }

    public NamespaceRegistryImpl(RepositoryImpl repository, NamespaceMapping nsMapping) throws RepositoryException {
        this(repository, (NamespaceRegistryImpl)null);
		for(Object prefix:nsMapping.getPrefixes()){
			String uri = nsMapping.getURI((String)prefix);
			addNamespace((long)1, (String) prefix, uri);
			
		}
    }
    
    public NamespaceRegistryImpl(RepositoryImpl repository, Properties nsMapping) throws RepositoryException {
        this(repository, (NamespaceRegistryImpl)null);
		for(Object prefix:nsMapping.keySet()){
			String uri = nsMapping.getProperty((String)prefix);
			addNamespace((long)1, (String) prefix, uri);
			
		}
    }
    
  
    
    public void registerNamespace(String prefix, String uri, DatabaseConnection conn) throws NamespaceException, 
                                   UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        if (_getByURI(uri)!= null){
            throw new NamespaceException("URI already registered");
        }
        if (_getByPrefix(prefix) != null){
            throw new NamespaceException("Prefix already registered");
        }
        try {
            Long id = conn.nextId();
            DatabaseInsertStatement st = DatabaseTools.createInsertStatement(Constants.TABLE_NAMESPACE);
            st.addValue(SQLParameter.create(Constants.FIELD_ID, id));
            st.addValue(SQLParameter.create(Constants.TABLE_NAMESPACE__PREFIX, prefix));
            st.addValue(SQLParameter.create(Constants.TABLE_NAMESPACE__URI, uri));
            st.execute(conn);
            addNamespace(id, prefix, uri);
        } catch (Exception exc){
            throw new RepositoryException("Error registering namespace", exc);
        }
            
    }

    public void registerNamespace(String prefix, String uri)
            throws NamespaceException, UnsupportedRepositoryOperationException,
            AccessDeniedException, RepositoryException {
    	if (prefix.startsWith("xml")){
    		throw new NamespaceException("Prefix xml cannot be registered");
    	}
        DatabaseConnection conn = repository.getConnectionProvider().createConnection();
        try {
            registerNamespace(prefix, uri, conn);
            conn.commit();
        } finally {
        	repository.reloadNamespaceRegistry(conn);
            conn.close();
        }
    }



    public void unregisterNamespace(String prefix) throws NamespaceException,
            UnsupportedRepositoryOperationException, AccessDeniedException,
            RepositoryException {
        throw new NamespaceException("Namespace unregistration not supported");
    }

   

    public Long _getNamespaceId(String uri) throws NamespaceException {
        for(Iterator it = namespaces.iterator() ; it.hasNext() ;){
            Namespace n = (Namespace) it.next();
            if (uri.equals(n.getUri())){
                return n.getId();
            }
        }
        throw new NamespaceException("Id for URI "+uri+" not found");
    }

    public String toString(){
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("namespaces",namespaces);
        return builder.toString();
    }

    /**
     * Returns a prefix that is unique among the already registered prefixes.
     *
     * @param uriHint namespace uri that serves as hint for the prefix generation
     * @return a unique prefix
     */
    public String getUniquePrefix(String uriHint) {
        // @todo smarter unique prefix generation
/*
        int number;
        if (uriHint == null || uriHint.length() == 0) {
            number = prefixToURI.size() + 1;
        } else {
            number = uriHint.hashCode();
        }
        return "_pre" + number;
*/
        return "_pre" + (namespaces.size() + 1);
    }


    


}



/*
 * $Log: NamespaceRegistryImpl.java,v $
 * Revision 1.5  2009/02/23 14:30:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2007/10/09 07:34:53  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.3  2007/08/07 14:05:22  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2007/05/21 10:58:02  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.9  2007/03/12 08:24:02  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.8  2007/03/02 09:31:58  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.7  2007/02/26 13:14:39  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.6  2007/02/26 09:46:00  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.5  2007/01/24 08:46:25  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.4  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.3  2006/07/06 09:29:15  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/04/20 11:42:46  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.4  2006/04/13 10:03:44  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/12 08:30:49  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/06 14:45:35  ivgirts
 * PTR #1801059 namespace and node types now cached in Repository
 *
 * Revision 1.1  2006/03/27 14:57:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.15  2006/03/24 08:55:03  ivgirts
 * PTR #1801059 registerNamespace uses connection passed as paramter
 *
 * Revision 1.14  2006/03/14 14:54:07  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.13  2006/03/13 09:24:33  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.12  2006/03/03 11:07:49  ivgirts
 * PTR #1801059 thorws SQLException replaced with throws RepositoryException
 *
 * Revision 1.11  2006/03/03 10:33:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.10  2006/02/27 15:53:55  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.9  2006/02/24 13:27:20  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.8  2006/02/22 12:04:03  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.7  2006/02/20 16:03:49  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.6  2006/02/20 15:32:25  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.5  2006/02/17 13:03:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.4  2006/02/16 15:47:59  dparhomenko
 * PTR#0144983 restructurize
 *
 * Revision 1.3  2006/02/16 13:53:04  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.2  2006/02/13 12:40:40  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 * Revision 1.1  2006/02/10 15:50:23  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */