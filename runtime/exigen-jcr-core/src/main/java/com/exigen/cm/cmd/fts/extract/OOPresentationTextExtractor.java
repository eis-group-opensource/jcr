/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.cmd.fts.extract;

import java.util.Map;

import com.sun.star.drawing.XDrawPages;
import com.sun.star.drawing.XDrawPagesSupplier;
import com.sun.star.drawing.XShapes;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XText;
import com.sun.star.uno.UnoRuntime;

/**
 * Extracts text from presentation formats like PowerPoint
 * 
 * @author Maksims
 *
 */
public class OOPresentationTextExtractor extends OODocumentTextExtractor {

    public OOPresentationTextExtractor(Map p){
        super(p);
    }
    
    /**
     * @inheritDoc 
     */
    protected String getText(XComponent document) throws Exception {
//        printSupportedServices(document);
        
        XDrawPagesSupplier pagesSupplier = (XDrawPagesSupplier)UnoRuntime.queryInterface(XDrawPagesSupplier.class, document);
        XDrawPages pages = pagesSupplier.getDrawPages();
        int pCount = pages.getCount();
        StringBuffer buffer = new StringBuffer();
        
        for(int i=0; i<pCount; i++){
            Object pageRef = pages.getByIndex(i);
            XShapes shapes = (XShapes)UnoRuntime.queryInterface(XShapes.class, pageRef);
            int sCount = shapes.getCount();
            for(int s=0; s<sCount; s++){
                Object shapeRef = shapes.getByIndex(s);
                XText textShape = (XText)UnoRuntime.queryInterface(XText.class, shapeRef);
                if(textShape == null) // not a text shape
                    continue;
                
                buffer.append(' ').append(textShape.getString());
            }
        }
        
        return buffer.toString();
    }

    /**
     * @inheritDoc 
     */
    protected String getFileExtension() {
        return ".ppt";
    }

}
/*
 * $$Log: OOPresentationTextExtractor.java,v $
 * $Revision 1.1  2007/04/26 08:58:50  dparhomenko
 * $PTR#1804279 migrate JCR to maven from B302 directory
 * $
 * $Revision 1.1  2006/06/27 12:58:58  zahars
 * $PTR#0144986 OO extractors renamed
 * $
 * $Revision 1.4  2006/06/27 08:48:53  maksims
 * $#1801897 File header/footer added
 * $$
 */