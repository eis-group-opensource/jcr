/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype.compact;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;

/**
 * Lexer
 */
public class Lexer {
    public static final char SINGLE_QUOTE = '\'';
    public static final char DOUBLE_QUOTE = '\"';
    public static final char BEGIN_NODE_TYPE_NAME = '[';
    public static final char END_NODE_TYPE_NAME = ']';
    public static final char EXTENDS = '>';
    public static final char LIST_DELIMITER = ',';
    public static final char PROPERTY_DEFINITION = '-';
    public static final char CHILD_NODE_DEFINITION = '+';
    public static final char BEGIN_TYPE = '(';
    public static final char END_TYPE = ')';
    public static final char DEFAULT = '=';
    public static final char CONSTRAINT = '<';

    public static final String[] ORDERABLE = new String[] {"orderable", "ord", "o"};
    public static final String[] MIXIN = new String[]{"mixin", "mix", "m"};

    public static final String[] PRIMARY = new String[]{"primary", "pri", "!"};
    public static final String[] AUTOCREATED = new String[]{"autocreated", "aut", "a"};
    public static final String[] MANDATORY = new String[]{"mandatory", "man", "m"};
    public static final String[] PROTECTED = new String[]{"protected", "pro", "p"};
    public static final String[] INDEXABLE = new String[]{"indexable", "idx", "i"};
    public static final String[] FTS = new String[]{"fts", "fts", "f"};
    public static final String[] MULTIPLE = new String[]{"multiple", "mul", "*"};

    public static final String[] COPY = new String[]{"copy", "Copy", "COPY"};
    public static final String[] VERSION = new String[]{"version", "Version", "VERSION"};
    public static final String[] INITIALIZE = new String[]{"initialize", "Initialize", "INITIALIZE"};
    public static final String[] COMPUTE = new String[]{"compute", "Compute", "COMPUTE"};
    public static final String[] IGNORE = new String[]{"ignore", "Ignore", "IGNORE"};
    public static final String[] ABORT = new String[]{"abort", "Abort", "ABORT"};

    public static final String[] ATTRIBUTE = new String[]{"primary", "pri", "!",
                                                          "autocreated", "aut", "a",
                                                          "mandatory", "man", "m",
                                                          "protected", "pro", "p",
                                                          "indexable", "idx", "i",
                                                          "fts", "fts", "f",
                                                          "multiple", "mul", "*",
                                                          "copy", "Copy", "COPY",
                                                          "version", "Version", "VERSION",
                                                          "initialize", "Initialize", "INITIALIZE",
                                                          "compute", "Compute", "COMPUTE",
                                                          "ignore", "Ignore", "IGNORE",
                                                          "abort", "Abort", "ABORT"};

    public static final String[] STRING = {"string", "String", "STRING"};
    public static final String[] BINARY = {"binary", "Binary", "BINARY"};
    public static final String[] LONG = {"long", "Long", "LONG"};
    public static final String[] DOUBLE = {"double", "Double", "DOUBLE"};
    public static final String[] BOOLEAN = {"boolean", "Boolean", "BOOLEAN"};
    public static final String[] DATE = {"date", "Date", "DATE"};
    public static final String[] NAME = {"name", "Name", "NAME"};
    public static final String[] PATH = {"path", "Path", "PATH"};
    public static final String[] REFERENCE = {"reference", "Reference", "REFERENCE"};
    public static final String[] WEAKREFERENCE = {"weakreference", "WeakReference", "WEAKREFERENCE"};

    public static final String[] UNDEFINED = new String[]{"undefined", "Undefined", "UNDEFINED", "*"};

    public static final String EOF = "eof";

    private final StreamTokenizer st;

    private final String systemId;

    /**
     * Constructor
     * @param r
     */
    public Lexer(Reader r, String systemId) {
        this.systemId = systemId;
        st = new StreamTokenizer(r);

        st.eolIsSignificant(false);

        st.lowerCaseMode(false);

        st.slashSlashComments(true);
        st.slashStarComments(true);

        st.wordChars('a', 'z');
        st.wordChars('A', 'Z');
        st.wordChars(':', ':');
        st.wordChars('_', '_');

        st.quoteChar(SINGLE_QUOTE);
        st.quoteChar(DOUBLE_QUOTE);

        st.ordinaryChar(BEGIN_NODE_TYPE_NAME);
        st.ordinaryChar(END_NODE_TYPE_NAME);
        st.ordinaryChar(EXTENDS);
        st.ordinaryChar(LIST_DELIMITER);
        st.ordinaryChar(PROPERTY_DEFINITION);
        st.ordinaryChar(CHILD_NODE_DEFINITION);
        st.ordinaryChar(BEGIN_TYPE);
        st.ordinaryChar(END_TYPE);
        st.ordinaryChar(DEFAULT);
        st.ordinaryChar(CONSTRAINT);
    }

    /**
     * getNextToken
     *
     * @return
     * @throws ParseException
     */
    public String getNextToken() throws ParseException {
        try {
            int tokenType = st.nextToken();
            if (tokenType == StreamTokenizer.TT_EOF) {
                return EOF;
            } else if (tokenType == StreamTokenizer.TT_WORD
                    || tokenType == SINGLE_QUOTE
                    || tokenType == DOUBLE_QUOTE) {
                return st.sval;
            } else if (tokenType == StreamTokenizer.TT_NUMBER) {
                return String.valueOf(st.nval);
            } else {
                return new String(new char[] {(char) tokenType});
            }
        } catch (IOException e) {
            fail("IOException while attempting to read input stream", e);
            return null;
        }
    }

    public void fail(String message) throws ParseException {
        throw new ParseException(message, st.lineno(), -1, systemId);
    }

    public void fail(String message, Throwable e) throws ParseException {
        throw new ParseException(message, e, st.lineno(), -1, systemId);
    }

    public void fail(Throwable e) throws ParseException {
        throw new ParseException(e, st.lineno(), -1, systemId);
    }
}
