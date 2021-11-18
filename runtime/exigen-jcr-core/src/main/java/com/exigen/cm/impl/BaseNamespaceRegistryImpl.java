/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.jackrabbit.name.AbstractNamespaceResolver;

public class BaseNamespaceRegistryImpl extends AbstractNamespaceResolver
		implements NamespaceRegistry {

	protected ArrayList<Namespace> namespaces = new ArrayList<Namespace>();

	public BaseNamespaceRegistryImpl() {

	}

	public String[] getPrefixes() throws RepositoryException {
		ArrayList<String> result = new ArrayList<String>();
		for (Namespace n : namespaces) {
			result.add(n.getPrefix());
		}
		return result.toArray(new String[result.size()]);
	}

	public String[] getURIs() throws RepositoryException {
		ArrayList<String> result = new ArrayList<String>();
		for (Namespace n : namespaces) {
			result.add(n.getUri());
		}
		return result.toArray(new String[result.size()]);
	}

	public String getURIById(String prefixId) {
		if ("".equals(prefixId)) {
			return "";
		}
		Long id = new Long(prefixId);
		Namespace n = _getById(id);
		if (n == null) {
			throw new RuntimeException("Prefix id " + prefixId + " not found");
		}
		return n.getUri();
	}

	public boolean hasPrefix(String prefix) {
		try {
			if (getURI(prefix) != null) {
				return true;
			}
		} catch (Exception e) {
		}
		return false;
	}

	public Namespace _getById(Long id) {
		for (Iterator it = namespaces.iterator(); it.hasNext();) {
			Namespace n = (Namespace) it.next();
			if (id.equals(n.getId())) {
				return n;
			}
		}
		return null;
	}

	public Namespace _getByPrefix(String prefix) {
		for (Iterator it = namespaces.iterator(); it.hasNext();) {
			Namespace n = (Namespace) it.next();
			if (prefix.equals(n.getPrefix())) {
				return n;
			}
		}
		return null;
	}

	public String getURI(String prefix) throws NamespaceException {
		if ("".equals(prefix)) {
			return "";
		}
		Namespace n = _getByPrefix(prefix);
		if (n == null) {
			throw new NamespaceException("Prefix " + prefix + " not found");
		}
		return n.getUri();
	}

	public String getPrefix(String uri) throws NamespaceException {
		if ("".equals(uri)) {
			return "";
		}
		Namespace n = _getByURI(uri);
		if (n == null) {
			throw new NamespaceException("URI " + uri + " not found");
		}
		return n.getPrefix();
	}

	public Namespace _getByURI(String uri) {
		for (Iterator it = namespaces.iterator(); it.hasNext();) {
			Namespace n = (Namespace) it.next();
			if (uri.equals(n.getUri())) {
				return n;
			}
		}
		return null;
	}

	List<Namespace> getNamespaces() {
		return namespaces;
	}

	public void registerNamespace(String prefix, String uri)
			throws NamespaceException, UnsupportedRepositoryOperationException,
			AccessDeniedException, RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void unregisterNamespace(String prefix) throws NamespaceException,
			UnsupportedRepositoryOperationException, AccessDeniedException,
			RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void loadNamespaces(DatabaseConnection conn)
			throws RepositoryException {
		// TODO load all namespaces
		namespaces.clear();
		DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(
				Constants.TABLE_NAMESPACE, true);
		st.execute(conn);
		while (st.hasNext()) {
			HashMap map = st.nextRow();
			// build namespace from map
			Long id = (Long) map.get(Constants.FIELD_ID);
			String prefix = (String) map.get(Constants.TABLE_NAMESPACE__PREFIX);
			String uri = (String) map.get(Constants.TABLE_NAMESPACE__URI);

			addNamespace(id, prefix, uri);
		}

		addNamespace(null, "", "");
		st.close();
	}

	protected void addNamespace(Long id, String prefix, String uri) {
		if (prefix.equals("")) {
			for (Namespace n : namespaces) {
				if (n.getPrefix().equals("")) {
					return;
				}
			}
		}
		namespaces.add(new Namespace(id, prefix, uri));

	}
	
public class Namespace {
        
        private Long id;
        private String prefix;
        private String uri;

        public Namespace(Long id, String prefix, String uri){
            this.id = id;
            this.prefix = prefix;
            this.uri = uri;
        }

        public Long getId() {
            return id;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getUri() {
            return uri;
        }
        
        public String toString(){
            ToStringBuilder builder = new ToStringBuilder(this);
            builder.append("id", id);
            builder.append("prefix", prefix);
            builder.append("uri", uri);
            return builder.toString();
        }
        
    }

}
