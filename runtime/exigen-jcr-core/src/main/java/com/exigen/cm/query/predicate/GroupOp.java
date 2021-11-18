/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;


/**
 * This class is not needed at all because () is just a hint for TreeBuilder 
 * on how to build syntax tree ..
 * Normally And is child of OR but () can change this and make Or to be child of And
 * After parsing this is reflected in a tree and may not be taken into account anymore ...
 */
class GroupOp extends BooleanOperation {
    GroupOp(BooleanOperation parent) {
        super(TYPE.GROUP,1,parent);
    }
    
/*    @Override
    public Condition open() {
        if(children[0].isLeaf())
            return children[0];
        
        BooleanOperation bop = (BooleanOperation)children[0];
        if(bop.type() == TYPE.GROUP)
            return children[0];
        return null;
    }//*/

}

/*
 * $Log: GroupOp.java,v $
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */