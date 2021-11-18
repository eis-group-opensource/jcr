/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package javax.jcr.nodetype;

import java.util.List;

/**
 * The <code>NodeTypeTemplate</code> interface represents a simple container
 * structure used to define node types which are then registered through the
 * <code>NodeTypeManager.registerNodeType</code> method.
 * <p/>
 * <code>NodeTypeTemplate</code>, like <code>NodeType</code>, is a subclass of
 * <code>NodeTypeDefinition</code> so it shares with <code>NodeType</code> those
 * methods that are relevant to a static definition. In addition,
 * <code>NodeTypeTemplate</code> provides methods for setting the attributes of
 * the definition. Implementations of this interface need not contain any
 * validation logic.
 * <p/>
 * See the corresponding get methods for each attribute in
 * <code>NodeTypeDefinition</code> for the default values assumed when a new
 * empty <code>NodeTypeTemplate</code> is created (as opposed to one extracted
 * from an existing <code>NodeType</code>).
 */
public interface NodeTypeTemplate extends NodeTypeDefinition {

    /**
     * Sets the name of the node type.
     *
     * @param name a <code>String</code>.
     */
    public void setName(String name);

    /**
     * Sets the names of the supertypes of the node type.
     *
     * @param names a <code>String</code> array.
     */
    public void setDeclaredSuperTypeNames(String[] names);

    /**
     * Sets the mixin flag of the node type.
     *
     * @param mixin a <code>boolean</code>.
     */
    public void setMixin(boolean mixin);

    /**
     * Sets the orderable child nodes flag of the node type.
     *
     * @param orderable a <code>boolean</code>.
     */
    public void setOrderableChildNodes(boolean orderable);

    /**
     * Sets the name of the primary item.
     *
     * @param name a <code>String</code>.
     */
    public void setPrimaryItemName(String name);

    /**
     * Returns a mutable <code>List</code> of
     * <code>PropertyDefinitionTemplate</code>
     * objects. To define a new <code>NodeTypeTemplate</code> or change an
     * existing one, <code>PropertyDefinitionTemplate</code> objects can be
     * added to or removed from this <code>List</code>.
     *
     * @return a mutable <code>List</code> of <code>PropertyDefinitionTemplate</code> objects.
     */
    public List getPropertyDefintionTemplates();

    /**
     * Returns a mutable <code>List</code> of <code>NodeDefinitionTemplate</code> objects. To define a new
     * <code>NodeTypeTemplate</code> or change an existing one, <code>NodeDefinitionTemplate</code>
     * objects can be added to or removed from this <code>List</code>.
     *
     * @return a mutable <code>List</code> of <code>NodeDefinitionTemplate</code> objects.
     */
    public List getNodeDefintionTemplates();
}
