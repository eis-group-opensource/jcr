/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package javax.jcr.nodetype;

/**
 * The <code>NodeTypeDefinition</code> interface provides methods for
 * discovering the static definition of a node type. These are accessible both
 * before and after the node type is registered. Its subclass
 * <code>NodeType</code> adds methods that are relevant only when the node type
 * is "live"; that is, after it has been registered. Note that the separate
 * <code>NodeDefinition</code> interface only plays a significant role in
 * implementations that support node type registration. In those cases it serves
 * as the superclass of both <code>NodeType</code> and
 * <code>NodeTypeTemplate</code>. In implementations that do not support node
 * type registration, only objects implementing the subinterface
 * <code>NodeType</code> will be encountered.
 */
public interface NodeTypeDefinition {

    /**
     * Returns the name of the node type. In implementations that support node
     * type registration, if this <code>NodeTypeDefinition</code> object is
     * actually a newly-created empty <code>NodeTypeTemplate</code>, then this
     * method will return <code>null</code>.
     */
    public String getName();

    /**
     * Returns the names of the supertypes actually declared in this node type.
     * In implementations that support node type registration, if this
     * <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return an array
     * containing a single string indicating the node type
     * <code>nt:base</code>.
     */
    public String[] getDeclaredSupertypeNames();

    /**
     * Returns <code>true</code> if this is a mixin type; returns
     * <code>false</code> if it is primary. In implementations that support node
     * type registration, if this <code>NodeTypeDefinition</code> object is
     * actually a newly-created empty <code>NodeTypeTemplate</code>, then this
     * method will return <code>false</code>.
     */
    public boolean isMixin();

    /**
     * Returns <code>true</code> if nodes of this type must support orderable
     * child nodes; returns <code>false</code> otherwise. If a node type returns
     * true on a call to this method, then all nodes of that node type
     * <i>must</i> support the method <code>Node.orderBefore</code>. If a node
     * type returns <code>false</code> on a call to this method, then nodes of
     * that node type <i>may</i> support <code>Node.orderBefore</code>. Only the
     * primary node type of a node controls that node's status in this regard.
     * This setting on a mixin node type will not have any effect on the node.
     * In implementations that support node type registration, if this
     * <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return
     * <code>false</code>.
     */
    public boolean hasOrderableChildNodes();

    /**
     * Returns the name of the primary item (one of the child items of the nodes
     * of this node type). If this node has no primary item, then this method
     * returns <code>null</code>. This indicator is used by the method
     * <code>Node.getPrimaryItem()</code>. In implementations that support node
     * type registration, if this <code>NodeTypeDefinition</code> object is
     * actually a newly-created empty <code>NodeTypeTemplate</code>, then this
     * method will return <code>null</code>.
     */
    public String getPrimaryItemName();

    /**
     * Returns an array containing the property definitions actually declared in
     * this node type. In implementations that support node type registration,
     * if this <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return <code>null</code>.
     */
    public PropertyDefinition[] getDeclaredPropertyDefinitions();

    /**
     * Returns an array containing the child node definitions actually declared
     * in this node type. In implementations that support node type
     * registration, if this <code>NodeTypeDefinition</code> object is actually
     * a newly-created empty <code>NodeTypeTemplate</code>, then this method
     * will return <code>null</code>.
     */
    public NodeDefinition[] getDeclaredChildNodeDefinitions();
}
