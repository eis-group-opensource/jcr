/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store;

/**
 * Thrown if attempt to remove unclosed content is performed.
 */
class UnclosedContentStreamException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    UnclosedContentStreamException(String message){
        super(message);
    }
}

/*
 * $Log: UnclosedContentStreamException.java,v $
 * Revision 1.1  2007/04/26 08:59:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/09/28 09:19:37  maksims
 * #0147862 Unclosed content streams made tracked
 *
 */