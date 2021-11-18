/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.web;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;

import javax.naming.InitialContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.exigen.cm.impl.RepositoryImpl;

public class RepositoryBindServlet extends HttpServlet implements Servlet {

    //private HashMap<String, String> repositoryNames = new HashMap<String, String>();
    
    
    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
        	
        	HashMap<String, String> repositoryNames = (HashMap<String, String>) getServletContext().getAttribute(JCRServletListener.REPOSITORIES_NAMES); 
        	
            if ("true".equals(request.getParameter("gc"))){
                System.gc();
            }
            write(response, "<html><body>");
            
            write(response, "<h1>Repositories</h1><br/>");
            
            write(response, "<table border='1'>");
            write(response, "<tr><td>");
            write(response, "Repository Name");
            write(response, "</td><td>");
            write(response, "JNDI name");
            write(response, "</td><td>");
            write(response, "Status");                    
            write(response, "</td><td>");
            write(response, "Session Count");
            write(response, "</td></tr>");
            for(String name: repositoryNames.keySet()){
                String jndiName = repositoryNames.get(name);
                write(response, "<tr><td>");
                write(response, name);
                write(response, "</td><td>");
                write(response, jndiName);
                write(response, "</td><td>");
                RepositoryImpl rep = null;
                try {
                    InitialContext ctx = new InitialContext();
                    rep = (RepositoryImpl) ctx.lookup(jndiName);
                    write(response, "OK");                    
                } catch (Exception exc){
                    write(response, exc.getMessage());                    
                }
                write(response, "</td><td>");
                if (rep != null){
                    write(response, Integer.toString(rep.getSessionCount()));
                } else {
                    write(response, "&nbsp;");
                }
                write(response, "</td></tr>");
            }
            write(response, "</table>");
            write(response, "<form method='GET'><input type='hidden' name='gc' value='true'/>");
            write(response, "<form method='GET'><input type='submit' value='Run Garbage Collector'/>");
            write(response, "</form>");
            write(response, "</body></html>");
        } catch (Throwable exc) {
        	System.out.println("-------------------------------------------------------");
            exc.printStackTrace();
            exc.printStackTrace(new PrintStream(response.getOutputStream()));
        }
    }

    private void write(HttpServletResponse response, String value) throws IOException {
        response.getOutputStream().write(
                value.getBytes());
    }

    /*
     * (non-Java-doc)
     * 
     * @see javax.servlet.Servlet#init(ServletConfig arg0)
     */
    public void init(ServletConfig config) throws ServletException {
    /*	HashSet<String> repositories = new HashSet<String>();
    	RepositoryList reps = new RepositoryList();
    	

    	String repsConfig = config.getInitParameter("repositories");
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
    		repositories.add("default");
    	}
    	
        try {
        	
        	
            InitialContext ctx = new InitialContext();
            
            RepositoryProvider provider = RepositoryProvider.getInstance();
            HashSet<String> registeredJNDINames = new HashSet<String>();
            for(String repName:repositories){
                RepositoryImpl rep = (RepositoryImpl)provider.getRepository(repName);
                reps.add(rep);
                String jndiName = rep.getConfigurationProperty(Constants.REPOSITORY_JNDI_NAME);
                if (registeredJNDINames.contains(jndiName)){
                	throw new ServletException("Duplicate JNDI name "+jndiName+" for repository "+repName);
                }
                Name fullName = ctx.getNameParser("").parse(jndiName);
                repositoryNames.put(repName, jndiName);
                NonSerializableFactory.rebind(fullName, rep, true);
            }
            config.getServletContext().setAttribute(REPOSITORIES_KEY, reps);
            	
        } catch (NamingException e) {
            e.printStackTrace();
            throw new ServletException("Error binding repository", e);
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new ServletException("Error creating repository", e);
        } catch (Throwable th){
            th.printStackTrace();
            throw new ServletException("Error binding repository", th);
        }
*/
    }

    @Override
    public void destroy() {
        /*for(String name: repositoryNames.keySet()){
            String jndiName = repositoryNames.get(name);
            try {
                NonSerializableFactory.unbind(jndiName);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }*/
    }

}

/*
 * $Log: RepositoryBindServlet.java,v $
 * Revision 1.1  2007/04/26 08:58:19  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.11  2007/02/26 13:14:51  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.10  2007/02/22 09:24:17  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.9  2006/11/30 11:00:07  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.8  2006/10/30 15:03:46  dparhomenko
 * PTR#1803272 a lot of fixes
 *
 * Revision 1.7  2006/10/17 10:46:55  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.6  2006/10/13 09:20:32  dparhomenko
 * PTR#0148476 fix exception text
 *
 * Revision 1.5  2006/04/21 12:11:40  dparhomenko
 * PTR#0144983 build procedure
 *
 * Revision 1.4  2006/04/20 11:43:13  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.3  2006/04/19 13:13:40  dparhomenko
 * PTR#0144983 tests
 *
 * Revision 1.2  2006/04/18 12:53:46  zahars
 * PTR#0144983 default value for repository JNDI name moved to RepositoryProviderImpl
 *
 * Revision 1.1  2006/04/17 06:47:05  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/13 10:03:42  dparhomenko
 * PTR#0144983 restructurization
 *
 */