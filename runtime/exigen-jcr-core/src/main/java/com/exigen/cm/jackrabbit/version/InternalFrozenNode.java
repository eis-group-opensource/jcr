/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import javax.jcr.version.VersionException;

import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.jackrabbit.name.QName;

/**
 * The InternalFrozenNode interface represents the frozen node that was generated
 * during a {@link javax.jcr.Node#checkin()}. It holds the set of frozen
 * properties, the frozen child nodes and the frozen version history
 * references of the original node.
 */
public interface InternalFrozenNode extends InternalFreeze {

    /**
     * Returns the list of frozen child nodes
     *
     * @return an array of internal freezes
     * @throws VersionException if the freezes cannot be retrieved
     */
    InternalFreeze[] getFrozenChildNodes() throws VersionException;

    /**
     * Returns the list of frozen properties.
     *
     * @return an array of property states
     */
    _PropertyState[] getFrozenProperties();

    /**
     * Returns the frozen UUID.
     *
     * @return the frozen uuid.
     */
    String getFrozenUUID();

    /**
     * Returns the name of frozen primary type.
     *
     * @return the name of the frozen primary type.
     */
    QName getFrozenPrimaryType();

    /**
     * Returns the list of names of the frozen mixin types.
     *
     * @return the list of names of the frozen mixin types.
     */
    QName[] getFrozenMixinTypes();

    /**
     * Checks if this frozen node has the frozen version history
     * @param uuid
     * @return
     */
    boolean hasFrozenHistory(String uuid);

}
