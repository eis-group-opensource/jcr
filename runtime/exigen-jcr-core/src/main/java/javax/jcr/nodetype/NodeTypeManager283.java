/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package javax.jcr.nodetype;

import java.util.Collection;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;

/**
 * Allows for the retrieval and (in implementations that support it)
 * the registration of node types. Accessed via
 * {@link Workspace#getNodeTypeManager}.
 */
public interface NodeTypeManager283 {

    /**
     * Returns the named node type.
     * <p>
     * Throws a <code>NoSuchNodeTypeException</code> if a node type by that name does not exist.
     * <p>
     * Throws a <code>RepositoryException</code> if another error occurs.
     *
     * @param nodeTypeName the name of an existing node type.
     * @return A <code>NodeType</code> object.
     * @throws NoSuchNodeTypeException if no node type by the given name exists.
     * @throws RepositoryException if another error occurs.
     */
    public NodeType getNodeType(String nodeTypeName) throws NoSuchNodeTypeException, RepositoryException;

    /**
     * Returns an iterator over all available node types (primary and mixin).
     *
     * @return An <code>NodeTypeIterator</code>.
     *
     * @throws RepositoryException if an error occurs.
     */
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException;

    /**
     * Returns an iterator over all available primary node types.
     *
     * @return An <code>NodeTypeIterator</code>.
     *
     * @throws RepositoryException if an error occurs.
     */
    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException;

    /**
     * Returns an iterator over all available mixin node types.
     * If none are available, an empty iterator is returned.
     *
     * @return An <code>NodeTypeIterator</code>.
     *
     * @throws RepositoryException if an error occurs.
     */
    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException;

    /**
     * Returns an empty <code>NodeTypeTemplate</code> which can then be used to
     * define a node type and passed to
     * <code>NodeTypeManager.registerNodeType</code>.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     * @return A <code>NodeTypeTemplate</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    public NodeTypeTemplate createNodeTypeTemplate() throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Returns a <code>NodeTypeTemplate</code> holding the definition of the
     * specified node type. This template can then be altered and passed to
     * <code>NodeTypeManager.registerNodeType</code>.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @param nodeType a <code>NodeType</code>.
     * @return A <code>NodeTypeTemplate</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    public NodeTypeTemplate createNodeTypeTemplate(NodeType nodeType) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Returns an empty <code>NodeDefinitionTemplate</code> which can then be
     * used to create a child node definition and attached to a
     * <code>NodeTypeTemplate</code>.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @return A <code>NodeDefinitionTemplate</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    public NodeDefinitionTemplate createNodeDefinitionTemplate() throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Returns an empty <code>PropertyDefinitionTemplate</code> which can then
     * be used to create a property definition and attached to a
     * <code>NodeTypeTemplate</code>.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @return A <code>PropertyDefinitionTemplate</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    public PropertyDefinitionTemplate createPropertyDefinitionTemplate() throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Registers a new node type or updates an existing node type using the
     * specified definition and returns the resulting <code>NodeType</code>
     * object.
     * <p/>
     * Typically, the object passed to this method will be a
     * <code>NodeTypeTemplate</code> (a subclass of
     * <code>NodeTypeDefinition</code>) acquired from
     * <code>NodeTypeManager.createNodeTypeTemplate</code> and then filled-in
     * with definition information.
     * <p/>
     * Throws an <code>InvalidNodeTypeDefinitionException</code> if the
     * <code>NodeTypeDefinition</code> is invalid.
     * <p/>
     * Throws a <code>NodeTypeExistsException</code> if <code>allowUpdate</code>
     * is <code>false</code> and the <code>NodeTypeDefinition</code> specifies a
     * node type name that is already registered.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @param ntd an <code>NodeTypeDefinition</code>.
     * @param allowUpdate a boolean
     * @return the registered node type
     * @throws InvalidNodeTypeDefinitionException if the
     *  <code>NodeTypeDefinition</code> is invalid.
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is
     *  <code>false</code> and the <code>NodeTypeDefinition</code> specifies a
     *  node type name that is already registered.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    public NodeType registerNodeType(NodeTypeDefinition ntd, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Registers or updates the specified <code>Collection</code> of
     * <code>NodeTypeDefinition</code> objects. This method is used to register
     * or update a set of node types with mutual dependencies. Returns an
     * iterator over the resulting <code>NodeType</code> objects.
     * <p/>
     * The effect of the method is 'all or nothing' if an error occurs, no node
     * types are registered or updated.
     * <p/>
     * Throws an <code>InvalidNodeTypeDefinitionException</code> if a
     * <code>NodeTypeDefinition</code> within the <code>Collection</code> is
     * invalid or if the <code>Collection</code> contains an object of a type
     * other than <code>NodeTypeDefinition</code>.
     * <p/>
     * Throws a <code>NodeTypeExistsException</code> if <code>allowUpdate</code>
     * is <code>false</code> and a <code>NodeTypeDefinition</code> within the
     * <code>Collection</code> specifies a node type name that is already
     * registered.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @param definitions
     * @param allowUpdate
     * @return the registered node types.
     * @throws InvalidNodeTypeDefinitionException if a
     *  <code>NodeTypeDefinition</code> within the <code>Collection</code> is
     *  invalid or if the <code>Collection</code> contains an object of a type
     *  other than <code>NodeTypeDefinition</code>.
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is
     *  <code>false</code> and a <code>NodeTypeDefinition</code> within the
     *  <code>Collection</code> specifies a node type name that is already
     *  registered.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    public NodeTypeIterator registerNodeTypes(Collection definitions, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Unregisters the specified node type.
     * <p/>
     * Throws a <code>NoSuchNodeTypeException</code> if no registered node type
     * exists with the specified name.
     *
     * @param name a <code>String</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws NoSuchNodeTypeException if no registered node type exists with
     *  the specified name.
     * @throws RepositoryException if another error occurs.
     */
    public void unregisterNodeType(String name) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException;

    /**
     * Unregisters the specified set of node types. Used to unregister a set of node types with mutual dependencies.
     * <p/>
     * Throws a <code>NoSuchNodeTypeException</code> if one of the names listed is not a registered node type.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this implementation does not support node type registration.
     *
     * @param names a <code>String</code> array
     * @throws UnsupportedRepositoryOperationException if this implementation does not support node type registration.
     * @throws NoSuchNodeTypeException if one of the names listed is not a registered node type.
     * @throws RepositoryException if another error occurs.
     */
    public void unregisterNodeTypes(String[] names) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException;
}