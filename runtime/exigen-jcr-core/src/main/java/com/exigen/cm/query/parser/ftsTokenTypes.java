/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query.parser;

import com.exigen.cm.query.predicate.ContainsComparison;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

public interface ftsTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int OR = 4;
	int NOT = 5;
	int AND = 6;
	int MINUS = 7;
	int PHRASE = 8;
	int WORD = 9;
	int PARENTESS = 10;
	int ESCAPE = 11;
	int WS = 12;
}
