/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.security;

import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.TABLE_ACE_RESTRICTION__ACE_ID;
import static com.exigen.cm.Constants.TABLE_ACE__GROUP_ID;
import static com.exigen.cm.Constants.TABLE_ACE__USER_ID;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.database.statements.RowMap;
public class ACERestriction {

    private Long id;
    private Long aceId;
    private String userId;
    private String groupId;

    public ACERestriction(Long id, Long aceId, String userId, String groupId) {
        super();
        this.id = id;
        this.aceId = aceId;
        this.userId = userId;
        this.groupId = groupId;
    }

    public ACERestriction(RowMap row) {
        this.id = row.getLong(FIELD_ID);
        this.aceId = row.getLong(TABLE_ACE_RESTRICTION__ACE_ID);
        this.userId = row.getString(TABLE_ACE__USER_ID);
        this.groupId = row.getString(TABLE_ACE__GROUP_ID);
    }

    public Long getId() {
        return id;
    }

    public Long getAceId() {
        return aceId;
    }

    public String getUserId() {
        return userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public boolean isUserRestriction() {
        return userId != null;
    }

    public boolean isGroupRestriction() {
        return groupId != null;
    }

    @Override
    public String toString() {
        
        return ToStringBuilder.reflectionToString(this);
    }
}
