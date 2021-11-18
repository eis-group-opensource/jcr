/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query.parser;

import com.exigen.cm.query.predicate.ContainsComparison;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import antlr.TreeParser;
import antlr.Token;
import antlr.collections.AST;
import antlr.RecognitionException;
import antlr.ANTLRException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.collections.impl.BitSet;
import antlr.ASTPair;
import antlr.collections.impl.ASTArray;


public class FTSBuilder extends antlr.TreeParser       implements FTSBuilderTokenTypes
 {

	
//	FTS Query builder running given parser
	private ContainsComparison builder;
public FTSBuilder() {
	tokenNames = _tokenNames;
}

	public final void parse(AST _t,
		ContainsComparison builder 
	) throws RecognitionException {
		
		AST parse_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		
				this.builder = builder;
			
		
		try {      // for error handling
			expression(_t);
			_t = _retTree;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
	}
	
	public final void expression(AST _t) throws RecognitionException {
		
		AST expression_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		
		try {      // for error handling
			orExpression(_t);
			_t = _retTree;
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
	}
	
	public final void orExpression(AST _t) throws RecognitionException {
		
		AST orExpression_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case OR:
			{
				AST __t15 = _t;
				AST tmp1_AST_in = (AST)_t;
				match(_t,OR);
				_t = _t.getFirstChild();
				builder.attachOr();
				orExpression(_t);
				_t = _retTree;
				orExpression(_t);
				_t = _retTree;
				_t = __t15;
				_t = _t.getNextSibling();
				break;
			}
			case NOT:
			case AND:
			case PHRASE:
			case WORD:
			{
				andExpression(_t);
				_t = _retTree;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
	}
	
	public final void andExpression(AST _t) throws RecognitionException {
		
		AST andExpression_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case AND:
			{
				AST __t17 = _t;
				AST tmp2_AST_in = (AST)_t;
				match(_t,AND);
				_t = _t.getFirstChild();
				builder.attachAnd();
				andExpression(_t);
				_t = _retTree;
				andExpression(_t);
				_t = _retTree;
				_t = __t17;
				_t = _t.getNextSibling();
				break;
			}
			case NOT:
			case PHRASE:
			case WORD:
			{
				value(_t);
				_t = _retTree;
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
	}
	
	public final void value(AST _t) throws RecognitionException {
		
		AST value_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		AST w = null;
		AST p = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case NOT:
			{
				AST __t19 = _t;
				AST tmp3_AST_in = (AST)_t;
				match(_t,NOT);
				_t = _t.getFirstChild();
				builder.attachNot();
				value(_t);
				_t = _retTree;
				_t = __t19;
				_t = _t.getNextSibling();
				break;
			}
			case WORD:
			{
				w = (AST)_t;
				match(_t,WORD);
				_t = _t.getNextSibling();
				builder.attachWord(w.getText());
				break;
			}
			case PHRASE:
			{
				p = (AST)_t;
				match(_t,PHRASE);
				_t = _t.getNextSibling();
				builder.attachPhrase(p.getText());
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
	}
	
	public final void negation(AST _t) throws RecognitionException {
		
		AST negation_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		AST w = null;
		AST p = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case WORD:
			{
				w = (AST)_t;
				match(_t,WORD);
				_t = _t.getNextSibling();
				builder.attachWord(w.getText());
				break;
			}
			case PHRASE:
			{
				p = (AST)_t;
				match(_t,PHRASE);
				_t = _t.getNextSibling();
				builder.attachPhrase(p.getText());
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			if (_t!=null) {_t = _t.getNextSibling();}
		}
		_retTree = _t;
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
		"WORD"
	};
	
	}
	
