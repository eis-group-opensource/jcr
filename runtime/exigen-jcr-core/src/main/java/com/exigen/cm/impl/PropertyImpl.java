/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.JCRHelper;
import com.exigen.cm.impl.state2.StoreContainer;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.uuid.UUID;
import com.exigen.cm.jackrabbit.value.BLOBFileValue;
import com.exigen.cm.jackrabbit.value.BooleanValue;
import com.exigen.cm.jackrabbit.value.DateValue;
import com.exigen.cm.jackrabbit.value.DoubleValue;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.cm.jackrabbit.value.LongValue;

/**
 * <code>PropertyImpl</code> implements the <code>Property</code> interface.
 */
public class PropertyImpl extends ItemImpl implements Property {

    /** Logger for this class */
    private static final Log log = LogFactory.getLog(PropertyImpl.class);

    private _NodeImpl parent;

	private _PropertyState state;
	
	private PropertyDefinitionImpl definition;

    public PropertyImpl(_NodeImpl node, _PropertyState state) {
        super(state, node.getStateManager());
        //this.id = new PropertyId(node, name);
        this.definition = state.getDefinition();
        this.parent = node;
        this.state = state;
    }

    protected ItemId getItemId() {
    	return new PropertyId(parent.getNodeId(), state.getName());
    }
    
    /**
     * Package private constructor.
     *
     * @param itemMgr    the <code>ItemManager</code> that created this <code>Property</code>
     * @param session    the <code>Session</code> through which this <code>Property</code> is acquired
     * @param id         id of this <code>Property</code>
     * @param state      state associated with this <code>Property</code>
     * @param definition definition of <i>this</i> <code>Property</code>
     * @param listeners  listeners on life cylce changes of this <code>PropertyImpl</code>
     */
/*    _PropertyImpl(ItemManager itemMgr, SessionImpl session, PropertyId id,
                 PropertyState state, PropertyDefinition definition,
                 ItemLifeCycleListener[] listeners) {
        super(itemMgr, session, id, state, listeners);
        this.definition = definition;
        // value will be read on demand
    }
*/

    /**
     * Determines the length of the given value.
     *
     * @param value value whose length should be determined
     * @return the length of the given value
     * @throws RepositoryException if an error occurs
     * @see javax.jcr.Property#getLength()
     * @see javax.jcr.Property#getLengths()
     */
    protected long getLength(InternalValue value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.STRING:
            case PropertyType.LONG:
            case PropertyType.DOUBLE:
                return value.toString().length();

            case PropertyType.NAME:
                QName name = (QName) value.internalValue();
                try {
                    return _getNamespaceResolver().getJCRName(name).length();
                } catch (NoPrefixDeclaredException npde) {
                    // should never happen...
                    String msg = safeGetJCRPath()
                            + ": the value represents an invalid name";
                    log.debug(msg);
                    throw new RepositoryException(msg, npde);
                }

            case PropertyType.PATH:
                Path path = (Path) value.internalValue();
                try {
                    return path.toJCRPath(_getNamespaceResolver()).length();
                } catch (NoPrefixDeclaredException npde) {
                    // should never happen...
                    String msg = safeGetJCRPath()
                            + ": the value represents an invalid path";
                    log.debug(msg);
                    throw new RepositoryException(msg, npde);
                }

            case PropertyType.BINARY:
                BLOBFileValue blob = (BLOBFileValue) value.internalValue();
                return blob.getLength();

            default:
                return -1;
        }
    }

    /**
     * Checks various pre-conditions that are common to all
     * <code>setValue()</code> methods. The checks performed are:
     * <ul>
     * <li>parent node must be checked-out</li>
     * <li>property must not be protected</li>
     * <li>parent node must not be locked by somebody else</li>
     * <li>property must be multi-valued when set to an array of values
     * (and vice versa)</li>
     * </ul>
     *
     * @param multipleValues flag indicating whether the property is about to
     *                       be set to an array of values
     * @throws ValueFormatException         if a single-valued property is set to an
     *                                      array of values (and vice versa)
     * @throws VersionException             if the parent node is not checked-out
     * @throws LockException                if the parent node is locked by somebody else
     * @throws ConstraintViolationException if the property is protected
     * @throws RepositoryException          if another error occurs
     * @see javax.jcr.Property#setValue
     */
    protected void checkSetValue(boolean multipleValues)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        _NodeImpl parent = (_NodeImpl) _getParent();

        // verify that parent node is checked-out
        if (!parent.internalIsCheckedOut(false)) {
            throw new VersionException("cannot set the value of a property of a checked-in node "
                    + safeGetJCRPath());
        }

        // check protected flag
        if (parent.checkProtection() &&  definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property "
                    + safeGetJCRPath());
        }

        // check multi-value flag
        if (multipleValues) {
            if (!definition.isMultiple()) {
                throw new ValueFormatException(safeGetJCRPath()
                        + " is not multi-valued");
            }
        } else {
            if (definition.isMultiple()) {
                throw new ValueFormatException(safeGetJCRPath()
                        + " is multi-valued and can therefore only be set to an array of values");
            }
        }

        // check lock status
        parent.checkLock();
    }

    protected void internalSetValue(InternalValue[] values, int type, boolean setModification) throws ConstraintViolationException, RepositoryException {
        internalSetValue(values, type, setModification, true);
    }
    /**
     * @param values
     * @param type
     * @param triggerEvents 
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    protected void internalSetValue(InternalValue[] values, int type, boolean setModification, boolean triggerEvents)
            throws ConstraintViolationException, RepositoryException {
        //check permission
        parent.canSetProperty();

        _PropertyState propState = ((_PropertyState)getItemState());
    	propState.internalSetValue(values, type, setModification, triggerEvents);

    }

    /**
     * Same as <code>{@link Property#setValue(String)}</code> except that
     * this method takes a <code>QName</code> instead of a <code>String</code>
     * value.
     *
     * @param name
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public void setValue(QName name)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.NAME;
        }

        if (name == null) {
            internalSetValue(null, reqType, true);
            return;
        }

        InternalValue internalValue;
        if (reqType != PropertyType.NAME) {
            // type conversion required
            internalValue =
                    InternalValue.create(InternalValue.create(name).toJCRValue(_getNamespaceResolver()),
                            reqType, _getNamespaceResolver(), getStoreContainer());
        } else {
            // no type conversion required
            internalValue = InternalValue.create(name);
        }

        internalSetValue(new InternalValue[]{internalValue}, reqType, true);
    }

    /**
     * Same as <code>{@link Property#setValue(String[])}</code> except that
     * this method takes an array of <code>QName</code> instead of
     * <code>String</code> values.
     *
     * @param names
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public void setValue(QName[] names)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(true);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.NAME;
        }

        InternalValue[] internalValues = null;
        // convert to internal values of correct type
        if (names != null) {
            internalValues = new InternalValue[names.length];
            for (int i = 0; i < names.length; i++) {
                QName name = names[i];
                InternalValue internalValue = null;
                if (name != null) {
                    if (reqType != PropertyType.NAME) {
                        // type conversion required
                        internalValue =
                                InternalValue.create(InternalValue.create(name).toJCRValue(_getNamespaceResolver()),
                                        reqType, _getNamespaceResolver(), getStoreContainer());
                    } else {
                        // no type conversion required
                        internalValue = InternalValue.create(name);
                    }
                }
                internalValues[i] = internalValue;
            }
        }

        internalSetValue(internalValues, reqType, true);
    }

    /**
     * {@inheritDoc}
     */
    public QName getQName() {
    	return state.getName();
    }

    /**
     * Returns the internal values of this property
     *
     * @return
     * @throws RepositoryException
     */
    public InternalValue[] internalGetValues() throws RepositoryException {

        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (!definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is not multi-valued");
        }

        return state.getValues();
    }

    /**
     * Returns the internal values of this property
     *
     * @return
     * @throws RepositoryException
     */
    public InternalValue internalGetValue() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        return state.getValues()[0];
    }

    //-------------------------------------------------------------< Property >
    /**
     * {@inheritDoc}
     */
    public Value[] getValues() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (!definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is not multi-valued");
        }

        InternalValue[] internalValues = state.getValues();
        Value[] values = new Value[internalValues.length];
        for (int i = 0; i < internalValues.length; i++) {
            values[i] = internalValues[i].toJCRValue(_getNamespaceResolver());
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public Value getValue() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        return JCRHelper.getPropertyValue((_PropertyState)getItemState(), _getNamespaceResolver());
    }

    /**
     * {@inheritDoc}
     */
    public String getString() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        return getValue().getString();
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getStream() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        return getValue().getStream();
    }

    /**
     * {@inheritDoc}
     */
    public long getLong() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        InternalValue val = state.getValues()[0];
        int type = val.getType();
        if (type == PropertyType.LONG) {
            return ((Long) val.internalValue()).longValue();
        }
        // not a LONG value, delegate conversion to Value object
        return val.toJCRValue(_getNamespaceResolver()).getLong();
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        // avoid unnecessary object creation if possible
        InternalValue val = state.getValues()[0];
        int type = val.getType();
        if (type == PropertyType.DOUBLE) {
            return ((Double) val.internalValue()).doubleValue();
        }
        // not a DOUBLE value, delegate conversion to Value object
        return val.toJCRValue(_getNamespaceResolver()).getDouble();
    }

    /**
     * {@inheritDoc}
     */
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        // avoid unnecessary object creation if possible
        InternalValue val = state.getValues()[0];
        int type = val.getType();
        if (type == PropertyType.DATE) {
            return (Calendar) val.internalValue();
        }
        // not a DATE value, delegate conversion to Value object
        return val.toJCRValue(_getNamespaceResolver()).getDate();
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        // avoid unnecessary object creation if possible
        InternalValue val = state.getValues()[0];
        int type = val.getType();
        if (type == PropertyType.BOOLEAN) {
            return ((Boolean) val.internalValue()).booleanValue();
        }
        // not a BOOLEAN value, delegate conversion to Value object
        return val.toJCRValue(_getNamespaceResolver()).getBoolean();
    }

    /**
     * {@inheritDoc}
     */
    public Node getNode() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        InternalValue val = state.getValues()[0];
        if (val.getType() == PropertyType.REFERENCE || val.getType() == PropertyType283.WEAKREFERENCE) {
            // reference, i.e. target UUID
            UUID targetUUID = (UUID) val.internalValue();
            //return (Node) itemMgr.getItem(new NodeId(targetUUID.toString()));
            return _getSession().getNodeByUUID(targetUUID.toString());
        } else {
            throw new ValueFormatException("property must be of type REFERENCE");
        }
        //throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Calendar date)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.DATE;
        }

        if (date == null) {
            internalSetValue(null, reqType, true);
            return;
        }

        InternalValue value;
        if (reqType != PropertyType.DATE) {
            // type conversion required
            value = InternalValue.create(new DateValue(date), reqType, _getNamespaceResolver(), getStoreContainer());
        } else {
            // no type conversion required
            value = InternalValue.create(date);
        }

        internalSetValue(new InternalValue[]{value}, reqType, true);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(double number)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.DOUBLE;
        }

        InternalValue value;
        if (reqType != PropertyType.DOUBLE) {
            // type conversion required
            value = InternalValue.create(new DoubleValue(number), reqType, _getNamespaceResolver(), getStoreContainer());
        } else {
            // no type conversion required
            value = InternalValue.create(number);
        }

        internalSetValue(new InternalValue[]{value}, reqType, true);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(InputStream stream)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.BINARY;
        }

        if (stream == null) {
            internalSetValue(null, reqType, true);
            return;
        }

        InternalValue value;
        try {
            if (reqType != PropertyType.BINARY) {
                // type conversion required
            	BLOBFileValue internalValue = new BLOBFileValue(stream, this._getSession().getStoreContainer());
                value = InternalValue.create(internalValue, reqType, _getNamespaceResolver(), getStoreContainer());
                value.setContentId(internalValue.getContentId());
            } else {
                // no type conversion required
                value = InternalValue.create(stream, null, stateManager.getStoreContainer());
                
            }
        } catch (IOException ioe) {
            String msg = "failed to spool stream to internal storage";
            log.debug(msg);
            throw new RepositoryException(msg, ioe);
        }

        internalSetValue(new InternalValue[]{value}, reqType, true);
        //throw new UnsupportedOperationException();

    }

    /**
     * {@inheritDoc}
     */
    public void setValue(String string)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.STRING;
        }

        if (string == null) {
            internalSetValue(null, reqType, true);
            return;
        }

        InternalValue internalValue;
        if (reqType != PropertyType.STRING) {
            // type conversion required
            internalValue = InternalValue.create(string, reqType, _getNamespaceResolver(), getStoreContainer());
        } else {
            // no type conversion required
            internalValue = InternalValue.create(string);
        }
        internalSetValue(new InternalValue[]{internalValue}, reqType, true);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(String[] strings)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(true);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.STRING;
        }

        InternalValue[] internalValues = null;
        // convert to internal values of correct type
        if (strings != null) {
            internalValues = new InternalValue[strings.length];
            for (int i = 0; i < strings.length; i++) {
                String string = strings[i];
                InternalValue internalValue = null;
                if (string != null) {
                    if (reqType != PropertyType.STRING) {
                        // type conversion required
                        internalValue = InternalValue.create(string, reqType, _getNamespaceResolver(), getStoreContainer());
                    } else {
                        // no type conversion required
                        internalValue = InternalValue.create(string);
                    }
                }
                internalValues[i] = internalValue;
            }
        }

        internalSetValue(internalValues, reqType, true);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(boolean b)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.BOOLEAN;
        }

        InternalValue value;
        if (reqType != PropertyType.BOOLEAN) {
            // type conversion required
            value = InternalValue.create(new BooleanValue(b), reqType, _getNamespaceResolver(), getStoreContainer());
        } else {
            // no type conversion required
            value = InternalValue.create(b);
        }

        internalSetValue(new InternalValue[]{value}, reqType, true);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Node target)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.REFERENCE;
        }

        if (target == null) {
            internalSetValue(null, reqType, true);
            return;
        }

        if (reqType == PropertyType.REFERENCE || reqType == PropertyType283.WEAKREFERENCE) {
            if (target instanceof NodeImpl) {
                NodeImpl targetNode = (NodeImpl) target;
                if (targetNode.isNodeType(QName.MIX_REFERENCEABLE)) {
                    InternalValue value = InternalValue.create(new UUID(targetNode.getUUID()), reqType == PropertyType283.WEAKREFERENCE);
                    internalSetValue(new InternalValue[]{value}, reqType, true);
                } else {
                    throw new ValueFormatException("target node must be of node type mix:referenceable");
                }
            } else {
                String msg = "incompatible Node object: " + target;
                log.debug(msg);
                throw new RepositoryException(msg);
            }
        } else {
            throw new ValueFormatException("property must be of type REFERENCE");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(long number)
            throws ValueFormatException, VersionException,
            LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.LONG;
        }

        InternalValue value;
        if (reqType != PropertyType.LONG) {
            // type conversion required
            value = InternalValue.create(new LongValue(number), reqType, _getNamespaceResolver(), getStoreContainer());
        } else {
            // no type conversion required
            value = InternalValue.create(number);
        }

        internalSetValue(new InternalValue[]{value}, reqType, true);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setValue(Value value)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            if (value != null) {
                reqType = value.getType();
            } else {
                reqType = PropertyType.STRING;
            }
        }

        if (value == null) {
            internalSetValue(null, reqType, true);
            return;
        }

        InternalValue internalValue;
        if (reqType != value.getType()) {
            // type conversion required
            internalValue = InternalValue.create(value, reqType, _getNamespaceResolver(), getStoreContainer());
        } else {
            // no type conversion required
            internalValue = InternalValue.create(value, _getNamespaceResolver(), getStoreContainer());
        }
        internalSetValue(new InternalValue[]{internalValue}, reqType, true);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Value[] values)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(true);

        if (values != null) {
            // check type of values
            int valueType = PropertyType.UNDEFINED;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    // skip null values as those will be purged later
                    continue;
                }
                if (valueType == PropertyType.UNDEFINED) {
                    valueType = values[i].getType();
                } else if (valueType != values[i].getType()) {
                    // inhomogeneous types
                    String msg = "inhomogeneous type of values";
                    log.debug(msg);
                    throw new ValueFormatException(msg);
                }
            }
        }

        int reqType = definition.getRequiredType();

        InternalValue[] internalValues = null;
        // convert to internal values of correct type
        if (values != null) {
            internalValues = new InternalValue[values.length];
            for (int i = 0; i < values.length; i++) {
                Value value = values[i];
                InternalValue internalValue = null;
                if (value != null) {
                    // check type according to definition of this property
                    if (reqType == PropertyType.UNDEFINED) {
                        // use the value's type as property type
                        reqType = value.getType();
                    }
                    if (reqType != PropertyType.UNDEFINED
                            && reqType != value.getType()) {
                        // type conversion required
                        internalValue = InternalValue.create(value, reqType, _getNamespaceResolver(), getStoreContainer());
                    } else {
                        // no type conversion required
                        internalValue = InternalValue.create(value, _getNamespaceResolver(), getStoreContainer());
                    }
                }
                internalValues[i] = internalValue;
            }
        }

        internalSetValue(internalValues, reqType, true);
    }

    /**
     * {@inheritDoc}
     */
    public long getLength() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued");
        }

        InternalValue[] values = state.getValues();
        if (values.length == 0) {
            // should never be the case, but being a little paranoid can't hurt...
            log.warn(safeGetJCRPath() + ": single-valued property with no value");
            return -1;
        }
        return getLength(values[0]);
    }

    /**
     * {@inheritDoc}
     */
    public long[] getLengths() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (!definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is not multi-valued");
        }

        InternalValue[] values =  state.getValues();
        long[] lengths = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            lengths[i] = getLength(values[i]);
        }
        return lengths;

    }

    /**
     * {@inheritDoc}
     */
    public PropertyDefinition getDefinition() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return definition;
    }

    /**
     * {@inheritDoc}
     */
    public int getType() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return internalGetType();
    }

	private int internalGetType() {
		return  this.state.getType();
	}

    //-----------------------------------------------------------------< Item >
    /**
     * {@inheritDoc}
     */
    public boolean isNode() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        QName name = state.getName();
        try {
            return _getNamespaceResolver().getJCRName(name);
        } catch (NoPrefixDeclaredException npde) {
            // should never get here...
            String msg = "internal error: encountered unregistered namespace " + name.getNamespaceURI();
            log.debug(msg);
            throw new RepositoryException(msg, npde);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Node getParent()
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

       return (Node) _getParent();
    }
    /**
     * {@inheritDoc}
     */
    public _NodeImpl _getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
       return parent;
    }


    public int getIndex() {
        return 1;
    }

    public boolean isMultiple() {
        return definition.isMultiple();
    }

    /**
     * {@inheritDoc}
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        visitor.visit(this);
    }

    public String toString(){
        ToStringBuilder builder = new ToStringBuilder(this);
        //builder.append("id", id);
        builder.append("value", state);
        builder.append("status", getItemState().getStatus().name());
        return builder.toString();
    }

    public NodeId getParentId() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return parent.getNodeItemId();
    }

	@Override
	protected SessionImpl _getSession() {
		return parent._getSession();
	}

	@Override
	public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PropertyImpl) {
        	PropertyImpl other = (PropertyImpl) obj;
            return getQName().equals(other.getQName()) && parent.equals(other.parent);
        }
        return false;	
    }

	
	public _PropertyState getPropertyState(){
		return state;
	}

	public Long getParentNodeId() throws ItemNotFoundException, AccessDeniedException, RepositoryException {		
		return parent.getParentNodeId();
	}
    
	public StoreContainer getStoreContainer(){
		return stateManager.getStoreContainer();
	}
}
