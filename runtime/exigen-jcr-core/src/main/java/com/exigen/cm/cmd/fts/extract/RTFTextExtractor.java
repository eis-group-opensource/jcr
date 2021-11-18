/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts.extract;

import java.io.InputStream;
import java.io.Writer;

import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;

import com.exigen.cm.cmd.fts.TextExtractor;

/**
 * RTF text exstractor
 * It works for English language only.
 * I failed to run it with Latvian.
 */
public class RTFTextExtractor implements TextExtractor {

    public void extract(String mimeType, InputStream source, Writer target)
                    throws Exception {
        
        String plainText=null;
        try{
            RTFEditorKit rtf = new RTFEditorKit();
            Document doc = rtf.createDefaultDocument();
            rtf.read(source, doc, 0);
            plainText=doc.getText(0, doc.getLength());
            
           target.write(plainText);
        }
        finally {
            source.close();
            target.close();
        }
    }

}


/*
 * $Log: RTFTextExtractor.java,v $
 * Revision 1.1  2007/04/26 08:58:50  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/07/06 09:33:17  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/06/28 12:17:06  zahars
 * PTR#0144986 Non OO extractors added
 *
 * Revision 1.1  2006/06/27 12:54:21  zahars
 * PTR#0144986 RTF text extractor added
 *
 */
