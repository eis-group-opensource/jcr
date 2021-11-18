/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Holds context data for building subquery
 */
public class PathSQL extends BuildingContextHolder {
    
    public enum QUERY_PART {FROM, WHERE};
    

   /*
    * Should maintain list of selected fields ... 
    * this list will be added to SELECT ... in XPathQuery.getAsPreparedStatement
    * It is expected that values in this list can be added only by Ordering builder
    * thus aliases in selection list will match to those referred in OrderingDefinitions after these are processed.
    */
    private List<String> selections;
//  Holds mapping alias to column in given PathSQL
    private Map<String, String> selectionsAliasMapping;
    
    private List<Object> fromParameters;
    private List<Object> whereParameters;
    
    private StringBuilder from, where;
    private StringBuilder currentAlias;
    
    public static final StringBuilder TARGET_ALIAS= new StringBuilder("n");
    public static final String TARGET_ALIAS_ACE="nnACE"; 
    
//    private int aliasCount = 1;
    
    public PathSQL(BuildingContext context) {
        super(context);
        
        selections = new ArrayList<String>();
        selectionsAliasMapping = new HashMap<String, String>();
        
        fromParameters = new ArrayList<Object>();
        whereParameters = new ArrayList<Object>();
        
        from = new StringBuilder();
        where = new StringBuilder();
    }

    /**
     * Forms a selection
     * @param columnAlias
     * @param columnName
     * @param alias
     */
    public void addSelection(String columnAlias, String columnName, String valueAlias){
        StringBuilder fullColumnName = QueryUtils.asPrefix(columnAlias).append(columnName);

        selectionsAliasMapping.put(valueAlias, fullColumnName.toString());
        selections.add(fullColumnName.append(' ').append(valueAlias).toString());
    }
    
    /**
     * Returns list of selections ready for addition to SQL SELECT statement
     * e.g. in a form &lt;table alias&gt;.&lt;column&gt; &lt;column alias&gt;
     * @return
     */
    public List<String> getSelections(){
        return selections;
    }
    
    /**
     * Return selected column full name (&lt;table alias&gt;.&lt;column&gt;) by its alias.
     * @param alias
     * @return
     */
    public String getSelectionColumnByAlias(String alias){
        return selectionsAliasMapping.get(alias);
    }
    
    /**
     * Adds parameter to a specified query part. Dialect specific 
     * data conversion is applied to parameter before addition to a parameters list.
     * @param param
     * @param target
     */
    public void addParameter(Object param, QUERY_PART target){
        switch(target){
            case FROM:
                QueryUtils.addParameter(param, getBuildingContext(), fromParameters);//.add(param);
                break;
            case WHERE:
                QueryUtils.addParameter(param, getBuildingContext(), whereParameters);//
                break;
            default:
                throw new IllegalStateException("Cannot add parameter to unsupported target!");
        }

    }
    
    /**
     * Returns currently active table alias.
     * @return
     */
    public StringBuilder currentAlias(){
        return currentAlias;
    }
    
    /**
     * Sets currently active table alias.
     * @param alias
     */
    public void currentAlias(StringBuilder alias){
        currentAlias=alias;
    }

    
    /**
     * Returns all parameters which is where parameters 
     * appended to from parameters.
     * @return
     */
    public List<Object> getParameters(){
        List<Object> all = new LinkedList<Object>(fromParameters);
        all.addAll(whereParameters);
        return all;
    }
    
    /**
     * Returns list of parameters for specified query part
     * ready for setting into PreparedStatement.
     * @param params
     * @return
     */
    public List<Object> getParameters(QUERY_PART params){
        switch(params){
            case FROM:
                return fromParameters;
            case WHERE:
                return whereParameters;
            default:
                return null;
        }
    }
    
    /**
     * Returns string to be added as SQL FROM statement.
     * @return
     */
    public StringBuilder from(){
        return from;
    }
    
    /**
     * Returns string to be added as SQL WHERE statement.
     * @return
     */
    public StringBuilder where(){
        return where;
    }
    
    /**
     * Returns next avaliable alias.
     * @return
     */
    public StringBuilder nextAlias(){
        return getBuildingContext().nextAlias();
    }
    
 


    

    
    
//    public DBQName toDBQname(String qNameString){
//        if(qNameString == null) return new DBQName();
//        
//        DBQName res = qNameCache.get(qNameString);
//        if(res != null)
//            return res;
//        
//        int nsSeparator = qNameString.lastIndexOf(':');
//        Long nsId = null;
//        String localName;
//        if(nsSeparator>0){
//            nsId = getBuildingContext().getNamespaceId(qNameString.substring(0, nsSeparator));
//            localName = qNameString.substring(nsSeparator+1);
//        }else
//            localName = qNameString;
//        
//        res = new DBQName(qNameString, nsId, localName);
//        qNameCache.put(qNameString, res);
//        
//        return res;
//        
//    }
}

/*
 * $Log: PathSQL.java,v $
 * Revision 1.1  2007/04/26 08:59:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/20 16:19:18  maksims
 * #1803635 javadocs added
 *
 * Revision 1.1  2006/12/15 13:13:29  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.5  2006/12/13 14:27:12  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.4  2006/12/05 15:52:22  maksims
 * #1803540 Added ability to search by uuid
 *
 * Revision 1.3  2006/11/15 13:16:48  maksims
 * #1802721 Ordering of props as strings added
 *
 * Revision 1.2  2006/11/06 13:11:03  maksims
 * #1801897 direct path processing fixed
 *
 * Revision 1.1  2006/11/02 17:28:06  maksims
 * #1801897 Query2 addition
 *
 */