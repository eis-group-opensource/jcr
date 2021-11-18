/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query.parser;

import com.exigen.cm.query.predicate.ContainsComparison;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;
import antlr.collections.AST;
import java.util.Hashtable;
import antlr.ASTFactory;
import antlr.ASTPair;
import antlr.collections.impl.ASTArray;

public class FTSParser extends antlr.LLkParser       implements ftsTokenTypes
 {

protected FTSParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public FTSParser(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected FTSParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public FTSParser(TokenStream lexer) {
  this(lexer,2);
}

public FTSParser(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

	public final void parse() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST parse_AST = null;
		
		try {      // for error handling
			orExpression();
			astFactory.addASTChild(currentAST, returnAST);
			parse_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_0);
			} else {
			  throw ex;
			}
		}
		returnAST = parse_AST;
	}
	
	public final void orExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST orExpression_AST = null;
		
		try {      // for error handling
			andExpression();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop4:
			do {
				if ((LA(1)==OR)) {
					AST tmp4_AST = null;
					tmp4_AST = astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp4_AST);
					match(OR);
					andExpression();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop4;
				}
				
			} while (true);
			}
			orExpression_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_0);
			} else {
			  throw ex;
			}
		}
		returnAST = orExpression_AST;
	}
	
	public final void andExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST andExpression_AST = null;
		AST e1_AST = null;
		AST e2_AST = null;
		
		try {      // for error handling
			boolean synPredMatched7 = false;
			if ((((LA(1) >= MINUS && LA(1) <= WORD)) && ((LA(2) >= MINUS && LA(2) <= WORD)))) {
				int _m7 = mark();
				synPredMatched7 = true;
				inputState.guessing++;
				try {
					{
					expression();
					expression();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched7 = false;
				}
				rewind(_m7);
				inputState.guessing--;
			}
			if ( synPredMatched7 ) {
				expression();
				e1_AST = (AST)returnAST;
				andExpression();
				e2_AST = (AST)returnAST;
				if ( inputState.guessing==0 ) {
					andExpression_AST = (AST)currentAST.root;
					andExpression_AST=(AST)astFactory.make( (new ASTArray(3)).add(astFactory.create(AND,"AND")).add(e1_AST).add(e2_AST));
					currentAST.root = andExpression_AST;
					currentAST.child = andExpression_AST!=null &&andExpression_AST.getFirstChild()!=null ?
						andExpression_AST.getFirstChild() : andExpression_AST;
					currentAST.advanceChildToEnd();
				}
			}
			else if (((LA(1) >= MINUS && LA(1) <= WORD)) && (_tokenSet_1.member(LA(2)))) {
				expression();
				astFactory.addASTChild(currentAST, returnAST);
				andExpression_AST = (AST)currentAST.root;
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_2);
			} else {
			  throw ex;
			}
		}
		returnAST = andExpression_AST;
	}
	
	public final void expression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expression_AST = null;
		AST t_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case MINUS:
			{
				AST tmp5_AST = null;
				tmp5_AST = astFactory.create(LT(1));
				match(MINUS);
				term();
				t_AST = (AST)returnAST;
				if ( inputState.guessing==0 ) {
					expression_AST = (AST)currentAST.root;
					expression_AST =(AST)astFactory.make( (new ASTArray(2)).add(astFactory.create(NOT,"NOT")).add(t_AST));
					currentAST.root = expression_AST;
					currentAST.child = expression_AST!=null &&expression_AST.getFirstChild()!=null ?
						expression_AST.getFirstChild() : expression_AST;
					currentAST.advanceChildToEnd();
				}
				break;
			}
			case PHRASE:
			case WORD:
			{
				term();
				astFactory.addASTChild(currentAST, returnAST);
				expression_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_3);
			} else {
			  throw ex;
			}
		}
		returnAST = expression_AST;
	}
	
	public final void term() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST term_AST = null;
		Token  p = null;
		AST p_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case PHRASE:
			{
				p = LT(1);
				p_AST = astFactory.create(p);
				match(PHRASE);
				if ( inputState.guessing==0 ) {
					term_AST = (AST)currentAST.root;
					
							String val = p_AST.getText();
							p_AST.setText(val.substring(1, val.length()-1));
							term_AST=p_AST;
							
					currentAST.root = term_AST;
					currentAST.child = term_AST!=null &&term_AST.getFirstChild()!=null ?
						term_AST.getFirstChild() : term_AST;
					currentAST.advanceChildToEnd();
				}
				break;
			}
			case WORD:
			{
				AST tmp6_AST = null;
				tmp6_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp6_AST);
				match(WORD);
				term_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_3);
			} else {
			  throw ex;
			}
		}
		returnAST = term_AST;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"or\"",
		"NOT",
		"AND",
		"MINUS",
		"PHRASE",
		"WORD",
		"PARENTESS",
		"ESCAPE",
		"WS"
	};
	
	protected void buildTokenTypeASTClassMap() {
		tokenTypeToASTClassMap=null;
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 786L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 18L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 914L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	
	}
