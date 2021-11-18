/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.IndexDefinition;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.jackrabbit.name.QName;

/**
 * A <code>NodeTypeDef</code> holds the definition of a node type.
 */
public class NodeTypeDef implements Cloneable {

    private QName name;

    private QName[] supertypes;
    private boolean mixin;
    private boolean orderableChildNodes;
    private QName primaryItemName;
    private HashSet<PropDef> propDefs;
    private HashSet<NodeDef> nodeDefs;
    //private Set dependencies;

    //private boolean isNew = true;
    private String tableName;
    private Long id;
	private String presenceColumn;
	
    /**
     * Default constructor.
     */
    public NodeTypeDef() {
        //dependencies = null;
        name = null;
        primaryItemName = null;
        nodeDefs = new HashSet<NodeDef>();
        propDefs = new HashSet<PropDef>();
        supertypes = QName.EMPTY_ARRAY;
        mixin = false;
        orderableChildNodes = false;
    }

    /**
     * Returns a collection of node type <code>QName</code>s that are being
     * referenced by <i>this</i> node type definition (e.g. as supertypes, as
     * required/default primary types in child node definitions, as REFERENCE
     * value constraints in property definitions).
     * <p/>
     * Note that self-references (e.g. a child node definition that specifies
     * the declaring node type as the default primary type) are not considered
     * dependencies.
     *
     * @return a collection of node type <code>QName</code>s
     */
    /*public Collection getDependencies() {
        if (dependencies == null) {
            dependencies = new HashSet();
            // supertypes
            dependencies.addAll(Arrays.asList(supertypes));
            // child node definitions
            for (Iterator iter = nodeDefs.iterator(); iter.hasNext();) {
                NodeDef nd = (NodeDef) iter.next();
                // default primary type
                QName ntName = nd.getDefaultPrimaryType();
                if (ntName != null && !name.equals(ntName)) {
                    dependencies.add(ntName);
                }
                // required primary type
                QName[] ntNames = nd.getRequiredPrimaryTypes();
                for (int j = 0; j < ntNames.length; j++) {
                    if (ntNames[j] != null && !name.equals(ntNames[j])) {
                        dependencies.add(ntNames[j]);
                    }
                }
            }
            // property definitions
            for (Iterator iter = propDefs.iterator(); iter.hasNext();) {
                PropDef pd = (PropDef) iter.next();
                // REFERENCE value constraints
                if (pd.getRequiredType() == PropertyType.REFERENCE) {
                    ValueConstraint[] ca = pd.getValueConstraints();
                    if (ca != null) {
                        for (int j = 0; j < ca.length; j++) {
                            ReferenceConstraint rc = (ReferenceConstraint) ca[j];
                            if (!name.equals(rc.getNodeTypeName())) {
                                dependencies.add(rc.getNodeTypeName());
                            }
                        }
                    }
                }
            }
        }
        return dependencies;
    }

    private void resetDependencies() {
        dependencies = null;
    }*/

    //----------------------------------------------------< setters & getters >
    /**
     * Sets the name of the node type being defined.
     *
     * @param name The name of the node type.
     */
    public void setName(QName name) {
        this.name = name;
    }

    /**
     * Sets the supertypes.
     *
     * @param names the names of the supertypes.
     */
    public void setSupertypes(QName[] names) {
        //resetDependencies();
        // Optimize common cases (zero or one supertypes)
        if (names.length == 0) {
            supertypes = QName.EMPTY_ARRAY;
        } else if (names.length == 1) {
            supertypes = new QName[] { names[0] };
        } else {
            // Sort and remove duplicates
            SortedSet types = new TreeSet();
            types.addAll(Arrays.asList(names));
            supertypes = (QName[]) types.toArray(new QName[types.size()]);
        }
    }

    /**
     * Sets the mixin flag.
     *
     * @param mixin flag
     */
    public void setMixin(boolean mixin) {
        this.mixin = mixin;
    }

    /**
     * Sets the orderableChildNodes flag.
     *
     * @param orderableChildNodes flag
     */
    public void setOrderableChildNodes(boolean orderableChildNodes) {
        this.orderableChildNodes = orderableChildNodes;
    }

    /**
     * Sets the name of the primary item (one of the child items of the node's
     * of this node type)
     *
     * @param primaryItemName The name of the primary item.
     */
    public void setPrimaryItemName(QName primaryItemName) {
        this.primaryItemName = primaryItemName;
    }

    /**
     * Sets the property definitions.
     *
     * @param defs An array of <code>PropertyDef</code> objects.
     */
    public void setPropertyDefs(PropDef[] defs) {
        //resetDependencies();
        propDefs.clear();
        propDefs.addAll(Arrays.asList(defs));
        for(PropDef propDef:defs){
            if (propDef.getDeclaringNodeType() == null){
            	((PropDefImpl) propDef).setDeclaringNodeType(getName());
            }

        }
    }

    /**
     * Sets the child node definitions.
     *
     * @param defs An array of <code>NodeDef</code> objects
     */
    public void setChildNodeDefs(NodeDef[] defs) {
        //resetDependencies();
        nodeDefs.clear();
        nodeDefs.addAll(Arrays.asList(defs));
    }

    /**
     * Returns the name of the node type being defined or
     * <code>null</code> if not set.
     *
     * @return the name of the node type or <code>null</code> if not set.
     */
    public QName getName() {
        return name;
    }

    /**
     * Returns an array containing the names of the supertypes. If no
     * supertypes have been specified, then an empty array is returned
     * for mixin types and the <code>nt:base</code> primary type and
     * an array containing just <code>nt:base<code> for other primary types.
     * <p>
     * The returned array must not be modified by the application.
     *
     * @return a sorted array of supertype names
     */
    public QName[] getSupertypes() {
        if (supertypes.length > 0
                || isMixin() || QName.NT_BASE.equals(getName())) {
            return supertypes;
        } else {
            return new QName[] { QName.NT_BASE };
        }
    }

    /**
     * Returns the value of the mixin flag.
     *
     * @return true if this is a mixin node type; false otherwise.
     */
    public boolean isMixin() {
        return mixin;
    }

    /**
     * Returns the value of the orderableChildNodes flag.
     *
     * @return true if nodes of this node type can have orderable child nodes; false otherwise.
     */
    public boolean hasOrderableChildNodes() {
        return orderableChildNodes;
    }

    /**
     * Returns the name of the primary item (one of the child items of the
     * node's of this node type) or <code>null</code> if not set.
     *
     * @return the name of the primary item or <code>null</code> if not set.
     */
    public QName getPrimaryItemName() {
        return primaryItemName;
    }

    /**
     * Returns an array containing the property definitions or
     * <code>null</code> if not set.
     *
     * @return an array containing the property definitions or
     *         <code>null</code> if not set.
     */
    public PropDef[] getPropertyDefs() {
        if (propDefs.isEmpty()) {
            return PropDef.EMPTY_ARRAY;
        }
        return (PropDef[]) propDefs.toArray(new PropDef[propDefs.size()]);
    }

    /**
     * Returns an array containing the child node definitions or
     * <code>null</code> if not set.
     *
     * @return an array containing the child node definitions or
     *         <code>null</code> if not set.
     */
    public NodeDef[] getChildNodeDefs() {
        if (nodeDefs.isEmpty()) {
            return NodeDef.EMPTY_ARRAY;
        }
        return (NodeDef[]) nodeDefs.toArray(new NodeDef[nodeDefs.size()]);
    }

    //-------------------------------------------< java.lang.Object overrides >
    public Object clone() {
        NodeTypeDef clone = new NodeTypeDef();
        clone.name = name;
        clone.primaryItemName = primaryItemName;
        clone.supertypes = supertypes; // immutable, thus ok to share
        clone.mixin = mixin;
        clone.orderableChildNodes = orderableChildNodes;
        clone.nodeDefs = (HashSet) nodeDefs.clone();
        clone.propDefs = (HashSet) propDefs.clone();
        
        //clone.isNew = isNew;
        clone.tableName = tableName;
        clone.id = id;
        clone.presenceColumn = presenceColumn;
        
        return clone;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeTypeDef) {
            NodeTypeDef other = (NodeTypeDef) obj;
            return (name == null ? other.name == null : name.equals(other.name))
                    && (primaryItemName == null ? other.primaryItemName == null : primaryItemName.equals(other.primaryItemName))
                    && Arrays.equals(getSupertypes(), other.getSupertypes())
                    && mixin == other.mixin
                    && orderableChildNodes == other.orderableChildNodes
                    && propDefs.equals(other.propDefs)
                    && nodeDefs.equals(other.nodeDefs);
        }
        return false;
    }

    /*
     * original method
     *     public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeTypeDef) {
            NodeTypeDef other = (NodeTypeDef) obj;
            return (name == null ? other.name == null : name.equals(other.name))
                    && (primaryItemName == null ? other.primaryItemName == null : primaryItemName.equals(other.primaryItemName))
                    && Arrays.equals(getSupertypes(), other.getSupertypes())
                    && mixin == other.mixin
                    && orderableChildNodes == other.orderableChildNodes
                    && propDefs.equals(other.propDefs)
                    && nodeDefs.equals(other.nodeDefs);
        }
        return false;
    }
     * 
     * 
     */
    
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
    
    
    public TableDefinition getTableDefinition(ArrayList<TableDefinition> existingTables, DatabaseConnection conn) throws RepositoryException {
    	String tableName = getTableName(); 
        TableDefinition table = new TableDefinition(tableName);
        TableDefinition nodesTable=new TableDefinition(Constants.TABLE_NODE,true);
        boolean present = false;
        for(TableDefinition td: existingTables){
        	if (td.getTableName().equals(tableName)){
        		present = true;
        		break;
        	}
        }
        if (present){
        	table.setAlter(true);
        } else {
        	ColumnDefinition col=new ColumnDefinition(table, Constants.FIELD_TYPE_ID, Types.INTEGER, true);
        	// do not create index due to FK - this column is PK too
        	boolean restoreAutoCreate=false; 
        	if (table.isAutoCreateIndex()){
        		restoreAutoCreate=true;
        		table.setAutoCreateIndex(false);
        	}
        	col.setForeignKey(nodesTable);
        	if (restoreAutoCreate){
        		table.setAutoCreateIndex(true);
        	}
	        table.addColumn(col);
        }
        presenceColumn = conn.getDialect().convertColumnName(getPresenceColumn());
        setPresenceColumn(presenceColumn);
        
        table.addColumn(new ColumnDefinition(table, presenceColumn, Types.BOOLEAN));
        PropDef[] props = getPropertyDefs();
        //QName tmpName = Constants.VF_RESUORCE;
        //QName tmpName1 = QName.NT_RESOURCE;
        for(int i = 0 ; i <props.length ; i++){
            PropDef prop = props[i];
            if (prop.getColumnName() != null){
                ColumnDefinition column = new ColumnDefinition(table, prop.getColumnName(), JCRHelper.getSQLTypeFromJCRType(prop.getRequiredType()));
                table.addColumn(column);
                /*if (prop.getName().getLocalName().equalsIgnoreCase("DATA") && (name.equals(tmpName) || name.equals(tmpName1))){
                    ColumnDefinition column1 = new ColumnDefinition(table, Constants.FIELD_FTS_DATA, 
                            Types.CLOB);
                    table.addColumn(column1);
                    //CREATE INDEX CMV_RES_IDX ON CMV_RESOURCE(X_DATA_TEXT) INDEXTYPE IS ctxsys.context;
                    IndexDefinition idf = new IndexDefinition(table);
                    idf.setName(getTableName().toUpperCase()+"_IDX_FTS");
                    idf.addColumn(column1);
                    idf.setFullTextSearch(true);
                    table.addIndexDefinition(idf);
                }*/

                if (prop.isIndexable()){
                    IndexDefinition idx = new IndexDefinition(table);
                    idx.addColumn(prop.getColumnName());
                    idx.setName(getTableName()+"_"+prop.getColumnName());
                    table.addIndexDefinition(idx);
                }
                /*if (prop.isFullTestSearch()){
                    IndexDefinition idf = new IndexDefinition(table);
                    idf.setName(getTableName().toUpperCase()+"_IDX_FTS");
                    idf.addColumn(column);
                    idf.setFullTextSearch(true);
                    table.addIndexDefinition(idf);
                }*/
            }
        }
        return table;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/*public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}*/

	public String getPresenceColumn() {
		return presenceColumn;
	}

	public void setPresenceColumn(String presenceColumn) {
		this.presenceColumn = presenceColumn;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
		if ("".equals(this.tableName)){
			this.tableName = null;
		}
	}

    /*public void loadedFromDB() {
        this.isNew = false;
    } */   


    @Deprecated
	public void addChildNodeDefs(NodeDefImpl nd) {
    	nodeDefs.add(nd);
		if (nd.getDeclaringNodeType() == null){
			nd.setDeclaringNodeType(getName());
		}
	}
    
    @Deprecated
    public void addPropDef(PropDefImpl propDef) {
    	propDefs.add(propDef);
        if (propDef.getDeclaringNodeType() == null){
        	((PropDefImpl) propDef).setDeclaringNodeType(getName());
        }
    }

    @Deprecated
	public void removePropDef(PropDef pd) {
    	propDefs.remove(pd);
	}

	public int generateHashCode() {
		HashCodeBuilder builder = new  HashCodeBuilder();
		builder.append(mixin);
		builder.append(name);
		builder.append(orderableChildNodes);
		builder.append(primaryItemName);
		//supertypes
		ArrayList<QName> st = new ArrayList<QName>(Arrays.asList(supertypes));
		Collections.sort(st);
		builder.append(st);
		//nodeDefs
		ArrayList<NodeDef> nd = new ArrayList<NodeDef>(nodeDefs);
		Collections.sort(nd, new Comparator<NodeDef>(){
			public int compare(NodeDef o1, NodeDef o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		for(NodeDef d:nd){
			builder.append(d.generateHashCode());
		}

		//propdefs
		ArrayList<PropDef> pd = new ArrayList<PropDef>(propDefs);
		Collections.sort(pd, new Comparator<PropDef>(){
			public int compare(PropDef o1, PropDef o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		for(PropDef d:pd){
			builder.append(d.generateHashCode());
		}
		
		return builder.toHashCode();
	}



}
