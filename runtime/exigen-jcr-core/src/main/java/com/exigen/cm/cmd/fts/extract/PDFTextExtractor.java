/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.cmd.fts.extract;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

import com.exigen.cm.cmd.fts.TextExtractor;

/**
 * @author Maksims
 *
 */
public class PDFTextExtractor implements TextExtractor {

    /** 
     * @inheritDoc
     */
    public void extract(String mimeType, InputStream source, Writer target) throws IOException {
        PDDocument doc = null;
        try {  
            doc = PDDocument.load(source);
            PDFTextStripper textStripper = new PDFTextStripper();
            String text = textStripper.getText(doc);
            target.write(text);
        } 
        finally {
            try {
                if (doc != null)
                    doc.close();
            }
            finally {
                target.close();
            }
        }
    }
    
}
/*
 * $$Log: PDFTextExtractor.java,v $
 * $Revision 1.1  2007/04/26 08:58:50  dparhomenko
 * $PTR#1804279 migrate JCR to maven from B302 directory
 * $
 * $Revision 1.8  2006/07/06 09:33:17  dparhomenko
 * $PTR#1802310 Add new features to DatabaseConnection
 * $
 * $Revision 1.7  2006/06/27 11:48:58  zahars
 * $PTR#0144986 Extractor interface changed
 * $
 * $Revision 1.6  2006/06/27 08:48:54  maksims
 * $#1801897 File header/footer added
 * $$
 */