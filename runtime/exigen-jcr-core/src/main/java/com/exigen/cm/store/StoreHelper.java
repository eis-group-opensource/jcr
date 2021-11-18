/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
/**
 * 
 */
package com.exigen.cm.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import com.exigen.cm.Constants;

/**
 * Helper class providing utility functionality.
 * @author Maksims
 * 
 */
public final class StoreHelper {

    /**
     * InputStream on temporary file which should be deleted after stream is
     * closed.
     * @author Maksims
     * 
     */
    private static class TmpInputStream extends FileInputStream {
        private final File target;

        TmpInputStream(File f) throws FileNotFoundException {
            super(f);
            target = f;
        }

        @Override
        public void close() throws IOException {
            super.close();
            target.delete();
        }
    }

    /* *
     * Helper class. Implements InputStream with known length.
     * /
    public static class MeasuredInputStream extends InputStream {

        private final InputStream src;

        private final long length;

        MeasuredInputStream(InputStream src) throws Exception {
            File tmpFile = File.createTempFile("mis", "tmp");
            tmpFile.deleteOnExit();
            FileOutputStream fOut = new FileOutputStream(tmpFile);
            transfer(src, fOut);
            fOut.close();
            this.src = createTmpInputStream(tmpFile);
            length = tmpFile.length();
        }

        @Override
        public int read() throws IOException {
            return src.read();
        }

        @Override
        public void close() throws IOException {
            src.close();
        }

        public long getLength() {
            return length;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return src.read(b, off, len);
        }

        @Override
        public int read(byte[] b) throws IOException {
            return src.read(b);
        }
    }
    //*/

    /**
     * Output stream that keeps data in memory if size less then defined.
     * Otherwise it uses temporary file for storing data
     * 
     */
    public static class FileBackedOutputStream extends OutputStream {

        /**
         * The internal buffer where data is stored.
         */
        protected byte buf[];

        /**
         * The number of valid bytes in the buffer. 
         */
        protected int count = 0;

        /**
         * Output stream backed by file
         */
        private OutputStream fos = null;

        private boolean isClosed = false;

        private File file = null;
        
        private long length =0;

        /**
         * Creates stream with predefined buffer length
         */
        public FileBackedOutputStream() {
            this(8192);
        }

        /**
         * Creates stream
         * @param size Buffer length
         */
        public FileBackedOutputStream(int size) {
            this(size, null);
        }

        /**
         * Creates stream
         * @param size Buffer length
         */
        public FileBackedOutputStream(int size, File file) {
            if (size <= 0) {
                throw new IllegalArgumentException("Buffer size <= 0");
            }
            buf = new byte[size];
            
            this.file = file;
        }        
        
        
        @Override
        public synchronized void write(int b) throws IOException {
            if ( isClosed )
                throw new IOException("Stream is closed for this operation");
           if (count < buf.length) {
                buf[count++] = (byte) b;
            } else {
                flushBuffer();
                fos.write(b);
            }
        }

        @Override
        public synchronized void write(byte b[], int off, int len)
                        throws IOException {
            if ( isClosed )
                throw new IOException("Stream is closed for this operation");
            if (len > buf.length - count) {
                flushBuffer();
                fos.write(b, off, len);
            } else {
                System.arraycopy(b, off, buf, count, len);
                count += len;
            }
        }

        @Override
        public void flush() throws IOException {
            if ( isClosed )
                throw new IOException("Stream is closed for this operation");
            if (fos != null)
                fos.flush();
        }

        @Override
        public void close() throws IOException {
            if (!isClosed){
                if (fos != null) {
                    fos.close();
                }
                isClosed = true;
                length = getSize();
            }
        }

        private void flushBuffer() throws IOException {
            if (fos == null) { // done only once
                if(file == null)
                    file = File.createTempFile("fbos", null, null);
                else
                    if(!file.exists())
                        file.createNewFile();
//                file = File.createTempFile("fbos", null, null);
                
                fos = new FileOutputStream(file);
                fos.write(buf, 0, count);
                buf = new byte[0];
                count = buf.length;
            }
        }
        
        /**
         * Data length. Could be called only after output stream closed 
         * @return data length in bytes
         * @throws IOException
         */
        
        public long getLength() throws IOException {
            if (!isClosed) {
                throw new IOException(
                                "Stream should be closed for this operation");
            }
            return length;
            
        }

        protected long getSize() throws IOException {
            if (!isClosed) {
                throw new IOException(
                                "Stream should be closed for this operation");
            }
            if (file == null) return count;
            return file.length();
        }

        /**
         * Input stream based on internal buffer of file. After this stream is closed
         * file is deleted. The method could be called only after output stream is closed
         * @return input stream
         * @throws IOException
         */
        public InputStream toInputStream() throws IOException {
            if (!isClosed) {
                throw new IOException(
                                "Stream should be closed for this operation");
            }
            if (file == null) {
                return new ByteArrayInputStream(buf, 0, count);
            }
            return new TmpInputStream(file);
        }
        
        /**
         * Used for testing only
         * @return: File name or buf content converted as UNICODE
         */
        public String dump(){
            if (file==null){
                try {
                    String content = new String(buf,0,count,Constants.EXTRACTED_TEXT_ENCODING);
                    return content;
                } catch (Exception e){
                    return "";
                }
            }
            return file.getAbsolutePath();
        }

    }
    

    
    /**
     * Output Stream implementation which creates file on first write invocation.
     */
    static class FileOndemandOutputStream extends OutputStream{

        private final File target;
        private FileOutputStream outStream;
        FileOndemandOutputStream(File target){
            this.target = target;
        }
        
        @Override
        public void write(int b) throws IOException {
            if(outStream == null){
                if(!target.exists())
                    target.createNewFile();
                outStream = new FileOutputStream(target);
            }
            
            outStream.write(b);
        }
        
        @Override
        public void close() throws IOException {
            if(outStream != null)
                outStream.close();
        }
    }

    /**
     * Hide default constructor.
     * 
     */
    private StoreHelper() {
    }

    /* *
     * Returns input stream of known length.
     * @param data
     * @return
     * @throws Exception
     * /
    public static MeasuredInputStream createMeasuredInputStream(InputStream data)
                    throws Exception {
        return new MeasuredInputStream(data);
    }//*/

    /**
     * Returns stream on file which should be deleted after stream is closed.
     * @param src
     * @return
     * @throws FileNotFoundException
     */
    public static InputStream createTmpInputStream(File src)
                    throws FileNotFoundException {
        return new TmpInputStream(src);
    }
    
    public static OutputStream createOndemandOutputStream(File src){
        return new FileOndemandOutputStream(src);
    }

    /**
     * Transfers data with default buffer size 8196
     * @param inStream
     * @param outStream
     * @throws Exception
     */
    public static int transfer(InputStream inStream, OutputStream outStream)
                    throws IOException {
        return transfer(inStream, outStream, -1);
    }

    /**
     * Transfers data from input stream to output stream.
     * @param inStream - input data.
     * @param outStream - sink
     * @param bufferSize - is a size of buffer to be used which transferring.
     * @throws Exception is thrown in case of error.
     */
    public static int transfer(InputStream inStream, OutputStream outStream,
                    int bufferSize) throws IOException {
        int total = 0;
        try {
            bufferSize = bufferSize < 1 ? 8196 : bufferSize;
            int read = 0;
            byte[] buffer = new byte[bufferSize];

            while ((read = inStream.read(buffer, 0, buffer.length)) > -1) {
                outStream.write(buffer, 0, read);
                total += read;
            }
        }
        finally {
            outStream.close();
            inStream.close();
        }
        return total;
    }
    
    
    /**
     * Converts Map to Properties string ready for storage in a text field.
     * @param comment
     * @param config
     * @return
     * @throws IOException
     */
    public static String mapToPropertiesString(String comment, Map<String, String> config) throws IOException{
        if(config == null)
            return null;
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(config.size()*4);
        Properties props = new Properties();
        props.putAll(config);
        props.store(bos, comment);
        return bos.toString();
    }
    
}
