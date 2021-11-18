/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package javax.jcr.nodetype;

/**
 * The <code>NodeDefinitionTemplate</code> interface extends
 * <code>NodeDefinition</code> with the addition of write methods, enabling the
 * characteristics of a child node definition to be set, after which the
 * <code>NodeDefinitionTemplate</code> is added to a
 * <code>NodeTypeTemplate</code>.
 * <p/>
 * See the corresponding get methods for each attribute in
 * <code>NodeDefinition</code> for the default values assumed when a new empty
 * <code>NodeDefinitionTemplate</code> is created (as opposed to one extracted
 * from an existing <code>NodeType</code>).
 */
public interface NodeDefinitionTemplate {

    /**
     * Sets the name of the node.
     *
     * @param name a <code>String</code>.
     */
    public void setName(String name);

    /**
     * Sets the auto-create status of the node.
     *
     * @param autoCreated a <code>boolean</code>.
     */
    public void setAutoCreated(boolean autoCreated);

    /**
     * Sets the mandatory status of the node.
     *
     * @param mandatory a <code>boolean</code>.
     */
    public void setMandatory(boolean mandatory);

    /**
     * Sets the on-parent-version status of the node.
     *
     * @param opv an <code>int</code> constant member of <code>OnParentVersionAction</code>.
     */
    public void setOnParentVersion(int opv);

    /**
     * Sets the protected status of the node.
     *
     * @param protectedStatus a <code>boolean</code>.
     */
    public void setProtected(boolean protectedStatus);

    /**
     * Sets the required primary types of this node.
     *
     * @param requiredPrimaryTypes a <code>String</code> array.
     */
    public void setRequiredPrimaryTypes(String[] requiredPrimaryTypes);

    /**
     * Sets the default primary type of this node.
     *
     * @param defaultPrimaryType a <code>String</code>.
     */
    public void setDefaultPrimaryType(String defaultPrimaryType);


    /**
     * Sets the same-name sibling status of this node.
     *
     * @param allowSameNameSiblings a <code>boolean</code>.
     */
    public void setSameNameSiblings(boolean allowSameNameSiblings);
}
