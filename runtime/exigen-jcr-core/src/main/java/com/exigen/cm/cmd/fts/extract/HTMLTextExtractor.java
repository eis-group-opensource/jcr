/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts.extract;

import java.io.InputStream;
import java.io.Writer;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import com.exigen.cm.cmd.fts.TextExtractor;

/**
 * 
 * HTML text extractor.
 * Possibly not work with non English languages
 * 
 */
public class HTMLTextExtractor implements TextExtractor {

	public void extract(final String mimeType, final InputStream source, 
			final Writer target) throws Exception {
		try {
			final HTMLEditorKit html = new HTMLEditorKit();
			final HTMLDocument doc = new HTMLDocument();
			doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
			html.read(source, doc, 0);
			final String plainText = doc.getText(0, doc.getLength());
			target.write(plainText);
		} finally {
			source.close();
			target.close();
		}

	}

}


/*
 * $Log: HTMLTextExtractor.java,v $
 * Revision 1.2  2010/09/07 14:14:48  vsverlovs
 * EPB-198: code_review_EPB-105_2010-09-02
 *
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
