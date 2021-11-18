/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.value;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.StorableInputStream;
import com.exigen.cm.impl.StorableInputStreamImpl;
import com.exigen.cm.impl.state2.StoreContainer;
import com.exigen.cm.jackrabbit.fs.FileSystemException;
import com.exigen.cm.jackrabbit.fs.FileSystemResource;
import com.exigen.cm.jackrabbit.util.ISO8601;
import com.exigen.cm.jackrabbit.util.TransientFileFactory;
import com.exigen.cm.store.ContentStore;
/**
 * <code>BLOBFileValue</code> represents a binary <code>Value</code> which is
 * backed by a resource or byte[]. Unlike <code>BinaryValue</code> it has no
 * state, i.e. the <code>getStream()</code> method always returns a fresh
 * <code>InputStream</code> instance.
 * <p/>
 * <b>Important Note:</b><p/>
 * This is class is for Jackrabbit-internal use only. Applications should
 * use <code>javax.jcr.ValueFactory</code> to create binary values.
 */
public class BLOBFileValue implements Value {

    /**
     * The default logger
     */
    private static Log log = LogFactory.getLog(BLOBFileValue.class);

    /**
     * the property type
     */
    public static final int TYPE = PropertyType.BINARY;

    /**
     * the default encoding
     */
    protected static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * empty array
     */
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * max size for keeping tmp data in memory
     */
    private static final int MAX_BUFFER_SIZE = 0x10000;

    /**
     * underlying file
     */
    private File file;

    /**
     * flag indicating if this instance is backed by a temporarily allocated resource/buffer
     */
    private boolean temp;

    /**
     * buffer for small-sized data
     */
    private byte[] buffer = EMPTY_BYTE_ARRAY;

    /**
     * underlying file system resource
     */
    private FileSystemResource fsResource;

    /**
     * converted text
     */
    private String text = null;

    private Long _contentId;
    private String _storeId;
    //private ContentStore store;

    private boolean storableStream= false;


    private Map storeProps;

    //private SessionImpl session;

	private Boolean ftsProcessing = null;
	
	private File textFile;

	private StoreContainer storeProvider;

	private InputStream storeStream;

	
	public BLOBFileValue(BLOBFileValue val) throws RepositoryException {
		this._contentId = val._contentId;;
		this._storeId = val._storeId;
		this.buffer = val.buffer;
		this.cachedLength = val.cachedLength;
		this.file = val.file;
		this.fsResource = val.fsResource;
		this.ftsProcessing = val.ftsProcessing;
		this.storableStream = val.storableStream;
		if (val.storeProps != null){
			this.storeProps = new HashMap(val.storeProps);
		}
		this.storeProvider = val.storeProvider;
		if (val.storeStream != null){
			throw new RepositoryException ("Can't create BlobFileValue copy with storeProperty not null");
		}
		//this.storeStream;
		this.temp = val.temp;
		this.text = val.text;
		this.textFile = val.textFile;
		
	}
	
	public BLOBFileValue copy() throws RepositoryException{
		ContentStore store = getStore();
        InputStream stream = store.get(_contentId);
        Long resultId = store.put(stream, getLength(), null);
        String newContentId =  resultId.toString();
        if (getStoreId() != null){
        	newContentId = getStoreId() + Constants.STORE_DELIMITER + newContentId;
        }
        BLOBFileValue result = new BLOBFileValue(newContentId, storeProvider);
        return result;
	}
	
    /**
     * Creates a new <code>BLOBFileValue</code> instance from an
     * <code>InputStream</code>. The contents of the stream is spooled
     * to a temporary file or to a byte buffer if its size is smaller than
     * {@link #MAX_BUFFER_SIZE}.
     *
     * @param in stream to be represented as a <code>BLOBFileValue</code> instance
     * @throws IOException if an error occurs while reading from the stream or
     *                     writing to the temporary file
     */
    public BLOBFileValue(InputStream in) throws IOException {
        loadFromStream(in);
        if (in instanceof StorableInputStream){
            StorableInputStream sIn = (StorableInputStream) in;
            this.storableStream = true;
            this._storeId = sIn.getStoreName();
            this.storeProps = sIn.getStoreProperties();
            if (sIn.getExtractedText() != null){
            	loadTextFromStream(sIn.getExtractedText());
            }
            ftsProcessing = sIn.isFTSProcessing();
        }
    }
    
    public BLOBFileValue(InputStream in, StoreContainer sc) throws IOException, RepositoryException{
        if (in instanceof StorableInputStream){
            StorableInputStream sIn = (StorableInputStream) in;
            this.storableStream = true;
            this._storeId = sIn.getStoreName();
            this.storeProps = sIn.getStoreProperties();
            if (sIn.getExtractedText() != null){
            	loadTextFromStream(sIn.getExtractedText());
            }
            ftsProcessing = sIn.isFTSProcessing();
        }
        //save content into store
        this.storeProvider = sc;
        ContentStore store = getStore();
        if (!store.isTransactionStarted()){
            store.begin(sc.getConnection());
        }

        /*
         * Store will return Longs as Content ID due to content IDs mapping introduction. 
         */
        int length = -1;
        if (in instanceof FileInputStream){
        	FileInputStream fIn = (FileInputStream) in;
        	length = (int) fIn.getChannel().size();
        }
        Long jcrId = store.put(in, length, storeProps);
        //sc.getConnection().commit();
        store.commitPut();
        store.commit();
        this._contentId = jcrId;
	}

    

    private void loadFromStream(InputStream in) throws IOException, FileNotFoundException {
        byte[] spoolBuffer = new byte[0x2000];
        int read;
        int len = 0;
        OutputStream out = null;
        File spoolFile = null;
        try {
            while ((read = in.read(spoolBuffer)) > 0) {
                if (out != null) {
                    // spool to temp file
                    out.write(spoolBuffer, 0, read);
                    len += read;
                } else if (len + read > MAX_BUFFER_SIZE) {
                    // threshold for keeping data in memory exceeded;
                    // create temp file and spool buffer contents
                    TransientFileFactory fileFactory = TransientFileFactory.getInstance();
                    spoolFile = fileFactory.createTransientFile("bin", null, null);
                    out = new FileOutputStream(spoolFile);
                    out.write(buffer, 0, len);
                    out.write(spoolBuffer, 0, read);
                    buffer = null;
                    len += read;
                } else {
                    // reallocate new buffer and spool old buffer contents
                    byte[] newBuffer = new byte[len + read];
                    System.arraycopy(buffer, 0, newBuffer, 0, len);
                    System.arraycopy(spoolBuffer, 0, newBuffer, len, read);
                    buffer = newBuffer;
                    len += read;
                }
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }

        // init vars
        file = spoolFile;
        fsResource = null;
        // this instance is backed by a temporarily allocated resource/buffer
        temp = true;
        spoolBuffer = null;
    }

    private void loadTextFromStream(Reader in) throws IOException, FileNotFoundException {
        char[] spoolBuffer = new char[0x2000];
        int read;
        OutputStream _out = null;
        File spoolFile = null;
        try {
        	TransientFileFactory fileFactory = TransientFileFactory.getInstance();
            spoolFile = fileFactory.createTransientFile("bin", null, null);
            
            _out = new FileOutputStream(spoolFile);
            OutputStreamWriter writer = new OutputStreamWriter(_out, Constants.EXTRACTED_TEXT_ENCODING);
            while ((read = in.read(spoolBuffer)) > 0) {
                    // spool to temp file
                    writer.write(spoolBuffer, 0, read);
            }
            writer.close();
        } finally {
            if (_out != null) {
                _out.close();
            }
        }

        // init vars
        textFile = spoolFile;
        spoolBuffer = null;
    }

    /**
     * Creates a new <code>BLOBFileValue</code> instance from a
     * <code>byte[]</code> array.
     *
     * @param bytes byte array to be represented as a <code>BLOBFileValue</code>
     *              instance
     */
    public BLOBFileValue(byte[] bytes) {
        buffer = bytes;
        file = null;
        fsResource = null;
        // this instance is not backed by a temporarily allocated buffer
        temp = false;
    }

    /**
     * Creates a new <code>BLOBFileValue</code> instance from a <code>File</code>.
     *
     * @param file file to be represented as a <code>BLOBFileValue</code> instance
     * @throws IOException if the file can not be read
     */
    public BLOBFileValue(File file) throws IOException {
        String path = file.getCanonicalPath();
        if (!file.isFile()) {
            throw new IOException(path + ": the specified file does not exist");
        }
        if (!file.canRead()) {
            throw new IOException(path + ": the specified file can not be read");
        }
        this.file = file;
        // this instance is backed by a 'real' file; set virtual fs resource to null
        fsResource = null;
        // this instance is not backed by temporarily allocated resource/buffer
        temp = false;
    }

    /**
     * Creates a new <code>BLOBFileValue</code> instance from a resource in the
     * virtual file system.
     *
     * @param fsResource resource in virtual file system
     * @throws IOException if the resource can not be read
     */
    public BLOBFileValue(FileSystemResource fsResource) throws IOException {
        try {
            if (!fsResource.exists()) {
                throw new IOException(fsResource.getPath()
                        + ": the specified resource does not exist");
            }
        } catch (FileSystemException fse) {
            throw new IOException(fsResource.getPath()
                    + ": Error while creating value: " + fse.toString());
        }
        // this instance is backed by a resource in the virtual file system
        this.fsResource = fsResource;
        // set 'real' file to null
        file = null;
        // this instance is not backed by temporarily allocated resource/buffer
        temp = false;
    }

    public BLOBFileValue(String contentId, StoreContainer storeProvider) {
        parseContentId(contentId); 
        this.storeProvider = storeProvider;
    }


	private void parseContentId(String contentId) {
        int pos = contentId.indexOf(Constants.STORE_DELIMITER); 
        if (pos > 0){
            _storeId = contentId.substring(0, pos);
            contentId = contentId.substring(pos+1);
        } else {
        	_storeId = null;
        }
        _contentId = Long.parseLong(contentId); 

		
	}

	private int cachedLength = -1;
	
	/**
     * Returns the length of this <code>BLOBFileValue</code>.
     *
     * @return The length, in bytes, of this <code>BLOBFileValue</code>,
     *         or -1L if the length can't be determined.
     * @throws RepositoryException 
     * @throws  
     */
    public int getLength() throws RepositoryException {
        /*try {
            loadData();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
    	if (cachedLength < 0){
	    	if (_contentId != null && storeProvider != null) {
	    		cachedLength = (int) getStore().getContentLength(_contentId);
	    	} else if (file != null) {
	            // this instance is backed by a 'real' file
	            if (file.exists()) {
	            	cachedLength = (int)file.length();
	            } else {
	            	cachedLength = -1;
	            }
	        } else if (fsResource != null) {
	            // this instance is backed by a resource in the virtual file system
	            try {
	            	cachedLength = (int)fsResource.length();
	            } catch (FileSystemException fse) {
	            	cachedLength = -1;
	            }
	        } else {
	            // this instance is backed by a in-memory buffer
	        	cachedLength = buffer.length;
	        }
    	}
    	return cachedLength;
    }

    /**
     * Frees temporarily allocated resources such as temporary file, buffer, etc.
     * If this <code>BLOBFileValue</code> is backed by a persistent resource
     * calling this method will have no effect.
     *
     * @see #delete()
     * @see #delete(boolean)
     */
    public void discard() {
        if (!temp) {
            // do nothing if this instance is not backed by temporarily
            // allocated resource/buffer
            return;
        }
        if (file != null) {
            // this instance is backed by a temp file
            file.delete();
        } else if (buffer != null) {
            // this instance is backed by a in-memory buffer
            buffer = EMPTY_BYTE_ARRAY;
        }
        
        if (textFile != null){
        	textFile.delete();
        }
    }

    /**
     * Deletes the persistent resource backing this <code>BLOBFileValue</code>.
     * Same as <code>{@link #delete(false)}</code>.
     * <p/>
     * If this <code>BLOBFileValue</code> is <i>not</i> backed by a persistent
     * resource calling this method will have no effect.
     *
     * @see #discard()
     */
    public void delete() {
        if (!temp) {
            delete(false);
        }
    }

    /**
     * Deletes the persistent resource backing this <code>BLOBFileValue</code>.
     *
     * @param pruneEmptyParentDirs if <code>true</code>, empty parent directories
     *                             will automatically be deleted
     */
    public void delete(boolean pruneEmptyParentDirs) {
        if (file != null) {
            // this instance is backed by a 'real' file
            file.delete();
            if (pruneEmptyParentDirs) {
                // prune empty parent directories
                File parent = file.getParentFile();
                while (parent != null && parent.delete()) {
                    parent = parent.getParentFile();
                }
            }
        } else if (fsResource != null) {
            // this instance is backed by a resource in the virtual file system
            try {
                fsResource.delete(pruneEmptyParentDirs);
            } catch (FileSystemException fse) {
                // ignore
                log.warn("Error while deleting BLOBFileValue: " + fse.getMessage());
            }
        } else {
            // this instance is backed by a in-memory buffer
            buffer = EMPTY_BYTE_ARRAY;
        }
    }

    /**
     * Spools the contents of this <code>BLOBFileValue</code> to the given
     * output stream.
     *
     * @param out output stream
     * @throws RepositoryException if the input stream for this
     *                             <code>BLOBFileValue</code> could not be obtained
     * @throws IOException         if an error occurs while while spooling
     */
    public void spool(OutputStream out) throws RepositoryException, IOException {
        //loadData();
        InputStream in;
        if (this._contentId != null && this.storeProvider != null){
        	in = getContentStream();
        } else if (file != null) {
            // this instance is backed by a 'real' file
            try {
                in = new FileInputStream(file);
            } catch (FileNotFoundException fnfe) {
                throw new RepositoryException("file backing binary value not found",
                        fnfe);
            }
        } else if (fsResource != null) {
            // this instance is backed by a resource in the virtual file system
            try {
                in = fsResource.getInputStream();
            } catch (FileSystemException fse) {
                throw new RepositoryException(fsResource.getPath()
                        + ": the specified resource does not exist", fse);
            }
        } else {
            // this instance is backed by a in-memory buffer
            in = new ByteArrayInputStream(buffer);
        }
        try {
            byte[] buffer = new byte[0x2000];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {
            }
        }
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Returns the path string of the backing file.
     *
     * @return The path string of the backing file.
     */
    public String toString() {
    	if (this._contentId != null && this.storeProvider != null){
    		try {
				return getContentStream().toString();
			} catch (RepositoryException e) {
				e.printStackTrace();
				return e.getMessage();
			}
    	} else if (file != null) {
            // this instance is backed by a 'real' file
            return file.toString();
        } else if (fsResource != null) {
            // this instance is backed by a resource in the virtual file system
            return fsResource.toString();
        } else {
            // this instance is backed by a in-memory buffer
            return buffer.toString();
        }
    }

    /*private void loadData() throws RepositoryException {
        //load data from content store
        if (file == null && contentId != null){
            InputStream inStream = getContentStream();
            try {
                loadFromStream(inStream);
            } catch (Exception e) {
                throw new RepositoryException(e);
            } 
        }
        
    }*/

    
    
    private InputStream getContentStream() throws RepositoryException {
    	ContentStore store = getStore();
        return store.get(_contentId);
    }

	private ContentStore getStore() throws RepositoryException {
		ContentStore store = null;
        if (store == null){
//        	if (this.storeId != null){
        		store = storeProvider.getContentStore(this._storeId);
        	/*} else {
	            int pos = contentId.indexOf(Constants.STORE_DELIMITER); 
	            if (pos > 0){
	                String storeId = contentId.substring(0, pos);
	                contentId = contentId.substring(pos+1);
	                store = storeProvider.getContentStore(storeId); 
	            } else {
	                store = storeProvider.getContentStore(null);
	            }
        	}*/
        }
		return store;
	}

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BLOBFileValue) {
            BLOBFileValue other = (BLOBFileValue) obj;
/*            try {
                loadData();
                other.loadData();
            } catch (Exception exc){
                throw new RuntimeException(exc);
            }*/
            return ( (this._contentId != null && other._contentId != null && this._contentId.equals(other._contentId))
            		&& (file == null ? other.file == null : file.equals(other.file))
                    && (fsResource == null ? other.fsResource == null : fsResource.equals(other.fsResource))
                    && Arrays.equals(buffer, other.buffer));
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

    //----------------------------------------------------------------< Value >
    /**
     * {@inheritDoc}
     */
    public int getType() {
        return TYPE;
    }

    /**
     * {@inheritDoc}
     */
    public String getString()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        if (text == null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                spool(out);
                byte[] data = out.toByteArray();
                text = new String(data, DEFAULT_ENCODING);
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException(DEFAULT_ENCODING
                        + " not supported on this platform", e);
            } catch (IOException e) {
                throw new ValueFormatException("conversion from stream to string failed", e);
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return text;
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getStream()
            throws IllegalStateException, RepositoryException {
        
        /*if (file == null && contentId != null){
            InputStream inStream = getContentStream();
            return inStream;
        } */       
        // always return a 'fresh' stream
        InputStream result = null;
        if (this._contentId != null && this.storeProvider != null){
        	result = getContentStream();
        } else if (storeStream !=null){
        	result = storeStream;
        } else if (file != null) {
            // this instance is backed by a 'real' file
            try {
                result = new FileInputStream(file);
            } catch (FileNotFoundException fnfe) {
                throw new RepositoryException("file backing binary value not found",
                        fnfe);
            }
        } else if (fsResource != null) {
            // this instance is backed by a resource in the virtual file system
            try {
                result = fsResource.getInputStream();
            } catch (FileSystemException fse) {
                throw new RepositoryException(fsResource.getPath()
                        + ": the specified resource does not exist", fse);
            }
        } else {
            result = new ByteArrayInputStream(buffer);
        }
        if (storableStream){
            result = new StorableInputStreamImpl(result, this._storeId, null);
            ((StorableInputStreamImpl)result).setStoreProperties(this.storeProps);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        try {
            return Double.parseDouble(getString());
        } catch (NumberFormatException e) {
            throw new ValueFormatException("conversion to double failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Calendar getDate()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        Calendar cal = ISO8601.parse(getString());
        if (cal != null) {
            return cal;
        } else {
            throw new ValueFormatException("not a valid date format");
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLong()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        try {
            return Long.parseLong(getString());
        } catch (NumberFormatException e) {
            throw new ValueFormatException("conversion to long failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        return Boolean.valueOf(getString()).booleanValue();
    }

	public String getStoreId() {
		return _storeId;
	}

	public void setStoreId(String sotreId) {
		this._storeId = sotreId;
	}

	public Map getSotreProps() {
		return storeProps;
	}

	public void setSotreProps(HashMap sotreProps) {
		this.storeProps = sotreProps;
	}

	public InputStream getTextStream() throws RepositoryException {
		if (textFile == null){
			return null;
		}
		try {
			return new FileInputStream(textFile);
		} catch (FileNotFoundException fnfe) {
            throw new RepositoryException("file backing binary value not found",
                    fnfe);
        }	}

	public long getTextStreamLength() {
		return textFile.length();
	}

	public Boolean isFtsProcessing() {
		return ftsProcessing;
	}

	public void assignStoreContainer(StoreContainer storeContainer) {
		this.storeProvider = storeContainer;
	}

	public String getContentId() {
		if (_contentId == null){
			return null;
		} else {
			StringBuffer sb = new StringBuffer();
			if (_storeId != null){
				sb.append(_storeId);
				sb.append(Constants.STORE_DELIMITER);
			}
			sb.append(_contentId);
			return sb.toString();
		}
	}
	
	public Long getSQLId(){
		return _contentId;
	}


}
