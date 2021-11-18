/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import com.exigen.cm.JCRHelper;
import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.InvalidNodeTypeDefException;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.compact.CompactNodeTypeDefReader;
import com.exigen.cm.jackrabbit.nodetype.compact.CompactNodeTypeDefWriter;
import com.exigen.cm.jackrabbit.nodetype.compact.ParseException;
import com.exigen.cm.jackrabbit.nodetype.xml.NodeTypeReader;
import com.exigen.cm.jackrabbit.nodetype.xml.NodeTypeWriter;
import com.exigen.cm.jackrabbit.util.name.NamespaceMapping;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

public class ConvertNodetypes extends AbstractUtil{

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        (new ConvertNodetypes()).process(args);
    }
    
	@Override
	protected SimpleJSAP createJSAPConfiguration() throws JSAPException {
        SimpleJSAP jsap = new SimpleJSAP(
                "ConvertNodetypes",
                "Convert nodetype definitions from one format to another (xml - > cnd, cnd -> xml)",
                new Parameter[] {
                		new FlaggedOption("inputFile", JSAP.STRING_PARSER, null, 
                				JSAP.REQUIRED, 'i', "in","input file"),
                		new FlaggedOption("outputFile", JSAP.STRING_PARSER, null, 
                				JSAP.REQUIRED, 'o', "out","output file")
                    });
        
        return jsap;
	}    

	@Override
	protected boolean execute() throws RepositoryException {
		try {
			String nodetypes = parameters.getString("inputFile");
			String nodetypes2 = parameters.getString("outputFile");
			InputStream in = JCRHelper.getInputStream(nodetypes, true);

			Reader r = new InputStreamReader(in);
			NamespaceRegistry registry;
			
			
			List defs;
			if (nodetypes.endsWith(".cnd")){
				//1.try cnd format
				try {
					CompactNodeTypeDefReader reader = new CompactNodeTypeDefReader(r, nodetypes);
					defs = reader.getNodeTypeDefs();
					NamespaceMapping nsMapping = reader.getNamespaceMapping();
					registry = new NamespaceRegistryImpl(null, nsMapping);
				} catch (ParseException e) {
					throw new RepositoryException("Error reading nodetype definitions. ", e);
				}
			} else if (nodetypes.endsWith(".xml")){
				//2. try xml format
				try {
					NodeTypeReader reader = new NodeTypeReader(in, null);
					defs = Arrays.asList(reader.getNodeTypeDefs());
					Properties namespaces = reader.getNamespaces();
					registry = new NamespaceRegistryImpl(null, namespaces);
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
				throw new RepositoryException("Unknown file format : "+nodetypes);
			}
			
			FileOutputStream fOut = new FileOutputStream(nodetypes2);
			if (nodetypes2.endsWith(".cnd")){
				//1.try cnd format
				CompactNodeTypeDefWriter.write(defs, (NamespaceResolver) registry, 
						new OutputStreamWriter(fOut));
			} else if (nodetypes2.endsWith(".xml")){
				//2. try xml format
				NodeTypeWriter.write(fOut, 
						(NodeTypeDef[]) defs
								.toArray(new NodeTypeDef[defs.size()]), 
						registry, 
						true);
			} else {
				throw new RepositoryException("Unknown file format2");
			}
			
			fOut.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Done");
		return false;
	}
	
	
}
