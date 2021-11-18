/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.nodetype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.impl.BaseNamespaceRegistryImpl;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.InvalidConstraintException;
import com.exigen.cm.jackrabbit.nodetype.NodeDef;
import com.exigen.cm.jackrabbit.nodetype.NodeDefImpl;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.nodetype.PropDefImpl;
import com.exigen.cm.jackrabbit.nodetype.ValueConstraint;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.vf.commons.logging.LogUtils;

public class DBNodeTypeReader {
	
	/** Log for this class */
    private static final Log log = LogFactory.getLog(DBNodeTypeReader.class);

    private ArrayList<NodeTypeDef> defs = new ArrayList<NodeTypeDef>();
    private BaseNamespaceRegistryImpl namespaceRegistry;
    
    public DBNodeTypeReader(BaseNamespaceRegistryImpl nmRegistry) throws RepositoryException{
        this.namespaceRegistry = nmRegistry;        
    }
    
    private HashMap<Long,InternalValue[]> _readPropertyDefaults(DatabaseConnection conn) throws RepositoryException{

    	LogUtils.debug(log,"Reading default values for node type property definitions...");
    	HashMap<Long,InternalValue[]> out=new HashMap<Long,InternalValue[]>();
    	DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(
    			Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE, true);
    	st.addResultColumn(Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__PROPERTY_ID);
    	st.addResultColumn(Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__VALUE);
    	st.addResultColumn(Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__TYPE);
    	st.addOrder(com.exigen.cm.database.statements.Order.asc(
    			Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__PROPERTY_ID)); // get them ordered by prop id
        st.execute(conn);
        ArrayList<InternalValue> list = new ArrayList<InternalValue>();
        Long currentPropId=null;
        
        while (st.hasNext()){
        	RowMap row = st.nextRow();
        	Long propId=(Long)row.get(Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__PROPERTY_ID);
        	if (currentPropId!=null && !currentPropId.equals(propId)) { 
        		// start of rows with default values for next property
        		out.put(currentPropId,(InternalValue[])list.toArray(new InternalValue[list.size()]));
        		list.clear();
			}
        	currentPropId=propId;
        	try {
				list.add( InternalValue.create(
					row.getString(Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__VALUE),
					row.getLong(Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__TYPE).intValue(),
					this.namespaceRegistry, null));
			} catch (Exception e) {
				throw new RepositoryException(
						"Error reading property default values for property with id="+currentPropId);
			}
        }
        
        if(currentPropId!=null){ // for last property
        	out.put(currentPropId,(InternalValue[])list.toArray(new InternalValue[list.size()]));
        }
        st.close();
        return out;
    }
    
    private HashMap<Long,String[]> _readPropertyConstraints(DatabaseConnection conn) 
    	throws RepositoryException{

    	LogUtils.debug(log,"Reading value constraints for node type property definitions...");
    	HashMap<Long,String[]> out=new HashMap<Long,String[]>();
    	DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(
    			Constants.TABLE_NODETYPE_PROPERTY_CONSTRAINT, true);
    	st.addResultColumn(Constants.TABLE_NODETYPE_PROPERTY_CONSTRAINT__PROPERTY_ID);
    	st.addResultColumn(Constants.TABLE_NODETYPE_PROPERTY_CONSTRAINT__VALUE);
    	st.addOrder(com.exigen.cm.database.statements.Order.asc(
    			Constants.TABLE_NODETYPE_PROPERTY_CONSTRAINT__PROPERTY_ID)); // get them ordered by prop id
        st.execute(conn);
        ArrayList<String> list = new ArrayList<String>();
        Long currentPropId=null;
        
        while (st.hasNext()){
        	RowMap row = st.nextRow();
        	Long propId=(Long)row.get(Constants.TABLE_NODETYPE_PROPERTY_CONSTRAINT__PROPERTY_ID);
        	if(currentPropId!=null && ! currentPropId.equals(propId)){ // start of next prop
        		out.put(currentPropId, (String[])list.toArray(new String[list.size()]));
        		list.clear();
        	}
        	currentPropId=propId;
        	list.add((String) row.get(Constants.TABLE_NODETYPE_PROPERTY_CONSTRAINT__VALUE));
	    }
        if (currentPropId!=null){ // for last one
        	out.put(currentPropId, (String[])list.toArray(new String[list.size()]));
        }	
        st.close();
        return out;
    }
    
    private void _loadPropertyDefinitions( DatabaseConnection conn ) throws RepositoryException{

		HashMap<Long,InternalValue[]> defaults=_readPropertyDefaults(conn);
		HashMap<Long,String[]> constraints=_readPropertyConstraints(conn);
    	
        LogUtils.debug(log,"Reading property definitions for node types...");
        DatabaseSelectAllStatement st=DatabaseTools.createSelectAllStatement(
        	Constants.TABLE_NODETYPE_PROPERTY,true);
        st.addOrder(com.exigen.cm.database.statements.Order.asc(
        	Constants.TABLE_NODETYPE_PROPERTY__NODE_TYPE)); // get them ordered by node type id
        st.execute(conn);
        ArrayList<PropDefImpl> list = new ArrayList<PropDefImpl>();
        Long currentTypeId=null;
        
        while (st.hasNext()){
        	RowMap row = st.nextRow();
        	Long typeId=(Long)row.get(Constants.TABLE_NODETYPE_PROPERTY__NODE_TYPE);
        	if (currentTypeId!=null && ! currentTypeId.equals(typeId)){
        		// start of rows with properties for next type
        		_findNodeTypeById(currentTypeId).setPropertyDefs((PropDef[]) list.toArray(new PropDef[list.size()]));
           		list.clear();
        	}
    		currentTypeId=typeId;
        	PropDefImpl prop = new PropDefImpl();
            prop.setSQLId((Long) row.get(Constants.FIELD_ID));
            prop.setAutoCreated(((Boolean)row.get(Constants.TABLE_NODETYPE_PROPERTY__AUTO_CREATE)).booleanValue());
            prop.setColumnName((String) row.get(Constants.TABLE_NODETYPE_PROPERTY__COLUMN_NAME));
            prop.setDeclaringNodeType((_findNodeTypeById(currentTypeId)).getName());
            prop.setMandatory(((Boolean)row.get(Constants.TABLE_NODETYPE_PROPERTY__MANDATORY)).booleanValue());
            prop.setMultiple(((Boolean)row.get(Constants.TABLE_NODETYPE_PROPERTY__MILTIPLE)).booleanValue());
            prop.setName(JCRHelper.assembleQName(row, namespaceRegistry));
            prop.setOnParentVersion(((Long)row.get(Constants.TABLE_NODETYPE_PROPERTY__ON_PARENT_VERSION)).intValue());
            prop.setProtected(((Boolean)row.get(Constants.TABLE_NODETYPE_PROPERTY__PROTECTED)).booleanValue());
            prop.setRequiredType(((Long)row.get(Constants.TABLE_NODETYPE_PROPERTY__REQUIRED_TYPE)).intValue());
            prop.setIndexable((row.getBoolean(Constants.TABLE_NODETYPE_PROPERTY__INDEXABLE)).booleanValue());
            prop.setFullTextSearch((row.getBoolean(Constants.TABLE_NODETYPE_PROPERTY__FTS)).booleanValue());
            if(defaults.containsKey(prop.getSQLId())){
            	prop.setDefaultValues(defaults.get(prop.getSQLId()));
            }
            if (constraints.containsKey(prop.getSQLId())){
            	ArrayList<ValueConstraint> x=new ArrayList<ValueConstraint>();
            	for (String s:constraints.get(prop.getSQLId())){
            		try {
                        x.add(ValueConstraint.create(prop.getRequiredType(), s, namespaceRegistry));
                    } catch (InvalidConstraintException e) {
                        throw new RepositoryException("Error reading property constraint for prop with id="
                        		+prop.getSQLId()); 
                    }
            	}
            	prop.setValueConstraints((ValueConstraint[])x.toArray(new ValueConstraint[x.size()]));
            }
        	list.add(prop);
        }
        
        if (currentTypeId!=null){
        	// properties for last type
        	_findNodeTypeById(currentTypeId).setPropertyDefs((PropDef[]) list.toArray(new PropDef[list.size()]));
        }
        st.close();
    }
    
    private void _loadChildNodes( DatabaseConnection conn) throws RepositoryException{
    	
    	HashMap<Long,QName[]> reqTypes=_readChildNodeRequiredTypes(conn);
    	
    	LogUtils.debug(log,"Reading child nodes for node types...");
        DatabaseSelectAllStatement st=DatabaseTools.createSelectAllStatement(Constants.TABLE_NODETYPE_CHILDS,true);
        st.addOrder(com.exigen.cm.database.statements.Order.asc(
        		Constants.TABLE_NODETYPE_CHILDS__NODE_TYPE));

        st.execute(conn);
        ArrayList<NodeDefImpl> list = new ArrayList<NodeDefImpl>();
        Long currentTypeId=null;
        
        while (st.hasNext()){
        	RowMap row = st.nextRow();
        	Long typeId=(Long)row.get(Constants.TABLE_NODETYPE_CHILDS__NODE_TYPE);
        	if (currentTypeId!=null && ! currentTypeId.equals(typeId)){
        		// start of rows with child nodes for next type
        		_findNodeTypeById(currentTypeId).setChildNodeDefs((NodeDef[]) list.toArray(new NodeDef[list.size()]));
        		list.clear();
        	}
    		currentTypeId=typeId;
    		NodeDefImpl child = new NodeDefImpl();
            child.setSQLId((Long) row.get(Constants.FIELD_ID));
            child.setAllowsSameNameSiblings(((Boolean)row.get(Constants.TABLE_NODETYPE_CHILDS__SAMENAMESIBLING)).booleanValue());
            child.setAutoCreated(((Boolean)row.get(Constants.TABLE_NODETYPE_CHILDS__AUTO_CREATE)).booleanValue());
            child.setDeclaringNodeType((_findNodeTypeById(currentTypeId)).getName());
            child.setMandatory(((Boolean)row.get(Constants.TABLE_NODETYPE_CHILDS__MANDATORY)).booleanValue());
            child.setName(JCRHelper.assembleQName(row, namespaceRegistry));
            child.setOnParentVersion(((Long)row.get(Constants.TABLE_NODETYPE_CHILDS__ON_PARENT_VERSION)).intValue());
            child.setProtected(((Boolean)row.get(Constants.TABLE_NODETYPE_CHILDS__PROTECTED)).booleanValue());
            Long defaultprimaryTypeId = (Long) row.get(Constants.TABLE_NODETYPE_CHILDS__DEFAULT_NODE_TYPE);
            if (defaultprimaryTypeId != null) {
                child.setDefaultPrimaryType(_findNodeTypeById(defaultprimaryTypeId).getName());
            }
            if (reqTypes.containsKey(child.getSQLId())){
            	child.setRequiredPrimaryTypes(reqTypes.get(child.getSQLId()));
            }
            list.add(child);
        }
        
        if (currentTypeId!=null){
        	// child nodes for last type
        	_findNodeTypeById(currentTypeId).setChildNodeDefs((NodeDef[]) list.toArray(new NodeDef[list.size()]));
        }
        st.close();
    }
    
    private HashMap<Long,QName[]> _readChildNodeRequiredTypes(DatabaseConnection conn) throws RepositoryException{
    	
    	LogUtils.debug(log,"Reading required types for child nodes of node type...");
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(
        	Constants.TABLE_NODETYPE_CHILDS_REQUIRED_TYPES,true);
        st.addResultColumn(Constants.TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__CHILD_ID);
        st.addResultColumn(Constants.TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__NODE_TYPE);
        st.addOrder(com.exigen.cm.database.statements.Order.asc(
        		Constants.TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__CHILD_ID)); // get them ordered by child  id
        st.execute(conn);
        ArrayList<QName> list = new ArrayList<QName>();
        HashMap<Long,QName[]> out=new HashMap<Long,QName[]>();
        Long currentChildId=null;
        
        while (st.hasNext()){
        	HashMap row = st.nextRow();
        	Long childId=(Long)row.get(Constants.TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__CHILD_ID);
        	if (currentChildId!=null && !currentChildId.equals(childId)){
        		// start of rows with required types for next child node
        		out.put(currentChildId, (QName[])list.toArray(new QName[list.size()]));
        		list.clear();
        	}
    		currentChildId=childId;
    		list.add(
    			_findNodeTypeById((Long) row.get(Constants.TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__NODE_TYPE))
    				.getName());
        }
        
        if (currentChildId!=null){
        	// required types for last child node
        	out.put(currentChildId, (QName[])list.toArray(new QName[list.size()]));
        }
        st.close();
        return out;
    }
    
    private void _loadSuperTypes(DatabaseConnection conn) throws RepositoryException{
    	
    	LogUtils.debug(log,"Reading supertypes for node type definitions");
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(
        	Constants.TABLE_NODETYPE_SUPERTYPES, true);
        st.addResultColumn(Constants.TABLE_NODETYPE_SUPERTYPES__CHILD);
        st.addResultColumn(Constants.TABLE_NODETYPE_SUPERTYPES__PARENT);
        st.addOrder(com.exigen.cm.database.statements.Order.asc(
        	Constants.TABLE_NODETYPE_SUPERTYPES__CHILD)); // get them ordered by child type id
        st.execute(conn);
        ArrayList<QName> list = new ArrayList<QName>();
        Long currentTypeId=null;
        
        while (st.hasNext()){
        	HashMap row = st.nextRow();
        	Long child=(Long)row.get(Constants.TABLE_NODETYPE_SUPERTYPES__CHILD);
        	if (currentTypeId!=null && !currentTypeId.equals(child)){
        		// start of rows with parent type for next type
        		_findNodeTypeById(currentTypeId).setSupertypes((QName[]) list.toArray(new QName[list.size()]));
           		list.clear();
        	}
    		currentTypeId=child;
        	list.add((_findNodeTypeById((Long)row.get(Constants.TABLE_NODETYPE_SUPERTYPES__PARENT))).getName());
        }
        
        if (currentTypeId!=null){
        	 // parent types for last type
        	_findNodeTypeById(currentTypeId).setSupertypes((QName[]) list.toArray(new QName[list.size()]));
        }	
        st.close();
    }
    
    public void loadNodeTypes(DatabaseConnection conn) throws RepositoryException {
        //load nodetypes
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODETYPE,true);
        st.execute(conn);
        defs.clear();
        while (st.hasNext()){
            RowMap map = st.nextRow();
            //build namespace from map
            NodeTypeDef def = new NodeTypeDef();
            def.setId((Long) map.get(Constants.FIELD_ID));
            def.setMixin((map.getBoolean(Constants.TABLE_NODETYPE__MIXIN)).booleanValue());
            def.setName(JCRHelper.assembleQName(map, namespaceRegistry));
            def. setOrderableChildNodes(((Boolean)map.get(Constants.TABLE_NODETYPE__ORDERABLE_CHILDS)).booleanValue());
            String primaryItemName = (String) map.get(Constants.TABLE_NODETYPE__PRIMARY_ITEM_NAME);
            Long primaryItemNamespace = (Long) map.get(Constants.TABLE_NODETYPE__PRIMARY_ITEM_NAMESPACE);
            if (primaryItemName != null && primaryItemNamespace != null) {
                def.setPrimaryItemName(JCRHelper.assembleQName(primaryItemName, primaryItemNamespace, namespaceRegistry));
            }
            def.setTableName((String) map.get(Constants.TABLE_NODETYPE__TABLENAME));
            def.setPresenceColumn(map.getString(Constants.TABLE_NODETYPE__PRESENCECOLUMN));
            //def.loadedFromDB();
            addNodeDefinition(def);
        }
        st.close();

        _loadSuperTypes(conn);
        _loadPropertyDefinitions(conn);
        _loadChildNodes(conn);
        
        
//        for(Iterator it = defs.iterator(); it.hasNext() ;){
//            NodeTypeDef def = (NodeTypeDef) it.next();
//            ArrayList<QName> superTypes = new ArrayList<QName>();
//            
//            st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODETYPE_SUPERTYPES, true);
//            st.addCondition(Conditions.eq(Constants.TABLE_NODETYPE_SUPERTYPES__CHILD, def.getId()));
//            st.execute(conn);
//            while (st.hasNext()){
//                HashMap map = st.nextRow();
//                Long nodeTypeId = (Long) map.get(Constants.TABLE_NODETYPE_SUPERTYPES__PARENT);
//                NodeTypeDef superType = _findNodeTypeById(nodeTypeId);
//                superTypes.add(superType.getName());
//            }
//            def.setSupertypes((QName[]) superTypes.toArray(new QName[superTypes.size()]));
//            st.close();
//        }
//        
//        for(Iterator it = defs.iterator(); it.hasNext() ;){
//            NodeTypeDef def = (NodeTypeDef) it.next();
//            ArrayList<PropDefImpl> props = new ArrayList<PropDefImpl>();
//            st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODETYPE_PROPERTY,true);
//            st.addCondition(Conditions.eq(Constants.TABLE_NODETYPE_PROPERTY__NODE_TYPE, def.getId()));
//            st.execute(conn);
//            while (st.hasNext()){
//                RowMap map = st.nextRow();
//                
//                PropDefImpl propDef = new PropDefImpl();
//                propDef.setSQLId((Long) map.get(Constants.FIELD_ID));
//                propDef.setAutoCreated(((Boolean)map.get(Constants.TABLE_NODETYPE_PROPERTY__AUTO_CREATE)).booleanValue());
//                propDef.setColumnName((String) map.get(Constants.TABLE_NODETYPE_PROPERTY__COLUMN_NAME));
//                propDef.setDeclaringNodeType(def.getName());
//                propDef.setMandatory(((Boolean)map.get(Constants.TABLE_NODETYPE_PROPERTY__MANDATORY)).booleanValue());
//                propDef.setMultiple(((Boolean)map.get(Constants.TABLE_NODETYPE_PROPERTY__MILTIPLE)).booleanValue());
//                /*if (JCRHelper.assembleQName(map, namespaceRegistry).getLocalName().equals("isCheckedOut")){
//                	System.out.println("Gotcha");
//                }*/
//                propDef.setName(JCRHelper.assembleQName(map, namespaceRegistry));
//                propDef.setOnParentVersion(((Long)map.get(Constants.TABLE_NODETYPE_PROPERTY__ON_PARENT_VERSION)).intValue());
//                propDef.setProtected(((Boolean)map.get(Constants.TABLE_NODETYPE_PROPERTY__PROTECTED)).booleanValue());
//                propDef.setRequiredType(((Long)map.get(Constants.TABLE_NODETYPE_PROPERTY__REQUIRED_TYPE)).intValue());
//                propDef.setIndexable((map.getBoolean(Constants.TABLE_NODETYPE_PROPERTY__INDEXABLE)).booleanValue());
//                propDef.setFullTextSearch((map.getBoolean(Constants.TABLE_NODETYPE_PROPERTY__FTS)).booleanValue());
//                
//                //TODO use one select for constraints and default value if it possible
//                //constraint
//                DatabaseSelectAllStatement st2 = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODETYPE_PROPERTY_CONSTRAINT,true);
//                st2.addCondition(Conditions.eq(Constants.TABLE_NODETYPE_PROPERTY_CONSTRAINT__PROPERTY_ID, propDef.getSQLId()));
//                st2.execute(conn);
//                {
//	                ArrayList<ValueConstraint> vcList = new ArrayList<ValueConstraint>();
//	                while (st2.hasNext()){
//	                    HashMap map2 = st2.nextRow();
//	                    String value = (String) map2.get(Constants.TABLE_NODETYPE_PROPERTY_CONSTRAINT__VALUE);
//	                    ValueConstraint vc;
//	                    try {
//	                        vc = ValueConstraint.create(propDef.getRequiredType(), value, namespaceRegistry);
//	                    } catch (InvalidConstraintException e) {
//	                        throw new RepositoryException("Error reading property constraint"); 
//	                    }
//	                    vcList.add(vc);
//	                }
//	                propDef.setValueConstraints((ValueConstraint[]) vcList.toArray(new ValueConstraint[vcList.size()]));
//                }
//                //default values
//                //constraint
//                st2 = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE,true);
//                st2.addCondition(Conditions.eq(Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__PROPERTY_ID, propDef.getSQLId()));
//                st2.execute(conn);
//                ArrayList<InternalValue> df = new ArrayList<InternalValue>();
//                while (st2.hasNext()){
//                    RowMap map2 = st2.nextRow();
//                    String value =  map2.getString(Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__VALUE);
//                    int type = map2.getLong(Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__TYPE).intValue();
//                    InternalValue v;
//                    try {
//                        v = InternalValue.create(value, type, this.namespaceRegistry, null);
//                    } catch (Exception e) {
//                        throw new RepositoryException("Error reading property default values"); 
//                    }
//                    df.add(v);
//                }
//                propDef.setDefaultValues(df.toArray(new InternalValue[df.size()]));
//
//                st2.close();
//                
//                props.add(propDef);
//            }
//            def.setPropertyDefs((PropDef[]) props.toArray(new PropDef[props.size()]));
//            st.close();
//        }
        
        
        //child nodes
        /*
        for(Iterator it = defs.iterator(); it.hasNext() ;){
            NodeTypeDef def = (NodeTypeDef) it.next();
            ArrayList<NodeDefImpl> childs = new ArrayList<NodeDefImpl>();
            st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODETYPE_CHILDS,true);
            st.addCondition(Conditions.eq(Constants.TABLE_NODETYPE_CHILDS__NODE_TYPE, def.getId()));
            st.execute(conn);
            while (st.hasNext()){
                RowMap map = st.nextRow();
                
                NodeDefImpl child = new NodeDefImpl();
            
                child.setSQLId((Long) map.get(Constants.FIELD_ID));
                child.setAllowsSameNameSiblings(((Boolean)map.get(Constants.TABLE_NODETYPE_CHILDS__SAMENAMESIBLING)).booleanValue());
                child.setAutoCreated(((Boolean)map.get(Constants.TABLE_NODETYPE_CHILDS__AUTO_CREATE)).booleanValue());
                child.setDeclaringNodeType(def.getName());
                child.setMandatory(((Boolean)map.get(Constants.TABLE_NODETYPE_CHILDS__MANDATORY)).booleanValue());
                child.setName(JCRHelper.assembleQName(map, namespaceRegistry));
                child.setOnParentVersion(((Long)map.get(Constants.TABLE_NODETYPE_CHILDS__ON_PARENT_VERSION)).intValue());
                child.setProtected(((Boolean)map.get(Constants.TABLE_NODETYPE_CHILDS__PROTECTED)).booleanValue());
                
                
                Long defaultprimaryTypeId = (Long) map.get(Constants.TABLE_NODETYPE_CHILDS__DEFAULT_NODE_TYPE);
                if (defaultprimaryTypeId != null) {
                    child.setDefaultPrimaryType(_findNodeTypeById(defaultprimaryTypeId).getName());
                }

                
                DatabaseSelectAllStatement st1 = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODETYPE_CHILDS_REQUIRED_TYPES,true);
                st1.addCondition(Conditions.eq(Constants.TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__CHILD_ID, child.getSQLId()));
                st1.execute(conn);
                ArrayList<QName> requiredTypes = new ArrayList<QName>();
                while (st1.hasNext()){
                    HashMap row1 = st1.nextRow();
                    Long typeId = (Long) row1.get(Constants.TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__NODE_TYPE);
                    QName name = _findNodeTypeById(typeId).getName();
                    requiredTypes.add(name);
                }
                st1.close();
                child.setRequiredPrimaryTypes((QName[]) requiredTypes.toArray(new QName[requiredTypes.size()]));
                
                childs.add(child);
            }
            def.setChildNodeDefs((NodeDef[]) childs.toArray(new NodeDef[childs.size()]));
            st.close();
        }
        */
    }
    
    private NodeTypeDef _findNodeTypeById(Long nodeTypeId) throws RepositoryException {
        for(Iterator it = defs.iterator(); it.hasNext() ; ){
            NodeTypeDef def = (NodeTypeDef) it.next();
            if (def.getId().equals(nodeTypeId)){
                return def;
            }
        }
        throw new RepositoryException("Node Type with id "+nodeTypeId+" not found");
    }    
    
    public void addNodeDefinition(NodeTypeDef def) {
        this.defs.add(def);
    }
    

	public void replaceNodeDef(NodeTypeDef def) {
		NodeTypeDef old = null;
		for(NodeTypeDef d:defs){
			if (d.getName().equals(def.getName())){
				old = d;
				break;
			}
		}
		if (old != null){
			defs.remove(old);
		}
		this.defs.add(def);
	}
    

    public List<NodeTypeDef> all() {
        ArrayList<NodeTypeDef> result = new ArrayList<NodeTypeDef>();
        result.addAll(defs);
        return result;
    }


}


/*
 * $Log: DBNodeTypeReader.java,v $
 * Revision 1.4  2009/02/23 14:30:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/03/28 13:46:00  dparhomenko
 * Autmaticaly unlock orphaned locks(session scoped)
 *
 * Revision 1.2  2007/12/05 10:56:17  vpukis
 * PTR#0153866 reduce # of SQL calls required to read nodetypes
 *
 * Revision 1.1  2007/04/26 09:02:18  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2007/03/02 09:32:23  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.1  2007/02/26 14:39:08  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.9  2007/01/24 08:46:50  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.8  2006/11/21 07:19:31  dparhomenko
 * PTR#1803402 fix errors
 *
 * Revision 1.7  2006/10/17 10:47:21  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.6  2006/10/11 13:09:05  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.5  2006/10/02 15:07:17  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.4  2006/08/11 11:30:12  dparhomenko
 * PTR#1802633 fix problem with delete node
 *
 * Revision 1.3  2006/04/24 08:55:23  dparhomenko
 * PTR#0144983 fts
 *
 * Revision 1.2  2006/04/20 11:42:56  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:47:22  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.11  2006/04/13 10:04:03  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.10  2006/04/06 14:45:50  ivgirts
 * PTR #1801059 namespace and node types now cached in Repository
 *
 * Revision 1.9  2006/04/05 14:30:50  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.8  2006/04/05 09:04:12  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.7  2006/03/27 15:05:12  dparhomenko
 * PTR#0144983 remove _JCRHelper
 *
 * Revision 1.6  2006/03/27 14:57:40  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.5  2006/03/20 09:00:40  ivgirts
 * PTR #1801375 added methods for registering and altering node types
 *
 * Revision 1.4  2006/03/14 11:55:55  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.3  2006/03/03 10:33:18  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.2  2006/02/27 15:02:53  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/16 13:53:11  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */