/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import javax.jcr.RepositoryException;

@SuppressWarnings("serial")
public class RepositoryConcurrentModificationException extends RepositoryException{

	private String uuid;

	public RepositoryConcurrentModificationException(String string, String uuid) {
		super(string);
		this.uuid = uuid;
	}

	public String getUuid() {
		return uuid;
	}


}
