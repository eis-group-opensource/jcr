/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import javax.jcr.RepositoryException;

@SuppressWarnings("serial")
public class EmptyDatabaseException extends RepositoryException {

	public EmptyDatabaseException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public EmptyDatabaseException(String message, Throwable rootCause) {
		super(message, rootCause);
		// TODO Auto-generated constructor stub
	}

	public EmptyDatabaseException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public EmptyDatabaseException(Throwable rootCause) {
		super(rootCause);
		// TODO Auto-generated constructor stub
	}

}
