/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query.parser;
import com.exigen.cm.query.order.OrderDefinition;
import com.exigen.cm.query.QueryBuilder;
import java.util.*;

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

public class JCRXPathParser extends antlr.LLkParser       implements JCRXPathTokenTypes
 {

	/**
	* Reverses comparision containing < or > symbols
	*/
	private String reverseComparison(AST opNode){
		StringBuffer op = new StringBuffer(opNode.getText());
		if(op.charAt(0) == '>')
			op.setCharAt(0, '<');
		else
		if(op.charAt(0) == '<')
			op.setCharAt(0, '>');
			
		return op.toString();
	}
	

	

protected JCRXPathParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public JCRXPathParser(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected JCRXPathParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public JCRXPathParser(TokenStream lexer) {
  this(lexer,2);
}

public JCRXPathParser(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

	public final void parse() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST parse_AST = null;
		
		pathes();
		astFactory.addASTChild(currentAST, returnAST);
		{
		switch ( LA(1)) {
		case ORDER:
		{
			orderByClause();
			astFactory.addASTChild(currentAST, returnAST);
			break;
		}
		case EOF:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		parse_AST = (AST)currentAST.root;
		returnAST = parse_AST;
	}
	
	public final void pathes() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST pathes_AST = null;
		AST x_AST = null;
		AST x1_AST = null;
		
		xPath();
		x_AST = (AST)returnAST;
		if ( inputState.guessing==0 ) {
			pathes_AST = (AST)currentAST.root;
			pathes_AST=(AST)astFactory.make( (new ASTArray(2)).add(astFactory.create(QUERY,"sub-query")).add(x_AST));
			currentAST.root = pathes_AST;
			currentAST.child = pathes_AST!=null &&pathes_AST.getFirstChild()!=null ?
				pathes_AST.getFirstChild() : pathes_AST;
			currentAST.advanceChildToEnd();
		}
		{
		_loop43:
		do {
			if ((LA(1)==UNION_STR||LA(1)==UNION)) {
				{
				switch ( LA(1)) {
				case UNION:
				{
					AST tmp1_AST = null;
					tmp1_AST = astFactory.create(LT(1));
					match(UNION);
					break;
				}
				case UNION_STR:
				{
					AST tmp2_AST = null;
					tmp2_AST = astFactory.create(LT(1));
					match(UNION_STR);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				xPath();
				x1_AST = (AST)returnAST;
				if ( inputState.guessing==0 ) {
					pathes_AST = (AST)currentAST.root;
					pathes_AST.setNextSibling( (AST)astFactory.make( (new ASTArray(2)).add(astFactory.create(QUERY,"sub-query")).add(x1_AST)));
				}
			}
			else {
				break _loop43;
			}
			
		} while (true);
		}
		returnAST = pathes_AST;
	}
	
	public final void orderByClause() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST orderByClause_AST = null;
		
		AST tmp3_AST = null;
		tmp3_AST = astFactory.create(LT(1));
		astFactory.makeASTRoot(currentAST, tmp3_AST);
		match(ORDER);
		match(LITERAL_by);
		orderSpecList();
		astFactory.addASTChild(currentAST, returnAST);
		orderByClause_AST = (AST)currentAST.root;
		returnAST = orderByClause_AST;
	}
	
	public final void xPath() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST xPath_AST = null;
		AST p1_AST = null;
		AST p2_AST = null;
		AST s1_AST = null;
		AST p3_AST = null;
		
		if ((LA(1)==SLASH) && (LA(2)==JCR_ROOT)) {
			AST tmp5_AST = null;
			tmp5_AST = astFactory.create(LT(1));
			match(SLASH);
			AST tmp6_AST = null;
			tmp6_AST = astFactory.create(LT(1));
			match(JCR_ROOT);
			rootBasedPath();
			p1_AST = (AST)returnAST;
			if ( inputState.guessing==0 ) {
				xPath_AST = (AST)currentAST.root;
				xPath_AST=p1_AST;
				currentAST.root = xPath_AST;
				currentAST.child = xPath_AST!=null &&xPath_AST.getFirstChild()!=null ?
					xPath_AST.getFirstChild() : xPath_AST;
				currentAST.advanceChildToEnd();
			}
		}
		else if ((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2)))) {
			relativePath();
			p2_AST = (AST)returnAST;
			if ( inputState.guessing==0 ) {
				xPath_AST = (AST)currentAST.root;
				xPath_AST=p2_AST;
				currentAST.root = xPath_AST;
				currentAST.child = xPath_AST!=null &&xPath_AST.getFirstChild()!=null ?
					xPath_AST.getFirstChild() : xPath_AST;
				currentAST.advanceChildToEnd();
			}
		}
		else if ((_tokenSet_2.member(LA(1))) && (_tokenSet_3.member(LA(2)))) {
			axisStep();
			s1_AST = (AST)returnAST;
			if ( inputState.guessing==0 ) {
				xPath_AST = (AST)currentAST.root;
				xPath_AST=(AST)astFactory.make( (new ASTArray(2)).add(astFactory.create(PATH_CHILD)).add(s1_AST));
				currentAST.root = xPath_AST;
				currentAST.child = xPath_AST!=null &&xPath_AST.getFirstChild()!=null ?
					xPath_AST.getFirstChild() : xPath_AST;
				currentAST.advanceChildToEnd();
			}
			{
			if ((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2)))) {
				relativePath();
				p3_AST = (AST)returnAST;
				if ( inputState.guessing==0 ) {
					xPath_AST = (AST)currentAST.root;
					xPath_AST.setNextSibling(p3_AST);
				}
			}
			else if ((_tokenSet_4.member(LA(1))) && (_tokenSet_1.member(LA(2)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		returnAST = xPath_AST;
	}
	
	public final void rootBasedPath() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST rootBasedPath_AST = null;
		AST p1_AST = null;
		
		boolean synPredMatched48 = false;
		if (((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2))))) {
			int _m48 = mark();
			synPredMatched48 = true;
			inputState.guessing++;
			try {
				{
				switch ( LA(1)) {
				case SLASH:
				{
					match(SLASH);
					break;
				}
				case DOUBLESLASH:
				{
					match(DOUBLESLASH);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
			}
			catch (RecognitionException pe) {
				synPredMatched48 = false;
			}
			rewind(_m48);
			inputState.guessing--;
		}
		if ( synPredMatched48 ) {
			relativePath();
			p1_AST = (AST)returnAST;
			if ( inputState.guessing==0 ) {
				rootBasedPath_AST = (AST)currentAST.root;
				rootBasedPath_AST=p1_AST;
				currentAST.root = rootBasedPath_AST;
				currentAST.child = rootBasedPath_AST!=null &&rootBasedPath_AST.getFirstChild()!=null ?
					rootBasedPath_AST.getFirstChild() : rootBasedPath_AST;
				currentAST.advanceChildToEnd();
			}
		}
		else if ((_tokenSet_4.member(LA(1))) && (_tokenSet_1.member(LA(2)))) {
			if ( inputState.guessing==0 ) {
				rootBasedPath_AST = (AST)currentAST.root;
				rootBasedPath_AST=(AST)astFactory.make( (new ASTArray(1)).add(astFactory.create(ROOT_NODE_PATH)));
				currentAST.root = rootBasedPath_AST;
				currentAST.child = rootBasedPath_AST!=null &&rootBasedPath_AST.getFirstChild()!=null ?
					rootBasedPath_AST.getFirstChild() : rootBasedPath_AST;
				currentAST.advanceChildToEnd();
			}
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		returnAST = rootBasedPath_AST;
	}
	
	public final void relativePath() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST relativePath_AST = null;
		
		{
		_loop51:
		do {
			if ((LA(1)==SLASH||LA(1)==DOUBLESLASH)) {
				relativePathStep();
				astFactory.addASTChild(currentAST, returnAST);
			}
			else {
				break _loop51;
			}
			
		} while (true);
		}
		relativePath_AST = (AST)currentAST.root;
		returnAST = relativePath_AST;
	}
	
	public final void axisStep() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST axisStep_AST = null;
		AST f_AST = null;
		Token  i = null;
		AST i_AST = null;
		
		forwardStep();
		f_AST = (AST)returnAST;
		astFactory.addASTChild(currentAST, returnAST);
		{
		switch ( LA(1)) {
		case LBRACKET:
		{
			match(LBRACKET);
			{
			boolean synPredMatched57 = false;
			if (((_tokenSet_5.member(LA(1))) && (_tokenSet_6.member(LA(2))))) {
				int _m57 = mark();
				synPredMatched57 = true;
				inputState.guessing++;
				try {
					{
					match(INTEGER);
					match(OP);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched57 = false;
				}
				rewind(_m57);
				inputState.guessing--;
			}
			if ( synPredMatched57 ) {
				filter();
				astFactory.addASTChild(currentAST, returnAST);
			}
			else if ((LA(1)==INTEGER) && (LA(2)==RBRACKET)) {
				i = LT(1);
				i_AST = astFactory.create(i);
				match(INTEGER);
				if ( inputState.guessing==0 ) {
					i_AST.setType(CHILD_INDEX); f_AST.setNextSibling(i_AST);
				}
			}
			else if ((_tokenSet_5.member(LA(1))) && (_tokenSet_6.member(LA(2)))) {
				filter();
				astFactory.addASTChild(currentAST, returnAST);
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			match(RBRACKET);
			break;
		}
		case EOF:
		case ORDER:
		case UNION_STR:
		case UNION:
		case SLASH:
		case DOUBLESLASH:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		axisStep_AST = (AST)currentAST.root;
		returnAST = axisStep_AST;
	}
	
	public final void relativePathStep() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST relativePathStep_AST = null;
		AST a_AST = null;
		AST r_AST = null;
		
		switch ( LA(1)) {
		case DOUBLESLASH:
		{
			AST tmp9_AST = null;
			tmp9_AST = astFactory.create(LT(1));
			match(DOUBLESLASH);
			axisStep();
			a_AST = (AST)returnAST;
			if ( inputState.guessing==0 ) {
				relativePathStep_AST = (AST)currentAST.root;
				
							relativePathStep_AST = (AST)astFactory.make( (new ASTArray(1)).add(astFactory.create(PATH_DESCENDANT_OR_SELF)));
							relativePathStep_AST.setFirstChild(a_AST);
						
				currentAST.root = relativePathStep_AST;
				currentAST.child = relativePathStep_AST!=null &&relativePathStep_AST.getFirstChild()!=null ?
					relativePathStep_AST.getFirstChild() : relativePathStep_AST;
				currentAST.advanceChildToEnd();
			}
			break;
		}
		case SLASH:
		{
			AST tmp10_AST = null;
			tmp10_AST = astFactory.create(LT(1));
			match(SLASH);
			axisStep();
			r_AST = (AST)returnAST;
			if ( inputState.guessing==0 ) {
				relativePathStep_AST = (AST)currentAST.root;
				
							relativePathStep_AST = (AST)astFactory.make( (new ASTArray(1)).add(astFactory.create(PATH_CHILD)));
							relativePathStep_AST.setFirstChild(r_AST);
						
				currentAST.root = relativePathStep_AST;
				currentAST.child = relativePathStep_AST!=null &&relativePathStep_AST.getFirstChild()!=null ?
					relativePathStep_AST.getFirstChild() : relativePathStep_AST;
				currentAST.advanceChildToEnd();
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = relativePathStep_AST;
	}
	
	public final void forwardStep() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST forwardStep_AST = null;
		AST k_AST = null;
		AST a_AST = null;
		AST an_AST = null;
		
		switch ( LA(1)) {
		case DEREF:
		case ELEMENT:
		case TEXT:
		{
			kindTest();
			astFactory.addASTChild(currentAST, returnAST);
			forwardStep_AST = (AST)currentAST.root;
			break;
		}
		case QNAME:
		case STAR:
		{
			nameTest();
			astFactory.addASTChild(currentAST, returnAST);
			forwardStep_AST = (AST)currentAST.root;
			break;
		}
		case DESC:
		case ASC:
		case OR:
		case AND:
		case NOT:
		case LIKE:
		case CONTAINS:
		case SCORE:
		case ORDER:
		case UNION_STR:
		case TRUE:
		case FALSE:
		case LAST:
		case FIRST:
		case POSITION:
		{
			keywordInPath();
			k_AST = (AST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			if ( inputState.guessing==0 ) {
				k_AST.setType(QNAME);
			}
			forwardStep_AST = (AST)currentAST.root;
			break;
		}
		case LBRACE:
		case AT:
		{
			{
			switch ( LA(1)) {
			case LBRACE:
			{
				AST tmp11_AST = null;
				tmp11_AST = astFactory.create(LT(1));
				match(LBRACE);
				break;
			}
			case AT:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			AST tmp12_AST = null;
			tmp12_AST = astFactory.create(LT(1));
			match(AT);
			attributeName();
			a_AST = (AST)returnAST;
			if ( inputState.guessing==0 ) {
				forwardStep_AST = (AST)currentAST.root;
				forwardStep_AST = (AST)astFactory.make( (new ASTArray(2)).add(astFactory.create(SELECT)).add(a_AST));
				currentAST.root = forwardStep_AST;
				currentAST.child = forwardStep_AST!=null &&forwardStep_AST.getFirstChild()!=null ?
					forwardStep_AST.getFirstChild() : forwardStep_AST;
				currentAST.advanceChildToEnd();
			}
			{
			_loop61:
			do {
				if ((LA(1)==UNION) && (LA(2)==AT)) {
					AST tmp13_AST = null;
					tmp13_AST = astFactory.create(LT(1));
					match(UNION);
					AST tmp14_AST = null;
					tmp14_AST = astFactory.create(LT(1));
					match(AT);
					attributeName();
					an_AST = (AST)returnAST;
					if ( inputState.guessing==0 ) {
						a_AST.setNextSibling(an_AST); /*#an.setType(ATTRIBUTE);*/
					}
				}
				else {
					break _loop61;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case RBRACE:
			{
				AST tmp15_AST = null;
				tmp15_AST = astFactory.create(LT(1));
				match(RBRACE);
				break;
			}
			case EOF:
			case ORDER:
			case UNION_STR:
			case UNION:
			case SLASH:
			case DOUBLESLASH:
			case LBRACKET:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = forwardStep_AST;
	}
	
	public final void filter() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST filter_AST = null;
		AST e_AST = null;
		
		orExpression();
		e_AST = (AST)returnAST;
		if ( inputState.guessing==0 ) {
			filter_AST = (AST)currentAST.root;
			// turn off default tree construction
					filter_AST = (AST)astFactory.make( (new ASTArray(1)).add(astFactory.create(FILTER,"FILTER")));
					 filter_AST.setFirstChild(e_AST);
				
			currentAST.root = filter_AST;
			currentAST.child = filter_AST!=null &&filter_AST.getFirstChild()!=null ?
				filter_AST.getFirstChild() : filter_AST;
			currentAST.advanceChildToEnd();
		}
		returnAST = filter_AST;
	}
	
	public final void kindTest() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST kindTest_AST = null;
		AST en_AST = null;
		AST t_AST = null;
		AST a_AST = null;
		AST qs_AST = null;
		
		boolean synPredMatched66 = false;
		if (((LA(1)==ELEMENT) && (LA(2)==LBRACE))) {
			int _m66 = mark();
			synPredMatched66 = true;
			inputState.guessing++;
			try {
				{
				match(ELEMENT);
				match(LBRACE);
				}
			}
			catch (RecognitionException pe) {
				synPredMatched66 = false;
			}
			rewind(_m66);
			inputState.guessing--;
		}
		if ( synPredMatched66 ) {
			AST tmp16_AST = null;
			tmp16_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp16_AST);
			match(ELEMENT);
			match(LBRACE);
			{
			switch ( LA(1)) {
			case QNAME:
			case STAR:
			{
				elementNameOrWildcard();
				en_AST = (AST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				{
				switch ( LA(1)) {
				case COMMA:
				{
					match(COMMA);
					typeNameOrWildcard();
					t_AST = (AST)returnAST;
					astFactory.addASTChild(currentAST, returnAST);
					if ( inputState.guessing==0 ) {
						t_AST.setType(NODE_TYPE);
					}
					break;
				}
				case RBRACE:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case RBRACE:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RBRACE);
			kindTest_AST = (AST)currentAST.root;
		}
		else if ((LA(1)==ELEMENT) && (_tokenSet_7.member(LA(2)))) {
			AST tmp20_AST = null;
			tmp20_AST = astFactory.create(LT(1));
			match(ELEMENT);
			if ( inputState.guessing==0 ) {
				kindTest_AST = (AST)currentAST.root;
				kindTest_AST=(AST)astFactory.make( (new ASTArray(1)).add(astFactory.create(QNAME,"element")));
				currentAST.root = kindTest_AST;
				currentAST.child = kindTest_AST!=null &&kindTest_AST.getFirstChild()!=null ?
					kindTest_AST.getFirstChild() : kindTest_AST;
				currentAST.advanceChildToEnd();
			}
		}
		else {
			boolean synPredMatched70 = false;
			if (((LA(1)==DEREF) && (LA(2)==LBRACE))) {
				int _m70 = mark();
				synPredMatched70 = true;
				inputState.guessing++;
				try {
					{
					match(DEREF);
					match(LBRACE);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched70 = false;
				}
				rewind(_m70);
				inputState.guessing--;
			}
			if ( synPredMatched70 ) {
				AST tmp21_AST = null;
				tmp21_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp21_AST);
				match(DEREF);
				match(LBRACE);
				match(AT);
				attributeName();
				a_AST = (AST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				match(COMMA);
				literal();
				qs_AST = (AST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
				if ( inputState.guessing==0 ) {
					qs_AST.setType(QNAME);
				}
				match(RBRACE);
				kindTest_AST = (AST)currentAST.root;
			}
			else if ((LA(1)==DEREF) && (_tokenSet_7.member(LA(2)))) {
				AST tmp26_AST = null;
				tmp26_AST = astFactory.create(LT(1));
				match(DEREF);
				if ( inputState.guessing==0 ) {
					kindTest_AST = (AST)currentAST.root;
					kindTest_AST=(AST)astFactory.make( (new ASTArray(1)).add(astFactory.create(QNAME,"jcr:deref")));
					currentAST.root = kindTest_AST;
					currentAST.child = kindTest_AST!=null &&kindTest_AST.getFirstChild()!=null ?
						kindTest_AST.getFirstChild() : kindTest_AST;
					currentAST.advanceChildToEnd();
				}
			}
			else {
				boolean synPredMatched72 = false;
				if (((LA(1)==TEXT) && (LA(2)==LBRACE))) {
					int _m72 = mark();
					synPredMatched72 = true;
					inputState.guessing++;
					try {
						{
						match(TEXT);
						match(LBRACE);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched72 = false;
					}
					rewind(_m72);
					inputState.guessing--;
				}
				if ( synPredMatched72 ) {
					AST tmp27_AST = null;
					tmp27_AST = astFactory.create(LT(1));
					match(TEXT);
					AST tmp28_AST = null;
					tmp28_AST = astFactory.create(LT(1));
					match(LBRACE);
					AST tmp29_AST = null;
					tmp29_AST = astFactory.create(LT(1));
					match(RBRACE);
					if ( inputState.guessing==0 ) {
						kindTest_AST = (AST)currentAST.root;
						kindTest_AST=(AST)astFactory.make( (new ASTArray(1)).add(astFactory.create(QNAME,"jcr:xmltext")));
						currentAST.root = kindTest_AST;
						currentAST.child = kindTest_AST!=null &&kindTest_AST.getFirstChild()!=null ?
							kindTest_AST.getFirstChild() : kindTest_AST;
						currentAST.advanceChildToEnd();
					}
				}
				else if ((LA(1)==TEXT) && (_tokenSet_7.member(LA(2)))) {
					AST tmp30_AST = null;
					tmp30_AST = astFactory.create(LT(1));
					match(TEXT);
					if ( inputState.guessing==0 ) {
						kindTest_AST = (AST)currentAST.root;
						kindTest_AST=(AST)astFactory.make( (new ASTArray(1)).add(astFactory.create(QNAME,"text")));
						currentAST.root = kindTest_AST;
						currentAST.child = kindTest_AST!=null &&kindTest_AST.getFirstChild()!=null ?
							kindTest_AST.getFirstChild() : kindTest_AST;
						currentAST.advanceChildToEnd();
					}
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}}
				returnAST = kindTest_AST;
			}
			
	public final void nameTest() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST nameTest_AST = null;
		
		switch ( LA(1)) {
		case QNAME:
		{
			AST tmp31_AST = null;
			tmp31_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp31_AST);
			match(QNAME);
			nameTest_AST = (AST)currentAST.root;
			break;
		}
		case STAR:
		{
			AST tmp32_AST = null;
			tmp32_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp32_AST);
			match(STAR);
			nameTest_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = nameTest_AST;
	}
	
	public final void keywordInPath() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST keywordInPath_AST = null;
		
		switch ( LA(1)) {
		case DESC:
		{
			AST tmp33_AST = null;
			tmp33_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp33_AST);
			match(DESC);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case ASC:
		{
			AST tmp34_AST = null;
			tmp34_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp34_AST);
			match(ASC);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case OR:
		{
			AST tmp35_AST = null;
			tmp35_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp35_AST);
			match(OR);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case AND:
		{
			AST tmp36_AST = null;
			tmp36_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp36_AST);
			match(AND);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case NOT:
		{
			AST tmp37_AST = null;
			tmp37_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp37_AST);
			match(NOT);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case LIKE:
		{
			AST tmp38_AST = null;
			tmp38_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp38_AST);
			match(LIKE);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case CONTAINS:
		{
			AST tmp39_AST = null;
			tmp39_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp39_AST);
			match(CONTAINS);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case SCORE:
		{
			AST tmp40_AST = null;
			tmp40_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp40_AST);
			match(SCORE);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case ORDER:
		{
			AST tmp41_AST = null;
			tmp41_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp41_AST);
			match(ORDER);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case UNION_STR:
		{
			AST tmp42_AST = null;
			tmp42_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp42_AST);
			match(UNION_STR);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case TRUE:
		{
			AST tmp43_AST = null;
			tmp43_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp43_AST);
			match(TRUE);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case FALSE:
		{
			AST tmp44_AST = null;
			tmp44_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp44_AST);
			match(FALSE);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case LAST:
		{
			AST tmp45_AST = null;
			tmp45_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp45_AST);
			match(LAST);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case FIRST:
		{
			AST tmp46_AST = null;
			tmp46_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp46_AST);
			match(FIRST);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		case POSITION:
		{
			AST tmp47_AST = null;
			tmp47_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp47_AST);
			match(POSITION);
			keywordInPath_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = keywordInPath_AST;
	}
	
	public final void attributeName() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST attributeName_AST = null;
		
		AST tmp48_AST = null;
		tmp48_AST = astFactory.create(LT(1));
		astFactory.addASTChild(currentAST, tmp48_AST);
		match(QNAME);
		attributeName_AST = (AST)currentAST.root;
		returnAST = attributeName_AST;
	}
	
	public final void elementNameOrWildcard() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST elementNameOrWildcard_AST = null;
		
		switch ( LA(1)) {
		case QNAME:
		{
			elementName();
			astFactory.addASTChild(currentAST, returnAST);
			elementNameOrWildcard_AST = (AST)currentAST.root;
			break;
		}
		case STAR:
		{
			AST tmp49_AST = null;
			tmp49_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp49_AST);
			match(STAR);
			elementNameOrWildcard_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = elementNameOrWildcard_AST;
	}
	
	public final void typeNameOrWildcard() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST typeNameOrWildcard_AST = null;
		
		switch ( LA(1)) {
		case QNAME:
		{
			typeName();
			astFactory.addASTChild(currentAST, returnAST);
			typeNameOrWildcard_AST = (AST)currentAST.root;
			break;
		}
		case STAR:
		{
			AST tmp50_AST = null;
			tmp50_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp50_AST);
			match(STAR);
			typeNameOrWildcard_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = typeNameOrWildcard_AST;
	}
	
	public final void literal() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST literal_AST = null;
		
		AST tmp51_AST = null;
		tmp51_AST = astFactory.create(LT(1));
		astFactory.addASTChild(currentAST, tmp51_AST);
		match(VALUE_STRING);
		literal_AST = (AST)currentAST.root;
		returnAST = literal_AST;
	}
	
	public final void elementName() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST elementName_AST = null;
		
		AST tmp52_AST = null;
		tmp52_AST = astFactory.create(LT(1));
		astFactory.addASTChild(currentAST, tmp52_AST);
		match(QNAME);
		elementName_AST = (AST)currentAST.root;
		returnAST = elementName_AST;
	}
	
	public final void typeName() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST typeName_AST = null;
		
		AST tmp53_AST = null;
		tmp53_AST = astFactory.create(LT(1));
		astFactory.addASTChild(currentAST, tmp53_AST);
		match(QNAME);
		typeName_AST = (AST)currentAST.root;
		returnAST = typeName_AST;
	}
	
	public final void attributeNameOrWildcard() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST attributeNameOrWildcard_AST = null;
		
		switch ( LA(1)) {
		case AT:
		{
			match(AT);
			attributeName();
			astFactory.addASTChild(currentAST, returnAST);
			attributeNameOrWildcard_AST = (AST)currentAST.root;
			break;
		}
		case DOT:
		{
			AST tmp55_AST = null;
			tmp55_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp55_AST);
			match(DOT);
			attributeNameOrWildcard_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = attributeNameOrWildcard_AST;
	}
	
	public final void orExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST orExpression_AST = null;
		
		andExpression();
		astFactory.addASTChild(currentAST, returnAST);
		{
		_loop80:
		do {
			if ((LA(1)==OR)) {
				AST tmp56_AST = null;
				tmp56_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp56_AST);
				match(OR);
				andExpression();
				astFactory.addASTChild(currentAST, returnAST);
			}
			else {
				break _loop80;
			}
			
		} while (true);
		}
		orExpression_AST = (AST)currentAST.root;
		returnAST = orExpression_AST;
	}
	
	public final void andExpression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST andExpression_AST = null;
		
		expression();
		astFactory.addASTChild(currentAST, returnAST);
		{
		_loop83:
		do {
			if ((LA(1)==AND)) {
				AST tmp57_AST = null;
				tmp57_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp57_AST);
				match(AND);
				expression();
				astFactory.addASTChild(currentAST, returnAST);
			}
			else {
				break _loop83;
			}
			
		} while (true);
		}
		andExpression_AST = (AST)currentAST.root;
		returnAST = andExpression_AST;
	}
	
	public final void expression() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST expression_AST = null;
		AST v_AST = null;
		Token  o = null;
		AST o_AST = null;
		AST a_AST = null;
		
		switch ( LA(1)) {
		case LBRACE:
		{
			parenthesizedExpr();
			astFactory.addASTChild(currentAST, returnAST);
			expression_AST = (AST)currentAST.root;
			break;
		}
		case AT:
		{
			match(AT);
			attributeTest();
			astFactory.addASTChild(currentAST, returnAST);
			expression_AST = (AST)currentAST.root;
			break;
		}
		case NOT:
		{
			AST tmp59_AST = null;
			tmp59_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp59_AST);
			match(NOT);
			match(LBRACE);
			orExpression();
			astFactory.addASTChild(currentAST, returnAST);
			match(RBRACE);
			expression_AST = (AST)currentAST.root;
			break;
		}
		case LIKE:
		case CONTAINS:
		case LAST:
		case FIRST:
		case POSITION:
		{
			functionCall();
			astFactory.addASTChild(currentAST, returnAST);
			expression_AST = (AST)currentAST.root;
			break;
		}
		case TRUE:
		case FALSE:
		case INTEGER:
		case VALUE_STRING:
		case 53:
		case DOUBLE:
		{
			value();
			v_AST = (AST)returnAST;
			o = LT(1);
			o_AST = astFactory.create(o);
			match(OP);
			AST tmp62_AST = null;
			tmp62_AST = astFactory.create(LT(1));
			match(AT);
			attributeName();
			a_AST = (AST)returnAST;
			if ( inputState.guessing==0 ) {
				expression_AST = (AST)currentAST.root;
				
				//			String val = #v.getText();
				//			#o = #( [OP, reverseComparison(#o)], [VALUE, val]);
							o_AST = (AST)astFactory.make( (new ASTArray(2)).add(astFactory.create(OP,reverseComparison(o_AST))).add(v_AST));
							
				//			#v.setType(VALUE);
				//			#o.setFirstChild(#v);
							
							a_AST.setFirstChild(o_AST);
							expression_AST=a_AST;
						
				currentAST.root = expression_AST;
				currentAST.child = expression_AST!=null &&expression_AST.getFirstChild()!=null ?
					expression_AST.getFirstChild() : expression_AST;
				currentAST.advanceChildToEnd();
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = expression_AST;
	}
	
	public final void parenthesizedExpr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST parenthesizedExpr_AST = null;
		AST e_AST = null;
		
		AST tmp63_AST = null;
		tmp63_AST = astFactory.create(LT(1));
		match(LBRACE);
		orExpression();
		e_AST = (AST)returnAST;
		AST tmp64_AST = null;
		tmp64_AST = astFactory.create(LT(1));
		match(RBRACE);
		if ( inputState.guessing==0 ) {
			parenthesizedExpr_AST = (AST)currentAST.root;
			parenthesizedExpr_AST=(AST)astFactory.make( (new ASTArray(2)).add(astFactory.create(GROUPING)).add(e_AST));
			currentAST.root = parenthesizedExpr_AST;
			currentAST.child = parenthesizedExpr_AST!=null &&parenthesizedExpr_AST.getFirstChild()!=null ?
				parenthesizedExpr_AST.getFirstChild() : parenthesizedExpr_AST;
			currentAST.advanceChildToEnd();
		}
		returnAST = parenthesizedExpr_AST;
	}
	
	public final void attributeTest() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST attributeTest_AST = null;
		AST a_AST = null;
		Token  o = null;
		AST o_AST = null;
		AST l_AST = null;
		
		switch ( LA(1)) {
		case JCR_PATH:
		{
			AST tmp65_AST = null;
			tmp65_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp65_AST);
			match(JCR_PATH);
			attributeTest_AST = (AST)currentAST.root;
			break;
		}
		case QNAME:
		{
			attributeName();
			a_AST = (AST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			{
			switch ( LA(1)) {
			case OP:
			{
				o = LT(1);
				o_AST = astFactory.create(o);
				match(OP);
				if ( inputState.guessing==0 ) {
					a_AST.setFirstChild(o_AST);
				}
				value();
				l_AST = (AST)returnAST;
				if ( inputState.guessing==0 ) {
					o_AST.setFirstChild(l_AST); /*#l.setType(VALUE);*/
				}
				break;
			}
			case OR:
			case AND:
			case RBRACKET:
			case RBRACE:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			attributeTest_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = attributeTest_AST;
	}
	
	public final void functionCall() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST functionCall_AST = null;
		
		switch ( LA(1)) {
		case CONTAINS:
		{
			AST tmp66_AST = null;
			tmp66_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp66_AST);
			match(CONTAINS);
			match(LBRACE);
			attributeNameOrWildcard();
			astFactory.addASTChild(currentAST, returnAST);
			match(COMMA);
			literal();
			astFactory.addASTChild(currentAST, returnAST);
			match(RBRACE);
			functionCall_AST = (AST)currentAST.root;
			break;
		}
		case LIKE:
		{
			AST tmp70_AST = null;
			tmp70_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp70_AST);
			match(LIKE);
			match(LBRACE);
			match(AT);
			attributeName();
			astFactory.addASTChild(currentAST, returnAST);
			match(COMMA);
			literal();
			astFactory.addASTChild(currentAST, returnAST);
			match(RBRACE);
			functionCall_AST = (AST)currentAST.root;
			break;
		}
		case LAST:
		case FIRST:
		case POSITION:
		{
			positioningConstraint();
			astFactory.addASTChild(currentAST, returnAST);
			functionCall_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = functionCall_AST;
	}
	
	public final void value() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_AST = null;
		AST b_AST = null;
		
		switch ( LA(1)) {
		case TRUE:
		case FALSE:
		{
			bool();
			b_AST = (AST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			if ( inputState.guessing==0 ) {
				b_AST.setType(VALUE_BOOL);
			}
			value_AST = (AST)currentAST.root;
			break;
		}
		case INTEGER:
		case 53:
		case DOUBLE:
		{
			signedNumeric();
			astFactory.addASTChild(currentAST, returnAST);
			value_AST = (AST)currentAST.root;
			break;
		}
		case VALUE_STRING:
		{
			literal();
			astFactory.addASTChild(currentAST, returnAST);
			value_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = value_AST;
	}
	
	public final void bool() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST bool_AST = null;
		
		{
		switch ( LA(1)) {
		case TRUE:
		{
			AST tmp75_AST = null;
			tmp75_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp75_AST);
			match(TRUE);
			break;
		}
		case FALSE:
		{
			AST tmp76_AST = null;
			tmp76_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp76_AST);
			match(FALSE);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(LBRACE);
		match(RBRACE);
		bool_AST = (AST)currentAST.root;
		returnAST = bool_AST;
	}
	
	public final void signedNumeric() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST signedNumeric_AST = null;
		AST n_AST = null;
		
			boolean isNegative=false;
		
		
		{
		switch ( LA(1)) {
		case 53:
		{
			match(53);
			if ( inputState.guessing==0 ) {
				isNegative=true;
			}
			break;
		}
		case INTEGER:
		case DOUBLE:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		numeric();
		n_AST = (AST)returnAST;
		astFactory.addASTChild(currentAST, returnAST);
		if ( inputState.guessing==0 ) {
			if(isNegative) n_AST.setText("-"+n_AST.getText()); /*#n.setType(VALUE_NUMBER);*/
		}
		signedNumeric_AST = (AST)currentAST.root;
		returnAST = signedNumeric_AST;
	}
	
	public final void positioningConstraint() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST positioningConstraint_AST = null;
		AST l_AST = null;
		AST f_AST = null;
		
		switch ( LA(1)) {
		case POSITION:
		{
			AST tmp80_AST = null;
			tmp80_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp80_AST);
			match(POSITION);
			match(LBRACE);
			match(RBRACE);
			AST tmp83_AST = null;
			tmp83_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp83_AST);
			match(OP);
			{
			switch ( LA(1)) {
			case INTEGER:
			{
				AST tmp84_AST = null;
				tmp84_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp84_AST);
				match(INTEGER);
				break;
			}
			case LAST:
			{
				last();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case FIRST:
			{
				first();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			positioningConstraint_AST = (AST)currentAST.root;
			break;
		}
		case LAST:
		{
			last();
			l_AST = (AST)returnAST;
			if ( inputState.guessing==0 ) {
				positioningConstraint_AST = (AST)currentAST.root;
				positioningConstraint_AST = (AST)astFactory.make( (new ASTArray(3)).add(astFactory.create(POSITION)).add(astFactory.create(OP,"=")).add(l_AST));
				currentAST.root = positioningConstraint_AST;
				currentAST.child = positioningConstraint_AST!=null &&positioningConstraint_AST.getFirstChild()!=null ?
					positioningConstraint_AST.getFirstChild() : positioningConstraint_AST;
				currentAST.advanceChildToEnd();
			}
			break;
		}
		case FIRST:
		{
			first();
			f_AST = (AST)returnAST;
			if ( inputState.guessing==0 ) {
				positioningConstraint_AST = (AST)currentAST.root;
				positioningConstraint_AST = (AST)astFactory.make( (new ASTArray(3)).add(astFactory.create(POSITION)).add(astFactory.create(OP,"=")).add(f_AST));
				currentAST.root = positioningConstraint_AST;
				currentAST.child = positioningConstraint_AST!=null &&positioningConstraint_AST.getFirstChild()!=null ?
					positioningConstraint_AST.getFirstChild() : positioningConstraint_AST;
				currentAST.advanceChildToEnd();
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = positioningConstraint_AST;
	}
	
	public final void last() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST last_AST = null;
		
		AST tmp85_AST = null;
		tmp85_AST = astFactory.create(LT(1));
		match(LAST);
		AST tmp86_AST = null;
		tmp86_AST = astFactory.create(LT(1));
		match(LBRACE);
		AST tmp87_AST = null;
		tmp87_AST = astFactory.create(LT(1));
		match(RBRACE);
		if ( inputState.guessing==0 ) {
			last_AST = (AST)currentAST.root;
			last_AST = (AST)astFactory.make( (new ASTArray(1)).add(astFactory.create(INTEGER,"0")));
			currentAST.root = last_AST;
			currentAST.child = last_AST!=null &&last_AST.getFirstChild()!=null ?
				last_AST.getFirstChild() : last_AST;
			currentAST.advanceChildToEnd();
		}
		returnAST = last_AST;
	}
	
	public final void first() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST first_AST = null;
		
		AST tmp88_AST = null;
		tmp88_AST = astFactory.create(LT(1));
		match(FIRST);
		AST tmp89_AST = null;
		tmp89_AST = astFactory.create(LT(1));
		match(LBRACE);
		AST tmp90_AST = null;
		tmp90_AST = astFactory.create(LT(1));
		match(RBRACE);
		if ( inputState.guessing==0 ) {
			first_AST = (AST)currentAST.root;
			first_AST = (AST)astFactory.make( (new ASTArray(1)).add(astFactory.create(INTEGER,"1")));
			currentAST.root = first_AST;
			currentAST.child = first_AST!=null &&first_AST.getFirstChild()!=null ?
				first_AST.getFirstChild() : first_AST;
			currentAST.advanceChildToEnd();
		}
		returnAST = first_AST;
	}
	
	public final void numeric() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST numeric_AST = null;
		
		switch ( LA(1)) {
		case DOUBLE:
		{
			AST tmp91_AST = null;
			tmp91_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp91_AST);
			match(DOUBLE);
			numeric_AST = (AST)currentAST.root;
			break;
		}
		case INTEGER:
		{
			AST tmp92_AST = null;
			tmp92_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp92_AST);
			match(INTEGER);
			numeric_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = numeric_AST;
	}
	
	public final void orderSpecList() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST orderSpecList_AST = null;
		
		orderSpec();
		astFactory.addASTChild(currentAST, returnAST);
		{
		_loop108:
		do {
			if ((LA(1)==COMMA)) {
				match(COMMA);
				orderSpec();
				astFactory.addASTChild(currentAST, returnAST);
			}
			else {
				break _loop108;
			}
			
		} while (true);
		}
		orderSpecList_AST = (AST)currentAST.root;
		returnAST = orderSpecList_AST;
	}
	
	public final void orderSpec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST orderSpec_AST = null;
		AST a_AST = null;
		AST o_AST = null;
		AST s_AST = null;
		AST om_AST = null;
		
		switch ( LA(1)) {
		case AT:
		{
			match(AT);
			attributeName();
			a_AST = (AST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			{
			switch ( LA(1)) {
			case DESC:
			case ASC:
			{
				orderModifier();
				o_AST = (AST)returnAST;
				if ( inputState.guessing==0 ) {
					a_AST.setFirstChild(o_AST);
				}
				break;
			}
			case EOF:
			case COMMA:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			orderSpec_AST = (AST)currentAST.root;
			break;
		}
		case SCORE:
		{
			scoreFunction();
			s_AST = (AST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			orderModifier();
			om_AST = (AST)returnAST;
			if ( inputState.guessing==0 ) {
				s_AST.setFirstChild(om_AST);
			}
			orderSpec_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = orderSpec_AST;
	}
	
	public final void orderModifier() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST orderModifier_AST = null;
		
		switch ( LA(1)) {
		case ASC:
		{
			AST tmp95_AST = null;
			tmp95_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp95_AST);
			match(ASC);
			orderModifier_AST = (AST)currentAST.root;
			break;
		}
		case DESC:
		{
			AST tmp96_AST = null;
			tmp96_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp96_AST);
			match(DESC);
			orderModifier_AST = (AST)currentAST.root;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		returnAST = orderModifier_AST;
	}
	
	public final void scoreFunction() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST scoreFunction_AST = null;
		
		AST tmp97_AST = null;
		tmp97_AST = astFactory.create(LT(1));
		astFactory.addASTChild(currentAST, tmp97_AST);
		match(SCORE);
		match(LBRACE);
		{
		switch ( LA(1)) {
		case TRUE:
		case FALSE:
		case INTEGER:
		case VALUE_STRING:
		case 53:
		case DOUBLE:
		{
			paramList();
			astFactory.addASTChild(currentAST, returnAST);
			break;
		}
		case RBRACE:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(RBRACE);
		scoreFunction_AST = (AST)currentAST.root;
		returnAST = scoreFunction_AST;
	}
	
	public final void paramList() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST paramList_AST = null;
		AST l_AST = null;
		AST l1_AST = null;
		
		value();
		l_AST = (AST)returnAST;
		astFactory.addASTChild(currentAST, returnAST);
		{
		_loop116:
		do {
			if ((LA(1)==COMMA)) {
				match(COMMA);
				value();
				l1_AST = (AST)returnAST;
				astFactory.addASTChild(currentAST, returnAST);
			}
			else {
				break _loop116;
			}
			
		} while (true);
		}
		paramList_AST = (AST)currentAST.root;
		returnAST = paramList_AST;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"descending\"",
		"\"ascending\"",
		"\"or\"",
		"\"and\"",
		"\"not\"",
		"\"jcr:like\"",
		"\"jcr:contains\"",
		"\"jcr:deref\"",
		"\"jcr:score\"",
		"\"jcr:path\"",
		"\"order\"",
		"\"element\"",
		"\"text\"",
		"\"union\"",
		"\"true\"",
		"\"false\"",
		"\"last\"",
		"\"first\"",
		"\"position\"",
		"\"jcr:root\"",
		"ROOT_NODE_PATH",
		"PATH_CHILD",
		"PATH_DESCENDANT_OR_SELF",
		"NODES_COLLECTION",
		"ATTRIBUTE",
		"VALUE",
		"VALUE_NUMBER",
		"VALUE_BOOL",
		"NODE_TYPE",
		"CHILD_INDEX",
		"FILTER",
		"SELECT",
		"QUERY",
		"GROUPING",
		"UNION",
		"SLASH",
		"DOUBLESLASH",
		"LBRACKET",
		"INTEGER",
		"OP",
		"RBRACKET",
		"LBRACE",
		"AT",
		"RBRACE",
		"COMMA",
		"QNAME",
		"STAR",
		"DOT",
		"a string enclosed in quotation marks",
		"\"-\"",
		"DOUBLE",
		"\"by\"",
		"COLON",
		"ESCAPE",
		"NUMERIC",
		"EXPONENT",
		"SIGN",
		"Prefix",
		"LocalPart",
		"NCName",
		"NCNameStartChar",
		"NCNameChar",
		"NameChar",
		"Letter",
		"BaseChar",
		"Ideographic",
		"CombiningChar",
		"Extender",
		"WS"
	};
	
	protected void buildTokenTypeASTClassMap() {
		tokenTypeToASTClassMap=null;
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 1924145496066L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 37825124149223410L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 1794402984910832L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 672626238439426L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 274878054402L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 31635148562499328L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 32206894608951040L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 4123168751618L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	
	}
