/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package javax.jcr;

/**
 * The entry point into the content repository. The <code>Repository</code> object
 * is usually acquired through JNDI.
 */
public interface Repository283 {

    /**
     * The descriptor key for the version of the specification
     * that this repository implements. For JCR 2.0
     * the value of this descriptor is '2.0'.
     */
     public static final String SPEC_VERSION_DESC = "jcr.specification.version";

    /**
     * The descriptor key for the name of the specification
     * that this repository implements. For JCR 2.0
     * the value of this descriptor is 'Content Repository for
     * Java Technology API'.
     */
     public static final String SPEC_NAME_DESC = "jcr.specification.name";

    /**
     * The descriptor key for the name of the repository vendor.
     */
     public static final String REP_VENDOR_DESC = "jcr.repository.vendor";

    /**
     * The descriptor key for the URL of the repository vendor.
     */
     public static final String REP_VENDOR_URL_DESC = "jcr.repository.vendor.url";

    /**
     * The descriptor key for the name of this repository implementation.
     */
     public static final String REP_NAME_DESC = "jcr.repository.name";

    /**
     * The descriptor key for the version of this repository implementation.
     */
     public static final String REP_VERSION_DESC = "jcr.repository.version";

    /**
     * Indicates whether this implementation supports all level 1 features.
     * This descriptor should always be 'true'.
     */
     public static final String LEVEL_1_SUPPORTED = "level.1.supported";

    /**
     * Indicates whether this implementation supports all level 2 features.
     * This descriptor will be either 'true' or 'false'.
     */
     public static final String LEVEL_2_SUPPORTED = "level.2.supported";

    /**
     * Indicates whether this implementation supports node type registration.
     * This descriptor will be either 'true' or 'false'.
     */
     public static final String OPTION_NODE_TYPE_REG_SUPPORTED = "option.node.type.reg.supported";

    /**
     * Indicates whether this implementation supports transactions.
     * This descriptor will be either 'true' or 'false'.
     */
     public static final String OPTION_TRANSACTIONS_SUPPORTED = "option.transactions.supported";

    /**
     * Indicates whether this implementation supports versioning.
     * This descriptor will be either 'true' or 'false'.
     */
     public static final String OPTION_VERSIONING_SUPPORTED = "option.versioning.supported";

    /**
     * Indicates whether this implementation supports observation.
     * This descriptor will be either 'true' or 'false'.
     */
     public static final String OPTION_OBSERVATION_SUPPORTED = "option.observation.supported";

    /**
     * Indicates whether this implementation supports locking.
     * This descriptor will be either 'true' or 'false'.
     */
     public static final String OPTION_LOCKING_SUPPORTED = "option.locking.supported";

    /**
     * Indicates whether this implementation supports the SQL query language.
     * This descriptor will be either 'true' or 'false'.
     */
     public static final String OPTION_QUERY_SQL_SUPPORTED = "option.query.sql.supported";

    /**
     * Indicates whether this implementation supports access control discovery.
     * This descriptor will be either 'true' or 'false'.
     */
    public static final String OPTION_AC_DISCOVERY_SUPPORTED = "option.ac.discovery.supported";

    /**
     * Indicates whether this implementation supports access control management.
     * This descriptor will be either 'true' or 'false'.
     */
    public static final String OPTION_AC_MANAGEMENT_SUPPORTED = "option.ac.management.supported";

    /**
     * Indicates whether this implementation supports lifecycle management.
     * This descriptor will be either 'true' or 'false'.
     */
    public static final String OPTION_LIFECYCLE_SUPPORTED = "option.lifecycle.supported";

    /**
     * Indicates whether the index position notation for
     * same-name siblings is supported within XPath queries.
     * This descriptor will be either 'true' or 'false'.
     */
     public static final String QUERY_XPATH_POS_INDEX = "query.xpath.pos.index";

    /**
     * Indicates whether XPath queries return results in document order.
     * This descriptor will be either 'true' or 'false'.
     */
     public static final String QUERY_XPATH_DOC_ORDER = "query.xpath.doc.order";

    /**
     * Returns a string array holding all descriptor keys available for this implementation.
     * This set must contain at least the built-in keys defined by the string constants in
     * this interface. Used in conjunction with {@link #getDescriptor(String name)}
     * to query information about this repository implementation.
     */
    public String[] getDescriptorKeys();

    /**
     * Returns the descriptor for the specified key. Used to query information about this
     * repository implementation. The set of available keys can be found by calling
     * {@link #getDescriptorKeys}. If the specifed key is not found, <code>null</code> is returned.
     *
     * @param key a string corresponding to a descriptor for this repository implementation.
     * @return a descriptor string
     */
    public String getDescriptor(String key);

    /**
     * Authenticates the user using the supplied <code>credentials</code>. If <code>workspaceName</code> is
     * recognized as the name of an existing workspace in the repository and
     * authorization to access that workspace is granted, then a new <code>Session</code> object is returned.
     * The format of the string <code>workspaceName</code> depends upon the implementation.
     * <p>
     * If <code>credentials</code> is <code>null</code>, it is assumed that authentication is handled by a
     * mechanism external to the repository itself (for example, through the JAAS framework) and that the
     * repository implementation exists within a context (for example, an application server) that allows
     * it to handle authorization of the request for access to the specified workspace.
     * <p>
     * If <code>workspaceName</code> is <code>null</code>, a default workspace is automatically selected by
     * the repository implementation. This may, for example, be the "home workspace" of the user whose
     * credentials were passed, though this is entirely up to the configuration and implementation of the
     * repository. Alternatively, it may be a "null workspace" that serves only to provide the method
     * {@link Workspace#getAccessibleWorkspaceNames}, allowing the client to select from among available "real"
     * workspaces.
     * <p>
     * If authentication or authorization for the specified workspace fails, a <code>LoginException</code> is
     * thrown.
     * <p>
     * If <code>workspaceName</code> is not recognized, a <code>NoSuchWorkspaceException</code> is thrown.
     *
     * @param credentials   The credentials of the user
     * @param workspaceName the name of a workspace.
     * @return a valid session for the user to access the repository.
     * @throws LoginException  If the login fails.
     * @throws NoSuchWorkspaceException If the specified <code>workspaceName</code> is not recognized.
     * @throws RepositoryException if another error occurs.
     */
    public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException;

    /**
     * Equivalent to <code>login(credentials, null)</code>.
     *
     * @param credentials   The credentials of the user
     * @return a valid session for the user to access the repository.
     * @throws LoginException  If the login authentication fails.
     * @throws RepositoryException if another error occurs.
     */
    public Session login(Credentials credentials) throws LoginException, RepositoryException;

    /**
     * Equivalent to <code>login(null, workspaceName)</code>.
     *
     * @param workspaceName the name of a workspace.
     * @return a valid session for the user to access the repository.
     * @throws LoginException  If the login authentication fails.
     * @throws NoSuchWorkspaceException If the specified <code>workspaceName</code> is not recognized.
     * @throws RepositoryException if another error occurs.
     */
    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException;

    /**
     * Equivalent to <code>login(null, null)</code>.
     *
     * @return a valid session for the user to access the repository.
     * @throws LoginException  If the login authentication fails.
     * @throws RepositoryException if another error occurs.
     */
    public Session login() throws LoginException, RepositoryException;
}
