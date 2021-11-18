/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;


/**
 * Defines a marker interface for {@link javax.jcr.observation.EventListener}
 * implementations that wish a synchronous notification of changes to the
 * workspace. That is, a <code>SynchronousEventListener</code> is called before
 * the call to {@link javax.jcr.Item#save()} returns. In contrast, a regular
 * {@link javax.jcr.observation.EventListener} might be called after
 * <code>save()</code> returns.
 */
public interface BeforeSaveEventListener extends AfterSaveEventListener {
}
