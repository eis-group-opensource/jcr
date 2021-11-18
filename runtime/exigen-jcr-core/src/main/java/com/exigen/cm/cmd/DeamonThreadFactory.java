/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd;

import java.util.concurrent.ThreadFactory;

public class DeamonThreadFactory implements ThreadFactory {

	private ThreadFactory factory;

	public DeamonThreadFactory(ThreadFactory factory) {
		this.factory = factory;
	}

	public Thread newThread(Runnable r) {
		Thread th = factory.newThread(r);
		th.setDaemon(true);
		return th;
	}

}
