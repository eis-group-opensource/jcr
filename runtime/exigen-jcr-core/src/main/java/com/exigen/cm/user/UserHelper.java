/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.user;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.JCRHelper;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.vf.commons.logging.LogUtils;

public class UserHelper {
    
    private static Log log = LogFactory.getLog(UserHelper.class);
    
    public static final String PRINCIPALS_NODE_TYPE = "rep:principal";
    
    public static final String USER_NODE_TYPE = "rep:user";
    public static final String USER_PASSWORD_PROPERTY = "rep:password";
    public static final String USER_FULLNAME_PROPERTY = "rep:fullname";
    
    public static final String GROUP_NODE_TYPE = "rep:group";
    public static final String GROUP_MEMBER_PROPERTY = "rep:members";
    public static final String GROUP_DESCRIPTION_PROPERTY = "rep:description";
    
    public static final String PRINCIPALS_NODE_PATH = "rep:principals";    
    public static final String USERS_NODE_PATH = PRINCIPALS_NODE_PATH+"/rep:users";
    public static final String GROUPS_NODE_PATH = PRINCIPALS_NODE_PATH+"/rep:groups";   
    
  
    
    public static void addUser(String userName, String password, Repository repository) throws RepositoryException {
        addUser(userName, null, password, repository);
    }    
    
    public static void addUser(String userName, String fullName, String password, Repository repository) throws RepositoryException {
        Session session = getSession(repository);
        Node user;
        try {
            Node usersNode = getUsersNode(session);
            try {
                user = usersNode.addNode(userName, USER_NODE_TYPE);
            } catch (ItemExistsException e) {
                String msg = "User with the user name \"{0}\" already exists!";
                msg = MessageFormat.format(msg, new Object[]{userName});
                LogUtils.error(log, msg);
                throw new RepositoryException(msg);
            }
            if (password == null) {
                password = "";
            }
            user.setProperty(USER_PASSWORD_PROPERTY, JCRHelper.hashPassword(password));
            if (fullName != null) {
                user.setProperty(USER_FULLNAME_PROPERTY, fullName);
            }
            session.save();
        } finally {
            session.logout();
        }
    }
    
    public static void addGroup(String groupName, Repository repository)  throws RepositoryException {
        addGroup(groupName, null, repository);
    }
    
    public static void addGroup(String groupName, String description, Repository repository)  throws RepositoryException {
        Session session = getSession(repository);
        Node groupsNode = getGroupsNode(session);
        Node group = null;
        try {
            try {            
                group = groupsNode.addNode(groupName, GROUP_NODE_TYPE);
                group.setProperty(GROUP_MEMBER_PROPERTY, new Value[]{});
            } catch (ItemExistsException e) {
                String msg = "Group with the name \"{0}\" already exists!";
                msg = MessageFormat.format(msg, new Object[]{groupName});
                LogUtils.error(log, msg);
                throw new RepositoryException(msg);
            }        
            if (description != null) {
                group.setProperty(GROUP_DESCRIPTION_PROPERTY, description);
            }
            session.save();            
        } finally {
            session.logout();
        }

    }    
    
    public static void removeUser(String userName, Repository repository) throws RepositoryException {
        Session session = getSession(repository);
        try {
            session.getItem("/"+USERS_NODE_PATH+"/"+userName).remove();
            session.save();
        } finally {
            session.logout();
        }
    }
    
    public static void removeGroup(String groupName, Repository repository) throws RepositoryException {
        Session session = getSession(repository);
        try {
            session.getItem("/"+GROUPS_NODE_PATH+"/"+groupName).remove();
            session.save();
        } finally {
            session.logout();
        }        
    }
    
    public static void assignUserToGroup(String userName, String groupName, Repository repository) throws RepositoryException {
        Session session = getSession(repository);
        try {
            Node usersNode = getUsersNode(session);
            Node groupsNode = getGroupsNode(session);        
            Node user = usersNode.getNode(userName);
            Value userReference = session.getValueFactory().createValue(user);
            Node parentGroup = groupsNode.getNode(groupName);
            addRefereceToGroup(userReference, parentGroup);
            session.save();
        } finally {
            session.logout();
        }
    }
    
    public static void assignGroupToGroup(String parentGroupName, String childGroupName, Repository repository) throws RepositoryException {
        Session session = getSession(repository);
        try {
            Node groupsNode = getGroupsNode(session);              
            Node childGroup = groupsNode.getNode(childGroupName);        
            Node parentGroup = groupsNode.getNode(parentGroupName);
            if (!isChildNodeAmongParents(parentGroup, childGroup)) {
                Value groupReference = session.getValueFactory().createValue(childGroup);
                addRefereceToGroup(groupReference, parentGroup);
                session.save();
            } else {
                String msg = "Group with the name \"{0}\" already is among parents of \"{1}\" group.";
                msg = MessageFormat.format(msg, new Object[]{childGroupName, parentGroupName});
                LogUtils.error(log, msg);
                throw new RepositoryException(msg);
            }
        } finally {
            session.logout();
        }
    }
    
    public static void revokeUserFromGroup(String userName, String groupName, Repository repository) throws RepositoryException {
        Session session = getSession(repository);
        try {
            Node usersNode = getUsersNode(session);
            Node groupsNode = getGroupsNode(session);        
            Node user = usersNode.getNode(userName);
            Node parentGroup = groupsNode.getNode(groupName);
            Value userReference = session.getValueFactory().createValue(user);
            removeRefereceFromGroup(userReference, parentGroup);
            session.save();
        } finally {
            session.logout();
        }
    }
    
    public static void revokeGroupFromGroup(String parentGroupName, String childGroupName, Repository repository) throws RepositoryException {
        Session session = getSession(repository);
        try {
            Node groupsNode = getGroupsNode(session);            
            Node childGroup = groupsNode.getNode(childGroupName);
            Node parentGroup = groupsNode.getNode(parentGroupName);
            Value groupReference = session.getValueFactory().createValue(childGroup);
            removeRefereceFromGroup(groupReference, parentGroup);
            session.save();
        } finally {
            session.logout();
        }
    }
    
    public static void changePassword(String userName, String password, Repository repository) throws RepositoryException {
        Session session = getSession(repository);
        try {
            Node usersNode = getUsersNode(session);
            Node user = usersNode.getNode(userName);
            user.setProperty(USER_PASSWORD_PROPERTY, JCRHelper.hashPassword(password));
            session.save();
        } finally {
            session.logout();
        }
    }
    
    public static List<String> getAllChildGroups(String groupName, Repository repository) throws RepositoryException {
        Session session = getSession(repository);
        try {
            return getAllChilds(groupName, GROUP_NODE_TYPE, session);
        } finally {
            session.logout();
        }
    }
    
    public static List<String> getAllChildUsers(String groupName, Repository repository) throws RepositoryException {
        Session session = getSession(repository);
        try {
            return getAllChilds(groupName, USER_NODE_TYPE, session);
        } finally {
            session.logout();
        }
    }    
    
    public static List<String> getDirectChildGroups(String groupName,  Repository repository) throws RepositoryException {
        return getDirectChilds(groupName, GROUP_NODE_TYPE, repository);
    }   
    
    public static List<String> getDirectChildUsers(String groupName,  Repository repository) throws RepositoryException {
        return getDirectChilds(groupName, USER_NODE_TYPE, repository);
    }    
    
    private static List<String> getAllChilds(String parentName, String type, Session session) throws RepositoryException {        
        List<String> result = new ArrayList<String>();
        
        Node groups = getGroupsNode(session);
        Node group = groups.getNode(parentName);
        Property members = group.getProperty(GROUP_MEMBER_PROPERTY);
        Value[] values = members.getValues(); 
        for (int i = 0; i < values.length; i++) {
            Value value = values[i];
            //getting referenced node
            Node memeberNode = session.getNodeByUUID(value.getString());
            //checking if node is group and adding it to result list
            if (memeberNode.getPrimaryNodeType().getName().equals(type)) {
                result.add(memeberNode.getName());                    
            }    
            //if child is group then going deeper, if user then skipping
            if (memeberNode.getPrimaryNodeType().getName().equals(GROUP_NODE_TYPE)) {
                result.addAll(getAllChilds(memeberNode.getName(), type, session));
            }
        }                          
        return result;          
    }
    
    private static List<String> getDirectChilds(String parentName, String type, Repository repository) throws RepositoryException {
        Session session = getSession(repository);
        List<String> result = new ArrayList<String>();
        try {
            Node groups = getGroupsNode(session);
            Node group = groups.getNode(parentName);
            Property members = group.getProperty(GROUP_MEMBER_PROPERTY);
            Value[] values = members.getValues(); 
            for (int i = 0; i < values.length; i++) {
                Value value = values[i];
                //getting referenced node
                Node memeberNode = session.getNodeByUUID(value.getString());
                //checking if node is group and adding it to result list
                if (memeberNode.getPrimaryNodeType().getName().equals(type)) {
                    result.add(memeberNode.getName());
                }
            }                          
        } finally {
            session.logout();
        }
        return result;
    }    
    

    
    private static void checkRepository(Repository repository) throws RepositoryException {
        if (!repository.getClass().isAssignableFrom(RepositoryImpl.class)) {
            String msg = "Unsupported repository instance. Please use Exigen JCR repository.";
            LogUtils.error(log, msg);
            throw new RepositoryException(msg);
        }
    }
    
    private static Node getUsersNode(Session session) throws RepositoryException {
        return (Node)session.getItem("/"+USERS_NODE_PATH);
    }
    
    private static Node getGroupsNode(Session session) throws RepositoryException {
        return (Node)session.getItem("/"+GROUPS_NODE_PATH);
    }      
    
    private static Session getSession(Repository repository) throws RepositoryException {
        checkRepository(repository);
        RepositoryImpl repositoryImpl = (RepositoryImpl)repository;
        return repositoryImpl.getSystemSession();        
    }
    
    private static void removeRefereceFromGroup(Value reference, Node group) throws RepositoryException {
        Property members = group.getProperty(GROUP_MEMBER_PROPERTY);
        Value[] values = members.getValues();        
        ArrayList<Value> newValues = new ArrayList<Value>();
        
        for (int i = 0; i < values.length; i++) {
            Value value = values[i];            
            if (value.getString().equals(reference.getString())) {
                continue;
            }
            newValues.add(values[i]);
        }        
        members.setValue((Value[]) newValues.toArray(new Value[newValues.size()]));
    }    
    
    
    private static void addRefereceToGroup(Value reference, Node group) throws RepositoryException {
        Property members = group.getProperty(GROUP_MEMBER_PROPERTY);
        Value[] values = members.getValues();        
        //cheking if user is already memeber of that group
        for (int i = 0; i < values.length; i++) {
            Value value = values[i];
            if (value.getString().equals(reference.getString())) {
                return;
            }
        }
        Value[] newValues = new Value[values.length + 1];
        System.arraycopy(values, 0, newValues, 0, values.length);
        newValues[values.length] = reference;
        members.setValue(newValues);
    }
    
    /**
     * recursively checks if <tt>childNode</tt> is already parent for <tt>node</tt>.
     * @param childNode
     * @param node
     * @return true if <tt>node</tt> is already parent for <tt>parentNode</tt>
     * @throws RepositoryException
     */
    private static boolean isChildNodeAmongParents(Node node, Node childNode) throws RepositoryException {
        if (node == null) {
            return false;
        }
        //getting references, iterator of rep:memebers properties, that has a reference to node
        PropertyIterator iter = node.getReferences();
        while (iter.hasNext()) {
            Property property = (Property) iter.nextProperty();
            if (property.getParent().getName().equals(childNode.getName())) {
                return true;
            }
            isChildNodeAmongParents(property.getParent(), childNode);
        }
        return false;
    }
    
    public static List<String> getUserGroups(String userName, Repository repository) throws RepositoryException {
        List<String> result = new ArrayList<String>();
        checkRepository(repository);
        RepositoryImpl repositoryImpl = (RepositoryImpl)repository;
        Session session = repositoryImpl.getSystemSession();
        try {
            Node usersNode = getUsersNode(session);
            Node user = usersNode.getNode(userName);
            populateUserGroups(result, user);
        } finally {
            session.logout();
        }
        return result;
    }
    
    private static void populateUserGroups(List<String> groups, Node user) throws RepositoryException {
        //getting references, iterator of rep:memebers property, that has a reference to user
        //in case if user belong to two or more groups itrator will contain two or more
        //rep:memebers property multivalue properties 
        PropertyIterator iter = user.getReferences();
        while (iter.hasNext()) {
            Property property = (Property) iter.nextProperty();
            if (!groups.contains(property.getParent().getName())) {
                groups.add(property.getParent().getName());
            }
            populateUserGroups(groups, property.getParent());
        }        
    }
    
}


/*
 * $Log: UserHelper.java,v $
 * Revision 1.1  2007/04/26 09:02:16  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2006/06/02 07:21:37  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.3  2006/04/27 16:56:17  ivgirts
 * PTR #1801676 UserHelper now has only static methods
 *
 * Revision 1.2  2006/04/20 11:42:58  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:45  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/13 15:04:56  ivgirts
 * PTR #1801676 UserManagement implementation
 *
 * Revision 1.2  2006/04/13 10:03:47  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/12 13:22:13  ivgirts
 * PTR #1801676 added implmentation of the user management and RepositoryAuthenticator
 *
 */
