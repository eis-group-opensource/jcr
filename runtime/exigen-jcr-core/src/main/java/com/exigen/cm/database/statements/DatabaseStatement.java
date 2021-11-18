/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;

public interface DatabaseStatement extends DatabaseOperation{

    public boolean execute(DatabaseConnection conn) throws RepositoryException;


}


/*
 * $Log: DatabaseStatement.java,v $
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2007/01/24 08:46:43  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.5  2006/05/03 12:07:08  dparhomenko
 * PTR#0144983 make DatabaseStatement as interface
 *
 * Revision 1.4  2006/04/20 14:07:04  dparhomenko
 * PTR#0144983 bild procedure
 *
 * Revision 1.3  2006/04/20 08:20:32  dparhomenko
 * PTR#0144983 stored procedure for Hypersonic check security
 *
 * Revision 1.2  2006/04/19 08:06:55  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/17 06:47:12  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.11  2006/04/05 12:48:05  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.10  2006/03/31 12:48:32  maksims
 * #0144985 BLOBs and CLOBs made aren't loaded in RowMap when building Row
 *
 * Revision 1.9  2006/03/24 08:51:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.8  2006/03/23 14:26:48  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.7  2006/03/14 11:55:38  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.6  2006/03/03 10:33:19  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.5  2006/03/01 11:54:42  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.4  2006/02/27 15:02:50  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.3  2006/02/16 13:53:07  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.2  2006/02/13 12:40:55  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 * Revision 1.1  2006/02/10 15:50:33  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */