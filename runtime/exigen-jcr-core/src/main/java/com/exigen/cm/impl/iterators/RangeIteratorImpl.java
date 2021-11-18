/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.RangeIterator;

import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.state2.IdIterator;
import com.exigen.cm.impl.state2.SizeIterator;

public abstract class RangeIteratorImpl implements RangeIterator {

    protected List data;

    protected SessionImpl session;

    protected int pos = 0;
    protected long cacheStart = -1;
    protected long cacheEnd = -1;

    private Iterator iterator;
    
    private boolean iteratorMode = false;
    
    

    public RangeIteratorImpl(SessionImpl session, List data) {
        this.data = data;
        this.session = session;
    }

    public RangeIteratorImpl(SessionImpl session, Iterator iterator) {
        this.iterator = iterator;
        this.session = session;
        iteratorMode = true;
    }

    public void skip(long skipNum) {
        if (isIterator()) {
            // do nothing
        } else {
            if (pos + skipNum > getSize()) {
                throw new NoSuchElementException();
            }
        }

        pos += skipNum;

        if (isIterator()) {
              for (int i = 0; i < skipNum; i++) {
                iterator.next();
            }
        }
    }

    
    public long getSize() {
        if (isIterator()) {
        	if (iterator instanceof SizeIterator){
        		return ((SizeIterator)iterator).getSize();
        	} else {
	            throw new UnsupportedOperationException(
	                    "getSize() not supported for iterators");
        	}
        }
        return data.size();
    }

    public long getPosition() {
        return pos;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        if (isIterator()) {
            return iterator.hasNext();
        } else {
            return pos < getSize() ? true : false;
        }
    }

    public ArrayList<Long> getNextIds(){
    	if (isIterator() && iterator instanceof IdIterator){
    		return ((IdIterator)iterator).getNextIds();
    	}
    	return null;
    }
    
    public Object get(int index){
        if (isIterator()) {
            if (iterator instanceof SizeIterator){
                return buildObject(((SizeIterator)iterator).get(index));
            } else {
                throw new UnsupportedOperationException(
                        "get(int index) not supported for iterators");
            }
        } else {
            return data.get(index);
        }
    }

    
    public Object next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        if (!isIterator()) {
            if (cacheEnd < pos && getSize() > 1){
                cacheStart = pos;
                cacheEnd = cacheStart + 100;
                if (cacheEnd > getSize()){
                    cacheEnd = getSize();
                }
                cache();
            }
            //cache
        }
        
        Object source;
        if (isIterator()) {
            source = iterator.next();
        } else {
            source = data.get(pos);
        }
        pos++;
        return buildObject(source);
    }

    protected void cache() {
    }

    abstract protected Object buildObject(Object source);

    private boolean isIterator() {
        return iteratorMode;
    }

    public void setCountResults(boolean countResults) {
        if (iterator != null && iterator instanceof IdIterator){
            ((IdIterator)iterator).setCountResults(countResults);
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
}

/*
 * $Log: RangeIteratorImpl.java,v $
 * Revision 1.3  2008/05/19 11:09:02  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2008/05/19 07:18:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 09:01:27  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/11/14 07:37:25  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.2  2006/11/01 14:11:22  dparhomenko
 * PTR#1803326 new features
 *
 * Revision 1.1  2006/04/17 06:47:00  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/03/27 14:57:33  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.3  2006/03/27 14:27:21  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.2  2006/03/01 13:56:21  dparhomenko
 * PTR#0144983 iterator support
 *
 * Revision 1.1  2006/02/17 13:03:40  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */