/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.iterators;

import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;

import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.PropertyImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl._NodeImpl;
import com.exigen.cm.impl.state2._PropertyState;

public class PropertyIteratorImpl extends RangeIteratorImpl implements PropertyIterator{

    private _NodeImpl node;

	public PropertyIteratorImpl(SessionImpl session, List<_PropertyState> data, _NodeImpl node) {
        super(session, data);
        this.node = node;
    }

    protected Object buildObject(Object source) {
    	if (source instanceof _PropertyState){
    		_PropertyState pState = (_PropertyState) source;
    		_NodeImpl n = node;
    		if (n == null){
    			n = new NodeImpl(pState.getParent(), session.getStateManager());
    		}
    			
    		return new PropertyImpl(n, pState);
    	}
        return source;
    }

    public Property nextProperty() {
        return (Property) next();
    }


}


/*
 * $Log: PropertyIteratorImpl.java,v $
 * Revision 1.1  2007/04/26 09:01:27  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/09/07 10:37:08  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.1  2006/04/17 06:47:00  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/03/27 14:57:33  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.1  2006/02/17 13:03:40  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */