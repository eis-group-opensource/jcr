/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.transport.http.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.store.remote.common.Batch;
import com.exigen.cm.store.remote.common.BatchOperation;
import com.exigen.cm.store.remote.common.BatchUnit;
import com.exigen.cm.store.remote.common.ContentDataSource;
import com.exigen.cm.store.remote.server.ContentRequestDispatcher;
import com.exigen.cm.store.remote.transport.http.TransportConstants;

/**
 * Http Transport Adapter Servlet used to process requests from Transport
 * Adapter Client to Content Stores.
 */
public class HttpTransportAdapterServlet extends HttpServlet implements
        TransportConstants {

    private static final long serialVersionUID = -4541898402675340092L;

    private ServletFileUpload upload = null;

    private ContentRequestDispatcher dispatcher;

    private static Log log = LogFactory
            .getLog(HttpTransportAdapterServlet.class);

    /**
     * Sevlet parameter pointing to uploaded files cache root.
     */
    public static final String PARAM_CACHE_ROOT = "cacheRoot";

    /**
     * Servlet parameter holding files size threshold.
     * 
     * @see DiskFileItemFactory
     */
    public static final String PARAM_SIZE_THRESHOLD = "sizeThreshold";

    @Override
    public void init(ServletConfig params) throws ServletException {
        super.init(params);

        /*
         * Here parameters passed with configuration should be used to
         * initialize: - ServerFileUpload instance - ContentStoreProvider
         */
        DiskFileItemFactory fiFactory = new DiskFileItemFactory();
//         MultipartRequestItemFactory fiFactory = new MultipartRequestItemFactory();

        String cacheRoot = params.getInitParameter(PARAM_CACHE_ROOT);
        if (cacheRoot != null)
            fiFactory.setRepository(new File(cacheRoot));

        String sizeThreshold = params.getInitParameter(PARAM_SIZE_THRESHOLD);
        if (sizeThreshold != null)
            fiFactory.setSizeThreshold(new Integer(sizeThreshold));

        upload = new ServletFileUpload(fiFactory);// DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD,
                                                    // new File("c:/temp")));

        /*
         * Servlet parameters must correspond to property names as used in JCR
         * properties file for specific repository i.e. connection properties
         * should be prefixed by: connection.
         * 
         * but Content Store related properties should be prefixed by: store.
         */
        // dispatcher = new ContentRequestDispatcher(createProperties(params));
        dispatcher = new ContentRequestDispatcher();
    }

    /***************************************************************************
     * Converts servlet configuration to map of strings suitable for passing to
     * ContentRequestDispatcher. @param params @return / private Map<String,
     * String> createProperties(ServletConfig params){
     * 
     * 
     * Map<String, String> config = new HashMap<String, String>();
     * 
     * Enumeration paramNames = params.getInitParameterNames();
     * while(paramNames.hasMoreElements()){ String name =
     * (String)paramNames.nextElement(); String val =
     * (String)params.getInitParameter(name); config.put(name, val); }
     * 
     * return config; }//
     */

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String command = request.getHeader(HEADER_CMD_NAME);
        String storeName = request.getHeader(HEADER_STORE);

        if (command == null || storeName == null) {
            String message = MessageFormat
                    .format(
                            "Cannot proceed with Command {0} and Content Store name {1}",
                            command, storeName);
            log.error(message);
            
            response.setStatus(HttpServletResponse.SC_OK);
            return;
//            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
//                    message);
        }

        if (log.isDebugEnabled()) {
            String message = MessageFormat.format(
                    "Processing command {0} on Content Store {1}", command,
                    storeName);
            log.debug(message);
        }

        Operation operation = Operation.valueOf(command);

        switch (operation) {
        case GET:
            get(storeName, request, response);
            break;

        case UPDATE:
            update(storeName, request, response);
            break;

        case PING:
            ping(storeName, request, response);
            break;

        default:
            // Command unknown
            String message = MessageFormat
                    .format(
                            "Command {0} is not supported and cannot be executed on Content Store {1}",
                            command, storeName);
            log.error(message);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message);
        }

        if (log.isDebugEnabled()) {
            String message = MessageFormat
                    .format(
                            "Processing of command {0} on Content Store {1} is Completed",
                            command, storeName);
            log.debug(message);
        }

    }

    /**
     * PING operation.
     * 
     * @param storeName
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void ping(String storeName, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        response
                .setStatus(dispatcher.isAlive(storeName) ? HttpServletResponse.SC_OK
                        : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    /**
     * GET operation. Returns response with HTTP status OK in case content in
     * Content Store accessed successfully. Returns response with HTTP statis
     * ITERNAL_SERVER_ERROR in case of failure.
     * 
     * @param storeName
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void get(String storeName, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String contentId = request.getParameter(CONTENT_ID);
        try {
            response.setContentType(TRANSFER_MIME_TYPE);
            dispatcher.getContent(storeName, contentId, response
                    .getOutputStream());
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception ex) {
            String message = MessageFormat
                    .format(
                            "Failed to get content with ID: {0} from Content Store: {1} due to error: {2}",
                            contentId, storeName, makeExceptionMessage(ex));

            log.error(message, ex);

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message);
        }
    }

    protected void update(String storeName, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        try {
            Batch batch = createBatch(storeName, request);
            dispatcher.processBatch(batch);
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception ex) {
            String message = MessageFormat
                    .format(
                            "Failed to execute batch on Content Store: {0} due to error: {1}",
                            storeName, makeExceptionMessage(ex));

            log.error(message, ex);

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message);
        }
    }

    /**
     * Creates Batch instance from provided request or throws Exception in case
     * request doesn't contain sufficient information to create Batch.
     * 
     * @param request
     * @return
     */
    @SuppressWarnings("unchecked")
    private Batch createBatch(String storeName, HttpServletRequest request)
            throws Exception {

        List<FileItem> items = (List<FileItem>) upload.parseRequest(request);
        // refs to Units by Content Id
        Map<String, BatchUnit> contentIdToUnit = new HashMap<String, BatchUnit>();

        // ordered list of units
        List<BatchUnit> units = new LinkedList<BatchUnit>();

        for (FileItem item : items) {
            String fieldName = item.getFieldName();

            int contentIdEndIdx = fieldName.indexOf('.');
            String contentIdVal = fieldName.substring(0, contentIdEndIdx);
            contentIdEndIdx++;

            BatchUnit unit = contentIdToUnit.get(contentIdVal);
            if (unit == null) { // new content ID found
                Long contentId = new Long(contentIdVal);
                unit = new BatchUnit(contentId);
                contentIdToUnit.put(contentIdVal, unit);
                units.add(unit);
            }

            int sbuFieldEndIdx = fieldName.indexOf('.', contentIdEndIdx);

            String sbuFieldStr = sbuFieldEndIdx < 0 ? fieldName
                    .substring(contentIdEndIdx) : fieldName.substring(
                    contentIdEndIdx, sbuFieldEndIdx);

            SBU_FIELD field = SBU_FIELD.valueOf(sbuFieldStr);
            switch (field) {
            case OPERATION:
                unit.setOperation(BatchOperation.valueOf(item.getString()));
                break;
            case PARAM:
                Map<String, String> params = unit.getParams();
                if (params == null) {
                    params = new HashMap<String, String>();
                    unit.setParams(params);
                }

                String param = fieldName.substring(sbuFieldEndIdx + 1);
                params.put(param, item.getString());
                break;
            case LENGTH:
                unit.setLength(Integer.parseInt(item.getString()));
                break;

            case DATA:
                ContentDataSource dataSrc = new FileItemContentSource(new Long(
                        contentIdVal), item);
                unit.setDataSource(dataSrc);
                break;
            }
        }

        return new Batch(storeName, units);
    }

    /**
     * Creates exception message suitable for sending to client.
     * 
     * @param ex
     * @return
     */
    private static String makeExceptionMessage(Throwable ex) {
        StringWriter sw = new StringWriter(50);
        sw.write(ex.getMessage());
        sw.write('\n');
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * FileItem based Content Data Source
     * 
     * @author Maksims
     * 
     */
    static class FileItemContentSource implements ContentDataSource {

        private final Long contentId;

        private final FileItem item;
//        private final File location;

        FileItemContentSource(Long contentId, FileItem item) throws Exception {
            this.contentId = contentId;
            this.item = item;
            if (item.isFormField()) {
                String message = MessageFormat
                        .format(
                                "Failed to create Content Data Source from Simple FileItem for Content ID {0}",
                                contentId);
                throw new Exception(message);
            }
//            location = item.getStoreLocation();
        }

        public Long getContentId() {
            return contentId;
        }

        public InputStream getData() {
            try {
                return item.getInputStream();
            } catch (IOException ex) {
                String message = MessageFormat.format(
                        "Failed to get data from FileItem for Content ID {0}",
                        contentId);
                throw new RuntimeException(message);
            }
        }

        public int getLength() {
//            long fs = location.length();
            long fs1 = item.getSize();
//            if(fs1 == 0)
//                System.out.println("   Length is 0 !!! ");
            
            return (int) fs1;
        }

        /**
         * Release resources captured by given item.
         * 
         */
        public void release() {
            item.delete();
        }
    }
}

/*
 * $Log: HttpTransportAdapterServlet.java,v $
 * Revision 1.2  2007/12/07 15:03:06  maksims
 * empty page returned on request with no params
 *
 * Revision 1.1  2007/04/26 09:02:26  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2006/08/14 16:18:34  maksims
 * #1802414 Content Store configuration fixed
 * Revision 1.4 2006/08/08 13:10:34
 * maksims #1802356 content length param added to store.put method
 * 
 * Revision 1.3 2006/07/28 15:49:04 maksims #1802356 Content ID is changed to
 * Long.
 * 
 * Revision 1.2 2006/07/12 11:51:03 dparhomenko PTR#1802310 Add new features to
 * DatabaseConnection
 * 
 * Revision 1.1 2006/07/04 14:04:34 maksims #1802356 Remote Content Stores
 * access implementation added
 * 
 */