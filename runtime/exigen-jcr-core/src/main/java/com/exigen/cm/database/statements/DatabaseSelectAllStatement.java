/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.dialect.AbstractDatabaseDialect;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.DatabaseCondition;
import com.exigen.vf.commons.logging.LogUtils;

public class DatabaseSelectAllStatement extends AbstractDatabaseStatement{

    /** Log for this class */
    private static final Log log = LogFactory.getLog(DatabaseSelectAllStatement.class);
    
    
    private boolean loadAll;
    private int position = 0;
    private ArrayList<RowMap> rows = new ArrayList<RowMap>();
    private ArrayList<DatabaseCondition> conditions = new ArrayList<DatabaseCondition>(); 
    private RowMap nextRow = null;
    private ArrayList<String> resultColumn = new ArrayList<String>();
    private ArrayList<Order> orders = new ArrayList<Order>();


    private ArrayList<DatabaseJoin> joins = new ArrayList<DatabaseJoin>();
    private ArrayList<DatabaseJoin> leftOuterJoins = new ArrayList<DatabaseJoin>(); 
    private ArrayList<String> groups = new ArrayList<String>();


    private DatabaseCondition havingCondition;


    private int maxResult = -1;
    private int startFrom = 0;


    private Long[] bindMaxResults = null;


    private boolean lockForUpdate = false;


    private String rootAlias = Constants.DEFAULT_ROOT_ALIAS;


	private Boolean distinct;


	private String distinctCriteria = null;

	
	
    public DatabaseSelectAllStatement(String tableName, boolean loadAll, boolean cacheStatement) {
        super(tableName);
        this.loadAll = loadAll;
        this.cacheStatement = cacheStatement;
    }

	public DatabaseSelectAllStatement(String tableName, boolean loadAll) {
        super(tableName);
        this.loadAll = loadAll;
    }

    public String assemblSQL(DatabaseConnection conn) throws RepositoryException {
        StringBuffer sb = new StringBuffer();
        sb.append("select ");
        if (distinct != null && distinct){
        	sb.append(" DISTINCT ");
        	if (distinctCriteria != null){
        		sb.append("(");
        		sb.append(distinctCriteria);
        		sb.append(") ");
        	}
        }
        assembleSQLFields(sb);
        sb.append(" from ");
        sb.append(getTableName(conn.getDialect()));
        sb.append(" ");
        sb.append(rootAlias);
        
        //add left outer joins
        for(Iterator it = leftOuterJoins.iterator() ; it.hasNext();){
            DatabaseJoin j = (DatabaseJoin) it.next();
            j.generateSQLFromFragment(sb, conn);
        }
        
        //add joins
        for(Iterator it = joins.iterator() ; it.hasNext();){
            DatabaseJoin j = (DatabaseJoin) it.next();
            j.generateSQLFromFragment(sb, conn);
        }
        
        // add conditions
        if (conditions.size() > 0){
            addWhere(sb, conn.getDialect());
            for(Iterator it = conditions.iterator() ; it.hasNext() ; ){
                DatabaseCondition condition = (DatabaseCondition) it.next();
                sb.append(" ( ");
                sb.append(condition.createSQLPart(rootAlias, conn));
                sb.append(" ) ");
                if (it.hasNext()){
                    sb.append(" AND ");
                }
            }
        }
        if (conditions.size() == 0 && (maxResult > -1 || lockForUpdate) ){
            addWhere(sb, conn.getDialect());
            sb.append(" 1=1 ");
        }
        
        //add order by
        if (orders.size() > 0){
            sb.append(" ORDER BY ");
            for(Iterator<Order> it = orders.iterator() ; it.hasNext() ; ){
                Order o = it.next();
                if (o.getField().indexOf(".")< 0){
                    sb.append(rootAlias);
                    sb.append(".");
                }
                sb.append(o.getField());
                if (o.getDesc()){
                    sb.append(" desc ");
                }
                if (it.hasNext()){
                    sb.append(",");
                }
            }
        }
        
        
        //add group by
        if (groups.size() > 0){
            sb.append(" GROUP BY ");
            for(Iterator it = groups.iterator() ; it.hasNext() ; ){
                sb.append(it.next());
                if (it.hasNext()){
                    sb.append(",");
                }
            }
        }
        //add having
        if (havingCondition != null){
            sb.append(" HAVING ");
            sb.append(havingCondition.createSQLPart(rootAlias,conn));
        }
        
        if (maxResult > -1 || startFrom > 0){
            this.bindMaxResults  = conn.getDialect().limitResults(sb, startFrom, maxResult, lockForUpdate);
        } 
        if (lockForUpdate){
            ((AbstractDatabaseDialect)conn.getDialect()).addForUpdateAfterStatement(sb);
        }

        return sb.toString();
    }

    private void addWhere(StringBuffer sb, DatabaseDialect dialect) {
        if (lockForUpdate){
            ((AbstractDatabaseDialect)dialect).addForUpdateWherePart(sb);
        }
        sb.append(" where ");
    }

    protected void assembleSQLFields(StringBuffer sb) {
        if (resultColumn.size() == 0){
            sb.append(getRootAlias()).append(".*");
            for(DatabaseJoin j: joins){
            	sb.append(",");
            	sb.append(j.getTableAlias());
            	sb.append(".*");
            }
            for(DatabaseJoin j: leftOuterJoins){
            	sb.append(",");
            	sb.append(j.getTableAlias());
            	sb.append(".*");
            }
            /*sb.append(rootAlias);
            sb.append(".*");
            
            for(DatabaseJoin j:leftOuterJoins){
                if (j.isAddToResult()){
                	sb.append(", ");
                    sb.append(j.getTableAlias());
                    sb.append(".* ");
                }
            }
            for(DatabaseJoin j:joins){
                if (j.isAddToResult()){
                	sb.append(", ");
                    sb.append(j.getTableAlias());
                    sb.append(".* ");
                }
            }*/

        } else {
            for(Iterator it = resultColumn.iterator() ; it.hasNext() ; ){
                String name = (String) it.next();
                String _name = name;
                if (_name.toLowerCase().indexOf(" as ") < 0){
                    if (name.indexOf(".") < 0){
                        sb.append(rootAlias);
                        sb.append(".");
                        //_name = rootAlias+"."+_name;
                    }
                    sb.append(_name);
                    if (!_name.contains("*")){
                        sb.append(" as ");
                        sb.append(_name.replace('.', '_'));
                    }
                } else {
                    sb.append(_name);
                }
                if (it.hasNext()){
                    sb.append(", ");
                }
            }
        }
        
    }

    protected boolean isAutoCloseStatement() {
        if (loadAll){
            return true;
        } else {
            return false;
        }
    }

    public int applyParameters(PreparedStatement st, DatabaseConnection conn, int startPos) throws RepositoryException {
        int pos = startPos;
        int total = 0;
        
        for(Iterator it = leftOuterJoins.iterator() ; it.hasNext();){
            DatabaseJoin j = (DatabaseJoin) it.next();
            int t = j.bindParameters(pos+total, conn, st);
            total+= t;
        }
        
        //add joins
        /*for(Iterator it = joins.iterator() ; it.hasNext();){
            DatabaseJoin j = (DatabaseJoin) it.next();
            j.generateSQLFromFragment(sb, conn);
        }*/
        
        
        for(Iterator it = conditions.iterator() ; it.hasNext() ; ){
            DatabaseCondition condition = (DatabaseCondition) it.next();
            int t = condition.bindParameters(pos+total, conn, st);
            total+= t;
        }
        if (havingCondition != null){
            total+= havingCondition.bindParameters(pos+total, conn, st);
        }
        if (bindMaxResults != null){
            try {
                for(Long v:bindMaxResults){
                    st.setLong((total++)+1, v);
                }
            } catch (SQLException e) {
                throw new RepositoryException();
            }
        }
        return total;
    }
    
    protected void processResulSet(DatabaseConnection conn) throws RepositoryException {
        if (loadAll){
            try {
                while (resultSet.next()){
                    RowMap row = buildRow();
                    rows.add(row);
                }
            } catch (SQLException exc){
                throw new RepositoryException("Error processing result set", exc);
            }
            position = 0;
            LogUtils.debug(log, "Result size : "+rows.size());
        }
    }   
    
    public RowMap nextRow() throws RepositoryException{
        if (!hasNext()){
            throw new RepositoryException("No more rows");
        }
        if (loadAll){
            RowMap result = (RowMap) rows.get(position++);
            return result;
        } else {
            RowMap r = nextRow;
            nextRow = null;
            return r;
        }
    }
    
    public List<RowMap> getAllRows() throws RepositoryException{
    	ArrayList<RowMap> allRows = new ArrayList<RowMap>();
        while(hasNext()){
            RowMap row = nextRow();
            allRows.add(row);
        }
        return allRows;
    }
    
    public boolean hasNext() throws RepositoryException{
        if (loadAll){
            return position < rows.size();
        } else {
            if (nextRow != null){
                return true;
            }
            try {
                if (resultSet.next()){
                    nextRow = buildRow();
                }
            } catch (SQLException e) {
                throw new RepositoryException("Error geting next row", e);
            }
            return nextRow != null;
        }
    }

    public void addCondition(DatabaseCondition condition) {
        conditions.add(condition);
        
    }
    public void addJoin(String table, String table_alias, String idFiled, String fkField) {
    	addJoin(table, table_alias, idFiled, fkField, false);
    }

    public void addJoin(String table, String table_alias, String idFiled, String fkField, boolean addToResult) {
        DatabaseJoin j = new DatabaseJoin(table, table_alias, idFiled, fkField, addToResult);
        this.joins.add(j);
        StringBuffer sb = new StringBuffer();
        sb.append(table_alias);
        sb.append(".");
        sb.append(fkField);
        addCondition(Conditions.eqProperty(sb.toString(), idFiled.indexOf(".") > 0 ? idFiled : rootAlias+"."+idFiled));
    }

    public DatabaseLeftOuterJoin addLeftOuterJoin(String table, String table_alias, String idFiled, String fkField, boolean addToResult) {
        //DatabaseLeftOuterJoin j = new DatabaseLeftOuterJoin(table, table_alias, idFiled, fkField, addToResult, getRootAlias());
        //this.leftOuterJoins.add(j);
        return addLeftOuterJoin(table, table_alias, idFiled, fkField, addToResult, getRootAlias());
    }

    public DatabaseLeftOuterJoin addLeftOuterJoin(String table, String table_alias, String idFiled, String fkField, boolean addToResult, String fkTableAlias) {
        DatabaseLeftOuterJoin j = new DatabaseLeftOuterJoin(table, table_alias, idFiled, fkField, addToResult, fkTableAlias);
        this.leftOuterJoins.add(j);
        return j;
    }

    public DatabaseLeftOuterJoin addLeftOuterJoin(String table, String table_alias, String idFiled, String fkField) {
        return addLeftOuterJoin(table, table_alias, idFiled, fkField, false);
    }


    public void addResultColumn(String fieldName) {
        resultColumn.add(fieldName);
        
    }

    public void addGroup(String groupBy) {
        groups.add(groupBy);
        
    }

    public void addHaving(DatabaseCondition havingCondition) {
        this.havingCondition = havingCondition;
        
    }

    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
        
    }

    public void setMaxResult(int startFrom, int maxResult) {
        this.maxResult = maxResult;
        this.startFrom = startFrom;
    }

    public void setStartFrom(int startFrom) {
        this.startFrom = startFrom;
        this.maxResult = Integer.MAX_VALUE;
    }

    public void setLockForUpdate(boolean value) {
        this.lockForUpdate = value;
        this.loadAll = true;
        
    }

    public String getRootAlias() {
        return rootAlias;
    }

    public void setRootAlias(String rootAlias) {
        this.rootAlias = rootAlias;
    }

    public void addOrder(Order order){
        orders.add(order);
    }
    
    
    public void setIgnoreBLOB(boolean b) {
        super.setIgnoreBLOB(b);
    }

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
		
	}   

	public void setDistinct(String criteria) {
		setDistinct(true);
		this.distinctCriteria  = criteria;
	}   

	private HashMap<String, String> tableAliases = null;
	
    protected HashMap<String, String> buildTableAliasMapping(){
    	if (tableAliases == null){
    		tableAliases = new HashMap<String, String>();
    		tableAliases.put(DatabaseTools.getUpperCase(this._tableName), DatabaseTools.getUpperCase(rootAlias));
            for(DatabaseJoin j:leftOuterJoins){
            	tableAliases.put(DatabaseTools.getUpperCase(j._getTable()), DatabaseTools.getUpperCase(j.getTableAlias()));
            }
            for(DatabaseJoin j:joins){
            	tableAliases.put(DatabaseTools.getUpperCase(j._getTable()), DatabaseTools.getUpperCase(j.getTableAlias()));
            }
    	}
    	return tableAliases;
    }

	public String createStackTrace() {
		// TODO print stack trace
		return "";
	}

    
}



/*
 * $Log: DatabaseSelectAllStatement.java,v $
 * Revision 1.13  2009/01/30 07:15:36  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.12  2009/01/30 07:09:08  maksims
 * *** empty log message ***
 *
 * Revision 1.11  2009/01/28 10:29:07  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.10  2009/01/19 08:36:56  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.9  2008/12/24 11:38:51  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2008/07/16 08:45:05  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/07/09 10:13:08  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2008/07/09 07:50:28  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2008/06/19 11:38:06  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2008/05/19 11:09:02  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/04/29 10:56:00  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2007/10/26 11:00:52  dparhomenko
 * Fix Lock problem in management environment
 *
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.14  2006/11/14 07:37:21  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.13  2006/11/02 09:49:34  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.12  2006/08/17 11:29:32  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.11  2006/08/11 09:04:43  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.10  2006/08/04 12:33:35  maksims
 * #1802426 SQL Framework used to generate queries in DBContentStore
 *
 * Revision 1.9  2006/07/12 07:44:43  dparhomenko
 * PTR#1802389 for update statement
 *
 * Revision 1.8  2006/06/30 14:38:04  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.7  2006/06/30 14:32:41  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.6  2006/06/30 10:34:36  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.5  2006/05/10 09:00:42  dparhomenko
 * PTR#0144983 dynamic table names
 *
 * Revision 1.4  2006/05/03 12:07:08  dparhomenko
 * PTR#0144983 make DatabaseStatement as interface
 *
 * Revision 1.3  2006/04/20 11:43:08  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.2  2006/04/19 08:06:55  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/17 06:47:12  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.9  2006/04/05 14:30:42  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.8  2006/03/27 07:22:21  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.7  2006/03/22 11:18:54  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.6  2006/03/21 13:19:30  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.5  2006/03/13 09:24:42  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.4  2006/03/03 10:33:19  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.3  2006/03/01 11:54:42  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.2  2006/02/16 13:53:07  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/13 12:40:55  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */