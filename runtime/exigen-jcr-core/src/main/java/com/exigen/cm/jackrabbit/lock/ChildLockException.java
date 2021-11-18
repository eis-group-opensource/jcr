/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.lock;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.lock.LockException;

@SuppressWarnings("serial")
public class ChildLockException extends LockException{

    private String[] paths;
	private List<Long> ids;

    public ChildLockException(String[] paths, ArrayList<Long> _ids){
        super("One of the child nodes is locked("+convertPaths(paths)+").");
        this.paths = paths;
        this.ids = _ids;
    }

    public List<Long> getIds() {
		return ids;
	}

	private static String convertPaths(String[] paths2) {
        StringBuffer result = new StringBuffer();
        for(int i = 0 ; i < paths2.length ; i++){
            result.append(paths2[i]);
            if (i < paths2.length-1){
                result.append(", ");
            }
        }
        return result.toString();
    }

    public String[] getPaths() {
        return paths;
    }
    
}
