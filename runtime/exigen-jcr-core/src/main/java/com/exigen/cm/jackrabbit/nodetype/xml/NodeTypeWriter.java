/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype.xml;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;
import javax.xml.parsers.ParserConfigurationException;

import com.exigen.cm.jackrabbit.core.util.DOMBuilder;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.NodeDef;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.nodetype.ValueConstraint;
import com.exigen.cm.jackrabbit.value.InternalValue;

/**
 * Node type definition writer. This class is used to write the
 * persistent node type definition files used by Jackrabbit.
 */
public final class NodeTypeWriter {

    /**
     * Writes a node type definition file. The file contents are written
     * to the given output stream and will contain the given node type
     * definitions. The given namespace registry is used for namespace
     * mappings.
     *
     * @param xml XML output stream
     * @param registry namespace registry
     * @param types node types
     * @throws IOException         if the node type definitions cannot
     *                             be written
     * @throws RepositoryException on repository errors
     */
    public static void write(
            OutputStream xml, NodeTypeDef[] types, NamespaceRegistry registry, boolean indenting)
            throws IOException, RepositoryException {
        try {
            NodeTypeWriter writer = new NodeTypeWriter(registry, indenting);
            for (int i = 0; i < types.length; i++) {
                writer.addNodeTypeDef(types[i]);
            }
            writer.write(xml);
        } catch (ParserConfigurationException e) {
            throw new IOException(e.getMessage());
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(
                    "Invalid namespace reference in a node type definition", e);
        }
    }

    /** The node type document builder. */
    private final DOMBuilder builder;

    /** The namespace resolver. */
    private final NamespaceResolver resolver;

    /**
     * Creates a node type definition file writer. The given namespace
     * registry is used for the XML namespace bindings.
     *
     * @param registry namespace registry
     * @throws ParserConfigurationException if the node type definition
     *                                      document cannot be created
     * @throws RepositoryException          if the namespace mappings cannot
     *                                      be retrieved from the registry
     */
    private NodeTypeWriter(NamespaceRegistry registry, boolean indenting)
            throws ParserConfigurationException, RepositoryException {
        builder = new DOMBuilder(Constants.NODETYPES_ELEMENT, indenting);

        String[] prefixes = registry.getPrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            if (!"".equals(prefixes[i])) {
                String uri = registry.getURI(prefixes[i]);
                builder.setAttribute("xmlns:" + prefixes[i], uri);
            }
        }

        resolver = new AdditionalNamespaceResolver(registry);
    }

    /**
     * Builds a node type definition element under the current element.
     *
     * @param def node type definition
     * @throws RepositoryException       if the default property values
     *                                   cannot be serialized
     * @throws NoPrefixDeclaredException if the node type definition contains
     *                                   invalid namespace references
     */
    private void addNodeTypeDef(NodeTypeDef def)
            throws RepositoryException, NoPrefixDeclaredException {
        builder.startElement(Constants.NODETYPE_ELEMENT);

        // simple attributes
        builder.setAttribute(
                Constants.NAME_ATTRIBUTE, def.getName().toJCRName(resolver));
        builder.setAttribute(
                Constants.ISMIXIN_ATTRIBUTE, def.isMixin());
        builder.setAttribute(
                Constants.HASORDERABLECHILDNODES_ATTRIBUTE,
                def.hasOrderableChildNodes());

        if (def.getTableName() != null && def.getTableName().length() > 0){
	        builder.setAttribute(
	                        Constants.TABLENAME_ATTRIBUTE,
	                        def.getTableName());        
        }

        // primary item name
        QName item = def.getPrimaryItemName();
        if (item != null) {
            builder.setAttribute(
                    Constants.PRIMARYITEMNAME_ATTRIBUTE,
                    item.toJCRName(resolver));
        } else {
            builder.setAttribute(Constants.PRIMARYITEMNAME_ATTRIBUTE, "");
        }

        // supertype declarations
        QName[] supertypes = def.getSupertypes();
        if (supertypes != null && supertypes.length > 0) {
            builder.startElement(Constants.SUPERTYPES_ELEMENT);
            for (int i = 0; i < supertypes.length; i++) {
                builder.addContentElement(
                        Constants.SUPERTYPE_ELEMENT,
                        supertypes[i].toJCRName(resolver));
            }
            builder.endElement();
        }

        // property definitions
        PropDef[] properties = def.getPropertyDefs();
        for (int i = 0; i < properties.length; i++) {
            addPropDef(properties[i]);
        }

        // child node definitions
        NodeDef[] nodes = def.getChildNodeDefs();
        for (int i = 0; i < nodes.length; i++) {
            addChildNodeDef(nodes[i]);
        }

        builder.endElement();
    }

    /**
     * Builds a property definition element under the current element.
     *
     * @param def property definition
     * @throws RepositoryException       if the default values cannot
     *                                   be serialized
     * @throws NoPrefixDeclaredException if the property definition contains
     *                                   invalid namespace references
     */
    private void addPropDef(PropDef def)
            throws RepositoryException, NoPrefixDeclaredException {
        builder.startElement(Constants.PROPERTYDEFINITION_ELEMENT);

        // simple attributes
        builder.setAttribute(
                Constants.NAME_ATTRIBUTE, def.getName().toJCRName(resolver));
        builder.setAttribute(
                Constants.AUTOCREATED_ATTRIBUTE, def.isAutoCreated());
        builder.setAttribute(
                Constants.MANDATORY_ATTRIBUTE, def.isMandatory());
        builder.setAttribute(
                Constants.PROTECTED_ATTRIBUTE, def.isProtected());
        builder.setAttribute(
                Constants.ONPARENTVERSION_ATTRIBUTE,
                OnParentVersionAction.nameFromValue(def.getOnParentVersion()));
        builder.setAttribute(
                Constants.MULTIPLE_ATTRIBUTE, def.isMultiple());
        builder.setAttribute(
                Constants.REQUIREDTYPE_ATTRIBUTE,
                PropertyType283.nameFromValue(def.getRequiredType()));        
        /*builder.setAttribute(
                Constants.COLUMNNAME_ATTRIBUTE,
                def.getColumnName());*/
        if (def.isIndexable()){
            builder.setAttribute(
                    Constants.COLUMNNAME_INDEXABLE,
                    def.isIndexable());
        }
        if (def.isFullTextSearch()){
            builder.setAttribute(
                    Constants.COLUMNNAME_INDEXABLE_FTS,
                    def.isFullTextSearch());
        }

        // value constraints
        ValueConstraint[] constraints = def.getValueConstraints();
        if (constraints != null && constraints.length > 0) {
            builder.startElement(Constants.VALUECONSTRAINTS_ELEMENT);
            for (int i = 0; i < constraints.length; i++) {
                builder.addContentElement(
                        Constants.VALUECONSTRAINT_ELEMENT,
                        constraints[i].getDefinition(resolver));
            }
            builder.endElement();
        }

        // default values
        InternalValue[] defaults = def.getDefaultValues();
        if (defaults != null && defaults.length > 0) {
            builder.startElement(Constants.DEFAULTVALUES_ELEMENT);
            for (int i = 0; i < defaults.length; i++) {
                builder.addContentElement(
                        Constants.DEFAULTVALUE_ELEMENT,
                        defaults[i].toJCRValue(resolver).getString());
            }
            builder.endElement();
        }

        builder.endElement();
    }

    /**
     * Builds a child node definition element under the current element.
     *
     * @param def child node definition
     * @throws NoPrefixDeclaredException if the child node definition contains
     *                                   invalid namespace references
     */
    private void addChildNodeDef(NodeDef def)
            throws NoPrefixDeclaredException {
        builder.startElement(Constants.CHILDNODEDEFINITION_ELEMENT);

        // simple attributes
        builder.setAttribute(
                Constants.NAME_ATTRIBUTE, def.getName().toJCRName(resolver));
        builder.setAttribute(
                Constants.AUTOCREATED_ATTRIBUTE, def.isAutoCreated());
        builder.setAttribute(
                Constants.MANDATORY_ATTRIBUTE, def.isMandatory());
        builder.setAttribute(
                Constants.PROTECTED_ATTRIBUTE, def.isProtected());
        builder.setAttribute(
                Constants.ONPARENTVERSION_ATTRIBUTE,
                OnParentVersionAction.nameFromValue(def.getOnParentVersion()));
        builder.setAttribute(
                Constants.SAMENAMESIBLINGS_ATTRIBUTE, def.allowsSameNameSiblings());

        // default primary type
        QName type = def.getDefaultPrimaryType();
        if (type != null) {
            builder.setAttribute(
                    Constants.DEFAULTPRIMARYTYPE_ATTRIBUTE,
                    type.toJCRName(resolver));
        } else {
            builder.setAttribute(Constants.DEFAULTPRIMARYTYPE_ATTRIBUTE, "");
        }

        // required primary types
        QName[] requiredTypes = def.getRequiredPrimaryTypes();
        builder.startElement(Constants.REQUIREDPRIMARYTYPES_ELEMENT);
        for (int i = 0; i < requiredTypes.length; i++) {
            builder.addContentElement(
                    Constants.REQUIREDPRIMARYTYPE_ELEMENT,
                    requiredTypes[i].toJCRName(resolver));
        }
        builder.endElement();

        builder.endElement();
    }

    /**
     * Writes the node type definition document to the given output stream.
     *
     * @param xml XML output stream
     * @throws IOException if the node type document could not be written
     */
    private void write(OutputStream xml) throws IOException {
        builder.write(xml);
    }

}
