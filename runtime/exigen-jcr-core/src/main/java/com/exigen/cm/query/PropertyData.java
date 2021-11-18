/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query;

import java.util.ArrayList;
import java.util.List;

import com.exigen.cm.impl.PropertyDefinitionImpl;

/**
 * Holds property definition data for single filtered property.
 */
public class PropertyData{
    List<PropertyDefinitionImpl> exactPropDefs;
    PropertyDefinitionImpl nameUndefinedPropDef;
    PropertyDefinitionImpl wildcardTypedPropDef;
    PropertyDefinitionImpl wildcard;
    
    PropertyDefinitionImpl unsupportedMatch;
    
    
    /**
     * Adds property definition which matches exactly to a filtered property
     * by both property name and property type.
     * @param propDef
     */
    public void addExact(PropertyDefinitionImpl propDef){
        if(exactPropDefs == null)
            exactPropDefs = new ArrayList<PropertyDefinitionImpl>();
        exactPropDefs.add(propDef);
    }
    
    /**
     * Returns <code>true</code> if given instance has property exact matches.
     * @return
     */
    public boolean hasExactMatches(){
        return exactPropDefs != null;
    }

    /**
     * Returns <code>true</code> if given instance has property unsupported matches.
     * @return
     */
    public boolean hasUnsupportedMatches(){
        return unsupportedMatch != null;
    }
    
    
    /**
     * Returns number of exact matches collected in given instance.
     * @return
     */
    public int getExactMatchCount(){
        return hasExactMatches() ? exactPropDefs.size() : 0;
    }
    
    /**
     * Returns <code>true</code> if given instance has not-exact matches
     * meaning that given property can be stored as unstructured property.
     * @return
     */
    public boolean hasUnstructuredMatches(){
        return nameUndefinedPropDef != null 
                || wildcardTypedPropDef != null 
                || wildcard!=null;
    }
    
    /**
     * Returns <code>true</code> if given instance has any matches for specific property.
     * @return
     */
    public boolean hasMatches(){
        return hasExactMatches() || hasUnstructuredMatches();
    }

    /**
     * Builds list from collection of all property matches.
     * @return
     */
    public List<PropertyDefinitionImpl> toList(){
        List<PropertyDefinitionImpl> all = new ArrayList<PropertyDefinitionImpl>();
        if(exactPropDefs != null)
            all.addAll(exactPropDefs);
        
        if(nameUndefinedPropDef != null)
            all.add(nameUndefinedPropDef);
        else
        if(wildcardTypedPropDef != null)
            all.add(wildcardTypedPropDef);
        else
        if(wildcard != null)
            all.add(wildcard);
        
        return all;
    }

    /**
     * Returns list of exact matches.
     * @return
     */
    public List<PropertyDefinitionImpl> getExact() {
        return exactPropDefs;
    }

    /**
     * Returns property definition referring property which name matches
     * to one specified for filtering but which type is undefined.
     * @return
     */
    public PropertyDefinitionImpl getNameUndefined() {
        return nameUndefinedPropDef;
    }

    
    /**
     * Returns property definition referring property
     * which is not supported by current implementation.
     * @return
     */
    public PropertyDefinitionImpl getUnsupportedMatch() {
        return unsupportedMatch;
    }
    
    
    /**
     * Returns property definition referring property declared as wildcard
     * but with type corresponding to one required for filtering.
     * @return
     */
    public PropertyDefinitionImpl getWildcardTyped() {
        return wildcardTypedPropDef;
    }

    /**
     * Returns property definition referring property of any type and name.
     * @return
     */
    public PropertyDefinitionImpl getWildcard() {
        return wildcard;
    }
    
    /**
     * Returns property definition referring unstructured property. Following 
     * order is preserved:
     * <li> if property with exact name match but unknown type is found it is returned.
     * <li> if property with any name but corresponding type is found it is returned.
     * <li> if prperty with any type and name is found it is returned.
     * <li> null is returned otherwise.
     * @return
     */
    public PropertyDefinitionImpl getUnstructuredMatch(){
        if(nameUndefinedPropDef != null)
            return nameUndefinedPropDef;
        
        if(wildcardTypedPropDef != null)
            return wildcardTypedPropDef;
        
        return wildcard;
    }

}