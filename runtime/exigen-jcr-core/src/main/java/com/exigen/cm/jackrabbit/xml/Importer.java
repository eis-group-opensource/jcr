/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.xml;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.QName;

/**
 * The <code>Importer</code> interface ...
 */
public interface Importer {

    /**
     * @throws RepositoryException
     */
    void start() throws RepositoryException;

    /**
     * @param nodeInfo
     * @param propInfos list of <code>PropInfo</code> instances
     * @param nsContext prefix mappings of current context
     * @throws RepositoryException
     */
    void startNode(NodeInfo nodeInfo, List propInfos, NamespaceResolver nsContext)
            throws RepositoryException;

    /**
     * @param nodeInfo
     * @throws RepositoryException
     */
    void endNode(NodeInfo nodeInfo) throws RepositoryException;

    /**
     * @throws RepositoryException
     */
    void end() throws RepositoryException;

    //--------------------------------------------------------< inner classes >
    static class NodeInfo {
        private QName name;
        private QName nodeTypeName;
        private QName[] mixinNames;
        private String uuid;

        public NodeInfo() {
        }

        public NodeInfo(QName name, QName nodeTypeName, QName[] mixinNames,
                        String uuid) {
            this.name = name;
            this.nodeTypeName = nodeTypeName;
            this.mixinNames = mixinNames;
            this.uuid = uuid;
        }

        public void setName(QName name) {
            this.name = name;
        }

        public QName getName() {
            return name;
        }

        public void setNodeTypeName(QName nodeTypeName) {
            this.nodeTypeName = nodeTypeName;
        }

        public QName getNodeTypeName() {
            return nodeTypeName;
        }

        public void setMixinNames(QName[] mixinNames) {
            this.mixinNames = mixinNames;
        }

        public QName[] getMixinNames() {
            return mixinNames;
        }

        public void setUUID(String uuid) {
            this.uuid = uuid;
        }

        public String getUUID() {
            return uuid;
        }
    }

    static class PropInfo {
        private QName name;
        private int type;
        private TextValue[] values;
		private TextValue[] values2;

        public PropInfo() {
        }

        public PropInfo(QName name, int type, TextValue[] values,TextValue[] values2) {
            this.name = name;
            this.type = type;
            this.values = values;
            this.values2 = values2;
        }

        public void setName(QName name) {
            this.name = name;
        }

        public QName getName() {
            return name;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public void setValues(TextValue[] values) {
            this.values = values;
        }

        public TextValue[] getValues() {
            return values;
        }

		public void _setValues(TextValue[] values2) {
			this.values2 = values2;
			
		}

        public TextValue[] _getValues() {
            return values2;
        }
    }

    /**
     * <code>TextValue</code> represents a serialized property value read
     * from a System or Document View XML document.
     */
    interface TextValue {
        /**
         * Returns the length of the serialized value.
         *
         * @return the length of the serialized value
         * @throws IOException if an I/O error occurs
         */
        long length() throws IOException;

        /**
         * Retrieves the serialized value.
         *
         * @return the serialized value
         * @throws IOException if an I/O error occurs
         */
        String retrieve() throws IOException;

        /**
         * Returns a <code>Reader</code> for reading the serialized value.
         *
         * @return a <code>Reader</code> for reading the serialized value.
         * @throws IOException if an I/O error occurs
         */
        Reader reader() throws IOException;
    }
}
