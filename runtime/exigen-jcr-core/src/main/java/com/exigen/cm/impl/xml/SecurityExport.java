/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.xml;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.SecurityEntry;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.security.SecurityPermission;
import com.exigen.cm.jackrabbit.name.QName;


/**
 * TODO Put class description here
 * 
 */
public class SecurityExport implements SecurityXMLConstants {

    public static final String CDATA_TYPE = "CDATA";
    /**
     * the content handler to feed the SAX events to
     */
    private ContentHandler contentHandler = null;
    private Node node = null;
    private boolean noRecurse = false;
    private SessionImpl session = null;
	private boolean skipSystem;
    
    public SecurityExport(ContentHandler contentHandler, Node node, boolean noRecurse,boolean skipSystem) throws RepositoryException {
        this.contentHandler = contentHandler;
        this.node = node;
        this.session = (SessionImpl)node.getSession();
        this.skipSystem = skipSystem;
        System.out.println("");
    }
    
    /**
     * Serializes the hierarchy of nodes and properties.
     *
     * @throws RepositoryException if an error occurs while traversing the hierarchy
     * @throws SAXException        if an error occured while feeding the events
     *                             to the content handler
     */
    public void serialize(boolean startDocument) throws RepositoryException, SAXException {
        // start document and declare namespaces
    	if (startDocument){
    		contentHandler.startDocument();
    	}

        contentHandler.startElement("", "", ROOT_ELEMENT, new AttributesImpl()); 
        // serialize node and subtree
        process(node, 0);
        
        contentHandler.endElement("", "", ROOT_ELEMENT);
        if (startDocument){
        	contentHandler.endDocument();
        }
    }    
    
    
    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected void process(Node node, int level) throws RepositoryException, SAXException {
        
    	if (((NodeImpl)node).getQName().equals(QName.JCR_SYSTEM) && node.getDepth() == 1 && skipSystem){
    		return;
    	}
        // enter node        
        entering(node, level);
        
        if (!noRecurse) {
            // child nodes
            NodeIterator nodeIter = node.getNodes();
            while (nodeIter.hasNext()) {
                Node childNode = nodeIter.nextNode();
                // recurse
                process(childNode, level + 1);
            }
        }
        // leaving node
        leaving(node, level);
    }
    
    private int counter = 0;
    
    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    private void entering(Node node, int level) throws RepositoryException, SAXException {
    	counter++;
    	if (counter > 100){
    		System.out.print(".");
    		counter = 0;
    	}

        NodeImpl nodeImpl = (NodeImpl)node;
        //if node id <> node security id, then export node security        
        if (nodeImpl.getNodeId().equals(nodeImpl.getSecurityId())) {
            
            AttributesImpl attrs = new AttributesImpl();        
            attrs.addAttribute("", "", NODE_PATH_ATTR, CDATA_TYPE, node.getPath());
            contentHandler.startElement("", "", NODE_ELEMENT, attrs);        
            contentHandler.startElement("", "", ACL_ELEMENT, new AttributesImpl());            
            
            List<SecurityEntry> list = session.getSecurityManager().getSecurityEntriesBySecurityId(nodeImpl.getSecurityId(), true);            
            for (int i = 0; i < list.size(); i++) {
                SecurityEntry entry = list.get(i);
                
                String aclEntryElement = (entry.getGroupId() == null) ? USER_ELEMENT : GROUP_ELEMENT;                
                String id = (entry.getGroupId() == null) ? entry.getUserId() : entry.getGroupId();
                
                AttributesImpl securityAtrrs = new AttributesImpl();
                securityAtrrs.addAttribute("", "", ID_ATTR, CDATA_TYPE, id);
                for(SecurityPermission p : SecurityPermission.values()){
                	if (entry.getPermission(p) != null){
                        securityAtrrs.addAttribute("", "", p.getExportName(), CDATA_TYPE, entry.getPermission(p).toString());
                        NodeImpl pn = ((SessionImpl)node.getSession()).getNodeManager().buildNode(nodeImpl.getSecurityId());
                        securityAtrrs.addAttribute("", "", p.getExportName()+"Parent", CDATA_TYPE, pn.getPath());
                	}
                }
                contentHandler.startElement("", "", aclEntryElement, securityAtrrs);
                contentHandler.endElement("", "", aclEntryElement);
            }            
            contentHandler.endElement("", "", ACL_ELEMENT);
            
        } else {
            /*AttributesImpl attrs = new AttributesImpl();        
            attrs.addAttribute("", "", NODE_PATH_ATTR, CDATA_TYPE, node.getPath());
            NodeImpl pn = ((SessionImpl)node.getSession()).getNodeManager().buildNode(nodeImpl.getSecurityId());
            attrs.addAttribute("", "", NODE_PATH_FROM_ATTR, CDATA_TYPE, pn.getPath());
            contentHandler.startElement("", "", NODE_ELEMENT, attrs);*/       
            
        }
    }
    
    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    private void leaving(Node node, int level) throws RepositoryException, SAXException {
        NodeImpl nodeImpl = (NodeImpl)node;
        //if node id <> node security id, then finish node element        
        if (nodeImpl.getNodeId().equals(nodeImpl.getSecurityId())) {        
            contentHandler.endElement("", "", NODE_ELEMENT);        
        }
    }       
}


/*
 * $Log: SecurityExport.java,v $
 * Revision 1.2  2009/01/09 13:54:45  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 09:00:44  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.7  2006/10/17 10:47:18  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.6  2006/09/26 12:31:45  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.5  2006/08/21 11:03:22  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.4  2006/08/10 10:26:05  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.3  2006/06/02 07:21:44  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.2  2006/04/24 16:24:22  ivgirts
 * PTR #1800998 export security updated
 *
 * Revision 1.1  2006/04/17 06:47:06  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/10 11:30:17  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.1  2006/04/06 14:34:22  ivgirts
 * PTR #1800998 added Security export/import
 *
 */
