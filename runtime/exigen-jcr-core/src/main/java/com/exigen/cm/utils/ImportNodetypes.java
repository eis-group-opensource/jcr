/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.jcr.RepositoryException;

import com.exigen.cm.JCRHelper;
import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.impl.NodeTypeManagerImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.InvalidNodeTypeDefException;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDefDiff;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDefDiff.ChildNodeDefDiff;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDefDiff.PropDefDiff;
import com.exigen.cm.jackrabbit.nodetype.compact.CompactNodeTypeDefReader;
import com.exigen.cm.jackrabbit.nodetype.compact.CompactNodeTypeDefWriter;
import com.exigen.cm.jackrabbit.nodetype.compact.ParseException;
import com.exigen.cm.jackrabbit.nodetype.xml.NodeTypeReader;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;

public class ImportNodetypes extends AbstractUtil{

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        (new ImportNodetypes()).process(args);
    }
	
	@Override
	protected SimpleJSAP createJSAPConfiguration() throws JSAPException {
        SimpleJSAP jsap = new SimpleJSAP(
                "ImportNodetypes",
                "Import nodetypes",
                new Parameter[] {
                        new Switch("print", 'p', "print" ,"Only print differences")
                    });
        
        jsap.registerParameter(new FlaggedOption("nodetypes", JSAP.STRING_PARSER, null, JSAP.REQUIRED, 'o', "nodetypes", "Nodetype definitions"));        
        UtilsHelper.addRepositoryPropertiesParameter(jsap);
        return jsap;
	}

	@Override
	protected boolean execute() throws RepositoryException {
        boolean print = parameters.getBoolean("print");
        String nodetypes = parameters.getString("nodetypes");
        
        List<NodeTypeDef> defs;
        
		InputStream in = JCRHelper.getInputStream(nodetypes, true);

		RepositoryImpl repository = getRepository();
		Reader r = new InputStreamReader(in);
		Properties namespaces = null;
		if (nodetypes.endsWith(".cnd")){
			//1.try cnd format
			try {
				CompactNodeTypeDefReader reader = new CompactNodeTypeDefReader(r, nodetypes);
				defs = reader.getNodeTypeDefs();
			} catch (ParseException e) {
				throw new RepositoryException("Error reading nodetype definitions. ", e);
			}
		} else if (nodetypes.endsWith(".xml")){
			//2. try xml format
			try {
				NodeTypeReader reader = new NodeTypeReader(in, repository);
				
				defs = Arrays.asList(reader.getNodeTypeDefs());
				namespaces = reader.getAddedNamespaces();
			} catch (IOException e) {
				throw new RepositoryException("Error reading nodetype definitions. ", e);
			} catch (InvalidNodeTypeDefException e) {
				throw new RepositoryException("Error reading nodetype definitions. ", e);
			} catch (IllegalNameException e) {
				throw new RepositoryException("Error reading nodetype definitions. ", e);
			} catch (UnknownPrefixException e) {
				throw new RepositoryException("Error reading nodetype definitions. ", e);
			}
		} else {
			throw new RepositoryException("Unknown file format");
		}
		
		SessionImpl session = repository.getSystemSession();
		

		
		if (print){
			try {
				printDifferences(defs, session);
			} catch (IOException exc){
				throw new RepositoryException(exc);
			}
		} else {
			if (namespaces != null){
				NamespaceRegistryImpl nr = repository.getNamespaceRegistry();
				for(Object _prefix:namespaces.keySet()){
					String prefix = (String) _prefix;
					String uri = namespaces.getProperty(prefix);
					if (!nr.hasPrefix(prefix)){
						nr.registerNamespace(prefix, uri);
					}
				}
					
			}
			importNodeTypes(defs, session);
		}
		
		
        
        return false;
	}

	private void importNodeTypes(List<NodeTypeDef> defs, SessionImpl session) throws RepositoryException{
		NodeTypeManagerImpl ntManager = session.getNodeTypeManager();
		ntManager.registerNodeDefs(session.getConnection(), defs, true);
		session.getConnection().commit();
		System.out.println("Done");
	}

	public static void printDifferences(List<NodeTypeDef> defs, SessionImpl session) throws RepositoryException, IOException{
		NodeTypeManagerImpl ntManager = session.getNodeTypeManager();
		ArrayList<NodeTypeDef> newDefs = new ArrayList<NodeTypeDef>();
    	ArrayList<NodeTypeDefDiff> modifiedDefs = new ArrayList<NodeTypeDefDiff>();
    	for(NodeTypeDef d: defs){
    		NodeTypeDef old = ntManager.getNodeTypeDef(d.getName());
    		if (old == null){
    			newDefs.add(d);
    		} else {
    			NodeTypeDefDiff diff = NodeTypeDefDiff.create(old, d);
    			if (diff.isModified()){
    				modifiedDefs.add(diff);
    			}
    		}
    	}
    	System.out.println("************************************************");
    	
    	if (newDefs.size() > 0){
    		System.out.println("New Definitions:");
    	}
    	Writer w1 = new OutputStreamWriter(System.out);
    	CompactNodeTypeDefWriter writer = new CompactNodeTypeDefWriter(w1, session.getNamespaceResolver(), false);
    	for(NodeTypeDef nd:newDefs){
    		writer.write(nd);
    	}
    	w1.flush();
    	if (modifiedDefs.size() > 0){
    		System.out.println("Modified Definitions:");
    	}
    	for(NodeTypeDefDiff diff:modifiedDefs){
    		
    		writer.writeName(diff.getOldDef());
    		writer.writeSupertypes(diff.getOldDef());
    		writer.writeOptions(diff.getOldDef());
    		writer.writeTable(diff.getOldDef());
    		
    		boolean msg = false;
    		for(PropDefDiff diff1:(List<PropDefDiff>)diff.getPropDefDiffs()){
    			if (diff1.getOldDef() == null){
    				if (!msg){
    					w1.write("\n" + writer.INDENT + "New Properties ");
    					msg = true;
    				}
    				writer.writePropDef(diff.getNewDef(), diff1.getNewDef());
    			}
    		}
    		msg = false;
    		for(PropDefDiff diff1:(List<PropDefDiff>)diff.getPropDefDiffs()){
    			if (diff1.getNewDef() == null){
    				if (!msg){
    					w1.write("\n" + writer.INDENT + "Removed Properties ");
    					msg = true;
    				}
    				writer.writePropDef(diff.getOldDef(), diff1.getOldDef());
    			}
    		}
    		msg = false;
    		for(PropDefDiff diff1:(List<PropDefDiff>)diff.getPropDefDiffs()){
    			if (diff1.isModified() && diff1.getNewDef() != null && diff1.getOldDef() != null){
    				if (!msg){
    					w1.write("\n" + writer.INDENT + "Changed Properties");
    					msg = true;
    				}
    				writer.writePropDef(diff.getOldDef(), diff1.getOldDef());
    				writer.writePropDef(diff.getNewDef(), diff1.getNewDef());
    			}
    		}
    		
    		//child nodes
    		msg = false;
    		for(ChildNodeDefDiff diff1:(List<ChildNodeDefDiff>)diff.getChildNodeDefDiffs()){
    			if (diff1.getOldDef() == null){
    				if (!msg){
    					w1.write("\n" + writer.INDENT + "New Child Nodes ");
    					msg = true;
    				}
    				writer.writeNodeDef(diff.getNewDef(), diff1.getNewDef());
    			}
    		}
    		msg = false;
    		for(ChildNodeDefDiff diff1:(List<ChildNodeDefDiff>)diff.getChildNodeDefDiffs()){
    			if (diff1.getNewDef() == null){
    				if (!msg){
    					w1.write("\n" + writer.INDENT + "Removed Child Nodes ");
    					msg = true;
    				}
    				writer.writeNodeDef(diff.getOldDef(), diff1.getOldDef());
    			}
    		}
    		msg = false;
    		for(ChildNodeDefDiff diff1:(List<ChildNodeDefDiff>)diff.getChildNodeDefDiffs()){
    			if (diff1.isModified() && diff1.getNewDef() != null && diff1.getOldDef() != null){
    				if (!msg){
    					w1.write("\n" + writer.INDENT + "Changed Child Nodes");
    					msg = true;
    				}
    				writer.writeNodeDef(diff.getOldDef(), diff1.getOldDef());
    				writer.writeNodeDef(diff.getNewDef(), diff1.getNewDef());
    			}
    		}
            w1.write("\n\n");
    	}
    	w1.flush();
    	writer.close();
    	System.out.println("************************************************");
		
	}

}
