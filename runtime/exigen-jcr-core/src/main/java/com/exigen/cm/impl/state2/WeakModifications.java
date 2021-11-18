/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import com.exigen.cm.impl.NodeReference;

public class WeakModifications {

	private NodeReference ref;
	private _NodeState state;

	public WeakModifications(NodeReference to, _NodeState from) {
		this.ref = to;
		this.state = from;
	}

	public NodeReference getRef() {
		return ref;
	}

	public void setRef(NodeReference ref) {
		this.ref = ref;
	}

	public _NodeState getState() {
		return state;
	}

	public void setState(_NodeState state) {
		this.state = state;
	}

}
