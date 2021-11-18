/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.xml;

import java.util.zip.ZipOutputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.jackrabbit.BaseException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.QName;

/**
 * <code>AbstractSAXEventGenerator</code> serves as the base class for
 * <code>SysViewSAXEventGenerator</code> and <code>DocViewSAXEventGenerator</code>
 * <p/>
 * It traverses a tree of <code>Node</code> & <code>Property</code>
 * instances, and calls the abstract methods
 * <ul>
 * <li><code>{@link #entering(Node, int)}</code></li>
 * <li><code>{@link #enteringProperties(Node, int)}</code></li>
 * <li><code>{@link #leavingProperties(Node, int)}</code></li>
 * <li><code>{@link #leaving(Node, int)}</code></li>
 * <li><code>{@link #entering(Property, int)}</code></li>
 * <li><code>{@link #leaving(Property, int)}</code></li>
 * </ul>
 * for every item it encounters.
 */
abstract class AbstractSAXEventGenerator {

    private static Log log = LogFactory.getLog(AbstractSAXEventGenerator.class);

    /**
     * the session to be used for resolving namespace mappings
     */
    protected final Session session;
    /**
     * the session's namespace resolver
     */
    protected final NamespaceResolver nsResolver;

    /**
     * the content handler to feed the SAX events to
     */
    protected final ContentHandler contentHandler;

    protected final Node startNode;
    protected final boolean skipBinary;
    protected final boolean noRecurse;
    protected final ZipOutputStream zout;

    /**
     * The jcr:primaryType property name (allowed for session-local prefix mappings)
     */
    protected final String jcrPrimaryType;
    /**
     * The jcr:mixinTypes property name (allowed for session-local prefix mappings)
     */
    protected final String jcrMixinTypes;
    /**
     * The jcr:uuid property name (allowed for session-local prefix mappings)
     */
    protected final String jcrUUID;
    /**
     * The jcr:root node name (allowed for session-local prefix mappings)
     */
    protected final String jcrRoot;
    /**
     * The jcr:xmltext node name (allowed for session-local prefix mappings)
     */
    protected final String jcrXMLText;
    /**
     * The jcr:xmlCharacters property name (allowed for session-local prefix mappings)
     */
    protected final String jcrXMLCharacters;

	private boolean skipSystem;

//    private String[] stopTypes;
	private NodeExportAcceptor exportAcceptor;
	
    
    
    /**
     * returns always <code>true</code>
     */
    private static final NodeExportAcceptor DEFAULT_ACCEPTOR = new NodeExportAcceptor(){
        public boolean isExportable(Node n) {return true;}
    };

    /**
     * Constructor
     *
     * @param node           the node state which should be serialized
     * @param noRecurse      if true, only <code>node</code> and its properties will
     *                       be serialized; otherwise the entire hierarchy starting with
     *                       <code>node</code> will be serialized.
     * @param skipBinary     flag governing whether binary properties are to be serialized.
     * @param contentHandler the content handler to feed the SAX events to
     * @throws RepositoryException if an error occurs
     */
    protected AbstractSAXEventGenerator(Node node, boolean noRecurse,
                                        boolean skipBinary,
                                        ZipOutputStream zout,
                                        ContentHandler contentHandler, boolean skipSystem, NodeExportAcceptor exportAcceptor)
            throws RepositoryException {
        startNode = node;
        session = node.getSession();
        nsResolver = ((SessionImpl)session)._getWorkspace()._getNamespaceRegistry();
        this.skipSystem = skipSystem;

        this.contentHandler = contentHandler;
        this.skipBinary = skipBinary;
        this.noRecurse = noRecurse;
        this.zout = zout;
        
        
        this.exportAcceptor = exportAcceptor == null 
                                ? DEFAULT_ACCEPTOR 
                                : exportAcceptor;

        // resolve the names of some wellknown properties
        // allowing for session-local prefix mappings
        try {
            jcrPrimaryType = QName.JCR_PRIMARYTYPE.toJCRName(nsResolver);
            jcrMixinTypes = QName.JCR_MIXINTYPES.toJCRName(nsResolver);
            jcrUUID = QName.JCR_UUID.toJCRName(nsResolver);
            jcrRoot = QName.JCR_ROOT.toJCRName(nsResolver);
            jcrXMLText = QName.JCR_XMLTEXT.toJCRName(nsResolver);
            jcrXMLCharacters = QName.JCR_XMLCHARACTERS.toJCRName(nsResolver);
        } catch (BaseException e) {
            // should never get here...
            String msg = "internal error: failed to resolve namespace mappings";
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }
    
    /**
     * @param node 
     * @param noRecurse 
     * @param skipBinary 
     * @param contentHandler 
     * @throws RepositoryException 
     * @see #AbstractSAXEventGenerator(Node, boolean, boolean, ZipOutputStream, ContentHandler)
     */
    protected AbstractSAXEventGenerator(Node node, boolean noRecurse,
                    boolean skipBinary, ContentHandler contentHandler, boolean skipSystem, NodeExportAcceptor exportAcceptor)
                    throws RepositoryException {
        this(node, noRecurse, skipBinary, null, contentHandler, skipSystem, exportAcceptor);
    }


    /**
     * Serializes the hierarchy of nodes and properties.
     *
     * @throws RepositoryException if an error occurs while traversing the hierarchy
     * @throws SAXException        if an error occured while feeding the events
     *                             to the content handler
     */
    public void serialize() throws RepositoryException, SAXException {
        // start document and declare namespaces
        contentHandler.startDocument();
        startNamespaceDeclarations();

        // serialize node and subtree
        process(startNode, 0);

        // clear namespace declarations and end document
        endNamespaceDeclarations();
        contentHandler.endDocument();
    }

    /**
     * @throws RepositoryException
     * @throws SAXException
     */
    protected void startNamespaceDeclarations()
            throws RepositoryException, SAXException {
        // start namespace declarations
        String[] prefixes = session.getNamespacePrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            String prefix = prefixes[i];
            if (QName.NS_XML_PREFIX.equals(prefix)) {
                // skip 'xml' prefix as this would be an illegal namespace declaration
                continue;
            }
            String uri = session.getNamespaceURI(prefix);
            contentHandler.startPrefixMapping(prefix, uri);
        }
    }

    /**
     * @throws RepositoryException
     * @throws SAXException
     */
    protected void endNamespaceDeclarations()
            throws RepositoryException, SAXException {
        // end namespace declarations
        String[] prefixes = session.getNamespacePrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            String prefix = prefixes[i];
            if (QName.NS_XML_PREFIX.equals(prefix)) {
                // skip 'xml' prefix as this would be an illegal namespace declaration
                continue;
            }
            contentHandler.endPrefixMapping(prefix);
        }
    }

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected void process(Node node, int level)
            throws RepositoryException, SAXException {
        // enter node
    	NodeImpl n = (NodeImpl) node;
    	if (skipSystem && n.getQName().equals(QName.JCR_SYSTEM) && n.getDepth() == 1){
    		return;
    	}
//    	for(String type : stopTypes) // nodes with type listed in stopTypes should not be exported
//    	    if(node.isNodeType(type))
//    	        return;
    	if(!exportAcceptor.isExportable(node))
    	    return;
    	
        entering(node, level);

        // enter properties
        enteringProperties(node, level);

        // serialize jcr:primaryType, jcr:mixinTypes & jcr:uuid first:
        // jcr:primaryType
        if (node.hasProperty(jcrPrimaryType)) {
            process(node.getProperty(jcrPrimaryType), level + 1);
        } else {
            String msg = "internal error: missing jcr:primaryType property on node "
                    + node.getPath();
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        // jcr:mixinTypes
        if (node.hasProperty(jcrMixinTypes)) {
            process(node.getProperty(jcrMixinTypes), level + 1);
        }
        // jcr:uuid
        if (node.hasProperty(jcrUUID)) {
            process(node.getProperty(jcrUUID), level + 1);
        }

        // serialize remaining properties
        PropertyIterator propIter = node.getProperties();
        while (propIter.hasNext()) {
            Property prop = propIter.nextProperty();
            String name = prop.getName();
            if (jcrPrimaryType.equals(name)
                    || jcrMixinTypes.equals(name)
                    || jcrUUID.equals(name)) {
                continue;
            }
            // serialize property
            process(prop, level + 1);
        }

        // leaving properties
        leavingProperties(node, level);

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

    /**
     * @param prop
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected void process(Property prop, int level)
            throws RepositoryException, SAXException {
        // serialize property
        entering(prop, level);
        leaving(prop, level);
    }

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void entering(Node node, int level)
            throws RepositoryException, SAXException;

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void enteringProperties(Node node, int level)
            throws RepositoryException, SAXException;

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void leavingProperties(Node node, int level)
            throws RepositoryException, SAXException;

    /**
     * @param node
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void leaving(Node node, int level)
            throws RepositoryException, SAXException;

    /**
     * @param prop
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void entering(Property prop, int level)
            throws RepositoryException, SAXException;

    /**
     * @param prop
     * @param level
     * @throws RepositoryException
     * @throws SAXException
     */
    protected abstract void leaving(Property prop, int level)
            throws RepositoryException, SAXException;

}
