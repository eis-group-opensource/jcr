/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.observation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.collections.map.LinkedMap;

import com.exigen.cm.impl.ItemId;
import com.exigen.cm.impl.state.ItemState;
import com.exigen.cm.impl.state2._ItemId;
import com.exigen.cm.impl.state2._ItemState;

/**
 * Registers changes made to states and references and consolidates
 * empty changes.
 */
public class ChangeLog {

    /**
     * Added states
     */
    private final Map<_ItemId, _ItemState> addedStates = new LinkedMap();

    /**
     * Modified states
     */
    private final Map<_ItemId, _ItemState> modifiedStates = new LinkedMap();

    /**
     * Deleted states
     */
    private final Map<_ItemId, _ItemState> deletedStates = new LinkedMap();

    /**
     * Modified references
     */
    private final Map<_ItemId, _ItemState> modifiedRefs = new LinkedMap();

    private ArrayList<EventState> events = new ArrayList<EventState>();
    
    
    public ChangeLog(){
    	
    }
    
    
    public void add(ChangeLog other){
    	//added
    	for(_ItemState s:other.addedStates.values()){
    		added(s);
    	}
    	//deleted
    	for(_ItemState s:other.deletedStates.values()){
    		deleted(s);
    	}
    	//modified
    	for(_ItemState s:other.modifiedStates.values()){
    		modified(s);
    	}
    	//modified refs
    	for(_ItemState s:other.modifiedRefs.values()){
    		throw new UnsupportedOperationException();
    	}
    }
    
    /**
     * A state has been added
     *
     * @param state state that has been added
     */
    public void added(_ItemState state) {
        addedStates.put(state.getId(), state);
    }

    /**
     * A state has been modified. If the state is not a new state
     * (not in the collection of added ones), then disconnect
     * the local state from its underlying shared state and add
     * it to the modified states collection.
     *
     * @param state state that has been modified
     */
    public void modified(_ItemState state) {
        if (!addedStates.containsKey(state.getId())) {
            //TODO state.disconnect();
            modifiedStates.put(state.getId(), state);
        }
    }

    /**
     * A state has been deleted. If the state is not a new state
     * (not in the collection of added ones), then disconnect
     * the local state from its underlying shared state, remove
     * it from the modified states collection and add it to the
     * deleted states collection.
     *
     * @param state state that has been deleted
     */
    public void deleted(_ItemState state) {
        if (addedStates.remove(state.getId()) == null) {
            //TODO state.disconnect();
            modifiedStates.remove(state.getId());
            deletedStates.put(state.getId(), state);
        }
    }

    /**
     * A references has been modified
     *
     * @param refs refs that has been modified
     */
    /*public void modified(NodeReferences refs) {
        modifiedRefs.put(refs.getId(), refs);
    }*/

    /**
     * Return an item state given its id. Returns <code>null</code>
     * if the item state is neither in the added nor in the modified
     * section. Throws a <code>NoSuchItemStateException</code> if
     * the item state is in the deleted section.
     *
     * @return item state or <code>null</code>
     * @throws NoSuchItemStateException if the item has been deleted
     */
    public ItemState get(ItemId id) throws RepositoryException {
        ItemState state = (ItemState) addedStates.get(id);
        if (state == null) {
            state = (ItemState) modifiedStates.get(id);
            if (state == null) {
                if (deletedStates.containsKey(id)) {
                    throw new RepositoryException("State has been marked destroyed: " + id);
                }
            }
        }
        return state;
    }

    /**
     * Return a flag indicating whether a given item state exists.
     *
     * @return <code>true</code> if item state exists within this
     *         log; <code>false</code> otherwise
     */
    /*public boolean has(ItemId id) {
        return addedStates.containsKey(id) || modifiedStates.containsKey(id);
    }*/

    /**
     * Return a node references object given its id. Returns
     * <code>null</code> if the node reference is not in the modified
     * section.
     *
     * @return node references or <code>null</code>
     */
    /*public NodeReferences get(NodeReferencesId id) {
        return (NodeReferences) modifiedRefs.get(id);
    }*/

    /**
     * Return an iterator over all added states.
     *
     * @return iterator over all added states.
     */
    public Iterator<_ItemState> addedStates() {
        return addedStates.values().iterator();
    }

    /**
     * Return an iterator over all modified states.
     *
     * @return iterator over all modified states.
     */
    public Iterator<_ItemState> modifiedStates() {
        return modifiedStates.values().iterator();
    }

    /**
     * Return an iterator over all deleted states.
     *
     * @return iterator over all deleted states.
     */
    public Iterator<_ItemState> deletedStates() {
        return deletedStates.values().iterator();
    }

    /**
     * Return an iterator over all modified references.
     *
     * @return iterator over all modified references.
     */
    /*public Iterator<_ItemState> modifiedRefs() {
        return modifiedRefs.values().iterator();
    }*/

    /**
     * Merge another change log with this change log
     *
     * @param other other change log
     */
    /*public void merge(ChangeLog other) {
        // Remove all states from our 'added' set that have now been deleted
        Iterator iter = other.deletedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            if (addedStates.remove(state.getId()) == null) {
                deletedStates.put(state.getId(), state);
            }
            // also remove from possibly modified state
            modifiedStates.remove(state.getId());
        }

        // only add modified states that are not already 'added'
        iter = other.modifiedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            if (!addedStates.containsKey(state.getId())) {
                modifiedStates.put(state.getId(), state);
            } else {
                // adapt status and replace 'added'
                state.setStatus(ItemState.STATUS_NEW);
                addedStates.put(state.getId(), state);
            }
        }

        // add 'added' state, but respect previously deleted
        iter = other.addedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            ItemState deletedState = (ItemState) deletedStates.remove(state.getId());
            if (deletedState != null) {
                // the newly 'added' state had previously been deleted;
                // merging those two operations results in a modified state

                // adapt status/modCount and add to modified
                state.setStatus(deletedState.getStatus());
                state.setModCount(deletedState.getModCount());
                modifiedStates.put(state.getId(), state);
            } else {
                addedStates.put(state.getId(), state);
            }
        }

        // add refs
        modifiedRefs.putAll(other.modifiedRefs);
    }*/

    /**
     * Push all states contained in the various maps of
     * items we have.
     */
    /*public void push() {
        Iterator iter = modifiedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).push();
        }
        iter = deletedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).push();
        }
        iter = addedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).push();
        }
    }*/

    /**
     * After the states have actually been persisted, update their
     * internal states and notify listeners.
     */
    /*public void persisted() {
        Iterator iter = modifiedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            state.setStatus(ItemState.STATUS_EXISTING);
            state.notifyStateUpdated();
        }
        iter = deletedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            state.setStatus(ItemState.STATUS_EXISTING_REMOVED);
            state.notifyStateDestroyed();
            state.discard();
        }
        iter = addedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            state.setStatus(ItemState.STATUS_EXISTING);
            state.notifyStateCreated();
        }
    }*/
    
    public void addEvents(Collection<EventState> states){
    	events.addAll(states);
    }

    public Collection<EventState> getEvents(){
    	return events;
    }
    
    /**
     * Reset this change log, removing all members inside the
     * maps we built.
     */
    public void reset() {
        addedStates.clear();
        modifiedStates.clear();
        deletedStates.clear();
        modifiedRefs.clear();
        events.clear();
    }

    public boolean isEmpty() {
        return addedStates.isEmpty() && modifiedStates.isEmpty()
                && deletedStates.isEmpty() && modifiedRefs.isEmpty();
    }    
    
    /**
     * Disconnect all states in the change log from their overlaid states.
     */
    /*public void disconnect() {
        Iterator iter = modifiedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).disconnect();
        }
        iter = deletedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).disconnect();
        }
        iter = addedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).disconnect();
        }
    }*/

    /**
     * Undo changes made to items in the change log. Discards
     * added items, refreshes modified and resurrects deleted
     * items.
     *
     * @param parent parent manager that will hold current data
     */
    /*public void undo(ItemStateManager parent) {
        Iterator iter = modifiedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            try {
                state.connect(parent.getItemState(state.getId()));
                state.pull();
            } catch (ItemStateException e) {
                state.discard();
            }
        }
        iter = deletedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            try {
                state.connect(parent.getItemState(state.getId()));
                state.pull();
            } catch (ItemStateException e) {
                state.discard();
            }
        }
        iter = addedStates();
        while (iter.hasNext()) {
            ((ItemState) iter.next()).discard();
        }
        reset();
    }*/

    /**
     * Returns a string representation of this change log for diagnostic
     * purposes.
     *
     * @return a string representation of this change log
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("{");
        buf.append("#addedStates=").append(addedStates.size());
        buf.append(", #modifiedStates=").append(modifiedStates.size());
        buf.append(", #deletedStates=").append(deletedStates.size());
        buf.append(", #modifiedRefs=").append(modifiedRefs.size());
        buf.append(", #events=").append(events.size());
        buf.append("}");
        return buf.toString();
    }




}
