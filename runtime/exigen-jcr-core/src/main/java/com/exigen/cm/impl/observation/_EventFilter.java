/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;

import javax.jcr.RepositoryException;

import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SessionImpl;

public interface _EventFilter {

	boolean blocks(EventState eventState) throws RepositoryException;
	
	SessionImpl getSession();

	RepositoryImpl getRepository();
}
