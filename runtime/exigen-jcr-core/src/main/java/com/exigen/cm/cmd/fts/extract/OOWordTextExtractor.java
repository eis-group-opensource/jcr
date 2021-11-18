/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.cmd.fts.extract;

import java.util.Map;

import com.sun.star.lang.XComponent;
import com.sun.star.text.XText;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
/**
 * Open Office based Word document text extractor.
 * @author Maksims
 */
public class OOWordTextExtractor extends OODocumentTextExtractor {

    /**
     * Constructs instance with properties specified.
     * @param p is an OpenOffice properties declared 
     * in OODocumentTextExtractor constants.
     */
    public OOWordTextExtractor(Map p){
        super(p);
    }

    /**
     * Returns MSWord file extension to give OO a hint
     * what is deals with ...
     * It might be enough to store text docs with .txt extension ...
     */
    protected String getFileExtension(){
        return ".txt";
    }
    
    
    /**
     * Returns text from provided document instance or throw exception in
     * case instance is of unappropriate type.
     */
    protected String getText(XComponent document) throws Exception {
        XTextDocument textDocument =(XTextDocument)UnoRuntime.queryInterface(XTextDocument.class, document );
        if(textDocument != null){
            XText textRef = textDocument.getText();
            return textRef.getString();
        }
        
        throw new Exception("Cannot get text from document instance other then XTextDocument");
    }
}
/*
 * $$Log: OOWordTextExtractor.java,v $
 * $Revision 1.1  2007/04/26 08:58:50  dparhomenko
 * $PTR#1804279 migrate JCR to maven from B302 directory
 * $
 * $Revision 1.1  2006/06/27 12:58:58  zahars
 * $PTR#0144986 OO extractors renamed
 * $
 * $Revision 1.4  2006/06/27 08:48:54  maksims
 * $#1801897 File header/footer added
 * $$
 */