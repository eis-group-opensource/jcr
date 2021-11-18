/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.text.MessageFormat;
import java.util.List;


/**
 * Defines Boolean operations.
 */
public abstract class BooleanOperation extends Condition{
    protected enum TYPE {NOT, GROUP, AND, OR};
    private final BooleanOperation parent;    
    
    /*
     *  1. Comparison & Comparison  => 0x02 + 0x02 = 0x04
     *  2. Comparison & BooleanOp   => 0x02 + 0x20 = 0x24
     *  3. BooleanOp  & BooleanOp   => 0x20 + 0x20 = 0x40
     *  4. Incomplete ... => error
     */
    public static final byte CHILD_COMPARISON = 0x02;
    public static final byte CHILD_BOOLEAN = 0x20;    
    private byte childrenType = 0;

    private final TYPE type;
    protected final Condition[] children;
    private int childCount = 0;

    
    protected BooleanOperation(TYPE type, int maxChildCount, BooleanOperation parent){
        children = new Condition[maxChildCount];
        this.type=type;        
        this.parent=parent;
    }
    
    /**
     * Returns <code>true</code> if no more children can be appended to a given operation.
     */
    public boolean isComplete(){
        return childCount == children.length;
    }
    
    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * Add generic child to a boolean operation.
     * @param condition
     */
    public void addChild(Condition condition){
        if(condition instanceof Comparison)
            addChild((Comparison)condition);
        
        else
            if(condition instanceof BooleanOperation)
                addChild((BooleanOperation)condition);
            else{
                String message  = MessageFormat.format("Cannot append child of type {0}. Child should be either instance of Comparison or BooleanOperation",
                        condition.getClass().getName());
                throw new IllegalArgumentException(message);
            }
    }
    
    /**
     * Add comparison child to a boolean operation.
     * @param condition
     */
    public void addChild(Comparison condition){
        children[childCount++]=condition;
        childrenType +=CHILD_COMPARISON;
    }

    /**
     * Add boolean child operation to a boolean operation.
     * @param condition
     */
    public void addChild(BooleanOperation condition){
        children[childCount++]=condition;
        childrenType +=CHILD_BOOLEAN;
    }
    
    
    /**
     * Returns operation type.
     * @return
     */
    protected TYPE type(){
        return type;
    }
    
    /**
     * Returns flag indicating children type.
     * @return
     */
    protected byte childrenType(){
        return childrenType;
    }

    /**
     * Opens boolean expression starting from negation level 0
     * @return
     */
    public List<List<Comparison>> open(){
        return open(0);
    }
    
    /**
     * Opens boolean expression
     */
    protected List<List<Comparison>> open(int negationLevel){
        switch(childrenType()){
            case 0x02:
                return open((Comparison)children[0], negationLevel);

            case 0x20:
                return open((BooleanOperation)children[0], negationLevel);
        
            case 0x04:
                return open((Comparison)children[0], (Comparison)children[1],negationLevel);
                
            case 0x22:
                int bopIdx = children[0] instanceof BooleanOperation ? 0:1;
                int compIdx = bopIdx ==0?1:0;
                return open((BooleanOperation)children[bopIdx], (Comparison)children[compIdx],negationLevel);
                
            case 0x40:
                return open((BooleanOperation)children[0], (BooleanOperation)children[1], negationLevel);
                
            default:
                String message = MessageFormat.format("Unrecognized composition of children is provided to {0}. Children flag: {1}",
                        type(), childrenType);
                throw new IllegalStateException(message);
        }
    }

    
    
    /**
     * Propagates negation level to comparison.
     * @param negationLevel
     * @return
     */
    protected List<List<Comparison>> open(Comparison c, int negationLevel){return null;}
    
    /**
     * Should be overriden in subclass to open Comparison-Comparison boolean relation
     * @param negationLevel
     * @return
     */
    protected List<List<Comparison>> open(BooleanOperation bop, int negationLevel){return null;}
    
    
    /**
     * Should be overriden in subclass to open Comparison-Comparison boolean relation
     * Opens expression of type:<br>
     * comparison AND comparison
     * <li>comparison ::=property (BinaryComparison | UnaryComparison)
     * <li>property ::= QNAME
     * <li>BinaryComparison ::= (>|<|<=|>=|LIKE|!=) value
     * <li>UnaryComparison ::= (IS NULL | IS NOT NULL)
     * <li>value ::= (String | Number)
     * @return
     */
    protected List<List<Comparison>> open(Comparison c1, Comparison c2, int negationLevel){return null;}
    
    /**
     * Should be overriden in subclass to open Comparison-Boolean boolean relation
     * Opens expression of type:<br>
     * BooleanOperation AND comparison
     * <li>BooleanOperation ::= AND | OR | NOT
     * <li>comparison ::=property (BinaryComparison | UnaryComparison)
     * <li>property ::= QNAME
     * <li>BinaryComparison ::= (>|<|<=|>=|LIKE|!=) value
     * <li>UnaryComparison ::= (IS NULL | IS NOT NULL)
     * <li>value ::= (String | Number)
     * @return
     */
    protected List<List<Comparison>> open(BooleanOperation bop, Comparison c, int negationLevel){return null;}
    
    /**
     * Should be overriden in subclass to open Boolean-Bookean boolean relation
     * Opens expression of type:<br>
     * BooleanOperation AND BooleanOperation
     * <li>BooleanOperation ::= AND | OR | NOT
     * @return
     */
    protected List<List<Comparison>> open(BooleanOperation bop1, BooleanOperation bop2, int negationLevel){return null;}
    
    public String toString(){
        return type.toString();
    }
    

    /**
     * @inheritDoc
     */
    @Override
    public Condition getParent() {
        return parent;
    }
    
    @Override
    public void validate() {
        if(childCount != children.length){
            String message = MessageFormat.format("Condition {0} is in invalid state. Number of children is unexpected",toString());
            throw new IllegalStateException(message);
        }
        
        for(int i=0; i<childCount; i++)
            children[i].validate();
    }
}

/*
 * $Log: BooleanOperation.java,v $
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