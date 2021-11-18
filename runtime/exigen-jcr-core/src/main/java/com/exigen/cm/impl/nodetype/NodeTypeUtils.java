/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.nodetype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;

public class NodeTypeUtils {

	public static int calculateNodeTypesHash(ArrayList<NodeTypeDef> _defs){
		ArrayList<NodeTypeDef> defs = new ArrayList<NodeTypeDef>(_defs);
		Collections.sort(defs, new Comparator<NodeTypeDef>(){
			public int compare(NodeTypeDef o1, NodeTypeDef o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		
		HashCodeBuilder builder = new HashCodeBuilder();
		for(NodeTypeDef def: defs){
			builder.append(def.generateHashCode());
		}
		
		return builder.toHashCode();
	}
	
}
