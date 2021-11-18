/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.transport.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartSource;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.store.StoreHelper;
import com.exigen.cm.store.remote.common.Batch;
import com.exigen.cm.store.remote.common.BatchOperation;
import com.exigen.cm.store.remote.common.BatchUnit;
import com.exigen.cm.store.remote.common.ContentDataSource;
import com.exigen.cm.store.remote.transport.TransportAdapterClient;
import com.exigen.cm.store.remote.transport.http.TransportConstants;

public class HttpTransportAdapterClient implements TransportAdapterClient, TransportConstants {

    private static final Log log = LogFactory.getLog(HttpTransportAdapterClient.class);
    
    /**
     * Name of property in configuration which holds URL to
     * server Transport Adapter.
     */
    public static String PROP_ADAPTER_URL="adapterURL";

//  Holds adapter URL
    public String adapterURL = null;
    
    public void init(Map<String, String> configuration) {
        adapterURL = (String)configuration.get(PROP_ADAPTER_URL);
    }

    /**
     * @inheritDoc
     */
    public void submit(Batch batch) {
        submit(batch, null);
    }

    /**
     * Submits batch to server side optionally filtering
     * Batch Units by opFilter.
     */
    public void submit(Batch batch, BatchOperation opFilter) {

        if(log.isDebugEnabled()){
            String message = MessageFormat.format("HTTP Adapter requested to call SUBMIT on Content Store {0} by address {1} with operation filter {2}",            
                    batch.getStoreName(),
                    adapterURL,
                    opFilter == null ? "DO ALL" : opFilter);
            log.debug(message);
        }
        
        HttpClient client = new HttpClient();
        PostMethod filePost = null;
        
        try {
            filePost = new PostMethod(adapterURL);
            filePost.addRequestHeader(HEADER_CMD_NAME, Operation.UPDATE.name());
            filePost.addRequestHeader(HEADER_STORE, batch.getStoreName());            

            filePost.setRequestEntity(prepareRequest(batch, opFilter, filePost));

            client.executeMethod(filePost);
            
            
            ensureSuccess(filePost, Operation.UPDATE);
            flagProcessed(batch, opFilter);
        } catch (Throwable e) {
            String message = MessageFormat.format("HTTP Adapter FAILED to call SUBMIT on Content Store {0} by address {1} with operation filter {2}",            
                    adapterURL,
                    opFilter == null ? "DO ALL" : opFilter,
                    batch.getStoreName());
            log.error(message, e);
            throw new RuntimeException(message, e);
        } finally {
//          Release the connection.
            if (filePost != null)
                filePost.releaseConnection();
        }        
    }
    
    
    /**
     * Flag units matching to operation filter as processed.
     * @param batch - batch which units are processed
     * @param opFilter - operation filter.
     */
    protected void flagProcessed(Batch batch, BatchOperation opFilter){
//      Flag units as processed
        Iterator<BatchUnit> units = batch.getUnitsIterator();
        while(units.hasNext()){
            BatchUnit unit = units.next();
            
            if(opFilter != null && unit.getOperation() != opFilter)
                continue; // ignore if filter exists and unit operation doesn't match
            unit.setProcessed(true);
        }              
    }
    
    /**
     * Prepares request entity from batch.
     * @param batch
     * @param opFilter
     * @param method
     * @return
     * @throws Exception
     */
    protected RequestEntity prepareRequest(Batch batch, BatchOperation opFilter, HttpMethod method) throws Exception{
        Iterator<BatchUnit> units = batch.getUnitsIterator();
        List<Part> submitParts = new ArrayList<Part>();
        
        while(units.hasNext()){
            BatchUnit unit = units.next();
            List<Part> unitParts = new ArrayList<Part>();
            
            if(opFilter != null && unit.getOperation() != opFilter || unit.isProcessed())
                continue; // ignore if filter exists and unit operation doesn't match or unit is already processed
            
            
            String message = null;
            if(log.isDebugEnabled())
                message = MessageFormat.format("Adding to submit batch unit: ContentID: {0}, Operation: {1}",
                        unit.getJCRContentId(), 
                        unit.getOperation().toString());
            
            
            String prefix = unit.getJCRContentId() + ".";

//          Ordinal of operation is passed.
            unitParts.add(new StringPart(prefix+SBU_FIELD.OPERATION, String.valueOf(unit.getOperation().name())));

            ContentDataSource data = unit.getDataSource();
            
//          Content length is passed.
            int length = unit.getLength();
            if(length < 0)
                length = data.getLength(); // get length from cache if not provided externally 
                
            if(length > 0)
                unitParts.add(new StringPart(prefix+SBU_FIELD.LENGTH, String.valueOf(length)));
            
            
            Map<String, String> params = unit.getParams();
            if(params != null && params.size() > 0){
                StringBuffer debugParams = null;
                
                
                String paramPrefix = prefix+SBU_FIELD.PARAM+".";
                for(String key:params.keySet()){
                    unitParts.add(new StringPart(paramPrefix+key, params.get(key)));

                    if(log.isDebugEnabled()){
                        if(debugParams == null)
                            debugParams = new StringBuffer(", Parameters are: ");
                        else
                            debugParams.append(',');
                        
                        debugParams.append(key).append('=').append(params.get(key)).append(' ');
                    }
                }
                

                if(log.isDebugEnabled()){
                    debugParams.append('\n');
                    message = message.concat(debugParams.toString());
                }
            }
            
            
            if(data != null){
//                submitParts.add(new StringPart(prefix+SBU_FIELD.LENGTH, String.valueOf(data.getLength())));
//                long length = data.getLength();
                if(unit.getOperation() == BatchOperation.INSERT && length == 0){
                    String warn = MessageFormat.format("Requested to insert ZERO sized content with ID: {0}. Request is ignored!",
                            unit.getJCRContentId());
                    log.warn(warn);
                    continue;
                }else
                if(log.isDebugEnabled())
                    message = message.concat("\nData Length="+data.getLength());

                
                unitParts.add(new FilePart(prefix+SBU_FIELD.DATA, new ContentPartSource(data)));
            }
            
            submitParts.addAll(unitParts);
            if(log.isDebugEnabled()) log.debug(message);
        }
        
        Part[] parts = submitParts.toArray(new Part[] {});
        
        return new MultipartRequestEntity(parts, method.getParams());
}
    

    public boolean isAlive(String storeName) {
        if(log.isDebugEnabled()){
            String message = MessageFormat.format("HTTP Adapter requested to call PING on Content Store {0} by address {1}",
                    storeName,
                    adapterURL);
            log.debug(message);
        }
        
        
        HttpClient client = new HttpClient();
        GetMethod ping = new GetMethod(adapterURL);
        
        ping.addRequestHeader(HEADER_CMD_NAME, Operation.PING.name());
        ping.addRequestHeader(HEADER_STORE, storeName);
        
        try{
            client.executeMethod(ping);
            if(ping.getStatusCode() == HttpURLConnection.HTTP_OK)
                return true;
            
            if(ping.getStatusCode() == HttpURLConnection.HTTP_UNAVAILABLE)
                return false;
            
        }catch(Throwable ex){
            String message = MessageFormat.format("HTTP Adapter FAILED to call PING on Content Store {0} by address {1}",
                    storeName,
                    adapterURL);
            log.error(message, ex);
        }finally{
            if(ping != null)
                ping.releaseConnection();
        }
        
        return false;
    }

    public void get(String storeName, Long contentId, OutputStream target){
        if(log.isDebugEnabled()){
            String message = MessageFormat.format("HTTP Adapter requested to call GET on Content Store {0} by address {1} for content {2}",
                    storeName,
                    adapterURL,
                    contentId);
            log.debug(message);
        }
        
        
        HttpClient client = new HttpClient();
        GetMethod fileGet = new GetMethod(adapterURL);
        
        fileGet.addRequestHeader(HEADER_CMD_NAME, Operation.GET.name());
        fileGet.addRequestHeader(HEADER_STORE, storeName);

        NameValuePair[] content = new NameValuePair[]{new NameValuePair(CONTENT_ID, contentId.toString())};
        fileGet.setQueryString(content);
        
        try{
            client.executeMethod(fileGet);
            
            
            ensureSuccess(fileGet, Operation.GET);
            
            StoreHelper.transfer(fileGet.getResponseBodyAsStream(), target, -1);
            
        }catch(Throwable ex){
            String message = MessageFormat.format("HTTP Adapter GET FAILED to call Content Store {0} by address {1} for content {2}",
                    storeName,
                    adapterURL,
                    contentId);
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }finally{
            if(fileGet != null)
                fileGet.releaseConnection();
        }
    }

    
   /**
    * Ensures that executed method has status OK and throws exception
    * with status code if it is not.
    * @param executed is a method executed
    * @param operation is an operation invoked
    * @throws Exception
    */
    protected void ensureSuccess(HttpMethod executed, Operation operation) throws Exception{
        if(executed.getStatusCode() == HttpURLConnection.HTTP_OK)
            return;
        
        String message = MessageFormat.format("Operation {0} failed with error: {1}",
                operation, executed.getStatusText());
        throw new Exception(message);
    }
    
    
    static class ContentPartSource implements PartSource{
        private final ContentDataSource source;
        
        ContentPartSource(ContentDataSource src){
            source = src;
        }

        public long getLength() {
            return source.getLength();
        }

        public String getFileName() {
            return source.getContentId().toString();
        }

        public InputStream createInputStream() throws IOException {
            return source.getData();
        }
        
    }
}

/*
 * $Log: HttpTransportAdapterClient.java,v $
 * Revision 1.2  2007/12/07 15:03:28  maksims
 * content length taken from cache in not provided as param
 *
 * Revision 1.1  2007/04/26 08:58:47  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.7  2006/10/30 15:03:57  dparhomenko
 * PTR#1803272 a lot of fixes
 *
 * Revision 1.6  2006/08/14 16:18:44  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.5  2006/08/08 13:10:41  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.4  2006/08/02 11:42:28  maksims
 * #1802426 SQL Framework used to generate queries
 *
 * Revision 1.3  2006/07/28 15:49:10  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.2  2006/07/12 11:51:28  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/04 14:04:47  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */