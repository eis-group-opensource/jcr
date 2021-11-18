/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {

    public static byte[] getBytes(InputStream in) throws IOException {
        ByteArrayOutputStream outS = new ByteArrayOutputStream();
        byte b[]= new byte[134000];
        int readed = 0;
        while ((readed = in.read(b, 0, b.length)) > 0){
            outS.write(b, 0, readed);
        }
        return outS.toByteArray();
    }



}


/*
 * $Log: StreamUtils.java,v $
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/04/19 08:06:46  dparhomenko
 * PTR#0144983 restructurization
 *
 */