/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts.extract;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import org.apache.poi.hwpf.HWPFDocument;

import com.exigen.cm.cmd.fts.TextExtractor;

/**
 * MS Word text extractor based on POI
 * 
 */
public class POIWordTextExtractor implements TextExtractor{

    public void extract(String mimeType, InputStream source, Writer target)  throws IOException{
        try {
            HWPFDocument doc =  new HWPFDocument(source);
            String s = doc.getRange().text();
            target.write(s);
        }
        finally {
            source.close();
            target.close();
        }
    }
    
}


/*
 * $Log: POIWordTextExtractor.java,v $
 * Revision 1.1  2007/04/26 08:58:50  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/07/06 09:33:17  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/06/28 12:17:05  zahars
 * PTR#0144986 Non OO extractors added
 *
 */
