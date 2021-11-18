/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database;

public class ColumnMetadata {

	private String name;
	private int length;
	private int type;

	public ColumnMetadata(String name, int columnLength, int columnType) {
		this.name = name;
		this.length = columnLength;
		this.type = columnType;
	
	}

	public int getLength() {
		return length;
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}

}
