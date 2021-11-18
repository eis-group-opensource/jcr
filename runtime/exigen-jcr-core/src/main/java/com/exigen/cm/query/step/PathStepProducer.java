/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.step;


import java.text.MessageFormat;

import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.BuildingContextHolder;
import com.exigen.cm.query.predicate.Condition;

/**
 * Reponsible for instantiating of proper PathStep instance
 * based on recieved information.
 */
public class PathStepProducer extends BuildingContextHolder {
    public enum STEP_TYPE {NAMED, DEREF, ELEMENT};
    
    public PathStepProducer(BuildingContext context){
        super(context);
    }

    
    /**
     * Produces path step instance using parameters provided.
     * @param parent
     * @param descendantOrSelf
     * @param name
     * @param stepType
     * @param index
     * @param referringAttribute
     * @param nodeTypeName
     * @param filter
     * @return
     * 
     * @see #STEP_TYPE
     */
    public PathStep produce(PathStep parent,
                            boolean descendantOrSelf, 
                            String name, 
                            STEP_TYPE stepType, 
                            int index, 
                            String referringAttribute, 
                            String nodeTypeName, 
                            Condition filter, 
                            boolean isRootQuery){

//      Must test filter for completeness before proceeding ...
        if(filter != null) filter.validate();
        if(index < PathStep.NO_INDEX) 
            throw new IllegalArgumentException("Node index cannot be negative value");

        if(isRootQuery)
            return new RootNodeStep(filter);
        
        switch(stepType){
        case ELEMENT:
            if(nodeTypeName != null) // if type isn't provided this is actually NAME case ...
                return new ElementPathStep(name,nodeTypeName, parent, filter, descendantOrSelf, index);
            case NAMED:
                return new NamedPathStep(name, parent, filter, descendantOrSelf, index);
            case DEREF:
                return new DerefPathStep(referringAttribute, name, parent, filter, descendantOrSelf, index);

            default:
                String message = MessageFormat.format("{0} is not yet implemented!", stepType);
                throw new UnsupportedOperationException(message);
        }
    }
}

/*
 * $Log: PathStepProducer.java,v $
 * Revision 1.1  2007/04/26 09:01:09  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2007/03/01 14:25:51  maksims
 * #1804008 fixed jcxpath grammar
 *
 * Revision 1.2  2006/12/20 16:19:16  maksims
 * #1803635 javadocs added
 *
 * Revision 1.1  2006/12/15 13:13:21  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:14  maksims
 * #1801897 Query2 addition
 *
 */