/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts.extract;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.catcode.odf.OpenDocumentTextInputStream;
import com.exigen.cm.cmd.fts.TextExtractor;

/**
 * OpenOffice documents extractor
 * based on  http://books.evc-cit.info/odf_utils/
 * 
 */
public class ODFTextExtractor implements TextExtractor {

    public void extract(String mimeType, InputStream source, Writer target)
                    throws IOException {
        
        ZipInputStream z = new ZipInputStream(source);
        try {
            for (ZipEntry e = z.getNextEntry(); e != null; e = z.getNextEntry()){
                if (e.getName().equals("content.xml")){
                    OpenDocumentTextInputStream odf=new OpenDocumentTextInputStream(z);
                    InputStreamReader isr=new InputStreamReader(odf,"UTF-8");
                    char[] buf = new char[8196];
                    int len;
                    while ( (len=isr.read(buf))>0 ){
                        target.write(buf,0,len);
                    }
                    break;
                }    
            }
        }
        finally {
            target.close();
            z.close();
        }
    }
}    


/*
 * $Log: ODFTextExtractor.java,v $
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
