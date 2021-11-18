/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query;

import java.util.List;

import com.exigen.cm.query.order.OrderDefinition;

/**
 * Holds QL (SQL, HQL) query data in a DB layer independent form.
 * Filled by XPath or JCRSQL query builders.
 * Used to generate DB layer specific queries.
 * 
 * @author Maksims
 *
 */
public interface QueryBuilder{

////  Holds target alias used to refer data to be retrieved
//    public static final String TARGET_ALIAS="nn";
//    public static final String TARGET_ALIAS_ACE="nnACE"; 
//    
////  Declares ascending ordering constant.
//    public static final String ORDER_ASC = new String("asc");
//    
////  Declares descending ordering constant.
//    public static final String ORDER_DESC = new String("desc");
//
//// Accoding to JSR-170 default ordering is always ascending
//    public static final String ORDER_DEFAULT = new String("asc");    
//
//    
////  Defines attribute types    
//    public static final int ATTRIBUTE_JCR_GENERAL=0;        
//    public static final int ATTRIBUTE_JCR_NAME=1;
//    public static final int ATTRIBUTE_JCR_PRIMARY_TYPE=2;
//    public static final int ATTRIBUTE_JCR_MIXINS=3;
//    public static final int ATTRIBUTE_JCR_ID = 4;    
    
//  Declares ordering constants
//    public enum ORDER {ASC, DESC};
//    public ORDER ORDER_DEFAULT = ORDER.ASC;
    
//    public enum PROPERTY_TYPE {GENERAL, JCR_NAME, JCR_PRIMARY_TYPE, JCR_MIXIN_TYPES, JCR_ID};
    
    
    /**
     * Starts new sub-query.
     *
     */
    public void startSubquery();
    public void endSubquery();    
    
    

    /**
     * Makes this query to be root query.
     *
     */
    public void setRootQuery();
    
    
    /**
     * Add attribute to select list for Row view on results.
     * @param attribute
     */
    public void addSelectedAttribute(String attribute);
    

    
    public void startPathElement();
    public void endPathElement();
    
    
//  relation to parent / or //
    public void setPathElementDescOrSelf();
    
//  node name
    public void setPathElementName(String name);
    
//  name = *
    public void setPathElementWildcard();

//  name[index]
    public void setPathElementIndex(String index);

//  deref(referringAttribute, targetNodeName)    
    public void setPathStepDeref(String referringAttribute, String targetNodeName);

//  element(...)
    public void setPathStepElement();
//  element(... , targetNodeType)
    public void setPathElementType(String targetNodeType);    
    
    
    /**
     * Use constants declared in this class to specify order type.
     * <code>null</code> can be passed, which means default ordering.
     * @param atribute
     * @param orderType
     */
    public void addOrderByAttribute(String attribute, OrderDefinition.ORDER orderType);
    public void addOrderByScore(List params, OrderDefinition.ORDER orderType);
    

//  A AND B
    public void attachAnd();
    
    
//  NOT (A)
    public void attachNot();
    
//  A OR B
    public void attachOr();
    
//  (A)
  /**
   * This method isn't needed  and can be removed later
    * Normally And is a child of OR but () can change this and make Or to be child of And
    * After parsing this is reflected in a tree and may not be taken into account anymore ...
    * @deprecated
    */
    public void attachGrouping();

//  position() comparison position
    public void attachPositionConstraint(String comparison, String position);

//  attr op value
    public void attachComparison(String attribute, String op, Object value);
    
//  contains(scope, filter)
    public void attachContains(String scope, String filter);
    
//  attribute LIKE filter
    public void attachLike(String attribute, String filter);
    
//  not(attribute)
    public void attachIsNull(String attribute);
    
    
//  attribute
    public void attachIsNotNull(String attribute);
    
    
    

//    /**
//     * Returns number of rows in a result set. 
//     * In case of 0 all result records should be returned;
//     * @return
//     */
//    public int getLimitResult();
//
//    /**
//     * Sets number of rows in a result set. 
//     * In case of 0 all result records should be returned;
//     * @param limitResult is a number of result items to be returned.
//     */    
//    public void setLimitResult(int limitResult);
//    
//    
//    /**
//     * Returns one of the ATTRIBUTE constants declared above.
//     * @param qName
//     * @return
//     */
//    public int getQNameType(String qName);
    
}
/*
 * $Log: QueryBuilder.java,v $
 * Revision 1.1  2007/04/26 08:59:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.7  2007/03/01 14:25:56  maksims
 * #1804008 fixed jcxpath grammar
 *
 * Revision 1.6  2006/12/15 13:13:29  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:06  maksims
 * #1801897 Query2 addition
 *
 * Revision 1.4  2006/09/01 08:10:46  maksims
 * #0148221 Added support for unstructured values
 *
 * Revision 1.3  2006/04/17 06:47:03  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.8  2006/04/11 08:46:32  maksims
 * #0144986 Security filter queries made dialect specific
 *
 * Revision 1.7  2006/03/27 14:57:36  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.6  2006/03/14 17:22:41  maksims
 * #0144986 Value casting to DB specific format dialect methods usage added
 *
 * Revision 1.5  2006/03/14 14:50:57  maksims
 * #0144986 Limit to result constraints
 *
 * Revision 1.4  2006/03/13 12:59:34  maksims
 * #0144986 filtered properties joins filter built along with main filter
 *
 * Revision 1.3  2006/03/03 14:44:58  maksims
 * #0144986 position() and first() support added. last() situation made recognizable ...
 *
 * Revision 1.2  2006/03/02 17:32:36  maksims
 * #0144986 ELEMENT processing fixed
 *
 * Revision 1.1  2006/03/01 16:12:02  maksims
 * #0144986 Initial addition
 *
 */