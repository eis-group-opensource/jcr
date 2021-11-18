/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation.events;

public class OcrEventData implements EventData{

	private Long id;
	private Long nodeId;

	public OcrEventData(Long id, Long nodeId) {
		this.id = id;
		this.nodeId = nodeId;
	}

	public Long getId() {
		return id;
	}

	public Long getNodeId() {
		return nodeId;
	}

}
