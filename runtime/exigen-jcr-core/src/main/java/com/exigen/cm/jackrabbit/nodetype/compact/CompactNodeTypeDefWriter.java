/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype.compact;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;

import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.NodeDef;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.nodetype.ValueConstraint;
import com.exigen.cm.jackrabbit.value.InternalValue;



/**
 * Prints node type defs in a compact notation
 * Print Format:
 * <ex = "http://apache.org/jackrabbit/example">
 * [ex:NodeType] > ex:ParentType1, ex:ParentType2
 * orderable mixin
 *   - ex:property (STRING) = 'default1', 'default2'
 *     primary mandatory autocreated protected multiple VERSION
 *     < 'constraint1', 'constraint2'
 *   + ex:node (ex:reqType1, ex:reqType2) = ex:defaultType
 *     mandatory autocreated protected multiple VERSION
 */
public class CompactNodeTypeDefWriter {

    /**
     * the indention string
     */
    public static final String INDENT = "  ";

    /**
     * the current namespace resolver
     */
    private final NamespaceResolver resolver;

    /**
     * the underlying writer
     */
    private Writer out;

    /**
     * special writer used for namespaces
     */
    private Writer nsWriter;

    /**
     * namespaces(prefixes) that are used
     */
    private HashSet usedNamespaces = new HashSet();

    /**
     * Creates a new nodetype writer
     *
     * @param out the underlying writer
     * @param r the namespace resolver
     */
    public CompactNodeTypeDefWriter(Writer out, NamespaceResolver r) {
        this(out, r, false);
    }

    /**
     * Creates a new nodetype writer
     *
     * @param out the underlaying writer
     * @param r the naespace resolver
     * @param includeNS if <code>true</code> all used namespace decl. are also
     *        written.
     */
    public CompactNodeTypeDefWriter(Writer out, NamespaceResolver r, boolean includeNS) {
        this.resolver = r;
        if (includeNS) {
            this.out = new StringWriter();
            this.nsWriter = out;
        } else {
            this.out = out;
            this.nsWriter = null;
        }
    }

    /**
     * Writes the given list of NodeTypeDefs to the output writer including the
     * used namespaces.
     *
     * @param l
     * @param r
     * @param out
     * @throws IOException
     * @throws RepositoryException 
     */
    public static void write(List l, NamespaceResolver r, Writer out)
            throws IOException, RepositoryException {
        CompactNodeTypeDefWriter w = new CompactNodeTypeDefWriter(out, r, true);
        Iterator iter = l.iterator();
        while (iter.hasNext()) {
            NodeTypeDef def = (NodeTypeDef) iter.next();
            w.write(def);
        }
        w.close();
    }

    /**
     * Write one NodeTypeDef to this writer
     *
     * @param d
     * @throws IOException
     * @throws RepositoryException 
     */
    public void write(NodeTypeDef d) throws IOException, RepositoryException {
        writeName(d);
        writeSupertypes(d);
        writeOptions(d);
        writeTable(d);
        writePropDefs(d);
        writeNodeDefs(d);
        out.write("\n\n");
    }

    /**
     * Flushes all pending write operations and Closes this writer. please note,
     * that the underlying writer remains open.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (nsWriter != null) {
            nsWriter.write("\n");
            out.close();
            nsWriter.write(((StringWriter) out).getBuffer().toString());
            out = nsWriter;
            nsWriter = null;
        }
        out.flush();
        out = null;
    }

    /**
     * write name
     */
    public void writeName(NodeTypeDef ntd) throws IOException {
        out.write("[");
        out.write(resolve(ntd.getName()));
        out.write("]");
    }

    /**
     * write supertypes
     */
    public void writeSupertypes(NodeTypeDef ntd) throws IOException {
        QName[] sta = ntd.getSupertypes();
        String delim = " > ";
        for (int i = 0; i < sta.length; i++) {
            //if (!sta[i].equals(QName.NT_BASE)) {
                out.write(delim);
                out.write(resolve(sta[i]));
                delim = ", ";
            //}
        }
    }

    /**
     * write options
     */
    public void writeOptions(NodeTypeDef ntd) throws IOException {
        if (ntd.hasOrderableChildNodes()) {
            out.write("\n" + INDENT);
            out.write("orderable");
            if (ntd.isMixin()) {
                out.write(" mixin");
            }
        } else if (ntd.isMixin()) {
            out.write("\n" + INDENT);
            out.write("mixin");
        }
    }

    public void writeTable(NodeTypeDef ntd) throws IOException {
        if (ntd.getTableName() != null && ntd.getTableName().trim().length() > 0) {
            out.write(INDENT + Lexer.BEGIN_TYPE);
            out.write(ntd.getTableName());
            out.write(Lexer.END_TYPE);
        } 
    }

    /**
     * write prop defs
     * @throws RepositoryException 
     */
    private void writePropDefs(NodeTypeDef ntd) throws IOException, RepositoryException {
        PropDef[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            writePropDef(ntd, pd);
        }
    }

    /**
     * write node defs
     */
    private void writeNodeDefs(NodeTypeDef ntd) throws IOException {
        NodeDef[] nda = ntd.getChildNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            NodeDef nd = nda[i];
            writeNodeDef(ntd, nd);
        }
    }

    /**
     * write prop def
     * @param pd
     */
    public void writePropDef(NodeTypeDef ntd, PropDef pd) throws IOException,RepositoryException {
        out.write("\n" + INDENT + "- ");
        writeItemDefName(pd.getName());
        out.write(" (");
        out.write(PropertyType283.nameFromValue(pd.getRequiredType()).toLowerCase());
        out.write(")");
        writeDefaultValues(pd.getDefaultValues());
        out.write(ntd.getPrimaryItemName() != null && ntd.getPrimaryItemName().equals(pd.getName()) ? " primary" : "");
        if (pd.isMandatory()) {
            out.write(" mandatory");
        }
        if (pd.isAutoCreated()) {
            out.write(" autocreated");
        }
        if (pd.isProtected()) {
            out.write(" protected");
        }
        if (pd.isMultiple()) {
            out.write(" multiple");
        }
        if (pd.isIndexable()) {
            out.write(" indexable");
        }
        if (pd.isFullTextSearch()) {
            out.write(" fts");
        }
        if (pd.getOnParentVersion() != OnParentVersionAction.COPY) {
            out.write(" ");
            out.write(OnParentVersionAction.nameFromValue(pd.getOnParentVersion()).toLowerCase());
        }
        writeValueConstraints(pd.getValueConstraints());
    }

    /**
     * write default values
     * @param dva
     */
    private void writeDefaultValues(InternalValue[] dva) throws IOException {
        if (dva != null && dva.length > 0) {
            String delim = " = '";
            for (int i = 0; i < dva.length; i++) {
                out.write(delim);
                try {
                    out.write(escape(dva[i].toJCRValue(resolver).getString()));
                } catch (RepositoryException e) {
                    out.write(escape(dva[i].toString()));
                }
                out.write("'");
                delim = ", '";
            }
        }
    }

    /**
     * write value constraints
     * @param vca
     */
    private void writeValueConstraints(ValueConstraint[] vca) throws IOException {
        if (vca != null && vca.length > 0) {
            String vc = vca[0].getDefinition(resolver);
            out.write("\n" + INDENT + INDENT + "< '");
            out.write(escape(vc));
            out.write("'");
            for (int i = 1; i < vca.length; i++) {
                vc = vca[i].getDefinition(resolver);
                out.write(", '");
                out.write(escape(vc));
                out.write("'");
            }
        }
    }

    /**
     * write node def
     * @param nd
     */
    public void writeNodeDef(NodeTypeDef ntd, NodeDef nd) throws IOException {
        out.write("\n" + INDENT + "+ ");
        writeItemDefName(nd.getName());
        writeRequiredTypes(nd.getRequiredPrimaryTypes());
        writeDefaultType(nd.getDefaultPrimaryType());
        out.write(ntd.getPrimaryItemName() != null && ntd.getPrimaryItemName().equals(nd.getName()) ? " primary" : "");
        if (nd.isMandatory()) {
            out.write(" mandatory");
        }
        if (nd.isAutoCreated()) {
            out.write(" autocreated");
        }
        if (nd.isProtected()) {
            out.write(" protected");
        }
        if (nd.allowsSameNameSiblings()) {
            out.write(" multiple");
        }
        if (nd.getOnParentVersion() != OnParentVersionAction.COPY) {
            out.write(" ");
            out.write(OnParentVersionAction.nameFromValue(nd.getOnParentVersion()).toLowerCase());
        }
    }

    /**
     * Write item def name
     * @param name
     * @throws IOException
     */
    private void writeItemDefName(QName name) throws IOException {
        out.write(resolve(name));
    }
    /**
     * write required types
     * @param reqTypes
     */
    private void writeRequiredTypes(QName[] reqTypes) throws IOException {
        if (reqTypes != null && reqTypes.length > 0) {
            String delim = " (";
            for (int i = 0; i < reqTypes.length; i++) {
                out.write(delim);
                out.write(resolve(reqTypes[i]));
                delim = ", ";
            }
            out.write(")");
        }
    }

    /**
     * write default types
     * @param defType
     */
    private void writeDefaultType(QName defType) throws IOException {
        if (defType != null && !defType.getLocalName().equals("*")) {
            out.write(" = ");
            out.write(resolve(defType));
        }
    }

    /**
     * resolve
     * @param qname
     * @return the resolved name
     */
    private String resolve(QName qname) throws IOException {
        if (qname == null) {
            return "";
        }
        try {
            String prefix = resolver.getPrefix(qname.getNamespaceURI());
            if (prefix != null && !prefix.equals(QName.NS_EMPTY_PREFIX)) {
                // check for writing namespaces
                if (nsWriter != null) {
                    if (!usedNamespaces.contains(prefix)) {
                        usedNamespaces.add(prefix);
                        nsWriter.write("<'");
                        nsWriter.write(prefix);
                        nsWriter.write("'='");
                        nsWriter.write(escape(qname.getNamespaceURI()));
                        nsWriter.write("'>\n");
                    }
                }
                prefix += ":";
            }

            String resolvedName = prefix + qname.getLocalName();

            // check for '-' and '+'
            if (resolvedName.indexOf('-') >= 0 || resolvedName.indexOf('+') >= 0) {
                return "'" + resolvedName + "'";
            } else {
                return resolvedName;
            }

        } catch (NamespaceException e) {
            return qname.toString();
        }
    }

    /**
     * escape
     * @param s
     * @return the escaped string
     */
    private String escape(String s) {
        StringBuffer sb = new StringBuffer(s);
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '\\') {
                sb.insert(i, '\\');
                i++;
            } else if (sb.charAt(i) == '\'') {
                sb.insert(i, '\'');
                i++;
            }
        }
        return sb.toString();
    }
}
