/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.step;

import static com.exigen.cm.Constants.FIELD_ID;

import com.exigen.cm.query.PathSQL;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.PathSQL.QUERY_PART;
import com.exigen.cm.query.predicate.Condition;

/**
 * Implements PathStep query.
 */
public class RootNodeStep extends PathStep {
    
    RootNodeStep(Condition filter){
        super(null, "jcr:root", filter, true, 1);
    }

    @Override
    protected void fillSQL(PathSQL context) {

        context.where()
        .append(" AND ")
        .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS))
        .append(context.getBuildingContext().getRealColumnName(FIELD_ID))
        .append("=?");
        
        context.addParameter(
                context
                    .getBuildingContext()
                    .getSession()
                    ._getWorkspace()
                    .getRootNodeId()
                    ,QUERY_PART.WHERE);
    }
}

/*
 * $Log: RootNodeStep.java,v $
 * Revision 1.1  2007/04/26 09:01:09  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2007/03/01 14:25:51  maksims
 * #1804008 fixed jcxpath grammar
 *
 */