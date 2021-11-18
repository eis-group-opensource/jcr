/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl._NodeImpl;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class NodeStateHelper {

	public static void dumpNode(Node node) throws RepositoryException {
		dumpNode(((NodeImpl)node).getNodeState(),0,false, true);
		
	}
	
	public static void dumpNode(Node node, boolean showProperties) throws RepositoryException {
		dumpNode(((NodeImpl)node).getNodeState(),0,false, showProperties);
		
	}
	

	public static void dumpNode(Node node, int shift, boolean dumpSecurity) throws RepositoryException {
		dumpNode(((_NodeImpl)node).getNodeState(), shift, dumpSecurity, true);
		
	}
	
	@SuppressWarnings("unchecked")
    public static void dumpNode(_NodeState node, int shift, boolean dumpSecurity, boolean showProperties) throws RepositoryException {
		_AbstractsStateManager stateManager  = node.stateManager;
		NamespaceResolver resolver = stateManager.getNamespaceResolver();
        printShift(shift);
        //prints node name
        String uuid = null;
        /*String vUUID = null;
        try {
            uuid = node.getUUID();
        } catch (Exception e) {
            // TODO: handle exception
        }
        try {
            vUUID = ((NodeStateEx)node).getProperty(QName.JCR_FROZENUUID).getString();
        } catch (Exception e) {
            // TODO: handle exception
        }*/
        String name = null;
		try {
			name = node.getName().toJCRName(resolver)+"<"+node.getPrimaryNodeType().getName()+">   (path="+node.getInternalPath()+"; id="+node.getNodeId()+")";
		} catch (NoPrefixDeclaredException e1) {
			throw new RepositoryException(e1);
		}
        if (uuid != null){
            name += " {"+uuid+"}";
        }
        /*if (vUUID != null){
            name += " f{"+vUUID+"}";
        }*/
        System.out.println(name);
        //get all child nodes and print theres names
        if (showProperties){
        	List<_PropertyState> props = new ArrayList<_PropertyState>(node.getProperties());
        	Collections.sort(props, new Comparator<_PropertyState>(){
        		public int compare(_PropertyState o1, _PropertyState o2) {
        			return o1.getName().compareTo(o2.getName());
        		}
        	});
	        for(_PropertyState child:props){
	            printShift(shift+1);
	            try {
					System.out.print("@"+child.getName().toJCRName(resolver)+"=");
				} catch (NoPrefixDeclaredException e) {
					e.printStackTrace();
				}
	            if (child.getDefinition().isMultiple()){
	            	ArrayList vv = new ArrayList();
	            	for(InternalValue v:child.getValues()){
	            		vv.add(convertInternalValue(v.internalValue()));
	            	}
	            	System.out.println(vv);
	            } else {
	            	InternalValue v = child.getValues()[0];
	            	if (v != null){
	            		System.out.println(convertInternalValue(v.internalValue()));
	            	} else {
	            		System.out.println("null");
	            	}
	            }
	        }
        }
        for(_NodeState child:stateManager.getNodesWithName(node, null, false)){
            dumpNode(child, shift + 1, dumpSecurity, showProperties);
        }
    }
	
    private static Object convertInternalValue(Object internalValue) {
		if (internalValue instanceof Calendar){
			return ((Calendar)internalValue).getTime();
		}
		return internalValue;
	}

	public static void printShift(int shift) {
        for (int i = 0 ; i < shift; i++){
            System.out.print("    ");
        }
        
    }
}
