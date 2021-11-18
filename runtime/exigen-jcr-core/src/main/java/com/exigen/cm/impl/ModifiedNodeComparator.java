/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.Comparator;

import com.exigen.cm.impl.state2.ItemStatus;
import com.exigen.cm.impl.state2._NodeState;

public class ModifiedNodeComparator implements Comparator<_NodeState> {

	public int compare(_NodeState n1, _NodeState n2) {
		if (n1.getStatus() == ItemStatus.Invalidated && n2.getStatus() == ItemStatus.Invalidated){
			return 0;
		}
		if (n1.getStatus() == ItemStatus.Invalidated){
			return -1;
		}
		if (n2.getStatus() == ItemStatus.Invalidated){
			return 1;
		}
		if (n2.getStatus() == ItemStatus.Modified && n1.getStatus() == ItemStatus.Modified && n1.getName().equals(n2.getName())){
			 return n1.getIndex() - n2.getIndex();
		}
		return 0;
	}

}
