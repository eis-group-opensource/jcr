/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.web;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.RepositoryProvider;
import com.exigen.cm.impl.RepositoryImpl;

public class JCRServletListener  implements ServletContextListener {

	private Log log = LogFactory.getLog(JCRServletListener.class);
	
	public static final String REPOSITORIES_KEY = "http://jcr.exigen.com/repositories/list";
	public static final String REPOSITORIES_NAMES = "http://jcr.exigen.com/repositories/names";
	public static final String REPOSITORIES = "http://jcr.exigen.com/repositories/jndi";

	public static final String CONFIGURATION_FILE_NAME = "repository-bindings.properties";
	public static final String CONFIGURATION_PARAMETER_NAME = "repositories";
	
	public void contextInitialized(ServletContextEvent event) {
        try {

		ServletContext context = event.getServletContext();
		
    	HashSet<String> repositories = new HashSet<String>();
    	HashMap<String, String> repositoryNames = new HashMap<String, String>();
    	HashMap<RepositoryImpl, Name> repositoryObjects = new HashMap<RepositoryImpl, Name>();
    	
    	RepositoryList reps = new RepositoryList();
    	
    	String repsConfig = null;
    	
    	try {
	    	InputStream stream = JCRHelper.getInputStream(CONFIGURATION_FILE_NAME, false);
	    	if (stream != null){
	    		Properties pp = new Properties();
	    		pp.load(stream);
	    		repsConfig = pp.getProperty(CONFIGURATION_PARAMETER_NAME);
	    		if (repsConfig != null){
	    			log.debug("Repository List getted from "+CONFIGURATION_FILE_NAME);
	    		}
	    	}
    	} catch (Exception exc){
    		exc.printStackTrace();
    	}
    	
    	if (repsConfig == null){
    		repsConfig = context.getInitParameter(CONFIGURATION_PARAMETER_NAME);
    		if (repsConfig != null){
    			log.debug("Repository List getted from web.xml");
    			
    		}
    	}
    	log.debug("Initialize repositories :"+repsConfig);
    	if (repsConfig != null && repsConfig.trim().length() > 0){
    		StringTokenizer st = new StringTokenizer(repsConfig,",");
    		while (st.hasMoreElements()){
    			String next = st.nextToken();
    			next = next.trim();
    			if (next.length() > 0){
    				repositories.add(next);
    			}
    		}
    	}
    	if (repositories.size() == 0){
			log.debug("Repository List not found, initialize default repository");
    		repositories.add("default");
    	}
    	
    	
        	
            InitialContext ctx = new InitialContext();
            
            System.out.println("--------------------------------");
            RepositoryProvider provider = RepositoryProvider.getInstance();
            HashSet<String> registeredJNDINames = new HashSet<String>();
            for(String repName:repositories){
                RepositoryImpl rep = (RepositoryImpl)provider.getRepository(repName);
                reps.add(rep);
                String jndiName = rep.getConfigurationProperty(Constants.REPOSITORY_JNDI_NAME);
                if (registeredJNDINames.contains(jndiName)){
                	throw new ServletException("Duplicate JNDI name "+jndiName+" for repository "+repName);
                }
                System.out.println("Parse "+jndiName);
                Name fullName = ctx.getNameParser("").parse(jndiName);
				repositoryNames.put(repName, jndiName);
				System.out.println("Bind "+fullName+" with value "+rep);
                NonSerializableFactory.rebind(fullName, rep, true);
                //NonSerializableFactory.rebind(jndiName,rep);
                repositoryObjects.put(rep, fullName);
            }
            context.setAttribute(REPOSITORIES_KEY, reps);
            context.setAttribute(REPOSITORIES, repositoryObjects);
            context.setAttribute(REPOSITORIES_NAMES, repositoryNames);
            	
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating repository", e);
        } catch (Throwable th){
            th.printStackTrace();
            throw new RuntimeException("Error binding repository", th);
        }
		
    }

    public void contextDestroyed(ServletContextEvent event) {
    	ServletContext context = event.getServletContext();
    	Object source = context.getAttribute(REPOSITORIES_KEY);
    	//HashMap<RepositoryImpl, Name> repositoryObjects = (HashMap<RepositoryImpl, Name>) context.getAttribute(REPOSITORIES);
    	if (source instanceof RepositoryList){
    		for(RepositoryImpl r:(RepositoryList) source){
    			r.shutdown();
    			/*Name key = repositoryObjects.get(r);
    			if (key != null){
    				try {
    					NonSerializableFactory.unbind(key);
    				} catch (Exception exc){
    					exc.printStackTrace();
    				}
    			}*/
    		}
    		HashMap<String, String> repositoryNames = (HashMap<String, String>) context.getAttribute(JCRServletListener.REPOSITORIES_NAMES);
    		for(String name: repositoryNames.keySet()){
                String jndiName = repositoryNames.get(name);
                try {
                	System.out.println("Unbind "+jndiName);
                    NonSerializableFactory.unbind(jndiName);
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
            }    		
    	}
    }

}

