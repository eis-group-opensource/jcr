/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.nodetype.DBNodeTypeReader;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.PropDef;

public class NodeTypeHelper {

	private Log log = LogFactory.getLog(NodeTypeHelper.class);
	private Long ocrTypeId = null;
	private DBNodeTypeReader reader;
	private String ocrTypeTableName;
	private String ocrTypeWorkIdColumnName;
	private String ocrTypeBinaryPorpertyNameColumnName;
	private String ocrTypeContentColumnName;
	private String ocrTypeContentMimeTypeColumnName;
	private String ocrTypeCompletedColumnName;
	private String ocrTypeCompletedDateColumnName;
	private String ocrTypeProcessFTSColumnName;
	private String ocrTypeFailureColumnName;

	public String getOcrTypeTableName() {
		return ocrTypeTableName;
	}

	public NodeTypeHelper(DBNodeTypeReader reader) throws RepositoryException {
		this.reader = reader;
		detectOCRType();
		
	}

	private void detectOCRType() throws RepositoryException {
		for(NodeTypeDef def : reader.all()){
			if (def.getName().equals(Constants.ECR_OCR_MIXIN)){
				this.ocrTypeId = def.getId();
				this.ocrTypeTableName = def.getTableName();
				
				ocrTypeWorkIdColumnName = findColumnName(def, Constants.ECR_OCR_MIXIN__WORK_ID);
				ocrTypeBinaryPorpertyNameColumnName  = findColumnName(def, Constants.ECR_OCR_MIXIN__BINARY_PROPERTY_NAME);
				ocrTypeContentColumnName  = findColumnName(def, Constants.ECR_OCR_MIXIN__CONTENT);
				ocrTypeContentMimeTypeColumnName  = findColumnName(def, Constants.ECR_OCR_MIXIN__CONTENT_MIME_TYPE);
				ocrTypeCompletedColumnName  = findColumnName(def, Constants.ECR_OCR_MIXIN__COMPLETED);
				ocrTypeFailureColumnName  = findColumnName(def, Constants.ECR_OCR_MIXIN__FAILED);
				ocrTypeCompletedDateColumnName  = findColumnName(def, Constants.ECR_OCR_MIXIN__COMPLETED_DATE);
				ocrTypeProcessFTSColumnName  = findColumnName(def, Constants.ECR_OCR_MIXIN__PROCESS_FTS);
			}
		}
		
		
	}

	public String getOcrTypeContentColumnName() {
		return ocrTypeContentColumnName;
	}

	public String getOcrTypeContentMimeTypeColumnName() {
		return ocrTypeContentMimeTypeColumnName;
	}

	private String findColumnName(NodeTypeDef def, QName propertyName) throws RepositoryException {
		for(PropDef pd: def.getPropertyDefs()){
			if (pd.getName().equals(propertyName)){
				return pd.getColumnName();
			}
		}
		throw new RepositoryException("Column for property "+propertyName+" not found in type "+def.getName());
	}

	public Long getOcrTypeId() {
		return ocrTypeId;
	}

	public String getOcrTypeWorkIdColumnName() {
		return ocrTypeWorkIdColumnName;
	}

	public String getOcrTypeBinaryPorpertyNameColumnName() {
		return ocrTypeBinaryPorpertyNameColumnName;
	}

	public String getOcrTypeCompletedColumnName() {
		return ocrTypeCompletedColumnName;
	}

	public String getOcrTypeCompletedDateColumnName() {
		return ocrTypeCompletedDateColumnName;
	}

	public String getOcrTypeProcessFTSColumnName() {
		return ocrTypeProcessFTSColumnName;
	}

	public String getOcrTypeFailureColumnName() {
		return ocrTypeFailureColumnName;
	}

}
