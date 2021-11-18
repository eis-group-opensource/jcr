/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.impl.SessionImpl;

public class IdIterator implements Iterator<Long>, Iterable<Long>, SizeIterator{

	private List<Long> removedId;
	private List<Long> cache = new ArrayList<Long>();
	private int pos = 0;
	private boolean statementExecuted = false;
	private DatabaseSelectAllStatement _st;
	private DatabaseConnection conn;
	private long batchSize;
	private ResultSet resultSet;

	private boolean countResults = false;
	private boolean countAdded = false;
    private Collection<Long> newId = new ArrayList<Long>();
	private int offset;
	private int limit;
	private boolean sizeDisabled = false;

	public IdIterator(DatabaseSelectAllStatement st, DatabaseConnection conn, Collection<Long> newId, List<Long> removedId, long batchSize){
		this(st, conn, newId, removedId, batchSize, 0, -1);
	}
    
	public IdIterator(DatabaseSelectAllStatement st, DatabaseConnection conn, Collection<Long> newId, List<Long> removedId, long batchSize, int offset, int limit){
		this._st = st;
		this.offset = offset;
		this.limit = limit;
		if (limit > 0 || offset > 0){
			sizeDisabled  = true;
		}
		this.newId = newId;
		if (_st == null){
			statementExecuted = true;
			size = newId.size() - removedId.size();
		}
		this.conn = conn;
		this.removedId = removedId;
		
		this.cache.addAll(newId);
		this.batchSize = batchSize;
		
	}
	public IdIterator(ResultSet resultSet, SessionImpl session, int limitResult){
		this.statementExecuted = true;
		this.resultSet = resultSet;
		
		_SessionStateManager stateManager = session.getStateManager();
		
		//this.removedId = stateManager.getRemovedIds();		
		//this.cache.addAll(stateManager.getNewIds());
		this.removedId = new ArrayList<Long>();		
		
		this.batchSize = session._getRepository().getBatchSize();
        batchSize = limitResult>0 && batchSize>limitResult?limitResult:batchSize;
	}
	
	public boolean hasNext() {
		while (offset > 0 && pos < cache.size()){
			pos++;
			offset--;
		}
		if (pos < cache.size()){
			return true;
		}
		if (_st == null && resultSet == null){
			return false;
		}
		try {
			if (!statementExecuted){
				executeStatement();
			}
			long counter = batchSize+offset;
			if (limit > 0){
				counter=limit+offset+1;
			}
			if (resultSet == null){
				while (_st.hasNext() && counter > 0){ // pos == cache.size() 
					RowMap row = _st.nextRow();
					Long id = (Long) row.get(Constants.FIELD_ID);
					if (!removedId.contains(id) && !newId.contains(id)){
						cache.add(id);
						counter--;
					}
					if (countAdded){
		                size = row.getLong("RECORDCOUNT").intValue();
					}
				} 
				
				if (!_st.hasNext()){
					close();
				}
			} else {
				while (counter > 0){ // pos == cache.size()
					try {
						if (resultSet.next()){
							Long id = (Long) resultSet.getLong(Constants.FIELD_ID);
		                    if (!removedId.contains(id) && !newId.contains(id)){
								cache.add(id);
								counter--;
							}
						} else {
							close();
							break;
						}
					} catch (SQLException e) {
						throw new RepositoryException(e);
					}
					/*RowMap row = st.nextRow();
					Long id = (Long) row.get(Constants.FIELD_ID);
					if (!removedId.contains(id)){
						cache.add(id);
						counter--;
					}*/
				} 
				
			}
		} catch (RepositoryException e) {
			throw new IllegalStateException("Error executing query", e);
		}
		if (pos < cache.size()){
			return true;
		} else {
			return false;
		}
	}
    private void executeStatement() throws RepositoryException {
        if (countResults){
            if (conn.getDialect().isResultCountSupported()){
                conn.getDialect().addResultCountToStatement(_st);
                countAdded = true;
            }
        }
        if (offset > 0){
        	_st.setStartFrom(offset);
        }
        _st.execute(conn);
        statementExecuted = true;
        
    }

	public Long next() {
		if (!hasNext()){
			throw new NoSuchElementException();
		}
		Long id = cache.get(pos++);
		return id;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	public void close(){
		if (_st != null){
			try {
				_st.close();
			} catch (Throwable th){
				
			}
			_st = null;
		}
		if (resultSet != null){
			try {
				resultSet.close();
			} catch (SQLException e) {
			}
			resultSet = null;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	public Iterator<Long> iterator() {
		return this;
	}

	public Long get(int index){
	    if (index >= getSize()){
	        throw new RuntimeException("Invalid position in iterator "+index);
	    }
	    return cache.get(index);
	    
	}
	
	int size = -1;
	
	public synchronized int getSize() {
		if (sizeDisabled){
			throw new UnsupportedOperationException("getSize() not allowed when using limit or offset");
		}
	    if (size >= 0){
	        return size;
	    }
		try {
			if (!statementExecuted){
				executeStatement();
			}
			if (countAdded){
			    if (_st.hasNext()){
                    RowMap row = _st.nextRow();
                    Long id = (Long) row.get(Constants.FIELD_ID);
                    if (!removedId.contains(id) && !newId.contains(id)){
                        cache.add(id);
                    }
			        size = row.getLong("RECORDCOUNT").intValue();
	                return size;
			    } 
			}
			if (resultSet == null){
				while (_st.hasNext()){
					RowMap row = _st.nextRow();
					Long id = (Long) row.get(Constants.FIELD_ID);
                    if (!removedId.contains(id) && !newId.contains(id)){
						cache.add(id);
					}
				}
			} else {
				try {
					while (resultSet.next()){
						Long id = (Long) resultSet.getLong(Constants.FIELD_ID);
	                    if (!removedId.contains(id) && !newId.contains(id)){
							cache.add(id);
						}
					}
				} catch (SQLException e) {
					throw new IllegalStateException(e);
				}
			}
			
			close();
		} catch (RepositoryException exc){
			throw new IllegalStateException(exc);
		}
		size = cache.size();
		return cache.size();
	}

	public ArrayList<Long> getNextIds() {
		hasNext();
		ArrayList<Long> ids = new ArrayList<Long>();
		for(int i = pos; i < cache.size() ; i++){
			ids.add(cache.get(i));
		}
		return ids;
	}
    public void setCountResults(boolean countResults) {
        this.countResults = countResults;
    }

	
	

}
