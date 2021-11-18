/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/


package com.exigen.cm.cmd.fts.extract;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.cmd.fts.TextExtractor;

/**
 * @author Maksims
 *
 */
public class PlainTextExtractor implements TextExtractor {

    private Log log = LogFactory.getLog(PlainTextExtractor.class);
    
    /**
     * Detects InputStream charset and copy it to target 
     * 
     */
    public void extract(String mimeType, InputStream source, Writer target) throws IOException{
        String cs = null;
        PushbackInputStream pis = new PushbackInputStream(source);
        Reader r = null;
        
        try {
            int flag = pis.read();
            if (flag != -1) { // file is not empty
                switch(flag){
                    case 0xFF: // UTF-16 
                        log.debug("Detected charset UTF-16");
                        cs = "UTF-16";
                        break;
                    case 0xFE:
                        cs = "UTF-16BE";
                        break;                
                    case 0xEF:
                        cs = "UTF-8";
                        log.debug("Detected charset UTF-8");                
                        break;
                    default:
                        cs = "ASCII";
                }

                log.debug("Detected charset: " + cs );
                pis.unread(flag);
                r = new InputStreamReader(pis,cs);        
                
                char[] buff = new char[8196];
                int read=0;
                while((read=r.read(buff))>0){
                    target.write(new String(buff, 0, read).toLowerCase());
                }
            }
        }    
        finally {
            try {
              target.close();
            }
            finally {
                if (r!=null)
                    r.close();
                else
                    pis.close();
            }
        }
        
    }
}
/*
 * $$Log: PlainTextExtractor.java,v $
 * $Revision 1.1  2007/04/26 08:58:50  dparhomenko
 * $PTR#1804279 migrate JCR to maven from B302 directory
 * $
 * $Revision 1.9  2006/07/06 09:33:17  dparhomenko
 * $PTR#1802310 Add new features to DatabaseConnection
 * $
 * $Revision 1.8  2006/06/27 11:48:58  zahars
 * $PTR#0144986 Extractor interface changed
 * $
 * $Revision 1.7  2006/06/27 08:48:54  maksims
 * $#1801897 File header/footer added
 * $$
 */