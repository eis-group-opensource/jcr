/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import com.exigen.cm.jackrabbit.name.QName;

public class IndexedQname {

	private QName name;

	private int index;

	public IndexedQname(QName name, int index) {
		super();
		this.name = name;
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public QName getName() {
		return name;
	}
	
	int hash = 0;

	@Override
	public int hashCode() {
		int h = hash;
        if (h == 0) {
            h = 17;
            h = 37 * h + name.hashCode();
            h = 37 * h + index;
            hash = h;
        }
        return h;
		
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
            return true;
        }
        if (obj instanceof IndexedQname) {
        	IndexedQname other = (IndexedQname)obj;
        	if (other.name.equals(name) && other.index == index){
        		return true;
        	}
        }
        return false;
	}

}
