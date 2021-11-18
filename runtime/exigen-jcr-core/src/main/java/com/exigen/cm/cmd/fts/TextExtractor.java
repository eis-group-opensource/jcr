/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.cmd.fts;

import java.io.InputStream;
import java.io.Writer;

/**
 * Implementation of this interface is responsible for text extraction
 * from provided InputStream.
 * 
 * 
 * @author Maksims
 *
 */
public interface TextExtractor{
    
    /**
     * Extracts text from binary. It's a responsibility of the exstractor to close
     *  input stream and Writer 
     * @param mimeType MIME type of the content
     * @param source content
     * @param target extracted text
     * @throws Exception 
     * 
     */
    public void extract(String mimeType, InputStream source, Writer target)throws Exception;
    
    
    
}

/*
 * $$Log: TextExtractor.java,v $
 * $Revision 1.1  2007/04/26 08:59:49  dparhomenko
 * $PTR#1804279 migrate JCR to maven from B302 directory
 * $
 * $Revision 1.9  2006/07/17 09:07:00  zahars
 * $PTR#0144986 Comments added
 * $
 * $Revision 1.8  2006/07/14 13:58:24  zahars
 * $PTR#0144986 Measured stream deleted
 * $
 * $Revision 1.7  2006/06/27 11:49:00  zahars
 * $PTR#0144986 Extractor interface changed
 * $
 * $Revision 1.6  2006/06/27 08:48:52  maksims
 * $#1801897 File header/footer added
 * $$
 */