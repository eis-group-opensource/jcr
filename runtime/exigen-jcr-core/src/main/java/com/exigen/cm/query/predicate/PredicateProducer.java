/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.BuildingContextHolder;
import com.exigen.cm.query.QueryUtils;

/**
 * Provides API to create predicates.
 */
public class PredicateProducer extends BuildingContextHolder{

    //  Declares attribute types which can be set for comparison
    private static final Log log = LogFactory.getLog(PredicateProducer.class);
    
    public PredicateProducer(BuildingContext context){
        super(context);
    }
    
    /**
     * Produces AND operation instance and assigns to it parent provided.
     * @param parent
     * @return
     */
    public Condition and(Condition parent){
        
        BooleanOperation _parent = (BooleanOperation)parent;
        return new AndOp(_parent);
    }

    /**
     * Produces OR operation instance and assigns to it parent provided.
     * @param parent
     * @return
     */
    public Condition or(Condition parent){
        BooleanOperation _parent = (BooleanOperation)parent;
        return new OrOp(_parent);
    }
    
    /**
     * Produces NOT operation instance and assigns to it parent provided.
     * @param parent
     * @return
     */
    public Condition not(Condition parent){
        BooleanOperation _parent = (BooleanOperation)parent;
        return new NotOp(_parent);
    }
    
    /**
     * Produces Binary Comparison instance and assigns to it parent provided.
     * @param parent
     * @return
     */
    public Condition binaryComparison(ComparisonType.BINARY comparison, 
            Object value,
            String attributeName){
        return binaryComparison(comparison, value, attributeName, getPropertyType(attributeName));
    }

    /**
     * Produces Binary Comparison instance and assigns to it parent provided.
     * @param parent
     * @return
     */
    public Condition binaryComparison(  ComparisonType.BINARY comparison,
                                        Object value,
                                        PROPERTY_TYPE attributeType){
        
        return binaryComparison(comparison, value /*, sequence*/, null, attributeType);
    }    
    
    /**
     * Produces Binary Comparison instance and assigns to it parent provided.
     * @param parent
     * @return
     */
    public Condition binaryComparison(ComparisonType.BINARY comparison, 
            Object value,
            String attributeName, 
            PROPERTY_TYPE filterType){

    	attributeName = QueryUtils.decodeEntity(attributeName);
        filterType = filterType == null ? getPropertyType(attributeName) : filterType;
        
        try{
            switch(filterType){
                case JCR_NAME:
                    return new JCRNameComparison((String)value,comparison);//, sequence);
                    
                case JCR_PRIMARY_TYPE:
                    return new PrimaryTypeComparison((String)value, comparison);//, sequence);
                    
                case JCR_MIXINS:
                    return new MixinNameComparison((String)value, comparison);//, sequence);
                    
                case JCR_ID:
                    return new NodeIdComparison(comparison, value);//, sequence);
                
                case POSITION:
                    return new PositionComparison(((Number)value).intValue(), comparison);//, sequence);
                    
                default:
                    switch(comparison){
                        case CONTAINS:
                            return new ContainsComparison(attributeName, (String)value);//, sequence);
                            
                        case LIKE:
                            return new LikeComparison(attributeName, value);//, sequence);
                        
                        default:
                            return new GeneralPropertyComparison(attributeName, comparison, value);//, sequence);
                    }
            }
            
        }catch(RuntimeException ex){
            throw ex;
            
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to create Comparison instance for {0}{1}{3}", attributeName, comparison, value);
            log.error(message, ex);
            throw new RuntimeException(message,ex);
        }
    }
    
    /**
     * Produces FTS Comparison instance.
     * @param parent
     * @return
     */
    public Condition ftsComparison(ComparisonType.FTS type, String value){
        return new FTSCondition(type, value);
    }
    
    /**
     * Produces Unary Comparison instance and assigns to it parent provided.
     * @param parent
     * @return
     */
    public Condition unaryComparison(String attributeName, 
                                    ComparisonType.UNARY comparison){

        switch(getPropertyType(attributeName)){

            case JCR_MIXINS:
                return new MixinNullComparison(comparison);//, sequence);
                
//            case POSITION: This cannot appear here!
                
            case JCR_ID:
            case JCR_NAME:            
            case JCR_PRIMARY_TYPE:
                return new ConstantResultComparison(comparison == ComparisonType.UNARY.IS_NOT_NULL);//, sequence);

            default:
            switch(comparison){
                case IS_NOT_NULL:
                    return new IsNotNullComparison(attributeName);//, sequence);

                    
                case IS_NULL:
                    return new IsNullComparison(attributeName);//, sequence);
                    
                default:
                    String message = MessageFormat.format("Failed to create Comparison instance for {0}{1}", attributeName, comparison);
                    log.error(message);
                    throw new RuntimeException(message);
            }

        }
    }
}

/*
 * $Log: PredicateProducer.java,v $
 * Revision 1.2  2007/10/29 13:25:14  dparhomenko
 * decodng attribute name added (Max)
 *
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