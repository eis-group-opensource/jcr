/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.order;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.PathSQL;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.predicate.FilterContext;
import com.exigen.cm.query.step.PathStep;


/**
 * Defines ordering by Node PrimaryType.
 */
public class OrderPrimaryType extends OrderDefinition {
    private String nameValueAlias;
    private String namespaceValueAlias;    
    
    protected OrderPrimaryType(ORDER order) {
        super(order);
    }
    
    /**
     * @inheritDoc
     */
    @Override
    public String[] getReferredColumns() {
        return namespaceValueAlias == null ? new String[]{nameValueAlias}:new String[]{namespaceValueAlias, nameValueAlias};
    }
    
    
    /*
    SELECT n.NAME, ns.PREFIX, nt.NAME

    FROM CM_NODE n 
        JOIN CMT_NODETYPE nt ON nt.ID = n.NODE_TYPE_ID 
        JOIN CM_NAMESPACE typeNs ON nt.NAMESPACE = typeNs.ID
        
    ORDER BY ns.PREFIX ASC , nt.NAME ASC   
*/        
    /**
     * Joins node type table to node table and binds name space to node type table to
     * allow sorting by node type namespace and local name.
     * @throws RepositoryException 
     */
    @Override
    public void toSQL(PathStep owner, PathSQL target, FilterContext fc) throws RepositoryException {
        nameValueAlias = nameValueAlias == null ? getNextOrderValueAlias(target):nameValueAlias;
        namespaceValueAlias = namespaceValueAlias == null ? getNextOrderValueAlias(target):namespaceValueAlias;
        
        BuildingContext bc = target.getBuildingContext();
        String typeTable = bc.getRealTableName(Constants.TABLE_NODETYPE);
        String nsTable = bc.getRealTableName(Constants.TABLE_NAMESPACE);
        
        StringBuilder typeAlias = bc.nextAlias();
        StringBuilder nsAlias = bc.nextAlias();

        target.addSelection(nsAlias.toString(), bc.getRealColumnName(Constants.TABLE_NAMESPACE__PREFIX), namespaceValueAlias);
        target.addSelection(typeAlias.toString(), bc.getRealColumnName(Constants.FIELD_NAME), nameValueAlias);

        
        target.from()
            .append(" JOIN ")
            .append(typeTable).append(' ').append(typeAlias)
            .append(" ON ")
            .append(QueryUtils.asPrefix(typeAlias)).append(bc.getRealColumnName(Constants.FIELD_ID))
            .append('=')
            .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS)).append(bc.getRealColumnName(Constants.TABLE_NODE__NODE_TYPE))

            .append(" JOIN ")
            .append(nsTable).append(' ').append(nsAlias)
            .append(" ON (")
            .append(QueryUtils.asPrefix(typeAlias)).append(bc.getRealColumnName(Constants.FIELD_NAMESPACE))
            .append('=')
            .append(QueryUtils.asPrefix(nsAlias)).append(bc.getRealColumnName(Constants.FIELD_ID))
            .append(" OR ")
            .append(QueryUtils.asPrefix(typeAlias)).append(bc.getRealColumnName(Constants.FIELD_NAMESPACE))
            .append(" IS NULL)");
    }
}

/*
 * $Log: OrderPrimaryType.java,v $
 * Revision 1.2  2007/10/09 07:34:51  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 09:01:08  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/21 12:33:19  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.1  2006/12/15 13:13:25  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.3  2006/12/13 14:27:10  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.2  2006/11/03 14:11:52  maksims
 * #1801897 NULL namespace processing fixed
 *
 * Revision 1.1  2006/11/02 17:28:09  maksims
 * #1801897 Query2 addition
 *
 */