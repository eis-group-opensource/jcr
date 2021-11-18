/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.params;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.impl.state2.StoreContainer;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.vf.commons.logging.LogUtils;

public abstract class SQLParameter implements Comparable<SQLParameter>{

    /** Log for this class */
    private static final Log log = LogFactory.getLog(SQLParameter.class);

    
    protected String name;
    //private int sqlType;
    private Object _value;
    
    public SQLParameter(String name, int sqlType, Object value){
        this.name = name;
        //this.sqlType = sqlType;
        this._value = value;
    }

    public String getName() {
        return name;
    }

    public static final SQLParameter create(String name, String value){
        return new StringSQLParameter(name, value);
    }
    

	@Override
	public String toString() {
		return name+"="+_value;
	}
        
    
    public static final SQLParameter create(String name, Boolean value){
        return new BooleanSQLParameter(name, value);
    }
    
    public static final SQLParameter create(String name, Integer value){
        return new IntegerSQLParameter(name, value);
    }
    public static final SQLParameter create(String name, int value){
        return new IntegerSQLParameter(name, new Integer(value));
    }
    
    public static final SQLParameter create(String name, Long value){
        return new LongSQLParameter(name, value);
    }
    
    public static final SQLParameter createSQL(String name, String value){
        return new SQLEqParameter(name, value);
    }
    
    abstract public int _apply(int pos, PreparedStatement st, DatabaseDialect dialect) throws SQLException;
    
    public final int apply(int pos, PreparedStatement st, DatabaseDialect dialect) throws SQLException{
    	if (log.isDebugEnabled()){
    		LogUtils.debug(log, "Set parameter "+(pos)+" to "+_value);
    	}
        if (isEmpty()){
            return setNull(pos, st, dialect);
        } else {
            return _apply(pos, st, dialect);
        }
    }

    protected int setNull(int pos, PreparedStatement st, DatabaseDialect dialect) throws SQLException {
        st.setNull(pos, getSQLType(dialect));
        return 1;
    }

    abstract protected int getSQLType(DatabaseDialect dialect);

    abstract protected boolean isEmpty();

    public static SQLParameter create(String name, boolean value) {
        return new BooleanSQLParameter(name, new Boolean(value));
    }

    public Object getValue() {
        return _value;
    }

    public static SQLParameter _create(StoreContainer sc, String valueColumn, Object valueObject, InternalValue value)  throws RepositoryException{
        if (valueObject instanceof String || valueObject == null){
            return create(valueColumn,(String) valueObject);
        } else if (valueObject instanceof Calendar){
            return create(valueColumn,(Calendar) valueObject);
        } else if (valueObject instanceof Long){
            return create(valueColumn,(Long) valueObject);
        } else if (valueObject instanceof Double){
            return create(valueColumn,(Double) valueObject);
        } else if (valueObject instanceof Boolean){
            return create(valueColumn,(Boolean) valueObject);
        } else if (valueObject instanceof InputStream){
            return create(sc, valueColumn,(InputStream) valueObject, value);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static SQLParameter create(StoreContainer sc, String name, InputStream stream, InternalValue value) throws RepositoryException{
        return new StreamSQLParameter(sc , name, stream, value);
    }

    public static SQLParameter create(String name, InputStream stream, int length) throws RepositoryException{
//    public static StreamSQLParameter2 create(String name, InputStream stream, int length) throws RepositoryException{        
        return new StreamSQLParameter2(name, stream, length);
    }

    public static SQLParameter create(String name, Double value) {
        return new DoubleSQLParameter(name, value);
    }

    public static SQLParameter create(String name, Calendar value) {
        return new CalendarSQLParameter(name, value);
    }

	public void registerParameter(DatabaseConnection conn, StringBuffer sb) throws RepositoryException {
		sb.append("?");
		
	}
	
	public int compareTo(SQLParameter o) {
		return name.compareTo(o.name);
	}

	public static Log getLog() {
		return log;
	}

	
	public boolean _equals(SQLParameter other){
		return name.equals(other.name) && getClass().equals(other.getClass());
	}
}



/*
 * $Log: SQLParameter.java,v $
 * Revision 1.2  2007/08/08 07:45:31  dparhomenko
 * PTR#1805084 fix ptr
 *
 * Revision 1.1  2007/04/26 08:59:17  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.10  2007/03/29 14:16:09  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.9  2006/11/21 07:19:27  dparhomenko
 * PTR#1803402 fix errors
 *
 * Revision 1.8  2006/10/17 10:46:50  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.7  2006/09/28 12:23:31  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.6  2006/09/26 12:31:47  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.5  2006/09/07 10:37:06  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.4  2006/08/08 13:08:41  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.3  2006/08/04 12:33:37  maksims
 * #1802426 SQL Framework used to generate queries in DBContentStore
 *
 * Revision 1.2  2006/07/12 10:10:23  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/06 09:29:17  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.11  2006/07/06 07:54:37  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.10  2006/07/04 10:52:07  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.9  2006/07/03 11:45:38  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.8  2006/07/03 09:25:22  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.7  2006/07/03 09:04:50  zahars
 * PTR#0144986MIME type detection command introduced
 *
 * Revision 1.6  2006/06/30 14:32:47  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.5  2006/06/30 12:40:44  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.4  2006/05/03 14:25:53  dparhomenko
 * PTR#0144983 dn stores
 *
 * Revision 1.3  2006/04/20 11:42:51  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.2  2006/04/19 08:06:52  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/17 06:46:40  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.13  2006/04/05 14:30:41  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.12  2006/04/03 13:08:21  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.11  2006/03/29 12:56:33  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.10  2006/03/28 15:45:22  maksims
 * #0144986 additional parameter added to Store.put method
 *
 * Revision 1.9  2006/03/27 14:57:45  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.8  2006/03/17 10:12:43  dparhomenko
 * PTR#0144983 add support for indexable_data
 *
 * Revision 1.7  2006/03/15 09:32:04  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.6  2006/03/14 11:55:51  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.5  2006/03/01 11:54:48  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.4  2006/02/20 15:32:30  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.3  2006/02/17 13:46:45  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 * Revision 1.2  2006/02/16 13:53:08  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/10 15:50:26  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */