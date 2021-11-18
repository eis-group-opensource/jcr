/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm;

import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.FIELD_NAME;
import static com.exigen.cm.Constants.FIELD_NAMESPACE;
import static com.exigen.cm.Constants.FIELD_TYPE_ID;
import static com.exigen.cm.Constants.LEFT_INDEX;
import static com.exigen.cm.Constants.PATH_DELIMITER;
import static com.exigen.cm.Constants.RIGHT_INDEX;
import static com.exigen.cm.Constants.TABLE_NODE;
import static com.exigen.cm.Constants.TABLE_NODE_LOCK;
import static com.exigen.cm.Constants.TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED_VALUES;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__BOOLEAN_VALUE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__DATE_VALUE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__LONG_VALUE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__MULTIPLE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__NODE_TYPE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__PROP_DEF;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__STRING_VALUE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__TYPE;
import static com.exigen.cm.Constants.TABLE_NODE__INDEX;
import static com.exigen.cm.Constants.TABLE_NODE__INDEX_MAX;
import static com.exigen.cm.Constants.TABLE_NODE__NODE_DEPTH;
import static com.exigen.cm.Constants.TABLE_NODE__NODE_PATH;
import static com.exigen.cm.Constants.TABLE_NODE__NODE_TYPE;
import static com.exigen.cm.Constants.TABLE_NODE__PARENT;
import static com.exigen.cm.Constants.TABLE_NODE__SECURITY_ID;
import static com.exigen.cm.Constants.TABLE_NODE__VERSION_;
import static com.exigen.cm.Constants.TABLE_NODE__WORKSPACE_ID;
import static com.exigen.cm.Constants.TABLE_SYSTEM_PROPERTIES;
import static com.exigen.cm.Constants.TABLE_SYSTEM_PROPERTIES__VALUE;
import static com.exigen.cm.Constants.TABLE_SYSTEM_PROPERTIES__VALUE__NT_VERSION;
import static com.exigen.cm.Constants.TABLE_TYPE;
import static com.exigen.cm.Constants.TABLE_TYPE__FROM_NODE_TYPE;
import static com.exigen.cm.Constants.TABLE_TYPE__NODE_TYPE;
import static com.exigen.cm.Constants._TABLE_NODE_LOCK_INFO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.DatabaseStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.ValueChangeDatabaseStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.impl.BaseNamespaceRegistryImpl;
import com.exigen.cm.impl.JCRPropertyType;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.PropertyDefinitionImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.BaseNamespaceRegistryImpl.Namespace;
import com.exigen.cm.impl.cache.CacheKey;
import com.exigen.cm.impl.state2.ChangeState;
import com.exigen.cm.impl.state2.StoreContainer;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.impl.state2._SessionStateManager;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.value.BLOBFileValue;
import com.exigen.cm.jackrabbit.value.BinaryValue;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.cm.jackrabbit.version.NodeStateEx;

public class JCRHelper {

    //private static Log log = LogFactory.getLog(JCRHelper.class);

    
    public static String getUnstructuredPropertyColumnNameByType(
            int propertyType) throws UnsupportedRepositoryOperationException {
        switch (propertyType) {
        case PropertyType.STRING:
            return TABLE_NODE_UNSTRUCTURED__STRING_VALUE;
        case PropertyType.DOUBLE:
            return TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE;
        case PropertyType.LONG:
            return TABLE_NODE_UNSTRUCTURED__LONG_VALUE;
        case PropertyType.UNDEFINED:
            //return TABLE_NODE_UNSTRUCTURED__STRING_VALUE;
        	throw new UnsupportedRepositoryOperationException();
        case PropertyType.DATE:
            return TABLE_NODE_UNSTRUCTURED__DATE_VALUE;
        case PropertyType.BOOLEAN:
            return TABLE_NODE_UNSTRUCTURED__BOOLEAN_VALUE;
        case PropertyType.REFERENCE:
            return TABLE_NODE_UNSTRUCTURED__STRING_VALUE;
        case PropertyType283.WEAKREFERENCE:
            return TABLE_NODE_UNSTRUCTURED__STRING_VALUE;
        case PropertyType.BINARY:
            return TABLE_NODE_UNSTRUCTURED__STRING_VALUE;
        case PropertyType.PATH:
            return TABLE_NODE_UNSTRUCTURED__STRING_VALUE;
        case PropertyType.NAME:
            return TABLE_NODE_UNSTRUCTURED__STRING_VALUE;

        default:
            throw new UnsupportedOperationException();
        }
    }

    static String getUnstructuredPropertyColumnNameByDefinition(
            PropDef definition) throws UnsupportedRepositoryOperationException {
        return getUnstructuredPropertyColumnNameByType(definition.getRequiredType());
    }
    
    static String getUnstructuredPropertyColumnNameByDefinition(
            PropertyDefinition definition) throws UnsupportedRepositoryOperationException {
        return getUnstructuredPropertyColumnNameByType(definition.getRequiredType());
    }  
    
    
/*    public static Value createValue(int requiredType, Object object, ValueFactory valueFactory, ContentStore contentStore) {
        if (requiredType == PropertyType.BINARY){
            InputStream stream = contentStore.get((String)object);
            return valueFactory.createValue(stream);
        } else if (object instanceof Date){
            Calendar c = Calendar.getInstance();
            c.setTime((Date) object);
            return valueFactory.createValue(c);
        } else if (object instanceof String){
            return valueFactory.createValue((String) object);
        } else if (object instanceof Long){
            return valueFactory.createValue(((Long) object).longValue());
        } else if (object instanceof Double){
            return valueFactory.createValue(((Double) object).doubleValue());
        } else if (object instanceof Boolean){
            return valueFactory.createValue(((Boolean) object).booleanValue());
        } else {
            throw new UnsupportedOperationException();
        }
    }*/

    @Deprecated
	static InternalValue createInternatValue(int requiredType, Object object, SessionImpl session) throws RepositoryException {
		return createInternatValue(requiredType, object, session._getWorkspace()._getNamespaceRegistry(), session.getStoreContainer());
	}    
    
    public static InternalValue createInternatValue(int requiredType, Object object, NamespaceResolver namespaceResolver, StoreContainer contentStoreProvider) throws RepositoryException{
        if (requiredType == PropertyType.BINARY){
            //InputStream stream = contentStore.get((String)object);
            try {
                return InternalValue.create(null, (String) object, contentStoreProvider);
            } catch (Exception exc){
                throw new RepositoryException("Error loading stream");
            }
        } else if (object instanceof Date){
            Calendar c = Calendar.getInstance();
            c.setTime((Date) object);
            return InternalValue.create(c);
        } else if (object instanceof Calendar){
            return InternalValue.create((Calendar) object);
        } else if (object instanceof String){
            return InternalValue.create((String) object, requiredType, namespaceResolver, contentStoreProvider);
        } else if (object instanceof Long){
            return InternalValue.create(((Long) object).longValue());
        } else if (object instanceof Double){
            return InternalValue.create(((Double) object).doubleValue());
        } else if (object instanceof Boolean){
            return InternalValue.create(((Boolean) object).booleanValue());
        } else {
            throw new UnsupportedOperationException();
        }
    }    
    
    
    @Deprecated
    public static List<DatabaseInsertStatement> createNodeStatement(Long nodeId, QName name,
            Long index, Long nodeTypeId, String nodePath, Long nodeDepth, Long parentId,
            Long securityId, Long parentLockId, Long workspaceId,Long storeConfigurationId,
            BaseNamespaceRegistryImpl nsRegistry, Long snsMax) {
    	ArrayList<DatabaseInsertStatement> result = new  ArrayList<DatabaseInsertStatement>();
    	
        DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(TABLE_NODE);
        insert.addValue(SQLParameter.create(FIELD_ID, nodeId));
        populateQName(insert, name, nsRegistry);
        insert.addValue(SQLParameter.create(TABLE_NODE__PARENT, parentId));
        insert.addValue(SQLParameter.create(TABLE_NODE__SECURITY_ID, securityId));
        insert.addValue(SQLParameter.create(TABLE_NODE__INDEX,index));
        insert.addValue(SQLParameter.create(TABLE_NODE__INDEX_MAX,snsMax));
        insert.addValue(SQLParameter.create(TABLE_NODE__VERSION_,new Long(1)));
        insert.addValue(SQLParameter.create(TABLE_NODE__NODE_TYPE,nodeTypeId));
        insert.addValue(SQLParameter.create(TABLE_NODE__NODE_PATH, nodePath));
        insert.addValue(SQLParameter.create(TABLE_NODE__NODE_DEPTH, nodeDepth));
        insert.addValue(SQLParameter.create(TABLE_NODE__WORKSPACE_ID, workspaceId));
        insert.addValue(SQLParameter.create(Constants.TABLE_NODE__CONTENT_STORE_CONFIG_NODE, storeConfigurationId));
        
        result.add(insert);
        
        insert = DatabaseTools.createInsertStatement(TABLE_NODE_LOCK);
        insert.addValue(SQLParameter.create(FIELD_TYPE_ID, nodeId));
        result.add(insert);
        
        insert = DatabaseTools.createInsertStatement(_TABLE_NODE_LOCK_INFO);
        insert.addValue(SQLParameter.create(FIELD_TYPE_ID, nodeId));
        insert.addValue(SQLParameter.create(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID, parentLockId));
        //insert.addValue(SQLParameter.create(TABLE_NODE_LOCK_INFO__PARENT_OWNER, parentLockId));
        result.add(insert);
        
        return result;
    }
    
    
    public static DatabaseInsertStatement createNodeTypeStatement(Long nodeId, Long nodeTypeId, Long fromNodeTypeId) {
        DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(TABLE_TYPE);
        //insert.addValue(SQLParameter.create(FIELD_ID, id));
        insert.addValue(SQLParameter.create(FIELD_TYPE_ID, nodeId));
        insert.addValue(SQLParameter.create(TABLE_TYPE__NODE_TYPE, nodeTypeId));
        insert.addValue(SQLParameter.create(TABLE_TYPE__FROM_NODE_TYPE, fromNodeTypeId));
        return insert;
    }

    public static DatabaseInsertStatement createNodeTypeDetailsStatement(Long nodeId, String tableName) {
        DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(tableName);
        insert.addValue(SQLParameter.create(FIELD_TYPE_ID, nodeId));
        return insert;
    }

    public static ArrayList<DatabaseStatement> createNewPropertyStatement(_PropertyState state, PropertyDefinitionImpl definition, 
    		DatabaseConnection conn, BaseNamespaceRegistryImpl nsRegistry, StoreContainer sc, 
    		ArrayList<DatabaseStatement> existingStatements, ChangeState changeState) throws ValueFormatException, RepositoryException {
        ArrayList<DatabaseStatement> result = new ArrayList<DatabaseStatement>();
        if (definition.isUnstructured()){
            Long unstructuredPropertyId = conn.nextId();
            state.setUnstructuredPropertyId(unstructuredPropertyId);
            if (definition.isMultiple()){
                //1. create unstructuredValue
                changeState.addNewUnstructuredMultiValueProperty(state, definition);
                //2. create all values
                //state.getValues()
                /*InternalValue values[] = state.getValues();
                for(int i = 0 ; i < values.length ; i++){
                    Value vv = values[i].toJCRValue(nsRegistry);
                    //save unstructured property
                    //TODO implement order
                    DatabaseInsertStatement st = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED_VALUES);
                    Long nextId = conn.nextId();
                    values[i].setSQLId(nextId);
                    st.addValue(SQLParameter.create(FIELD_ID, nextId));
                    st.addValue(SQLParameter.create(TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY, unstructuredPropertyId));
                    if (vv.getType() == PropertyType.LONG || vv.getType() == PropertyType.DATE){
                        st.addValue(SQLParameter._create(sc ,TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE, vv.getDouble(), values[i]));
                    }
                    st.addValue(SQLParameter._create(sc ,getValueColumn(vv.getType()), getValueObject(vv), values[i]));
                    result.add(st);
                }*/
                //throw new UnsupportedOperationException();
            } else {
                DatabaseInsertStatement st = DatabaseTools.createInsertStatement(TABLE_NODE_UNSTRUCTURED);
                st.addValue(SQLParameter.create(FIELD_ID, unstructuredPropertyId));
                st.addValue(SQLParameter.create(FIELD_TYPE_ID, state.getNodeId()));
                st.addValue(SQLParameter.create(TABLE_NODE_UNSTRUCTURED__NODE_TYPE, ((NodeTypeImpl)definition.getDeclaringNodeType()).getSQLId()));
                st.addValue(SQLParameter.create(TABLE_NODE_UNSTRUCTURED__PROP_DEF, definition.getSQLId()));
                populateQName(st, state.getName(), nsRegistry);
                st.addValue(SQLParameter.create(TABLE_NODE_UNSTRUCTURED__TYPE, new Long(state.getType()) ));
                st.addValue(SQLParameter.create(TABLE_NODE_UNSTRUCTURED__MULTIPLE, definition.isMultiple()));
                //value
                Value v = getPropertyValue(state, nsRegistry);
                st.addValue(SQLParameter._create(sc ,getValueColumn(v.getType()), getValueObject(v),  state.getValues()[0]));
                if (v.getType() == PropertyType.LONG || v.getType() == PropertyType.DATE){
                    st.addValue(SQLParameter._create(sc ,TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE, v.getDouble(), null));
                }
                result.add(st);
                //throw new UnsupportedOperationException();
            }
        } else {
            String tableName = ((NodeTypeImpl)definition.getDeclaringNodeType()).getTableName();
            String columnName = definition.getColumnName();
            DatabaseStatement st = null;
            st = JCRHelper.findPropertyStatement(state, existingStatements, tableName);
            //for single value property
            if (st == null){
            	st = DatabaseTools.createUpdateStatement(tableName,FIELD_TYPE_ID,state.getNodeId());
                result.add(st);
            }
           	((ValueChangeDatabaseStatement)st).addValue(SQLParameter._create(sc ,columnName ,getValueObject(getPropertyValue(state, nsRegistry)),  state.getValues()[0]));
            //if (true){
            //    throw new UnsupportedOperationException();
            //}
        }
        return result;
    }

    public static Value[] getValues(InternalValue[] values, NamespaceResolver nsResolver) throws RepositoryException {
        Value[] result = new Value[values.length];
        for(int i = 0 ; i < values.length ; i++){
            result[i] = values[i].toJCRValue(nsResolver);
        }
            
        return result;
    }
    
    public static String getValueColumn(int type) throws ValueFormatException, RepositoryException {
        return JCRHelper.getUnstructuredPropertyColumnNameByType(type);
    } 
    
    public static Object getValueObject(Value value) throws ValueFormatException, RepositoryException {
        Value v = value;
        switch (v.getType()) {
        case PropertyType.STRING:
            return value.getString();
        case PropertyType.BOOLEAN:
            return new Boolean(value.getBoolean());
        case PropertyType.DOUBLE:
            return new Double(value.getDouble());
        case PropertyType.LONG:
            return new Long(value.getLong());
        case PropertyType.NAME:
            return value.getString();
        case PropertyType.PATH:
            return value.getString();
        case PropertyType.DATE:
            return value.getDate();
        case PropertyType.REFERENCE:
            return value.getString();
        case PropertyType283.WEAKREFERENCE:
            return value.getString();
        case PropertyType.BINARY:
        	if (value instanceof BinaryValue){
        		BLOBFileValue content = ((BinaryValue)value).getBlobValue();
        		if (content != null){
        			String contentId = content.getContentId();
        			if (contentId != null){
        				return contentId;
        			}
        		}
        	}
            return value.getStream();

        default:
            throw new ValueFormatException("unsupported type : "+PropertyType.nameFromValue(value.getType()));
        }
    }    
    
    
    public static Value getPropertyValue(_PropertyState state, NamespaceResolver nsResolver) throws ValueFormatException, RepositoryException {

        // check multi-value flag
        if (state.getDefinition().isMultiple()) {
            throw new ValueFormatException(state.safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        try {
            InternalValue val = state.getValues()[0];
            return val.toJCRValue(nsResolver);
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            String msg = "Internal error while retrieving value of "
                    + state.safeGetJCRPath();
            throw new RepositoryException(msg, e);
        }
    }

    public static ArrayList<DatabaseStatement> createRemovedPropertyStatement(_PropertyState state, PropertyDefinitionImpl definition,
    		StoreContainer sc, ArrayList<DatabaseStatement> existingStatements)  throws RepositoryException{
        ArrayList<DatabaseStatement> result = new ArrayList<DatabaseStatement>();
        if (definition.isUnstructured()){
            if (definition.isMultiple()){
                //TODO remove values
                InternalValue[] iv = state.getInitialValues();
                if (iv != null){
	                for(int i = 0 ; i < iv.length ; i++){
	                    //TODO use batch delete be property id
	                    DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_NODE_UNSTRUCTURED_VALUES, FIELD_ID, iv[i].getSQLId());
	                    result.add(st);
	                }
                }
                //remove property
                DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_NODE_UNSTRUCTURED, FIELD_ID, state.getUnstructuredPropertyId());
                result.add(st);
            } else {
                DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_NODE_UNSTRUCTURED, FIELD_ID, state.getUnstructuredPropertyId());
                result.add(st);
            }
        } else {
            
            String tableName = ((NodeTypeImpl)definition.getDeclaringNodeType()).getTableName();
            String columnName = definition.getColumnName();
            DatabaseStatement st = null;
            st = JCRHelper.findPropertyStatement(state, existingStatements, tableName);
            if (st == null){
            	st = DatabaseTools.createUpdateStatement(tableName,FIELD_TYPE_ID,state.getNodeId());
                result.add(st);
            }
            ((ValueChangeDatabaseStatement)st).addValue(SQLParameter._create(sc ,columnName ,null, null));
        }
        return result;
    }
    
	public static DatabaseStatement findPropertyStatement(_PropertyState state, ArrayList<DatabaseStatement> existingStatements, String tableName) {
		return findPropertyStatement(state.getParent().getNodeId(), existingStatements, tableName);
	}

	public static DatabaseStatement findPropertyStatement(_NodeState state, ArrayList<DatabaseStatement> existingStatements, String tableName) {
		return findPropertyStatement(state.getNodeId(), existingStatements, tableName);
	}

	public static DatabaseStatement findPropertyStatement(Long nodeId, ArrayList<DatabaseStatement> existingStatements, String tableName) {
		DatabaseStatement st = null;
		for(DatabaseStatement _st:existingStatements){
			if (_st instanceof ValueChangeDatabaseStatement){
				ValueChangeDatabaseStatement vst = (ValueChangeDatabaseStatement) _st;
				if (vst.getOriginalTableName().equals(tableName)){
					if (vst instanceof DatabaseInsertStatement){
						DatabaseInsertStatement ist = (DatabaseInsertStatement)vst;
						for(SQLParameter p:ist.getValues()){
							if (p.getName().equals(Constants.FIELD_TYPE_ID) && p.getValue().equals(nodeId)){
		            			st = vst;
							}
						}
					} else if (vst instanceof DatabaseUpdateStatement){
						DatabaseUpdateStatement ust = (DatabaseUpdateStatement) vst;
						if (ust.getPkValue().equals(nodeId)){
							st = vst;
						}
					}
				}
			}
		}
		return st;
	}


    public static ArrayList<DatabaseStatement> createRemoveNodeStatements(NodeStateEx node, ArrayList<CacheKey> evictList) throws RepositoryException {
        /*ArrayList<DatabaseStatement> result = new ArrayList<DatabaseStatement>();
        DatabaseDeleteStatement st;
        //1.add remove parents
        //2. remove unstruct values multi
        //3. remove unstruct values single
        //4. remove types && type details
        for(Iterator it = node.getAllEffectiveTypes().iterator(); it.hasNext();){
            NodeTypeContainer type = (NodeTypeContainer) it.next();
            st = DatabaseTools.createDeleteStatement(type.getNodeType().getTableName(), FIELD_TYPE_ID, node.getNodeIdLong());
            result.add(st);
            st = DatabaseTools.createDeleteStatement(TABLE_TYPE, FIELD_TYPE_ID, node.getNodeIdLong());
            result.add(st);
        }
        //5 remove node
        evictList.add(new CacheKey(TABLE_NODE, node.getNodeIdLong()));
        node.getRepository().getCacheManager().evict(TABLE_NODE, node.getNodeIdLong());
        st = DatabaseTools.createDeleteStatement(TABLE_NODE, FIELD_ID, node.getNodeIdLong());
        result.add(st);
    
        return result;*/
    	throw new UnsupportedOperationException();
    }
    
    
    public static int getSQLTypeFromJCRType(int type) throws RepositoryException {
        if (type == PropertyType.STRING){
            return Types.VARCHAR;
        } else if (type == PropertyType.BINARY){
            return Types.VARCHAR;
        } else if (type == PropertyType.LONG){
            return Types.INTEGER;
        } else if (type == PropertyType.DOUBLE){
            return Types.FLOAT;
        } else if (type == PropertyType.DATE){
            return Types.TIMESTAMP;
        } else if (type == PropertyType.BOOLEAN){
            return Types.BOOLEAN;
        } else if (type == PropertyType.NAME){
            return Types.VARCHAR;
        } else if (type == PropertyType.PATH){
            return Types.VARCHAR;
        } else if (type == PropertyType.REFERENCE){
            return Types.VARCHAR;
        } else if (type == PropertyType283.WEAKREFERENCE){
            return Types.VARCHAR;
        } else if (type == PropertyType.UNDEFINED){
            return Types.VARCHAR;
        } else if (type == JCRPropertyType.TEXT){
            return Types.CLOB;
        } else {
            throw new RepositoryException("Can't assign SQL type for "+PropertyType.nameFromValue(type)); 
        } 
    }

    public static QName assembleQName(RowMap map, BaseNamespaceRegistryImpl namespaceRegistry) {
        return assembleQName(map.getString(FIELD_NAME), map.getLong(FIELD_NAMESPACE), namespaceRegistry);
    }

    public static QName assembleQName(String localName, Long namespaceId, BaseNamespaceRegistryImpl namespaceRegistry) {
        String uri = "";
        if (namespaceId != null){
            uri = namespaceRegistry._getById(namespaceId).getUri();            
        }
        /*if (localName == null){
            localName = "";
        }*/
        QName qname = new QName(uri, localName);
        return qname;
    }

    public static void populateQName(DatabaseInsertStatement insert, QName name, BaseNamespaceRegistryImpl ns) {
        ArrayList<SQLParameter> params = getSQLParametersFromQname(name, ns, true);
        for(SQLParameter p : params){
            insert.addValue(p);
        }
    }

    public static void populateQName(DatabaseInsertStatement insert, QName name, BaseNamespaceRegistryImpl ns,boolean skipEmptyNamespace) {
        ArrayList<SQLParameter> params = getSQLParametersFromQname(name, ns, skipEmptyNamespace);
        for(SQLParameter p : params){
            insert.addValue(p);
        }
    }

    public static void populateQName(DatabaseUpdateStatement st, QName name, BaseNamespaceRegistryImpl ns,boolean skipEmptyNamespace) {
        ArrayList<SQLParameter> params = getSQLParametersFromQname(name, ns, skipEmptyNamespace);
        for(SQLParameter p : params ){
            st.addCondition(Conditions.eq(p.getName(), p.getValue()));
        }
    }

    public static ArrayList<SQLParameter> getSQLParametersFromQname(QName _name, BaseNamespaceRegistryImpl ns, boolean skipEmptyNamespace) {
        ArrayList<SQLParameter> result = new ArrayList<SQLParameter>();
        
        String name = _name.getLocalName();
        String uri = _name.getNamespaceURI();
        Long namespaceId = null;
        if (uri != null && !uri.equals("")){
            namespaceId =  ns._getByURI(uri).getId();
        } 
        
        result.add(SQLParameter.create(FIELD_NAME, name));
        
        if (namespaceId != null || !skipEmptyNamespace){
            result.add(SQLParameter.create(FIELD_NAMESPACE, namespaceId));
        }
        return result;
    }

    public static void populateQNameCondition(DatabaseSelectAllStatement st, QName name, BaseNamespaceRegistryImpl ns) {
        ArrayList<SQLParameter> params = getSQLParametersFromQname(name, ns, true);
        for(SQLParameter p : params){
            st.addCondition(Conditions.eq(p.getName(), p.getValue()));
        }
    }    
    
    @SuppressWarnings("unchecked")
    public static Map<String, String> getPropertiesByPrefix(String prefix, Map configuration) {
        Map<String, String> result = new HashMap<String, String>();
        String s = prefix+".";
        for (String key : (Set<String>) configuration.keySet()) {
            if (key.startsWith(s)) {
                result.put(key.replaceFirst(s, ""), (String) configuration.get(key));
            }
        }           
        return result;
    }
    
    /**
     * Returns "submap" for given prefix, i.e. all key/value pares where key has specified prefix
     * @param prefix
     * @param configuration
     * @return Map for given prefix with prefix removed from keys
     */
    public static Map<String, String> getStringPropertiesByPrefix(String prefix, Map<String, String> configuration){
        Map<String, String> result = new HashMap<String, String>();
        Set<String> keySet = configuration.keySet();
        String s = prefix+".";
        for (String key: keySet) {
            if (key.startsWith(s)) {
                result.put(key.replaceFirst(s, ""), configuration.get(key));
            }
        }           
        return result;
        
    }
    
    
    /**
     * Perform MD5 hash of <code>password</code>
     * and return text representation of MD5 digest.
     *
     * @param   password    password to hash
     * @return  MD5 digest of password in string form.
     */
    public static final String hashPassword(String password) {                   
        try {
            java.security.MessageDigest md=java.security.MessageDigest.getInstance("MD5");
            byte[] input = password.getBytes("UTF-16");
            md.update(input);
            byte[] tmp = md.digest();
            return toHexString(tmp).toUpperCase();            
        } catch (Exception e) {            
            throw new RuntimeException("Unable to hash password.");
        }
    } 
    
    private static final String HEX_DIGITS = "0123456789abcdef";
    
    private static String toHexString(byte[] v) {
        StringBuffer sb = new StringBuffer(v.length * 2);
        for (int i = 0; i < v.length; i++) {
            int b = v[i] & 0xFF;
            sb.append(HEX_DIGITS.charAt(b >>> 4))
                .append(HEX_DIGITS.charAt(b & 0xF));
        }
        return sb.toString();
    }

    public static void populateQName(DatabaseUpdateStatement st, QName name, BaseNamespaceRegistryImpl ns) {
        ArrayList<SQLParameter> params = getSQLParametersFromQname(name, ns, true);
        for(SQLParameter p : params ){
            st.addValue(p);
        }
    }

	public static boolean isEmpty(String s){
		if (s == null || s.trim().equals(""))
			return true;
		return false;
	}

	@SuppressWarnings("unchecked")
    public static Object loadAndInstantiateClass(String className) throws RepositoryException {
	    Class theClass;
	    Object theInstance;
	    try {
	        theClass = Class.forName(className);
	    } catch (ClassNotFoundException e) {
	        throw new RepositoryException("Error loading  class "+className);
	    }
	    try {
	        theInstance = theClass.newInstance();
	    } catch (Exception e) {
	        throw new RepositoryException("Error instantiating class "+className);
	    }
	    return theInstance;
		
	}
    
    // Time Zone that should be used to store time in DB
    public static TimeZone getDBTimeZone(){
        return TimeZone.getTimeZone("GMT");
    }


    public static String saveContent(SessionImpl session, InputStream stream){
    	
    	return null;
    }
    
    public static final String allowedCharacters = "1234567890_qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";

	public static String excludeSpecialCharacters(String value) {
		StringBuffer result = new StringBuffer();
		char[] cc = value.toCharArray();
		for(int i = 0 ; i < cc.length ; i++){
			if (allowedCharacters.indexOf(cc[i]) < 0){
				result.append('_');
				result.append((int)cc[i]);
				result.append('_');
			} else {
				result.append(cc[i]);
			}
		}
		return result.toString();
	}


    public static InputStream getInputStream(String path, boolean required) throws RepositoryException {
        InputStream in = null;
        //File nodeTypeFile = new File(path);
        try {
        	File f = new File(path);
        	if (!f.exists()){
        		throw new FileNotFoundException(path);
        	}
            in = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            in = JCRHelper.class.getClassLoader().getResourceAsStream(path);
            if (in == null && required){
                //LogUtils.error(log, e.getMessage(), e);
                throw new RepositoryException("Resource not found : "+path);
            }
        }
        return in;
    }

	public static String getNodePath(Long nodeId, SessionImpl session, DatabaseConnection conn) {
		if (nodeId == null){
			return "null";
		}
		
		try {
			
			_SessionStateManager stM = session.getStateManager();
			_NodeState state = stM.getNodeFromCache(nodeId);
			String path = state.getInternalPath();
			path = convertPath(path);
			return path;
		} catch (Throwable e) {
		}
		
		
		try {
			RowMap row = conn.loadRow(Constants.TABLE_NODE, Constants.FIELD_ID, nodeId);
			String  path = row.getString(Constants.TABLE_NODE__NODE_PATH);
			path = convertPath(path);
			return path;
		} catch (Throwable e) {
		}
		
		
		return nodeId.toString();

	}

	
	public static final String convertPath(String p){
        String _path = p;
        //TODO optimize this
        _path = _path.replace(Constants.LEFT_INDEX,'[');
        _path = _path.replace(Constants.RIGHT_INDEX,']');
        return _path;
		
	}


    public static String convertPathToDBString(Path path, BaseNamespaceRegistryImpl namespaceRegistry) {
        StringBuffer newpath = new StringBuffer();
           Path.PathElement[] elements = path.getElements();
           
           for(Path.PathElement pe:elements){
               if (pe.denotesRoot()){
                   continue;
               }
               QName name = pe.getName();
               int index = pe.getIndex();
               String uri = name.getNamespaceURI();
               Namespace ns = null;
               if (uri != null && uri.length() > 0){
                    ns = namespaceRegistry._getByURI(uri);
               }
               newpath.append(PATH_DELIMITER);
               if (ns != null){
                   newpath.append(ns.getId());
                   newpath.append(":");
               }
               newpath.append(name.getLocalName());
               newpath.append(LEFT_INDEX);
               if (index == 0){
                   index = 1;
               }
               newpath.append(index);
               newpath.append(RIGHT_INDEX);
               
           }
           
           String pathStr = newpath.toString();
        return pathStr;
    }
	
    
    public static String getNodeTypeVersion(DatabaseConnection conn) throws RepositoryException{
        DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(TABLE_SYSTEM_PROPERTIES, FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__NT_VERSION);
    	st.execute(conn);
    	RowMap row = st.getRow();
    	String ver = row.getString(TABLE_SYSTEM_PROPERTIES__VALUE);
    	st.close();
    	return ver;
    	
    }
}

/*
 * $Log: JCRHelper.java,v $
 * Revision 1.9  2009/02/23 14:30:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2008/07/17 06:41:41  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/06/13 09:35:29  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2008/06/11 10:07:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2008/03/28 13:45:58  dparhomenko
 * Autmaticaly unlock orphaned locks(session scoped)
 *
 * Revision 1.1  2006/02/28 15:59:11  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */