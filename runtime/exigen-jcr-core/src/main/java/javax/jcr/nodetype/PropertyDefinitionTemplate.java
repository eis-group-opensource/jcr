/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package javax.jcr.nodetype;

import javax.jcr.Value;

/**
 * The PropertyDefinitionTemplate interface extends PropertyDefinition (see
 * 4.7.13 PropertyDefinition) with the addition of write methods, enabling the
 * characteristics of a child property definition to be set, after which the
 * PropertyDefinitionTemplate is added to a NodeTypeTemplate.
 * <p/>
 * See the corresponding get methods for each attribute in
 * PropertyDefinition for the default
 * values assumed when a new empty PropertyDefinitionTemplate is created (as
 * opposed to one extracted from an existing NodeType).
 */
public interface PropertyDefinitionTemplate {

    /**
     * Sets the name of the property.
     *
     * @param name a <code>String</code>.
     */
    public void setName(String name);

    /**
     * Sets the auto-create status of the property.
     *
     * @param autoCreated a <code>boolean</code>.
     */
    public void setAutoCreated(boolean autoCreated);

    /**
     * Sets the mandatory status of the property.
     *
     * @param mandatory a <code>boolean</code>.
     */
    public void setMandatory(boolean mandatory);

    /**
     * Sets the on-parent-version status of the property.
     *
     * @param opv an <code>int</code> constant member of <code>OnParentVersionAction</code>.
     */
    public void setOnParentVersion(int opv);

    /**
     * Sets the protected status of the property.
     *
     * @param protectedStatus a <code>boolean</code>.
     */
    public void setProtected(boolean protectedStatus);

    /**
     * Sets the required type of the property.
     *
     * @param type an <code>int</code> constant member of <code>PropertyType</code>.
     */
    public void setRequiredType(int type);

    /**
     * Sets the value constraints of the property.
     *
     * @param constraints a <code>String</code> array.
     */
    public void setValueConstarints(String[] constraints);

    /**
     * Sets the default value (or values, in the case of a multi-value property) of the property.
     *
     * @param defaultValues a <code>Value</code> array.
     */
    public void setDefaultValues(Value[] defaultValues);

    /**
     * Sets the multi-value status of the property.
     *
     * @param multiple a <code>boolean</code>.
     */
    public void setMultiple(boolean multiple);
}
