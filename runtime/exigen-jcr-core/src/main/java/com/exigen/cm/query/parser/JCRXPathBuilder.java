/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.query.parser;
import com.exigen.cm.query.order.OrderDefinition;
import com.exigen.cm.query.QueryBuilder;
import java.util.*;

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


public class JCRXPathBuilder extends antlr.TreeParser       implements JCRXPathBuilderTokenTypes
 {

	private QueryBuilder query;
	protected void illegalAttributeReport(){
		throw new RuntimeException("jcr:path attribute is not supported in XPath query. Use XPath path definition instead.");		
	}
	
	protected Object instantiate(AST valNode){
		switch(valNode.getType()){
//			case VALUE_NUMBER:
//				return new Double(valNode.getText());

			case INTEGER:
				return new Integer(valNode.getText());
				
			case DOUBLE:
				return new Double(valNode.getText());


			case VALUE_STRING:
//			case QUOTED_STRING_VALUE:
				return valNode.getText();
			
			case VALUE_BOOL:
				return Boolean.valueOf(valNode.getText());
		}
		
		return null;
	}
public JCRXPathBuilder() {
	tokenNames = _tokenNames;
}

	public final void build(AST _t,
		QueryBuilder query
	) throws RecognitionException {
		
		AST build_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		
			this.query = query;
		
		
		try {      // for error handling
			{
			int _cnt119=0;
			_loop119:
			do {
				if (_t==null) _t=ASTNULL;
				if ((_t.getType()==QUERY)) {
					subquery(_t);
					_t = _retTree;
				}
				else {
					if ( _cnt119>=1 ) { break _loop119; } else {throw new NoViableAltException(_t);}
				}
				
				_cnt119++;
			} while (true);
			}
			{
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case ORDER:
			{
				AST __t121 = _t;
				AST tmp101_AST_in = (AST)_t;
				match(_t,ORDER);
				_t = _t.getFirstChild();
				{
				int _cnt123=0;
				_loop123:
				do {
					if (_t==null) _t=ASTNULL;
					if ((_t.getType()==SCORE||_t.getType()==QNAME)) {
						orderDef(_t);
						_t = _retTree;
					}
					else {
						if ( _cnt123>=1 ) { break _loop123; } else {throw new NoViableAltException(_t);}
					}
					
					_cnt123++;
				} while (true);
				}
				_t = __t121;
				_t = _t.getNextSibling();
				break;
			}
			case 3:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				if (_t!=null) {_t = _t.getNextSibling();}
			} else {
			  throw ex;
			}
		}
		_retTree = _t;
	}
	
	public final void subquery(AST _t) throws RecognitionException {
		
		AST subquery_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		
		try {      // for error handling
			AST __t125 = _t;
			AST tmp102_AST_in = (AST)_t;
			match(_t,QUERY);
			_t = _t.getFirstChild();
			if ( inputState.guessing==0 ) {
				query.startSubquery();
			}
			{
			int _cnt127=0;
			_loop127:
			do {
				if (_t==null) _t=ASTNULL;
				if (((_t.getType() >= ROOT_NODE_PATH && _t.getType() <= PATH_DESCENDANT_OR_SELF))) {
					step(_t);
					_t = _retTree;
					if ( inputState.guessing==0 ) {
						query.endPathElement();
					}
				}
				else {
					if ( _cnt127>=1 ) { break _loop127; } else {throw new NoViableAltException(_t);}
				}
				
				_cnt127++;
			} while (true);
			}
			_t = __t125;
			_t = _t.getNextSibling();
			if ( inputState.guessing==0 ) {
				query.endSubquery();
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				if (_t!=null) {_t = _t.getNextSibling();}
			} else {
			  throw ex;
			}
		}
		_retTree = _t;
	}
	
	public final void orderDef(AST _t) throws RecognitionException {
		
		AST orderDef_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		AST a2 = null;
		AST o2 = null;
		AST s = null;
		AST v = null;
		AST o3 = null;
		
				List attrs = new LinkedList();
			
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case QNAME:
			{
				AST __t172 = _t;
				a2 = _t==ASTNULL ? null :(AST)_t;
				match(_t,QNAME);
				_t = _t.getFirstChild();
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case DESC:
				case ASC:
				{
					o2 = _t==ASTNULL ? null : (AST)_t;
					orderType(_t);
					_t = _retTree;
					break;
				}
				case 3:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(_t);
				}
				}
				}
				_t = __t172;
				_t = _t.getNextSibling();
				if ( inputState.guessing==0 ) {
					
							query.addOrderByAttribute(a2.getText(), o2 == null 
																		? OrderDefinition.ORDER_DEFAULT 
																		:  OrderDefinition.ORDER.valueOf(o2.getText())
																		);
				}
				break;
			}
			case SCORE:
			{
				AST __t174 = _t;
				s = _t==ASTNULL ? null :(AST)_t;
				match(_t,SCORE);
				_t = _t.getFirstChild();
				{
				_loop176:
				do {
					if (_t==null) _t=ASTNULL;
					if ((_tokenSet_0.member(_t.getType()))) {
						v = _t==ASTNULL ? null : (AST)_t;
						param(_t);
						_t = _retTree;
						if ( inputState.guessing==0 ) {
							attrs.add(v.getText());
						}
					}
					else {
						break _loop176;
					}
					
				} while (true);
				}
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case DESC:
				case ASC:
				{
					o3 = _t==ASTNULL ? null : (AST)_t;
					orderType(_t);
					_t = _retTree;
					break;
				}
				case 3:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(_t);
				}
				}
				}
				_t = __t174;
				_t = _t.getNextSibling();
				if ( inputState.guessing==0 ) {
					
							query.addOrderByScore(attrs, o3 == null 
															? OrderDefinition.ORDER_DEFAULT 
															: OrderDefinition.ORDER.valueOf(o2.getText())
															);
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				if (_t!=null) {_t = _t.getNextSibling();}
			} else {
			  throw ex;
			}
		}
		_retTree = _t;
	}
	
	public final void step(AST _t) throws RecognitionException {
		
		AST step_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		
			query.startPathElement();
		
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case ROOT_NODE_PATH:
			{
				AST tmp103_AST_in = (AST)_t;
				match(_t,ROOT_NODE_PATH);
				_t = _t.getNextSibling();
				if ( inputState.guessing==0 ) {
					query.setRootQuery();
				}
				break;
			}
			case PATH_DESCENDANT_OR_SELF:
			{
				AST __t129 = _t;
				AST tmp104_AST_in = (AST)_t;
				match(_t,PATH_DESCENDANT_OR_SELF);
				_t = _t.getFirstChild();
				if ( inputState.guessing==0 ) {
					query.setPathElementDescOrSelf();
				}
				{
				int _cnt131=0;
				_loop131:
				do {
					if (_t==null) _t=ASTNULL;
					if ((_tokenSet_1.member(_t.getType()))) {
						pathElement(_t);
						_t = _retTree;
					}
					else {
						if ( _cnt131>=1 ) { break _loop131; } else {throw new NoViableAltException(_t);}
					}
					
					_cnt131++;
				} while (true);
				}
				_t = __t129;
				_t = _t.getNextSibling();
				break;
			}
			case PATH_CHILD:
			{
				AST __t132 = _t;
				AST tmp105_AST_in = (AST)_t;
				match(_t,PATH_CHILD);
				_t = _t.getFirstChild();
				{
				int _cnt134=0;
				_loop134:
				do {
					if (_t==null) _t=ASTNULL;
					if ((_tokenSet_1.member(_t.getType()))) {
						pathElement(_t);
						_t = _retTree;
					}
					else {
						if ( _cnt134>=1 ) { break _loop134; } else {throw new NoViableAltException(_t);}
					}
					
					_cnt134++;
				} while (true);
				}
				_t = __t132;
				_t = _t.getNextSibling();
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				if (_t!=null) {_t = _t.getNextSibling();}
			} else {
			  throw ex;
			}
		}
		_retTree = _t;
	}
	
	public final void pathElement(AST _t) throws RecognitionException {
		
		AST pathElement_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		AST q = null;
		AST c = null;
		AST a = null;
		AST n = null;
		AST a1 = null;
		AST v1 = null;
		AST a2 = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case QNAME:
			{
				q = (AST)_t;
				match(_t,QNAME);
				_t = _t.getNextSibling();
				if ( inputState.guessing==0 ) {
					query.setPathElementName(q.getText());
				}
				break;
			}
			case STAR:
			{
				AST tmp106_AST_in = (AST)_t;
				match(_t,STAR);
				_t = _t.getNextSibling();
				if ( inputState.guessing==0 ) {
					query.setPathElementWildcard();
				}
				break;
			}
			case CHILD_INDEX:
			{
				c = (AST)_t;
				match(_t,CHILD_INDEX);
				_t = _t.getNextSibling();
				if ( inputState.guessing==0 ) {
					query.setPathElementIndex(c.getText());
				}
				break;
			}
			case ELEMENT:
			{
				AST __t136 = _t;
				AST tmp107_AST_in = (AST)_t;
				match(_t,ELEMENT);
				_t = _t.getFirstChild();
				if ( inputState.guessing==0 ) {
					query.setPathStepElement();
				}
				{
				if (_t==null) _t=ASTNULL;
				switch ( _t.getType()) {
				case QNAME:
				case STAR:
				{
					{
					if (_t==null) _t=ASTNULL;
					switch ( _t.getType()) {
					case QNAME:
					{
						a = (AST)_t;
						match(_t,QNAME);
						_t = _t.getNextSibling();
						if ( inputState.guessing==0 ) {
							query.setPathElementName(a.getText());
						}
						break;
					}
					case STAR:
					{
						AST tmp108_AST_in = (AST)_t;
						match(_t,STAR);
						_t = _t.getNextSibling();
						if ( inputState.guessing==0 ) {
							query.setPathElementWildcard();
						}
						break;
					}
					default:
					{
						throw new NoViableAltException(_t);
					}
					}
					}
					{
					if (_t==null) _t=ASTNULL;
					switch ( _t.getType()) {
					case NODE_TYPE:
					{
						n = (AST)_t;
						match(_t,NODE_TYPE);
						_t = _t.getNextSibling();
						if ( inputState.guessing==0 ) {
							query.setPathElementType(n.getText());
						}
						break;
					}
					case 3:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(_t);
					}
					}
					}
					break;
				}
				case 3:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(_t);
				}
				}
				}
				_t = __t136;
				_t = _t.getNextSibling();
				break;
			}
			case DEREF:
			{
				AST __t140 = _t;
				AST tmp109_AST_in = (AST)_t;
				match(_t,DEREF);
				_t = _t.getFirstChild();
				a1 = (AST)_t;
				match(_t,QNAME);
				_t = _t.getNextSibling();
				v1 = (AST)_t;
				match(_t,QNAME);
				_t = _t.getNextSibling();
				if ( inputState.guessing==0 ) {
					query.setPathStepDeref( a1.getText(), v1.getText());
				}
				_t = __t140;
				_t = _t.getNextSibling();
				break;
			}
			case FILTER:
			{
				AST __t141 = _t;
				AST tmp110_AST_in = (AST)_t;
				match(_t,FILTER);
				_t = _t.getFirstChild();
				orExpression(_t);
				_t = _retTree;
				_t = __t141;
				_t = _t.getNextSibling();
				break;
			}
			case SELECT:
			{
				AST __t142 = _t;
				AST tmp111_AST_in = (AST)_t;
				match(_t,SELECT);
				_t = _t.getFirstChild();
				{
				_loop144:
				do {
					if (_t==null) _t=ASTNULL;
					if ((_t.getType()==QNAME)) {
						a2 = _t==ASTNULL ? null : (AST)_t;
						attribute(_t);
						_t = _retTree;
						if ( inputState.guessing==0 ) {
							query.addSelectedAttribute(a2.getText());
						}
					}
					else {
						break _loop144;
					}
					
				} while (true);
				}
				_t = __t142;
				_t = _t.getNextSibling();
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				if (_t!=null) {_t = _t.getNextSibling();}
			} else {
			  throw ex;
			}
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
				AST __t147 = _t;
				AST tmp112_AST_in = (AST)_t;
				match(_t,OR);
				_t = _t.getFirstChild();
				if ( inputState.guessing==0 ) {
					query.attachOr();
				}
				{
				if (_t==null) _t=ASTNULL;
				if ((_tokenSet_2.member(_t.getType()))) {
					andExpression(_t);
					_t = _retTree;
				}
				else if ((_tokenSet_3.member(_t.getType()))) {
					orExpression(_t);
					_t = _retTree;
				}
				else {
					throw new NoViableAltException(_t);
				}
				
				}
				{
				if (_t==null) _t=ASTNULL;
				if ((_tokenSet_2.member(_t.getType()))) {
					andExpression(_t);
					_t = _retTree;
				}
				else if ((_tokenSet_3.member(_t.getType()))) {
					orExpression(_t);
					_t = _retTree;
				}
				else {
					throw new NoViableAltException(_t);
				}
				
				}
				_t = __t147;
				_t = _t.getNextSibling();
				break;
			}
			case AND:
			case NOT:
			case LIKE:
			case CONTAINS:
			case JCR_PATH:
			case POSITION:
			case GROUPING:
			case QNAME:
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
			if (inputState.guessing==0) {
				reportError(ex);
				if (_t!=null) {_t = _t.getNextSibling();}
			} else {
			  throw ex;
			}
		}
		_retTree = _t;
	}
	
	public final void attribute(AST _t) throws RecognitionException {
		
		AST attribute_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		
		try {      // for error handling
			AST tmp113_AST_in = (AST)_t;
			match(_t,QNAME);
			_t = _t.getNextSibling();
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				if (_t!=null) {_t = _t.getNextSibling();}
			} else {
			  throw ex;
			}
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
				AST __t151 = _t;
				AST tmp114_AST_in = (AST)_t;
				match(_t,AND);
				_t = _t.getFirstChild();
				if ( inputState.guessing==0 ) {
					query.attachAnd();
				}
				orExpression(_t);
				_t = _retTree;
				orExpression(_t);
				_t = _retTree;
				_t = __t151;
				_t = _t.getNextSibling();
				break;
			}
			case NOT:
			case LIKE:
			case CONTAINS:
			case JCR_PATH:
			case POSITION:
			case GROUPING:
			case QNAME:
			{
				expression(_t);
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
			if (inputState.guessing==0) {
				reportError(ex);
				if (_t!=null) {_t = _t.getNextSibling();}
			} else {
			  throw ex;
			}
		}
		_retTree = _t;
	}
	
	public final void expression(AST _t) throws RecognitionException {
		
		AST expression_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		AST a = null;
		AST a4 = null;
		AST o3 = null;
		AST v3 = null;
		AST a5 = null;
		AST d = null;
		AST v4 = null;
		AST n = null;
		AST v5 = null;
		AST n4 = null;
		AST v9 = null;
		AST po = null;
		AST pi = null;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case GROUPING:
			{
				AST __t158 = _t;
				AST tmp115_AST_in = (AST)_t;
				match(_t,GROUPING);
				_t = _t.getFirstChild();
				if ( inputState.guessing==0 ) {
					query.attachGrouping();
				}
				orExpression(_t);
				_t = _retTree;
				_t = __t158;
				_t = _t.getNextSibling();
				break;
			}
			case LIKE:
			{
				AST __t169 = _t;
				AST tmp116_AST_in = (AST)_t;
				match(_t,LIKE);
				_t = _t.getFirstChild();
				n4 = (AST)_t;
				match(_t,QNAME);
				_t = _t.getNextSibling();
				v9 = (AST)_t;
				match(_t,VALUE_STRING);
				_t = _t.getNextSibling();
				_t = __t169;
				_t = _t.getNextSibling();
				if ( inputState.guessing==0 ) {
					query.attachLike(n4.getText(), v9.getText());
				}
				break;
			}
			case POSITION:
			{
				AST __t170 = _t;
				AST tmp117_AST_in = (AST)_t;
				match(_t,POSITION);
				_t = _t.getFirstChild();
				po = (AST)_t;
				match(_t,OP);
				_t = _t.getNextSibling();
				pi = (AST)_t;
				match(_t,INTEGER);
				_t = _t.getNextSibling();
				_t = __t170;
				_t = _t.getNextSibling();
				if ( inputState.guessing==0 ) {
					query.attachPositionConstraint(po.getText(), pi.getText());
				}
				break;
			}
			case JCR_PATH:
			{
				AST tmp118_AST_in = (AST)_t;
				match(_t,JCR_PATH);
				_t = _t.getNextSibling();
				if ( inputState.guessing==0 ) {
					illegalAttributeReport();
				}
				break;
			}
			default:
				boolean synPredMatched155 = false;
				if (((_t.getType()==NOT))) {
					AST __t155 = _t;
					synPredMatched155 = true;
					inputState.guessing++;
					try {
						{
						AST __t154 = _t;
						AST tmp119_AST_in = (AST)_t;
						match(_t,NOT);
						_t = _t.getFirstChild();
						attribute(_t);
						_t = _retTree;
						_t = __t154;
						_t = _t.getNextSibling();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched155 = false;
					}
					_t = __t155;
					inputState.guessing--;
				}
				if ( synPredMatched155 ) {
					AST __t156 = _t;
					AST tmp120_AST_in = (AST)_t;
					match(_t,NOT);
					_t = _t.getFirstChild();
					a = _t==ASTNULL ? null : (AST)_t;
					attribute(_t);
					_t = _retTree;
					_t = __t156;
					_t = _t.getNextSibling();
					if ( inputState.guessing==0 ) {
						query.attachIsNull(a.getText());
					}
				}
				else if ((_t.getType()==NOT)) {
					AST __t157 = _t;
					AST tmp121_AST_in = (AST)_t;
					match(_t,NOT);
					_t = _t.getFirstChild();
					if ( inputState.guessing==0 ) {
						query.attachNot();
					}
					orExpression(_t);
					_t = _retTree;
					_t = __t157;
					_t = _t.getNextSibling();
				}
				else {
					boolean synPredMatched161 = false;
					if (((_t.getType()==QNAME))) {
						AST __t161 = _t;
						synPredMatched161 = true;
						inputState.guessing++;
						try {
							{
							AST __t160 = _t;
							AST tmp122_AST_in = (AST)_t;
							match(_t,QNAME);
							_t = _t.getFirstChild();
							AST tmp123_AST_in = (AST)_t;
							match(_t,OP);
							_t = _t.getNextSibling();
							_t = __t160;
							_t = _t.getNextSibling();
							}
						}
						catch (RecognitionException pe) {
							synPredMatched161 = false;
						}
						_t = __t161;
						inputState.guessing--;
					}
					if ( synPredMatched161 ) {
						AST __t162 = _t;
						a4 = _t==ASTNULL ? null :(AST)_t;
						match(_t,QNAME);
						_t = _t.getFirstChild();
						AST __t163 = _t;
						o3 = _t==ASTNULL ? null :(AST)_t;
						match(_t,OP);
						_t = _t.getFirstChild();
						v3 = _t==ASTNULL ? null : (AST)_t;
						param(_t);
						_t = _retTree;
						_t = __t163;
						_t = _t.getNextSibling();
						_t = __t162;
						_t = _t.getNextSibling();
						if ( inputState.guessing==0 ) {
							query.attachComparison(a4.getText(), o3.getText(), instantiate(v3));
						}
					}
					else if ((_t.getType()==QNAME)) {
						a5 = _t==ASTNULL ? null : (AST)_t;
						attribute(_t);
						_t = _retTree;
						if ( inputState.guessing==0 ) {
							query.attachIsNotNull(a5.getText());
						}
					}
					else {
						boolean synPredMatched166 = false;
						if (((_t.getType()==CONTAINS))) {
							AST __t166 = _t;
							synPredMatched166 = true;
							inputState.guessing++;
							try {
								{
								AST __t165 = _t;
								AST tmp124_AST_in = (AST)_t;
								match(_t,CONTAINS);
								_t = _t.getFirstChild();
								AST tmp125_AST_in = (AST)_t;
								match(_t,DOT);
								_t = _t.getNextSibling();
								_t = __t165;
								_t = _t.getNextSibling();
								}
							}
							catch (RecognitionException pe) {
								synPredMatched166 = false;
							}
							_t = __t166;
							inputState.guessing--;
						}
						if ( synPredMatched166 ) {
							AST __t167 = _t;
							AST tmp126_AST_in = (AST)_t;
							match(_t,CONTAINS);
							_t = _t.getFirstChild();
							d = (AST)_t;
							match(_t,DOT);
							_t = _t.getNextSibling();
							v4 = (AST)_t;
							match(_t,VALUE_STRING);
							_t = _t.getNextSibling();
							_t = __t167;
							_t = _t.getNextSibling();
							if ( inputState.guessing==0 ) {
								query.attachContains(null, v4.getText());
							}
						}
						else if ((_t.getType()==CONTAINS)) {
							AST __t168 = _t;
							AST tmp127_AST_in = (AST)_t;
							match(_t,CONTAINS);
							_t = _t.getFirstChild();
							n = (AST)_t;
							match(_t,QNAME);
							_t = _t.getNextSibling();
							v5 = (AST)_t;
							match(_t,VALUE_STRING);
							_t = _t.getNextSibling();
							_t = __t168;
							_t = _t.getNextSibling();
							if ( inputState.guessing==0 ) {
								query.attachContains(n.getText(), v5.getText());
							}
						}
					else {
						throw new NoViableAltException(_t);
					}
					}}}
				}
				catch (RecognitionException ex) {
					if (inputState.guessing==0) {
						reportError(ex);
						if (_t!=null) {_t = _t.getNextSibling();}
					} else {
					  throw ex;
					}
				}
				_retTree = _t;
			}
			
	public final void param(AST _t) throws RecognitionException {
		
		AST param_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case VALUE_STRING:
			{
				AST tmp128_AST_in = (AST)_t;
				match(_t,VALUE_STRING);
				_t = _t.getNextSibling();
				break;
			}
			case VALUE_BOOL:
			{
				AST tmp129_AST_in = (AST)_t;
				match(_t,VALUE_BOOL);
				_t = _t.getNextSibling();
				break;
			}
			case DOUBLE:
			{
				AST tmp130_AST_in = (AST)_t;
				match(_t,DOUBLE);
				_t = _t.getNextSibling();
				break;
			}
			case INTEGER:
			{
				AST tmp131_AST_in = (AST)_t;
				match(_t,INTEGER);
				_t = _t.getNextSibling();
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				if (_t!=null) {_t = _t.getNextSibling();}
			} else {
			  throw ex;
			}
		}
		_retTree = _t;
	}
	
	public final void orderType(AST _t) throws RecognitionException {
		
		AST orderType_AST_in = (_t == ASTNULL) ? null : (AST)_t;
		
		try {      // for error handling
			if (_t==null) _t=ASTNULL;
			switch ( _t.getType()) {
			case ASC:
			{
				AST tmp132_AST_in = (AST)_t;
				match(_t,ASC);
				_t = _t.getNextSibling();
				if ( inputState.guessing==0 ) {
					orderType_AST_in.setText(OrderDefinition.ORDER.ASC.name());
				}
				break;
			}
			case DESC:
			{
				AST tmp133_AST_in = (AST)_t;
				match(_t,DESC);
				_t = _t.getNextSibling();
				if ( inputState.guessing==0 ) {
					orderType_AST_in.setText(OrderDefinition.ORDER.DESC.name());
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(_t);
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				if (_t!=null) {_t = _t.getNextSibling();}
			} else {
			  throw ex;
			}
		}
		_retTree = _t;
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
		"\"by\""
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 22522398330847232L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 1688909989840896L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 563087396579200L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 563087396579264L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	}
	
