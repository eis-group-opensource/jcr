/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.util.List;


/**
 * Base class for all conditions.
 */
public abstract class Condition{

    /**
     * Returns <code>true</code> if Condition is complete and no child can be added.
     * @return
     */
    public boolean isComplete(){
        return true;
    }
    
    
    /**
     * Return <code>true</code> of current codition is a leaf.
     * @return
     */
    public boolean isLeaf(){
        return true;
    }

    /**
     * Returns condition parent.
     * @return
     */
    public Condition getParent(){
        return null;
    }
    
    /**
     * Validates condition and its children if any ...
     * Must be overriden in sub-classes to implement proper validation.
     *
     */
    public void validate(){
    }
    
    /**
     * Returns parent from parent hierarchy which has available attachment points e.g. Condition.isComplete() method returns <code>false</code>.
     * @return
     */
    public Condition getAvailableParent(){
        Condition parent = getParent();
        while(parent != null && parent.isComplete()){
            if(parent.getParent() == null)
                return parent;
            
            parent = parent.getParent();            
        }
        
        return parent;
    }    

    /**
     * Opens condition by transforming it to a form of: <br/>
     *  &lt;c1&gt;> and &lt;c2&gt;> and ... and &lt;cN&gt;> <br/>
     *  <b>or</b> <br/>
     *  &lt;c1&gt;> and &lt;c2&gt;> and ... and &lt;cN&gt;> <br/>
     *  <b>or</b> <br/>
     *  &lt;c1&gt;> and &lt;c2&gt;> and ... and &lt;cN&gt;> <br/>
     *  <b>or</b> <br/>
     *  ...
     * @return
     */
    public abstract List<List<Comparison>> open();

}

/*
 * $Log: Condition.java,v $
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/21 12:33:17  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */