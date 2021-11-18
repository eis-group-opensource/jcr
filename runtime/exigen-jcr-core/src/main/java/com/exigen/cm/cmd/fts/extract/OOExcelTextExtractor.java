/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/


package com.exigen.cm.cmd.fts.extract;

import java.util.Map;

import com.sun.star.lang.XComponent;
import com.sun.star.sheet.CellFlags;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XSheetCellRanges;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.text.XText;
import com.sun.star.uno.UnoRuntime;

/**
 * Extracts text from Spreadsheet documents.
 * @author Maksims
 *
 */
public class OOExcelTextExtractor extends OODocumentTextExtractor {

    public OOExcelTextExtractor(Map p){
        super(p);
    }
    
    
    protected String getText(XComponent document) throws Exception {
        XSpreadsheetDocument xDocument = (XSpreadsheetDocument)UnoRuntime.queryInterface(
                XSpreadsheetDocument.class, document);

        if(xDocument == null)
            throw new Exception("Cannot get text from document instance other then XTextDocument");

        XSpreadsheets spreadsheets = xDocument.getSheets();
        StringBuffer buffer = new StringBuffer();
        
        String[] names = spreadsheets.getElementNames();
        for(int i=0; i<names.length; i++){
            Object spreadsheetRef = spreadsheets.getByName(names[i]);
            
            XSpreadsheet spreadsheet = (XSpreadsheet)UnoRuntime.queryInterface(
                    XSpreadsheet.class, spreadsheetRef);

            XCellRangesQuery dataAccess = (XCellRangesQuery)UnoRuntime.queryInterface( XCellRangesQuery.class, spreadsheet );
            
            if(dataAccess == null){
                throw new Exception("Cannot get data from spreadsheeet. XCellRangeData interface not supported");  
            }



            XSheetCellRanges xCellRanges = dataAccess.queryContentCells( (short)CellFlags.STRING );
            CellRangeAddress[] addresses = xCellRanges.getRangeAddresses();
            int count = addresses.length;
            for(int a=0; a<count; a++){
                for(int r=addresses[a].StartRow; r < addresses[a].EndRow+1; r++){
                    for(int c=addresses[a].StartColumn; c < addresses[a].EndColumn+1; c++){
                        Object cell = spreadsheet.getCellByPosition(c,r);
//                        printSupportedServices(cell);
                        
                        XText text = (XText)UnoRuntime.queryInterface(XText.class, cell);                
                        if(text == null) // not a text
                            continue;

                        buffer.append(' ').append(text.getString());

                    }
                }
            }

        }
        
        return buffer.toString();
    }

    protected String getFileExtension() {
        return ".csv";
    }

}
/*
 * $$Log: OOExcelTextExtractor.java,v $
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