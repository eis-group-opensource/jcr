/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

public enum ItemStatus {
	 New(-1),
	 Normal(0),
	 Modified(1),
	 Destroyed(2),
	 Invalidated(3);
	
	 
	 private int status;

	ItemStatus(int val){
		 this.status = val;
	 }

	public int getStatus() {
		return status;
	}

}
