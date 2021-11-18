/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype;


import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.exigen.cm.jackrabbit.name.QName;

/**
 * This abstract class implements the <code>ItemDef</code>
 * interface and additionally provides setter methods for the
 * various item definition attributes.
 */
public abstract class ItemDefImpl implements ItemDef {

    /**
     * The name of the child item.
     */
    private QName name = ItemDef.ANY_NAME;

    /**
     * The name of the declaring node type.
     */
    protected QName declaringNodeType = null;

    /**
     * The 'autoCreated' flag.
     */
    private boolean autoCreated = false;

    /**
     * The 'onParentVersion' attribute.
     */
    private int onParentVersion = OnParentVersionAction.COPY;

    /**
     * The 'protected' flag.
     */
    private boolean writeProtected = false;

    /**
     * The 'mandatory' flag.
     */
    private boolean mandatory = false;

    /**
     * Default constructor.
     */
    public ItemDefImpl() {
    }

    /**
     * Sets the name of declaring node type.
     *
     * @param declaringNodeType name of the declaring node type (must not be
     *                          <code>null</code>)
     */
    public void setDeclaringNodeType(QName declaringNodeType) {
        if (declaringNodeType == null) {
            throw new IllegalArgumentException("declaringNodeType can not be null");
        }
        this.declaringNodeType = declaringNodeType;
    }

    /**
     * Sets the name of the child item.
     *
     * @param name name of child item (must not be  <code>null</code>)
     */
    public void setName(QName name) {
        if (name == null) {
            throw new IllegalArgumentException("name can not be null");
        }
        this.name = name;
    }

    /**
     * Sets the 'autoCreated' flag.
     *
     * @param autoCreated a <code>boolean</code>
     */
    public void setAutoCreated(boolean autoCreated) {
        this.autoCreated = autoCreated;
    }

    /**
     * Sets the 'onParentVersion' attribute.
     *
     * @param onParentVersion any of the following constants:
     * <UL>
     *    <LI><code>OnParentVersionAction.COPY</code>
     *    <LI><code>OnParentVersionAction.VERSION</code>
     *    <LI><code>OnParentVersionAction.INITIALIZE</code>
     *    <LI><code>OnParentVersionAction.COMPUTE</code>
     *    <LI><code>OnParentVersionAction.IGNORE</code>
     *    <LI><code>OnParentVersionAction.ABORT</code>
     * </UL>
     */
    public void setOnParentVersion(int onParentVersion) {
        this.onParentVersion = onParentVersion;
    }

    /**
     * Sets the 'protected' flag.
     *
     * @param writeProtected a <code>boolean</code>
     */
    public void setProtected(boolean writeProtected) {
        this.writeProtected = writeProtected;
    }

    /**
     * Sets the 'mandatory' flag.
     *
     * @param mandatory a <code>boolean</code>
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    //--------------------------------------------------------------< ItemDef >
    /**
     * {@inheritDoc}
     */
    public QName getDeclaringNodeType() {
        return declaringNodeType;
    }

    /**
     * {@inheritDoc}
     */
    public QName getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAutoCreated() {
        return autoCreated;
    }

    /**
     * {@inheritDoc}
     */
    public int getOnParentVersion() {
        return onParentVersion;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isProtected() {
        return writeProtected;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * {@inheritDoc}
     */
    public boolean definesResidual() {
        return name.equals(ItemDef.ANY_NAME);
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Compares two item definitions for equality. Returns <code>true</code>
     * if the given object is an item defintion and has the same attributes
     * as this item definition.
     *
     * @param obj the object to compare this item definition with
     * @return <code>true</code> if the object is equal to this item definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ItemDefImpl) {
            ItemDefImpl other = (ItemDefImpl) obj;
            return (declaringNodeType == null
                    ? other.declaringNodeType == null
                    : declaringNodeType.equals(other.declaringNodeType))
                    && (name == null ? other.name == null : name.equals(other.name))
                    && autoCreated == other.autoCreated
                    && onParentVersion == other.onParentVersion
                    && writeProtected == other.writeProtected
                    && mandatory == other.mandatory;
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

    public void configure(QName name2, boolean _protected, boolean mandatory2, boolean autocreated2, int onParentVersion2) {
        this.name = name2;
        this.writeProtected = _protected;
        this.mandatory = mandatory2;
        this.autoCreated = autocreated2;
        this.onParentVersion = onParentVersion2;
        
    }

	protected HashCodeBuilder getBuilder() {
		HashCodeBuilder builder = new HashCodeBuilder();
		builder.append(autoCreated);
		builder.append(declaringNodeType);
		builder.append(mandatory);
		builder.append(name);
		builder.append(onParentVersion);
		builder.append(writeProtected);
		return builder;
	}

}
