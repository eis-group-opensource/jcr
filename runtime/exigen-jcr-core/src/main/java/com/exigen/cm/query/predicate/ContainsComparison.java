/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.parser.FTSBuilder;
import com.exigen.cm.query.parser.FTSLexer;
import com.exigen.cm.query.parser.FTSParser;

/**
 * Implements jcr:contains() comparison.
 */
public class ContainsComparison extends BinaryComparison{
    private static final Log log = LogFactory.getLog(ContainsComparison.class);
    
    private PredicateProducer predicateProducer;
    private Condition currentFTSStatement = null;
    
    ContainsComparison(String attributeName, String value){
        super(attributeName,  ComparisonType.BINARY.CONTAINS, QueryUtils.stripQuotes(value).toLowerCase());
    }
    
    /**
     * Builds FTS SQL by delegating sql generation to corresponding DB dialect
     * specified FTS SQL builders.
     */
    @Override
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType){
            /* For Testing purposes only!
            if(context == null) 
                predicateProducer = new PredicateProducer(null);
            else //*/
           predicateProducer = new PredicateProducer(context.getOwner().getBuildingContext());

           Exception err=null;
           try{
                FTSLexer lex = new FTSLexer (new StringReader((String)value()));
                FTSParser parser = new FTSParser(lex);
                parser.parse();
        
                AST node = parser.getAST();
                FTSBuilder builder = new FTSBuilder();
                builder.parse(node, this);
           }catch(TokenStreamException rex){
                err = rex;
           }catch(RecognitionException rex){
                err = rex;            
           }
            
           if(err != null){
               String message = MessageFormat.format("Failed to parse jcr:contains({0},{1})"
                       , getPropertyName() == null ? ".":getPropertyName(), value());
               log.error(message, err);
               throw new RuntimeException(message, err);
           }
            
           if(currentFTSStatement == null)// parse produces no results
               throw new NoResultsQuery("FTS statement parsing produces no results");
            
           currentFTSStatement.validate();            
            
            
           List<List<Comparison>> ftsFilter = currentFTSStatement.open();
            
           FilterSQL filterHolder = new FilterSQL();
           try{
                context.getOwner().getBuildingContext()
                                  .getDialect()
                                  .getFTSBuilder_().build(context, filterHolder, getPropertyName(), ftsFilter, negated());
                
                return filterHolder;
            }catch(NoResultsQuery nrq){
                throw nrq;
            }catch(RuntimeException ex){
                throw ex;
            }catch(Exception ex){
                String message = MessageFormat.format("Failed to execute query part: jcr:contains({0},{1})"
                        , getPropertyName(), value());
                log.error(message, ex);
                throw new RuntimeException(message, ex);
            }
    }    
    
    /**
     * Attaches FTS AND. Invoked from tree FTSBuilder when AST tree is processed.
     *
     */
    public void attachAnd() {
        attach(predicateProducer.and(currentFTSStatement));
    }

    /**
     * Attaches FTS OR. Invoked from tree FTSBuilder when AST tree is processed.
     *
     */
    public void attachOr() {
        attach(predicateProducer.or(currentFTSStatement));
    }    

    /**
     * Attaches FTS Not. Invoked from tree FTSBuilder when AST tree is processed.
     *
     */
    public void attachNot() {
        attach(predicateProducer.not(currentFTSStatement));
    }

    /**
     * Throws exception because Phrase filter is not implemented.
     *
     */
    public void attachPhrase(String phrase) {
//        throw new UnsupportedOperationException("Phrase filter is not supported!");
        attach(predicateProducer.ftsComparison(ComparisonType.FTS.PHRASE, phrase));
    }

    /**
     * Attach word comparison to an FTS query tree.
     * @param value
     */
    public void attachWord(String value) {
        attach(predicateProducer.ftsComparison(ComparisonType.FTS.WORD, value));
    }
   
    /**
     * Used to build FTS query tree.
     * @param statement
     */
    protected void attach(Condition statement){
        if(currentFTSStatement == null){
            currentFTSStatement = statement;
            return;
        }

        if( !currentFTSStatement.isLeaf() && !currentFTSStatement.isComplete()){
            ((BooleanOperation)currentFTSStatement).addChild(statement);
            
            if(!statement.isLeaf())
                currentFTSStatement = statement;
            else
            if(currentFTSStatement.isComplete()){
                Condition parent = currentFTSStatement.getAvailableParent();

                if( parent != null )
                    currentFTSStatement=parent;
            }
            return;
        }

        throw new RuntimeException("Failed to build FTS query tree! Cannot attach comparison!");
    } 
    
    
    /*
    public static void main(String[] args) {
        ContainsComparison cc = new ContainsComparison(null, "-jojo mumba or daas or maas bubu");
        cc.createFilterData(null, null);
    }//*/
}

/*
 * $Log: ContainsComparison.java,v $
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
 */