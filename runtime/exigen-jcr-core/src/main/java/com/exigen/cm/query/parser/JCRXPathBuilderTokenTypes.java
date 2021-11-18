/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query.parser;
import com.exigen.cm.query.order.OrderDefinition;
import com.exigen.cm.query.QueryBuilder;
import java.util.*;

public interface JCRXPathBuilderTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int DESC = 4;
	int ASC = 5;
	int OR = 6;
	int AND = 7;
	int NOT = 8;
	int LIKE = 9;
	int CONTAINS = 10;
	int DEREF = 11;
	int SCORE = 12;
	int JCR_PATH = 13;
	int ORDER = 14;
	int ELEMENT = 15;
	int TEXT = 16;
	int UNION_STR = 17;
	int TRUE = 18;
	int FALSE = 19;
	int LAST = 20;
	int FIRST = 21;
	int POSITION = 22;
	int JCR_ROOT = 23;
	int ROOT_NODE_PATH = 24;
	int PATH_CHILD = 25;
	int PATH_DESCENDANT_OR_SELF = 26;
	int NODES_COLLECTION = 27;
	int ATTRIBUTE = 28;
	int VALUE = 29;
	int VALUE_NUMBER = 30;
	int VALUE_BOOL = 31;
	int NODE_TYPE = 32;
	int CHILD_INDEX = 33;
	int FILTER = 34;
	int SELECT = 35;
	int QUERY = 36;
	int GROUPING = 37;
	int UNION = 38;
	int SLASH = 39;
	int DOUBLESLASH = 40;
	int LBRACKET = 41;
	int INTEGER = 42;
	int OP = 43;
	int RBRACKET = 44;
	int LBRACE = 45;
	int AT = 46;
	int RBRACE = 47;
	int COMMA = 48;
	int QNAME = 49;
	int STAR = 50;
	int DOT = 51;
	int VALUE_STRING = 52;
	// "-" = 53
	int DOUBLE = 54;
	int LITERAL_by = 55;
}
