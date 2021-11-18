/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query.predicate;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

/**
 * Base class for FTS query builder.
 * @author mzizkuns
 *
 */
public abstract class AbstractFTSQueryBuilder implements FTSQueryBuilder {
    
//  Minimal length word should have to participate in FTS query
    private static final int MIN_WORDS_LENGTH = 3;

    /**
     * Moves values of FTS conditions into corresponding String lists for easier processing.
     * @param src
     * @param words
     * @param notWords
     * @param phrases
     * @param notPhrases
     */
    protected void split(List<Comparison> src, List<String> words, List<String> notWords, List<String> phrases, List<String> notPhrases){
        for(Comparison c:src){
            FTSCondition ftsc = (FTSCondition)c;
            String value = ftsc.getValue();
            if(value.length() < MIN_WORDS_LENGTH)
                continue;
            
            switch(ftsc.getType()){
//                case PHRASE:
//                    (c.negated()?notPhrases:phrases).add(ftsc.getValue());
//                    break;
                    
                case WORD:
                    (c.negated()?notWords:words).add(value);
                    break;
                    
                default:
                    String message = MessageFormat.format("FTS Search type {0} is not yet implemented", ftsc.getType());
                    throw new UnsupportedOperationException(message);
            }
        }
    }
    

    /**
     * Tests and clears stop words from words list. Stop words are those
     * which are shorter then 3 chars or listed in a Repository.getStopWords().
     * @param context
     * @param words
     */
    protected void filterStopWords(FilterContext context, List<String> words){
        Set stopwords = context.getOwner()
                                  .getBuildingContext()
                                  .getSession()
                                  ._getRepository().getStopWords();
        
        for(int i=0;i<words.size(); i++){
            String word = words.get(i);
            if(word.length() < MIN_WORDS_LENGTH || stopwords.contains(word))
                words.remove(i--);
        }
    }    
}
/*
 * $Log: AbstractFTSQueryBuilder.java,v $
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2007/03/09 14:50:59  maksims
 * #1804051 fixed incorrect phrase search grammar
 *
 * Revision 1.2  2006/12/21 12:33:17  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 * Revision 1.1  2006/04/17 06:47:03  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.4  2006/04/12 12:18:46  maksims
 * #0144986 Seekable support added to File Store
 *
 */