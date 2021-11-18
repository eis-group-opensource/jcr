/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.security;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public abstract class SecurityPrincipal {

    

    public static final class GroupPrincipal extends SecurityPrincipal{

        public GroupPrincipal(String name, String contextId) {
            super(name, contextId);
        }

        public GroupPrincipal(String name) {
            super(name);
        }

        @Override
        public boolean isUser() {
            return false;
        }
        
    }
    
    public static final class UserPrincipal extends SecurityPrincipal{

        public UserPrincipal(String name, String contextId) {
            super(name, contextId);
        }

        public UserPrincipal(String name) {
            super(name);
        }

        @Override
        public boolean isUser() {
            return true;
        }
        
    }
    
    private String name;
    
    private String contextId;

    private boolean ignoreCase;
    
    
    public SecurityPrincipal(String name, String contextId) {
        this.name = name;
        this.contextId = contextId;
    }
    
    public static final SecurityPrincipal user(String name, String context){
        return new UserPrincipal(name, context);
    }

    public static final SecurityPrincipal user(String name){
        return new UserPrincipal(name);
    }

    public static final SecurityPrincipal group(String name, String context){
        return new GroupPrincipal(name, context);
    }

    public static final SecurityPrincipal group(String name){
        return new GroupPrincipal(name);
    }

    public SecurityPrincipal(String name) {
        this.name = name;
        this.contextId = null;
    }

    abstract public boolean isUser();
    
    public boolean isGroup(){
        return !isUser();
    }
    
    public String getName(){
        if (name == null){
            return null;
        }
        if (ignoreCase){
            return name.toUpperCase();
        } else {
            return name;
        }
    }
    
    public String getContextId(){
        return contextId;
    }

    public void setIgnoreCase(boolean value) {
        this.ignoreCase = value;
        
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static SecurityPrincipal create(String userId, String groupId,
            String contextId) {
        if (userId != null){
            return new UserPrincipal(userId, contextId);
        } else {
            return new GroupPrincipal(groupId, contextId);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SecurityPrincipal){
            if (obj.hashCode() != hashCode()){
                return false;
            }
            EqualsBuilder builder = new EqualsBuilder();
            SecurityPrincipal other = (SecurityPrincipal) obj;

            builder.append(ignoreCase ? name.toUpperCase() : name, other.ignoreCase ? other.name.toUpperCase() : name);
            builder.append(contextId, other.contextId);
            builder.append(isUser(), other.isUser());

            return builder.equals(obj);
        } else {
            return false;
        }
    }

    int hashCode = -1;
    
    @Override
    public int hashCode() {
        if (hashCode == -1) {
            HashCodeBuilder builder = new HashCodeBuilder();
            builder.append(ignoreCase ? name.toUpperCase() : name);
            builder.append(contextId);
            builder.append(isUser());
            hashCode = builder.toHashCode();
        }
        return hashCode;
    }

}
