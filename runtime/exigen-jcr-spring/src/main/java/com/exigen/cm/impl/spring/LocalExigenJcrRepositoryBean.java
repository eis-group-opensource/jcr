/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.spring;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.exigen.cm.Constants;
import com.exigen.cm.RepositoryProvider;
import com.exigen.cm.database.transaction.JCRTransactionManager;
import com.exigen.cm.database.transaction.MockJCRTransactionManager;
import com.exigen.cm.database.transaction.jta.JTAJCRTransactionManager;
import com.exigen.cm.database.transaction.spring.SpringJCRTransactionManager;
import com.exigen.cm.impl.RepositoryConfigurator;

public class LocalExigenJcrRepositoryBean implements FactoryBean,BeanNameAware, InitializingBean, BeanFactoryAware{//, BeanFactoryAware
	private String name;
	private String beanName;
	private Map<String, String> options = new HashMap<String, String>();
	private Properties properties = new Properties();;
	private DataSource datasource = null;
	private PlatformTransactionManager transactionManager;
	private boolean mockTransactionManager = false;
	private BeanFactory beanFactory;
	
	private RepositoryConfigurator configurator; 
	private String nodetypes;
	private String importData;
	private Boolean dropCreate;
	private Boolean  allowUpgrade;
	private Boolean skipCheck;
	private Boolean developmentMode;
	
	public Map<String, String> getOptions() {
		return options;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOptions(Map<String, String> options) {
		this.options = options;
	}
	
	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public Object getObject() throws BeansException {
		try {
			return RepositoryProvider.getInstance().getRepository(name);
		} catch (RepositoryException e) {
			throw new FatalBeanException("Unable to get repository by name "+name,e);
		}
	}

	public void setBeanName(String value) {
		this.beanName = value;
	}

	
	
	public void afterPropertiesSet() throws Exception {
		for (Iterator iter = properties.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			if (key != null){
				key = key.trim();
			}
			String value = properties.getProperty(key);
			if (value != null){
				value= value.trim();
			}
			options.put(key,value);
		}
		if (name==null)
			name = beanName; 
		
		if (nodetypes != null){
			options.put(Constants.PROPERTY_IMPORT_NODETYPES, nodetypes);
		}
		if (importData != null){
			options.put(Constants.PROPERTY_IMPORT_DATA, importData);
		}
		if (dropCreate != null){
			options.put(Constants.PROPERTY_DATASOURCE_DROP_CREATE, dropCreate.toString());
		}
		if (allowUpgrade != null){
			options.put(Constants.PROPERTY_DATASOURCE_ALLOW_UPGRADE, allowUpgrade.toString());
		}
		if (skipCheck != null){
			options.put(Constants.PROPERTY_DATASOURCE_SKIP_CHECK, skipCheck.toString());
		}
		if (developmentMode != null){
			options.put(Constants.PROPERTY_DEVELOPMENT_MODE, developmentMode.toString());

		}
		
		
		RepositoryProvider.getInstance().configure(name, options);
		if (datasource != null){
			RepositoryProvider.getInstance().configure(name, datasource);
		}
		if (beanFactory != null){
			RepositoryProvider.getInstance().setAttribute(name, Constants.CONFIGURATION_ATTRIBUTE__BEAN_FACTORY ,beanFactory);
		}
		if (configurator != null){
			RepositoryProvider.getInstance().setAttribute(name, Constants.CONFIGURATION_ATTRIBUTE__CONFIGURATOR ,configurator);
		}

		if (transactionManager != null){
			JCRTransactionManager _transactionManager;
			if (transactionManager instanceof JtaTransactionManager){
				_transactionManager = new JTAJCRTransactionManager(((JtaTransactionManager)transactionManager).getTransactionManager()); 
 			} else {
				_transactionManager = new SpringJCRTransactionManager(transactionManager);
 			}
			RepositoryProvider.getInstance().configure(name, _transactionManager);
		} else if (mockTransactionManager){
			MockJCRTransactionManager _transactionManager = new MockJCRTransactionManager();
			RepositoryProvider.getInstance().configure(name, _transactionManager);
		}
			
	}

	public Class getObjectType() {
		return Repository.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void setDatasource(DataSource datasource) {
		this.datasource = datasource;
	}

	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
		
	}

	public void setConfigurator(RepositoryConfigurator configurator) {
		this.configurator = configurator;
	}

	public void setDevelopmentMode(Boolean developmentMode) {
		this.developmentMode = developmentMode;
	}

	public void setDropCreate(Boolean dropCreate) {
		this.dropCreate = dropCreate;
	}

	public void setNodetypes(String nodetypes) {
		this.nodetypes = nodetypes;
	}

	public void setSkipCheck(Boolean skipCheck) {
		this.skipCheck = skipCheck;
	}

	public String getImportData() {
		return importData;
	}

	public void setImportData(String importData) {
		this.importData = importData;
	}

	public boolean isMockTransactionManager() {
		return mockTransactionManager;
	}

	public void setMockTransactionManager(boolean mockTransactionManager) {
		this.mockTransactionManager = mockTransactionManager;
	}

	public Boolean getAllowUpgrade() {
		return allowUpgrade;
	}

	public void setAllowUpgrade(Boolean allowUpgrade) {
		this.allowUpgrade = allowUpgrade;
	}

}
