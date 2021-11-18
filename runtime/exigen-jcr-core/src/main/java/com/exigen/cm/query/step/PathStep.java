/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.step;

import static com.exigen.cm.Constants.LEFT_INDEX;
import static com.exigen.cm.Constants.PATH_DELIMITER;
import static com.exigen.cm.Constants.RIGHT_INDEX;
import static com.exigen.cm.Constants.TABLE_NODE;
import static com.exigen.cm.Constants.TABLE_NODE__NODE_DEPTH;
import static com.exigen.cm.Constants.TABLE_NODE__WORKSPACE_ID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.PathSQL;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.BuildingContext.DBQName;
import com.exigen.cm.query.PathSQL.QUERY_PART;
import com.exigen.cm.query.order.OrderDefinition;
import com.exigen.cm.query.predicate.Condition;
import com.exigen.cm.query.predicate.FilterContext;
import com.exigen.cm.query.predicate.NoResultsQuery;

/**
 * Basic class for all path steps: named, wildcard, element, deref
 */
public abstract class PathStep {
    
/*   select    n.id 
     from    CM_NODE n
     where          n.workspace_id=:wsId
*/
    private boolean nodeIdCalculated = false;
    private Long nodeId;
    
    private static final Log log = LogFactory.getLog(PathStep.class);
    
    private final PathStep parent;
    private boolean descendantOrSelf;
    private final Condition filterRoot;
    private final int index;
    private final String name;
    
    protected DBQName dbQName;

//  Assigned after property filter SQL is built in addFilteringSQL method.
    private FilterContext filterContext;
    
    public static final int NO_INDEX = -1;
    
    /**
     * Defines that given step is first
     */
    private boolean isFirst = false;
    
    /**
     * First PathStep holds orderings declared for Path.
     */
    private Collection<OrderDefinition> orderings;
    private Map<String, String> tableAliases;
    private Map<String, NodeTypeImpl> nodeTypes;
    
    protected PathStep(PathStep parent, String name, Condition filter, boolean isDescendantOrSelf, int index){
        this.parent=parent;
        this.name=name;
        filterRoot = filter;
        descendantOrSelf=isDescendantOrSelf;
        this.index=index;
    }
    
    /**
     * Returns <code>true</code> if given path step points to descendats.
     * @return
     */
    public boolean isDescendantOrSelf(){
        return descendantOrSelf;
    }
    
    /**
     * Returns step parent or <code>null</code> if this step has no parent.
     * @return
     */
    public PathStep getParent(){
        return parent;
    }

    /**
     * Returns <code>true</code> if this step is a first step in a path starting from right side.
     * @return
     */
    public boolean isFirst(){
        return isFirst;
    }

    
    /**
     * Returns root of conditions tree of filter assigned to given path step or
     * <code>null</code> if no filter is assigned.
     * @return
     */
    protected Condition filterRoot(){
        return filterRoot;
    }

    /**
     * Returns <code>true</code> if given path step has filter assigned.
     * @return
     */
    protected boolean hasFilter(){
        return filterRoot != null;
    }
    
    /**
     * Returns same name sibling index or -1 if no index is provided.
     * @return
     */
    public int index(){
        return index;
    }
    
    /**
     * Returns <code>true</code> if given path step can be used to compute direct path. 
     * @return
     */
    protected boolean canUseAsDirect(){    
        return index != NO_INDEX && name != null && !descendantOrSelf;
    }
    
    /**
     * Returns full node name or <code>null</code> if wildcard is specified.
     * @return
     */
    public String getName(){
        return name;
    }
    
    /**
     * Returns node type name given step describes.
     * @return
     */
    public String getNodeTypeName(){
        return null;
    }
    
    /**
     * Returns name of this node as DBQname which holds JCR DB id of namespace
     * and node local name separately.
     * @param context
     * @return
     */
    protected DBQName getStepNameAsDBQname(PathSQL context){
        if(dbQName == null)
            dbQName= context.getBuildingContext().toDBQname(name);
        return dbQName;
    }
    
    /**
     * Returns ID of node to which points path which ends by given node
     * or <code>null</code> if direct path cannot be computed.
     * @param context
     * @return
     * @throws RepositoryException 
     */
    protected Long getDirectPathNodeId(PathSQL context) throws RepositoryException{
        if(nodeIdCalculated)
            return nodeId;
        
        StringBuilder path = getDirectPath(context);
        if(path == null)
            return null;

        BuildingContext bc = context.getBuildingContext();
        final String sql = new StringBuilder()
            .append("SELECT ")
                .append((bc.getRealColumnName(Constants.FIELD_ID)))
            .append(" FROM ")
                .append(bc.getRealTableName(Constants.TABLE_NODE))
            .append(" WHERE ")
                .append((bc.getRealColumnName(Constants.TABLE_NODE__NODE_PATH)))
                .append("=? AND (")
                .append(bc.getRealColumnName(TABLE_NODE__WORKSPACE_ID)).append("=? OR ")
                .append(bc.getRealColumnName(TABLE_NODE__WORKSPACE_ID)).append(" IS NULL)")
            .toString();
        
        
        PreparedStatement ps;
        ResultSet rs;
        Throwable error;
        try {
            ps = context.getBuildingContext().getConnection().prepareStatement(sql, true);
            ps.setString(1, bc.getDialect().convertStringToSQL(path.toString()));
            ps.setObject(2, bc.getDialect().convertToDBLong(bc.getSession()._getWorkspace().getWorkspaceId()));        
            rs = ps.executeQuery();
            if(!rs.next()){
                String msg = MessageFormat.format("Cannot find node by path {0}", path);
                log.error(msg);
                throw new RuntimeException(msg);
            }
            
            Long id = rs.getLong(1);
            DatabaseTools.closePreparedStatement(ps, context.getBuildingContext().getConnection());

            nodeId = id;
            nodeIdCalculated = true;            
            return nodeId;

        } catch (RepositoryException e) {
            error = e;
        } catch (SQLException e) {
            error = e;
        }
        
        String msg = MessageFormat.format("Cannot find parent node by path {0}", path);
        log.warn(msg, error);
        throw new NoResultsQuery(msg);
    }

    /**
     * Returns direct path depth or 0 if there is no direct path.
     * Direct path steps are: haveing index, name, not descendant or self, not deref ...
     * @return
     */
    private StringBuilder getDirectPath(PathSQL context){
        try{
            StringBuilder path = new StringBuilder();
            boolean started = false;
            
            PathStep step = this;
            do{
               if(!step.canUseAsDirect()) 
                   return null;
               
//               if(started)
//                   path.insert(0, PATH_DELIMITER);
               
               DBQName stepQName = step.getStepNameAsDBQname(context);
               
               
               StringBuilder stepName = new StringBuilder();
               stepName.append(PATH_DELIMITER);
               if(stepQName.hasNamespace())
                   stepName.append(stepQName.getNamespaceId()).append(':');
               
               stepName.append(stepQName.getLocalName())
                                       .append(LEFT_INDEX).append(step.index()).append(RIGHT_INDEX);
               
               path.insert(0,stepName);
               
               if(!started) started = true;
               
               
               
            }while((step = step.getParent())!=null);
        
        return path;
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }
    
    
    
    /**
     * Fills context with SQL for current step, its filter and descendants.
     * @param context
     * @param orderings list of ordering definitions or <code>null</code>
     * @throws RepositoryException 
     */
    public PathSQL toSQL(BuildingContext context, Collection<OrderDefinition> orderings) throws RepositoryException{
        
        this.orderings = orderings;
        
//      PathSQL should contain list of selected columns
//      not ID only should be used!!!
        PathSQL sql = new PathSQL(context);
        
//      Add default selection
        sql.addSelection(PathSQL.TARGET_ALIAS.toString(), context.getRealColumnName(Constants.FIELD_ID), Constants.FIELD_ID);
        
        isFirst = true; // declares this step as first
        
        
        sql.from().append(sql.getBuildingContext().getRealTableName(TABLE_NODE)).append(' ').append(PathSQL.TARGET_ALIAS);

//      nodes with Workspace IS NULL are included in results to allow system node search
//      See JSR-170 8.2.2.2 section
        sql.where()
        .append('(')
        .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS)).append(sql.getBuildingContext().getRealColumnName(TABLE_NODE__WORKSPACE_ID))
        .append("=? OR ")
        .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS)).append(sql.getBuildingContext().getRealColumnName(TABLE_NODE__WORKSPACE_ID))
        .append(" IS NULL AND ")
        .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS)).append(sql.getBuildingContext().getRealColumnName(TABLE_NODE__NODE_DEPTH))
        .append(">1)")
        ;
        sql.addParameter(sql.getBuildingContext().getSession()._getWorkspace().getWorkspaceId(), PathSQL.QUERY_PART.WHERE);
        
        sql.currentAlias(PathSQL.TARGET_ALIAS);
        
        fillSQL(sql);
        return sql;
    }
    
    /**
     * Adds ordering references to result SQL.
     * @param target
     * @param fc
     * @throws RepositoryException 
     */
    protected void addOrderingReferences(PathSQL target, FilterContext fc) throws RepositoryException{
        if(!isFirst() || orderings == null )
            return;

        for(OrderDefinition od:orderings)
            od.toSQL(this, target, fc);
    }
    
    /**
     * Returns table aliase if registered or <code>null</code>
     * @param tableName
     * @return
     */
    public String getTableAlias(String tableName){
        return tableAliases == null ? null : tableAliases.get(tableName);
    }
    
    /**
     * Adds table alias.
     * @param tableName
     * @param alias
     */
    public void addTableAlias(String tableName, String alias){
        if(tableAliases == null) tableAliases = new HashMap<String, String>();
        tableAliases.put(tableName, alias);
    }

    /**
     * Appends node type given PathStep is constrained by.
     * @param type
     */
    public void addNodeType(NodeTypeImpl type){
        if(nodeTypes == null) 
            nodeTypes = new HashMap<String, NodeTypeImpl>();
        nodeTypes.put(type.getName(), type);
    }
    
    /**
     * Returns node types assigned to given path step.
     * @return
     */
    public Collection<NodeTypeImpl> getNodeTypes(){
        return nodeTypes == null ? null : nodeTypes.values();
    }
    /**
     * Generates SQL specific for given path step.
     * @param context
     * @throws RepositoryException 
     */
    protected abstract void fillSQL(PathSQL context) throws RepositoryException;

    /**
     * Fills context with filtering constraints.
     * @param context
     * @throws RepositoryException 
     */
    protected void addFilteringSQL(PathSQL context, FilterContext fc) throws RepositoryException{
        if(!hasFilter())
            return;

        context.getBuildingContext().getFilterSQLGenerator().generate(filterRoot(), fc);
        filterContext = fc;
    }
    
    /**
     * Returns reference to properties in SQL. Used to avoid
     * unneeded JOINs for ORDER BY SQL.
     * @param propertyName
     * @return
     */
    public Map<String, String> getPropertyReferences(){
        if(filterContext == null)
            return null;
        return filterContext.getPropertyReferences();
    }

    /**
     * Returns <code>true</code> if given step has index
     * @return
     */
    protected boolean hasIndex(){
        return index != NO_INDEX;
    }
    
    /**
     * Returns <code>true</code> if given step is last and its parent is jcr:root
     * @return
     */
    protected boolean isLast(){
        return parent == null;
    }
    

    /**
     * Adds node name constraint statement to result SQL.
     * @param context
     */
    protected void addContextNodeConstraintByName(PathSQL context){
        if(getStepNameAsDBQname(context).isWildcard())
            return;
        
        context.where()
        .append(" AND ")
        .append(QueryUtils.asPrefix(context.currentAlias()))
        .append(context.getBuildingContext().getRealColumnName(Constants.FIELD_NAME)).append("=?")
        .append(" AND ")
        .append(QueryUtils.asPrefix(context.currentAlias()))
        .append(context.getBuildingContext().getRealColumnName(Constants.FIELD_NAMESPACE));
        
        context.addParameter(getStepNameAsDBQname(context).getLocalName(), QUERY_PART.WHERE);
        
        if(getStepNameAsDBQname(context).hasNamespace()){
            context.where().append("=?");
            context.addParameter(getStepNameAsDBQname(context).getNamespaceId(), QUERY_PART.WHERE);
        }else
            context.where().append(" IS NULL");
        
        if(hasIndex()){
            context.where()
            .append(" AND ")
            .append(QueryUtils.asPrefix(context.currentAlias()))
            .append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE__INDEX)).append("=?");
            
            context.addParameter(index(), QUERY_PART.WHERE);
        }
    }
}

/*
 * $Log: PathStep.java,v $
 * Revision 1.2  2007/10/09 07:34:52  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 09:01:09  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2007/01/25 15:04:11  maksims
 * #1803814 fixed incorrect index use in direct path calculation
 *
 * Revision 1.3  2007/01/24 08:46:54  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.2  2006/12/20 16:16:21  maksims
 * #0148846 fix for getDirectPathByNodeId not to throw exception
 *
 * Revision 1.1  2006/12/15 13:13:21  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.9  2006/12/13 14:27:09  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.8  2006/12/01 13:09:08  maksims
 * #0149528 comments added
 *
 * Revision 1.7  2006/12/01 13:03:37  maksims
 * #0149528 Fixed problem with incorrect direct parent ID search
 *
 * Revision 1.6  2006/11/30 14:54:43  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.5  2006/11/29 14:59:02  maksims
 * #0149480 Fixed sql for node ID by direct path
 *
 * Revision 1.4  2006/11/29 13:10:20  maksims
 * #1802721 added ability to use in ordering table containing property column joined by last step filter.
 *
 * Revision 1.3  2006/11/23 14:55:29  maksims
 * #1802721 reference  to direct parent simplified
 *
 * Revision 1.2  2006/11/17 10:17:28  maksims
 * #0149157 added query siimplification for case when properties used in predicate belong to explicitly declared context node type
 *
 * Revision 1.1  2006/11/02 17:28:14  maksims
 * #1801897 Query2 addition
 *
 */