/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.lock;

import javax.jcr.lock.Lock;

public interface JCRLock extends Lock{

	public Object getAttribute(String name);
	
}
