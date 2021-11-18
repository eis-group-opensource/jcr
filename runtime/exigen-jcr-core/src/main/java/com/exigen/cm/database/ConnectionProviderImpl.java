/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database;

import static com.exigen.cm.Constants.DEFAULT_ID_RANGE;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_DIALECT_CLASSNAME;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_JNDI_NAME;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.dialect.AbstractDatabaseDialect;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.vf.commons.logging.LogUtils;
import com.mchange.v2.c3p0.DataSources;
import com.mchange.v2.c3p0.PoolConfig;

public class ConnectionProviderImpl extends ConnectionProvider{

    private static Log log = LogFactory.getLog(ConnectionProviderImpl.class);
    
    private DataSource ds;
    private String datasourceName;
    private DatabaseDialect dialect;
    
    private Long currentId = new Long(0);
    private Long maxId = new Long(0);     

    public ConnectionProviderImpl(){
    	log.debug("Instantiate ConnectionproviderImpl");
    }
    
	public DatabaseConnection createConnection(final String userName, final String password) throws RepositoryException {
	//  Connection conn;
		try {
		  //conn = ds.getConnection(userName, password);
			DatabaseConnection c = new DatabaseConnection(this) {
			    @Override
			    protected Connection createConnection() throws Exception {
			        return ds.getConnection(userName, password);
			    }
			};
	        return c;
		} catch (Exception e) {
            throw new RepositoryException("Error getting connection for user "+userName, e);
		}
    }

    public DatabaseConnection createConnection() throws RepositoryException {
        try {
        	/*try {
		        	Connection conn2 = org.springframework.jdbc.datasource.DataSourceUtils.getConnection(ds);
		        	System.out.println(conn2);
		        	conn2.close();
        	} catch (Exception exc){
        		exc.printStackTrace();
        	}*/
           DatabaseConnection c = new DatabaseConnection(this) {
           		@Override
           		protected Connection createConnection() throws Exception {
                    Connection conn = ds.getConnection();
                    if (TransactionHelper.getInstance().getTransactionManager() == null || TransactionHelper.getInstance().getTransactionManager().getTransaction() == null){
                        setAutoCommit(conn, false);
                    } 
                    dialect.sessionSetup(conn);
                    	return conn;
                }
            };
            return c;
//            Connection conn = ds.getConnection();
//            return buildConnection(conn);
        } catch (Exception exc){
            throw new RepositoryException("Error getting connection ", exc);
        }
    }

	private DatabaseConnection buildConnection(Connection conn) throws RepositoryException, SQLException {
		DatabaseConnection c = new DatabaseConnection(this, conn);
		if (TransactionHelper.getInstance().getTransactionManager() == null || TransactionHelper.getInstance().getTransactionManager().getTransaction() == null){
		    c.setAutoCommit(false);
		} 
		/*if (getDialect().getDatabaseVendor().equals(DatabaseDialect.VENDOR_ORACLE)){
		    Statement st = conn.createStatement();
		    st.execute("alter session set hash_join_enabled=false");
		    st.execute("alter session set optimizer_index_caching=25");
		    st.close();
		}*/
		dialect.sessionSetup(conn);
		return c;
	}

    public DatabaseDialect getDialect() {
        return dialect;
    }

    public synchronized Long nextId(DatabaseConnection conn) throws RepositoryException {
        
        if (maxId.equals(currentId) || currentId > maxId) {
            currentId = getDialect().reserveIdRange(conn, DEFAULT_ID_RANGE);
            maxId = new Long(currentId.longValue() + DEFAULT_ID_RANGE.longValue()) -2 ;        
        }                        
        currentId = new Long(currentId.longValue() + 1);
        return currentId;
     }


    public void configure(Map config, DataSource _ds) throws RepositoryException {
        String dialectClassName = (String)config.get(PROPERTY_DATASOURCE_DIALECT_CLASSNAME);
    	if (_ds != null){
    		this.ds = _ds;
    	} else {
	        this.datasourceName = (String)config.get(PROPERTY_DATASOURCE_JNDI_NAME);         
	
	        String driverName = (String)config.get(Constants.PROPERTY_DATASOURCE_DRIVER_CLASSNAME);
	
	
	        try {
	            InitialContext ctx = new InitialContext();
	            try {
	                String dsName = datasourceName;
	                ds = (DataSource) ctx.lookup(dsName);
	            } catch (javax.naming.NameNotFoundException e) {
	                if (TransactionHelper.getInstance().getType() == TransactionHelper.APPLICATION_SERVER_JBOSS){
	                	String dsName = datasourceName;
	                    if (!dsName.startsWith("java:")){
	                        dsName = "java:"+dsName;
	                    }
	                    try {
	                    	ds = (DataSource) ctx.lookup(dsName);
	    	            } catch (javax.naming.NameNotFoundException e1) {
	    	            	
	    	            }
	                } 
	                if (ds == null){
	                    try {
                            ds = (DataSource) ctx.lookup("java:comp/env/"+datasourceName);
                            datasourceName = "java:comp/env/"+datasourceName;
                        } catch (javax.naming.NameNotFoundException e1) {
                            
                        }
	                }
	                
	            }
	            if (ds == null){
	            	LogUtils.info(log, "DataSource with name \"{0}\" not found in JNDI. Creating and binding...", datasourceName);
	                if (ds == null){
	        	        if ( !(dialectClassName != null && !dialectClassName.trim().equals(""))
	        	        		&& !(driverName != null && !driverName.trim().equals(""))
	        	        		) {
	        	            throw new RepositoryException("Database dialect class name not provided in configuration.");
	        	        } 
	                }
	                bindDatasource(config, datasourceName, dialectClassName, ctx);            	
	            }
	            if (ds == null){
	                ds = (DataSource) ctx.lookup(datasourceName);
	                if (ds == null){
	                	throw new RepositoryException("Cannot bind datasource. Current JNDi implementation ("+ctx.getClass().getName()+" does not support object binding");
	                }
	            }
	        } catch (Exception exc){
	            throw new RepositoryException("Error getting datasource", exc);
	        }
    	}
        

        
        LogUtils.debug(log, "Following database dialect \"{0}\" is used.", dialectClassName);            
        try {
        	if ( (dialectClassName == null || dialectClassName.trim().equals(""))){
        		dialectClassName = findDialect(config);
        	}
        	this.dialect = (AbstractDatabaseDialect)Class.forName(dialectClassName).newInstance();
            dialect.setConnectionProvider(this);
        } catch (Exception e) {
            LogUtils.error(log, e.getMessage(), e);
            throw new RepositoryException("Error initilizing configuration.", e);
        }

        
    }

    
    
    private String findDialect(Map<String, String> config) throws RepositoryException{
          
        /*
Oracle :
Product name :Oracle
Product version :Oracle9i Release 9.2.0.6.0 - Production
JServer Release 9.2.0.6.0 - Production
Product version :9.2
Driver name :Oracle JDBC driver
DB2 :
Product name :DB2 UDB for AS/400
Product version :05.02.0000 V5R2m0
Product version :5.0
Driver name :AS/400 Toolbox for Java JDBC Driver
         */
    	String detectedDialect = null;
    	Connection connection = null;
        try {
        	try {
        		connection = ds.getConnection();
        	} catch(SQLException exc){
        		throw new RepositoryException("Error getting connection", exc);
        	}
            DatabaseMetaData md = connection.getMetaData();
            String productName = md.getDatabaseProductName();
            int majorVersion = -1;
            int minorVersion = -1;
            int productVersion = -1;
            try {
                productVersion = md.getDatabaseMajorVersion();
            } catch (Throwable exc){
                //do nothing
                ;
            }
            try {
                majorVersion = md.getDatabaseMajorVersion();
            } catch (Throwable exc){
                //do nothing
                ;
            }
            try {
                minorVersion = md.getDatabaseMinorVersion();
            } catch (Throwable exc){
                //do nothing
                ;
            }
            
            if (productName.indexOf("Oracle") > -1){
            	//check Driver class version
            	int drMajor = connection.getMetaData().getDriverMajorVersion();
            	int drMinor = connection.getMetaData().getDriverMinorVersion();
            	
            	//evaluate version
            	CallableStatement cstmt = connection.prepareCall("call DBMS_UTILITY.DB_VERSION(?,?)");
            	cstmt.registerOutParameter(1, java.sql.Types.VARCHAR);
            	cstmt.registerOutParameter(2, java.sql.Types.VARCHAR);
            	cstmt.executeQuery();
                String oracleVersion = cstmt.getString(1);
                StringTokenizer st = new StringTokenizer(oracleVersion, ".");
                String v1 = st.nextToken();
                String v2 = st.nextToken();
                String v3 = st.nextToken();
                String v4 = st.nextToken();
                String v5 = st.nextToken();

            	
                Integer v1i = Integer.parseInt(v1);
                Integer v2i = Integer.parseInt(v2);
                Integer v3i = Integer.parseInt(v3);
                Integer v4i = Integer.parseInt(v4);
                Integer v5i = Integer.parseInt(v5);
            	
            	if (drMajor < 10 || (drMajor ==  10 && drMinor <=2)){
            		log.warn("Please use Oracle driver version 10.2 or higher");
            	}
            	
                //oracle
                if (productVersion == 9 || (productVersion == -1) || (productVersion == 10 && minorVersion == 1)){
                    detectedDialect =  "com.exigen.cm.database.dialect.oracle.OracleDatabaseDialect";
                    return detectedDialect;
                } else if (productVersion == 11 && v2i >= 1 && (v3i > 0 || (v3i == 0 && v4i >=7))){ //11.1.0.7.0
                	detectedDialect = "com.exigen.cm.database.dialect.oracle.Oracle11DatabaseDialect";
                    return detectedDialect;
                } else {
                	detectedDialect = "com.exigen.cm.database.dialect.oracle.Oracle10DatabaseDialect";
                	return detectedDialect;
                }
            }
            /*if (productName.indexOf("DB2") > -1){
                //DB2
                if (productName.indexOf("400") > -1){
                    props.put("hibernate.dialect", DB2400Dialect.class.getName());
                } else if (productName.indexOf("390") > -1){
                    props.put("hibernate.dialect", DB2390Dialect.class.getName());
                } else {
                    props.put("hibernate.dialect", DB2Dialect.class.getName());
                }
                return;
            }*/
            if (productName.indexOf("Microsoft SQL Server") > -1){
                //MS SQL 2000
//            	if (majorVersion == 9 ){
                if (majorVersion >= 9 ){
            		detectedDialect = "com.exigen.cm.database.dialect.mssql.MsSQL2005DatabaseDialect";
            	} else {
            		detectedDialect = "com.exigen.cm.database.dialect.mssql.MsSQLDatabaseDialect";
            	}
            	
            	return detectedDialect;
            }
            if (productName.startsWith("HSQL Database Engine")){
            	detectedDialect =  "com.exigen.cm.database.dialect.hsql.HyperSonicSQLDatabaseDialect";
            	return detectedDialect;
            }
            throw new RepositoryException("Cannot detect dialect for "+ productName);

        } catch (SQLException e1) {
        	e1.printStackTrace();
        	throw new RepositoryException("Cannot detect dialect");
        } finally {
        	if (detectedDialect != null){
        		config.put(PROPERTY_DATASOURCE_DIALECT_CLASSNAME, detectedDialect);
        	}
        	if (connection != null){
        		try {
					connection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
	}

	private static void bindDatasource(Map config, String jndiName, String dialectClassName, InitialContext ctx) throws RepositoryException, ClassNotFoundException, SQLException, NamingException {
        //InitialContext ctx = new InitialContext();            
        DataSource pooled;
        String driver = (String)config.get(Constants.PROPERTY_DATASOURCE_DRIVER_CLASSNAME);
        if (driver == null){
            //get from dialect
            DatabaseDialect dialect;
            try {
                dialect = (DatabaseDialect) Class.forName(dialectClassName).newInstance();
            } catch (ClassNotFoundException e) {
                LogUtils.error(log, e.getMessage(), e);
                String msg = "Dialect class \"{0}\" not found!";
                msg = MessageFormat.format(msg, new Object[]{dialectClassName});
                throw new RepositoryException(msg);
            } catch (Exception e) {
                LogUtils.error(log, e.getMessage(), e);
                String msg = "Error instantiating dialect \"{0}\": {1}";
                msg = MessageFormat.format(msg, new Object[]{dialectClassName, e.getMessage()});
                throw new RepositoryException(msg);
            }
            driver = dialect.getJDBCDriverName();
        }
        if (driver == null) {
            String propertyNotFound = MessageFormat.format(Constants.PROPERTY_NOT_FOUND, new Object[]{Constants.PROPERTY_DATASOURCE_DRIVER_CLASSNAME});
            LogUtils.error(log, propertyNotFound);
            throw new RepositoryException(propertyNotFound);                    
        }
        String connUrl = (String)config.get(Constants.PROPERTY_DATASOURCE_URL);
        if (connUrl == null) {
            String propertyNotFound = MessageFormat.format(Constants.PROPERTY_NOT_FOUND, new Object[]{Constants.PROPERTY_DATASOURCE_URL});
            LogUtils.error(log, propertyNotFound);
            throw new RepositoryException(propertyNotFound);                    
        }
        String user = (String)config.get(Constants.PROPERTY_DATASOURCE_USER);
        if (user == null) {
            String propertyNotFound = MessageFormat.format(Constants.PROPERTY_NOT_FOUND, new Object[]{Constants.PROPERTY_DATASOURCE_USER});
            LogUtils.error(log, propertyNotFound);
            throw new RepositoryException(propertyNotFound);                                        
        }     
        String password = (String)config.get(Constants.PROPERTY_DATASOURCE_PASSWORD);
        if (password == null) {
            String propertyNotFound = MessageFormat.format(Constants.PROPERTY_NOT_FOUND, new Object[]{Constants.PROPERTY_DATASOURCE_PASSWORD});
            LogUtils.error(log, propertyNotFound);
            throw new RepositoryException(propertyNotFound);                    
        }                   
        
        
        Class.forName(driver);
        /*Driver driverInstance;
		try {
			driverInstance = (Driver)Class.forName(driver).newInstance();
		} catch (InstantiationException e) {
			throw new RepositoryException("Error instantiating driver:"+e.getMessage());
		} catch (IllegalAccessException e) {
			throw new RepositoryException("Error instantiating driver:"+e.getMessage());
		}
        
		
        DriverManager.registerDriver(driverInstance);
        DriverManager.registerDriver(new com.microsoft.jdbc.sqlserver.SQLServerDriver());
        */
        DataSource unpooled = DataSources.unpooledDataSource(
                        connUrl, 
                        user, 
                        password);
        PoolConfig cfg = new PoolConfig();
        cfg.setAcquireIncrement(1);
        cfg.setInitialPoolSize(1);
        cfg.setAcquireRetryAttempts(20);
        cfg.setMaxIdleTime(60);
        cfg.setMinPoolSize(2);	
        //cfg.setMaxStatements(1000);
        cfg.setMaxPoolSize(200);
        pooled = DataSources.pooledDataSource(unpooled, cfg);
        ctx.rebind(jndiName, pooled);
        LogUtils.info(log, "DataSource bound to nameservice under the name \"{0}\"", jndiName);
    }

    public void setAllowCommitRollback(DatabaseConnection conn, boolean value) {
        conn.allowCommitRollback = value;
    }

	public Connection createSQLConnection() throws RepositoryException {
		try {
			return ds.getConnection();
		} catch (SQLException e) {
			throw new RepositoryException(e);
		}
	}
    
    
}


/*
 * $Log: ConnectionProviderImpl.java,v $
 * Revision 1.14  2011/09/09 09:42:55  jkersovs
 * EPB-335 'DatabaseConnection statements cache breaks on WAS 7.0.0.17'
 * Fix provided by V. Beilins
 *
 * Revision 1.13  2009/07/27 14:49:40  maksims
 * mssql dialect 2005 made default for MSSQLs higher then version 2005
 *
 * Revision 1.12  2009/05/15 07:11:40  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.11  2009/02/26 06:56:23  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.10  2008/07/02 07:17:41  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.9  2008/05/15 06:46:41  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2008/03/28 13:46:00  dparhomenko
 * Autmaticaly unlock orphaned locks(session scoped)
 *
 * Revision 1.7  2007/10/09 07:34:53  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.6  2007/09/03 14:09:51  dparhomenko
 * remove geronimo dependency
 *
 * Revision 1.5  2007/07/05 08:52:15  dparhomenko
 * PTR#0152003 fix insert statemnt for oracle blobs
 *
 * Revision 1.4  2007/06/06 07:51:21  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.3  2007/06/01 09:39:20  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.2  2007/05/31 08:54:22  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.1  2007/04/26 09:00:52  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.15  2007/02/26 13:14:52  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.14  2007/02/22 09:24:28  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.13  2007/02/02 08:49:00  dparhomenko
 * PTR#1803853 fix session scope lock
 *
 * Revision 1.12  2007/01/31 08:35:54  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.11  2006/12/15 09:36:46  dparhomenko
 * PTR#0149618 fix child node remove premission check
 *
 * Revision 1.10  2006/12/07 09:00:05  dparhomenko
 * PTR#1803559 fix trigger check
 *
 * Revision 1.9  2006/11/30 14:00:57  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.8  2006/11/30 11:00:05  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.7  2006/10/30 15:03:37  dparhomenko
 * PTR#1803272 a lot of fixes
 *
 * Revision 1.6  2006/09/08 11:43:35  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.5  2006/09/07 10:37:07  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.4  2006/07/18 12:51:17  zahars
 * PTR#0144986 INFO log level improved
 *
 * Revision 1.3  2006/07/14 11:54:29  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/07/13 13:01:31  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/06/22 12:00:28  dparhomenko
 * PTR#0146672 move operations
 *
 * Revision 1.12  2006/06/02 07:21:28  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.11  2006/05/26 09:25:48  dparhomenko
 * PTR#1801955 add JBOSS support
 *
 * Revision 1.10  2006/05/26 08:13:59  dparhomenko
 * PTR#1801955 add JBOSS support
 *
 * Revision 1.9  2006/05/25 14:49:27  dparhomenko
 * PTR#1801955 add JBOSS support
 *
 * Revision 1.8  2006/05/18 14:53:54  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 * Revision 1.7  2006/05/17 14:53:20  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 * Revision 1.6  2006/05/10 08:04:12  dparhomenko
 * PTR#0144983 build 004
 *
 * Revision 1.5  2006/05/08 14:45:12  dparhomenko
 * PTR#0144983 fixes
 *
 * Revision 1.4  2006/04/21 12:11:34  dparhomenko
 * PTR#0144983 build procedure
 *
 * Revision 1.3  2006/04/20 11:42:46  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.2  2006/04/19 13:13:34  dparhomenko
 * PTR#0144983 tests
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/12 08:56:02  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/12 08:30:49  dparhomenko
 * PTR#0144983 restructurization
 *
 */