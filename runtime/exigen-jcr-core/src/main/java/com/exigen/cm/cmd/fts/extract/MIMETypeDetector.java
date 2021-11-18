/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts.extract;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatch;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hslf.extractor.PowerPointExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;

import eu.medsea.util.MimeUtil;

/**
 * Detects MIME type of provided type based on its content
 * (based on jmimemagic product)
 * 
 */
public class MIMETypeDetector {
    
    private Log log = LogFactory.getLog(MIMETypeDetector.class);

    
    
    private String guessMIMETypeByMagicNumbers(String fileName){
        String answer=null;
        try {
            MagicMatch match = Magic.getMagicMatch(new File(fileName), true);
            answer=match.getMimeType();
        }catch(MagicException e){
            /**/
        }catch(MagicParseException e){
            /**/
        }catch(MagicMatchNotFoundException e){
            /**/
        }
        return answer;
    }
    
    private String getMIMETypeOfMSOfficeDoc(String fileName, String mimeType){
        
        FileInputStream is = null;
        //String mime = mimeType;
        String mime = null;
        try {
            try{ // Excel ?
                is=new FileInputStream(fileName);
                new HSSFWorkbook(is);
                mime = "application/vnd.ms-excel";
            }catch (Exception e){/**/}
            finally {
                if ( is!=null )
                    is.close();
                is = null;
            }

            if (mime == null){
                try{ // Word ?
                    is=new FileInputStream(fileName);
                    new HWPFDocument(is);
                    mime = "application/msword";
                }catch (Exception e){ /**/ }
                finally {
                    if ( is!=null )
                        is.close();
                    is = null;
                }
            }    

            if (mime == null){
                try{ // PowerPoint ?
                    is=new FileInputStream(fileName);
                    new PowerPointExtractor(is);
                    mime = "application/vnd.ms-powerpoint";
                }catch (Exception e){ /**/  }
                finally {
                    if ( is!=null )
                        is.close();
                    is = null;
                }
            }    
        } catch (IOException ioe){
            log.error("failed to close stream",ioe);
        }
        return mime; 
    }
    
    private String testForCompiledHTMLHelpFile(String fileName){
        InputStream is = null;
        String mime = null;
        try {
            is = new FileInputStream(fileName);
            // http://www.speakeasy.org/~russotto/chm/chmformat.html
            byte[] buf = {0,0,0,0,0,0,0,0};
            is.read(buf);
            if (buf[0]==73 && buf[1]==84 && buf[2]==83 && buf[3]==70 &&
                    buf[4]==3 && buf[5]==0 && buf[6]==0 && buf[7]==0){
                // Not sure about right MIME type for Compiled HTML Help files
                mime = "application/mshelp";
            }
        } catch(Exception e){
            /**/
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ioe){
                log.error("failed to close stream",ioe);
            }
        }
        return mime;
    }
    
    private String testForODFDoc(String fileName,String detectedMIMEType){
        
        InputStream is = null;
        ZipInputStream zis = null;
        String mime = detectedMIMEType;
        boolean contentFound=false;
        boolean mimeTypeFound=false;
        String  odfMIMEType=null;
        
        try {
            is = new FileInputStream(fileName);
            zis = new ZipInputStream(is);
            ZipEntry e=zis.getNextEntry();
            while (e!=null && !(contentFound && mimeTypeFound)){
                String eName=e.getName();
                if ( eName.equals("content.xml") ){
                    contentFound=true;
                }else if(eName.equals("mimetype")){
                    if (e.getSize()>=100){
                        break;
                    }   
                    int c=zis.read();
                    StringBuilder s=new StringBuilder();
                    while (c!=-1){
                        s.append(new Character((char)c).toString());
                        c=zis.read();
                    }
                    odfMIMEType=s.toString();
                    if (! odfMIMEType.startsWith("application/vnd.oasis.")){
                        break;
                    }
                    mimeTypeFound=true;
                }
                e=zis.getNextEntry();
            }
        } catch(FileNotFoundException e){
            mime = null;
        } catch(IOException e){
            mime = null;
        }
        finally {
            try {
                zis.close();
            } catch (IOException ioe){
                log.error(" failed to close file",ioe);
            }
        }
        if (contentFound && mimeTypeFound)
            mime = odfMIMEType;
        return mime;
    }
    
    private String testForHTMLDoc(String fileName,String detectedMIMEType){
        // From Apache magic file
        //      0   string      \<!DOCTYPE\ HTML    text/html
        //      0   string      \<!doctype\ html    text/html
        //      0   string      \<HEAD      text/html
        //      0   string      \<head      text/html
        //      0   string      \<TITLE     text/html
        //      0   string      \<title     text/html
        //      0       string          \<html          text/html
        //      0       string          \<HTML          text/html
        //      0   string      \<!--       text/html
        //      0   string      \<h1        text/html
        //      0   string      \<H1        text/html
        String mime = detectedMIMEType;
        InputStream is = null;
        InputStreamReader ir = null;
        try {
            is = new FileInputStream(fileName);
            ir=new InputStreamReader(is);
            char[] buf=new char[100]; 
            if (ir.read(buf,0,100)>0){
                String s=new String(buf);
                s=s.toLowerCase();
                if (s.startsWith("<!doctype html") || s.startsWith("<head") ||
                        s.startsWith("<title") || s.startsWith("<html") ||
                        s.startsWith("<!--") || s.startsWith("<h1")){
                    mime = "text/html";
                }
            }   
        } catch(FileNotFoundException e){
            /**/
        } catch(IOException  e){
            /**/
        }
        finally {
            if (ir != null){
                try {
                    ir.close();
                } catch (IOException ioe){
                    log.error("failed to close stream",ioe);
                }
            }    
            
        }
        return mime;    
    }
    
    private boolean isANSITextFile(String fileName){
        boolean answer = false;
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            int validChars=0;
            byte[] buf=new byte[1000];
            int readChars=is.read(buf,0,1000);
            for (int n=0; n<readChars; n++){
                if (buf[n]>31 && buf[n]<127){
                    validChars++;
                }
            }
            if (validChars>0 && validChars>=(90*readChars)/100){
                answer=true;
            }
        } catch(FileNotFoundException e){
            answer=false;
        } catch(IOException  e){
            answer=false;
        }
        finally {
            if (is != null){
                try {
                    is.close();
                } catch (IOException ioe){
                    log.error("failed to close stream",ioe);
                }
            }    
        }
        return answer;
    }
    
    private boolean isUnicodeTextFile(String fileName){
        // not 100% correct method
        FileInputStream is = null;
        InputStreamReader ir = null;
        boolean answer = false;

        try {
            is = new FileInputStream(fileName);
            ir=new InputStreamReader(is,"UNICODE");
            int validChars=0;
            int readChars=0;
            for (int n=0;n<1000;n++){
                int c=ir.read();
                if (c==-1){
                    break;
                }
                readChars++;
                if (Character.isDefined(c) && (Character.isLetterOrDigit(c) || Character.isWhitespace(c))){
                    validChars++;
                }           
            }
            if (validChars>0 && validChars>=(90*readChars)/100){
                answer=true;
            }
        } catch(FileNotFoundException e){
            answer=false;
        } catch(IOException  e){
            answer=false;
        }
        finally {
            if (ir != null){
                try {
                    ir.close();
                } catch (IOException ioe){
                    log.error("failed to close stream",ioe);
                }
            }    
        }
        return answer;
    }
    
    /**
     * Detects MIME type by file content.
     * If i/o errors happen or MIME type could not be determined "application/octet-stream"
     * is returned
     * @param fileName
     * @return MIME type
     */
    public String detect(String fileName){
    	
    	
        String mimeType=null;
        // Guess MIME type by magic numbers
        mimeType= MimeUtil.getFirstMimeType(MimeUtil.getMimeType(fileName)); //guessMIMETypeByMagicNumbers(fileName);
        
        // PROBLEMS after guessing MIME type by magic number which should be fixed:
        // (0) sometimes returns ???
        if ( mimeType!=null && mimeType.equals("???") ){
            mimeType=null;
        }
        // (1) returns application/msword for all MS Office documents
        //if ( mimeType!=null && mimeType.equals("application/msword")){
        //    return getMIMETypeOfMSOfficeDoc(fileName, mimeType);
        //}
        // (2) returns application/zip for ODF/OpenOffice documents
        //if ( mimeType!=null && mimeType.equals("application/zip") ){
        //    return testForODFDoc(fileName, mimeType);
        //}
        // (3) too strict(?) checks for html - returns text/sgml
        //if ( mimeType!=null && mimeType.equals("text/sgml") ){
        //    return testForHTMLDoc(fileName,mimeType);
        //}
        // (4) returns null for CHM Compiled HTML Help files
        //if (mimeType==null){
        //    mimeType=testForCompiledHTMLHelpFile(fileName);
        //}
        // Try at the end as ANSI/Unicode text
        //if (mimeType==null){
        //    if (isANSITextFile(fileName) || isUnicodeTextFile(fileName)){
        //        mimeType="text/plain";
        //    }else{
        //        mimeType=Constants.UNDEFINED_MIME_TYPE;
        //    }
       // }
        return mimeType;
    }

    public String detect(InputStream in){
        String mimeType=null;
        // Guess MIME type by magic numbers
        mimeType= MimeUtil.getFirstMimeType(MimeUtil.getMimeType(in)); //guessMIMETypeByMagicNumbers(fileName);
        return mimeType;
    }

    public String detect(File f){
        String mimeType=null;
        // Guess MIME type by magic numbers
        mimeType= MimeUtil.getFirstMimeType(MimeUtil.getMimeType(f)); //guessMIMETypeByMagicNumbers(fileName);
        return mimeType;
    }

    
}


/*
 * $Log: MIMETypeDetector.java,v $
 * Revision 1.4  2009/02/23 14:30:20  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2009/02/12 15:20:25  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2009/02/11 15:08:18  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:58:50  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2006/09/11 08:31:14  zahars
 * PTR#0144986 Version jmimemagic 0.1.0 is used instead of 0.0.4
 *
 * Revision 1.4  2006/07/17 09:07:07  zahars
 * PTR#0144986 Comments added
 *
 * Revision 1.3  2006/07/06 09:33:17  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/07/03 13:50:39  zahars
 * PTR#0144986 MIMEMIMECommandTest updated
 *
 * Revision 1.1  2006/06/29 11:57:53  zahars
 * PTR#0144986 MIME type detector added
 *
 */
