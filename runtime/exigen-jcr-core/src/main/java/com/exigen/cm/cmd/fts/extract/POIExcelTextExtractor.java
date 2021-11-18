/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts.extract;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.exigen.cm.cmd.fts.TextExtractor;

/**
 * MS Excel text extractor based on POI
 * 
 */
public class POIExcelTextExtractor implements TextExtractor {

    public void extract(String mimeType, InputStream source, Writer target)
                    throws IOException {
        try {
            HSSFWorkbook wb     = new HSSFWorkbook(source);
            StringBuffer s = new StringBuffer();
            int numOfSheets=wb.getNumberOfSheets();
            for (int i=0; i<numOfSheets;i++){
                HSSFSheet sh = wb.getSheetAt(i);
                int r0=sh.getFirstRowNum();
                int r1=sh.getLastRowNum();
                for(int j=r0;j<=r1;j++){
                    HSSFRow r = sh.getRow(j);
                    if (r==null){ continue; }
                    short c0=r.getFirstCellNum();
                    short c1=r.getLastCellNum();
                    for (short k=c0;k<=c1;k++){
                        HSSFCell c = r.getCell(k);
                        if (c==null){continue;}
                        int ct=c.getCellType();
                        if (ct==HSSFCell.CELL_TYPE_STRING){
                            s.append(c.getRichStringCellValue().getString()+" ");
                        }else if (ct==HSSFCell.CELL_TYPE_NUMERIC){
                            s.append(c.getNumericCellValue()+" ");
                        }
                    }
                    s.append("\n");
                }
            }
            target.write( s.toString() );
        } 
        finally {
            source.close();
            target.close();
        }
    }

}


/*
 * $Log: POIExcelTextExtractor.java,v $
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
