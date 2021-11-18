/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.transport;

import java.text.MessageFormat;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Returns configured instance of Transport Adapter corresponding
 * to provided type. 
 * @author Maksims
 *
 */
public class TransportAdapters {
    
    /**
     * Enumeration of supported protocols.
     */
    public static enum Protocol {HTTP};


    private final Map<Protocol, Class<? extends TransportAdapterClient>> adapters = new IdentityHashMap<Protocol, Class<? extends TransportAdapterClient>>();
    
    private TransportAdapters(){
        adapters.put(Protocol.HTTP, com.exigen.cm.store.remote.transport.http.client.HttpTransportAdapterClient.class);        
    }

    private static final TransportAdapters instance = new TransportAdapters();

    
    
    private TransportAdapterClient newClientInstance(Protocol protocol){
        Class<? extends TransportAdapterClient> taClass = adapters.get(protocol);
        if(taClass == null){
            String message = MessageFormat.format("Transport Adapter of type {0} is not found", protocol);
            throw new RuntimeException(message);
        }
        
        try{
            TransportAdapterClient adapter = taClass.newInstance();
            return adapter;
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to create Transport Adapter of type {0}", protocol);
            throw new RuntimeException(message, ex);
        }
    }

    /**
     * Creates new instance of Transport adapter and
     * configures it with provided configuration data.
     * Configuration is specific to transport adapter type.
     * @param config
     * @return
     */
    public static TransportAdapterClient newClient(String protocolName, Map<String, String> config){
        return newClient(Protocol.valueOf(protocolName.toUpperCase().trim()), config);
    }    
    
    /**
     * Creates new instance of Transport adapter and
     * configures it with provided configuration data.
     * Configuration is specific to transport adapter type.
     * @param config
     * @return
     */
    public static TransportAdapterClient newClient(Protocol protocol, Map<String, String> config){
        TransportAdapterClient adapter = instance.newClientInstance(protocol);
        adapter.init(config);

        return adapter;
    }
}

/*
 * $Log: TransportAdapters.java,v $
 * Revision 1.2  2010/01/18 17:12:36  RRosickis
 * EPB-85. Class com.exigen.cm.store.remote.transport.TransportAdapters from jcr core modified to trim whitespaces from protocol name.
 *
 * Revision 1.1  2007/04/26 08:59:53  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/07/12 11:51:07  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/04 14:04:36  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */