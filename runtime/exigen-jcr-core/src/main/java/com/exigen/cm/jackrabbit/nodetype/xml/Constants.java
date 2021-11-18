/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype.xml;

/**
 * Name constants for the node type XML elements and attributes.
 */
public interface Constants {

    /** Name of the node type definition root element. */
    String NODETYPES_ELEMENT = "nodeTypes";

    /** Name of the node type definition element. */
    String NODETYPE_ELEMENT = "nodeType";

    /** Name of the child node definition element. */
    String CHILDNODEDEFINITION_ELEMENT = "childNodeDefinition";

    /** Name of the property definition element. */
    String PROPERTYDEFINITION_ELEMENT = "propertyDefinition";

    /** Name of the <code>isMixin</code> attribute. */
    String ISMIXIN_ATTRIBUTE = "isMixin";

    /** Name of the <code>hasOrderableChildNodes</code> attribute. */
    String HASORDERABLECHILDNODES_ATTRIBUTE = "hasOrderableChildNodes";

    /** Name of the primary item name attribute. */
    String PRIMARYITEMNAME_ATTRIBUTE = "primaryItemName";
    
    /** Name of the table name attribute. */
    String TABLENAME_ATTRIBUTE = "tableName";
    
    /** Name of the column name attribute. */
    String COLUMNNAME_ATTRIBUTE = "columnName";    

    /** Name of the column name attribute. */
    String COLUMNNAME_INDEXABLE = "indexable";    
    String COLUMNNAME_INDEXABLE_FTS = "fts";    

    /** Name of the supertypes element. */
    String SUPERTYPES_ELEMENT = "supertypes";

    /** Name of the supertype element. */
    String SUPERTYPE_ELEMENT = "supertype";

    /** Name of the <code>name</code> attribute. */
    String NAME_ATTRIBUTE = "name";

    /** Name of the <code>autoCreated</code> attribute. */
    String AUTOCREATED_ATTRIBUTE = "autoCreated";

    /** Name of the <code>mandatory</code> attribute. */
    String MANDATORY_ATTRIBUTE = "mandatory";

    /** Name of the <code>onParentVersion</code> attribute. */
    String ONPARENTVERSION_ATTRIBUTE = "onParentVersion";

    /** Name of the <code>protected</code> attribute. */
    String PROTECTED_ATTRIBUTE = "protected";

    /** Name of the required type attribute. */
    String REQUIREDTYPE_ATTRIBUTE = "requiredType";

    /** Name of the value constraints element. */
    String VALUECONSTRAINTS_ELEMENT = "valueConstraints";

    /** Name of the value constraint element. */
    String VALUECONSTRAINT_ELEMENT = "valueConstraint";

    /** Name of the default values element. */
    String DEFAULTVALUES_ELEMENT = "defaultValues";

    /** Name of the default value element. */
    String DEFAULTVALUE_ELEMENT = "defaultValue";

    /** Name of the <code>multiple</code> attribute. */
    String MULTIPLE_ATTRIBUTE = "multiple";

    /** Name of the required primary types element. */
    String REQUIREDPRIMARYTYPES_ELEMENT = "requiredPrimaryTypes";

    /** Name of the required primary type element. */
    String REQUIREDPRIMARYTYPE_ELEMENT = "requiredPrimaryType";

    /** Name of the default primary type attribute. */
    String DEFAULTPRIMARYTYPE_ATTRIBUTE = "defaultPrimaryType";

    /** Name of the <code>sameNameSiblings</code> attribute. */
    String SAMENAMESIBLINGS_ATTRIBUTE = "sameNameSiblings";

}
