/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store;

import java.io.IOException;

/**
 * Declares seek API.
 * @author Maksims
 *
 */
public interface Seekable {
    
    /**
     * Sets data pointer offset mesured from the beginning of data.
     * @param position
     * @throws IOException
     */
    public void seek(long position) throws IOException;
}
/*
 * $Log: Seekable.java,v $
 * Revision 1.1  2007/04/26 08:59:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/04/17 06:47:14  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/12 12:18:43  maksims
 * #0144986 Seekable support added to File Store
 *
 */
