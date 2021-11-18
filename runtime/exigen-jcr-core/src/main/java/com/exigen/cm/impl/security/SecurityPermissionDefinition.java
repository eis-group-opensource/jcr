/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.security;

import java.util.ArrayList;
import java.util.Arrays;

import javax.jcr.RepositoryException;

import com.exigen.cm.impl.SecurityEntry;

public class SecurityPermissionDefinition {

	final private BaseSecurityPermission permission;

	private Boolean permit;

	final private boolean propogate;

	final private SecurityPrincipal principal;
	
    private ArrayList<SecurityPrincipal> restrictions = new ArrayList<SecurityPrincipal>();
    
    private boolean directPermission = false;
	
    public SecurityPermissionDefinition(final BaseSecurityPermission permission,
            SecurityPrincipal principal, final Boolean permit,
            final boolean propogate) {
        this(permission, principal, permit, propogate, false);
    }

    public SecurityPermissionDefinition(final BaseSecurityPermission permission,
            SecurityPrincipal principal, final Boolean permit,
            final boolean propogate, boolean directPermission) {
        super();
        this.permission = permission;
        this.principal = principal;
        this.permit = permit;
        if (permission == SecurityPermission.X_SUPER_DENY){
            this.propogate = true;
            this.directPermission = false;
        } else {
            this.propogate = propogate;
            this.directPermission = directPermission;
        }
        
    }


    SecurityPermissionDefinition createCopy(BaseSecurityPermission sp) {
        SecurityPermissionDefinition def = new SecurityPermissionDefinition(sp, principal, permit, isPropogate(), isDirectPermission());
        def.restrictions.addAll(restrictions);
        return def;
    }


	public BaseSecurityPermission getPermission() {
		return permission;
	}

	public Boolean isPermit() {
		return permit;
	}

	public boolean isPropogate() {
		return this.permission.isDirect() ? false : propogate;
	}

    public SecurityPrincipal getPrincipal() {
        return principal;
    }




    private void checkGrantPermission() throws RepositoryException {
       if (permission != SecurityPermission.X_GRANT){
           throw new RepositoryException("Restriction canbe defined only for grant permission");
       }
    }



    public void setIgnoreCase(boolean ignoreCaseInSecurity) {
        principal.setIgnoreCase(ignoreCaseInSecurity);
        
    }

    void setPermit(Boolean permit){
        this.permit = permit;
    }


    public SecurityPrincipal[] getRestrictions(){
        return (SecurityPrincipal[]) restrictions.toArray(new SecurityPrincipal[restrictions.size()]);
    }
    
    public void addRestriction(SecurityPrincipal principal){
        restrictions.add(principal);
    }
    
    public void importRestrictions(SecurityEntry ace) {
        restrictions.addAll(Arrays.asList(ace.getRestrictions()));
        
    }


    public boolean isDirectPermission() {
        return this.permission.isDirect() ? true : this.directPermission;
    }


    public void setDirectPermission(boolean directPermission) {
        this.directPermission = directPermission;
    }


}
