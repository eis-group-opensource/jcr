/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.beans.Introspector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;

import javax.jcr.RepositoryException;

public class ReflectHelper {

    /**
     * Gets constructor for specified class with specified parameters
     * 
     * @param clazz class for which need to find constructor
     * @param params parameter classes
     * @return constructor
     * @throws VFConfigurationException if error occurred when finding constructor, or constructor does not  exist
     */
    public static Constructor getConstructor(Class clazz, Class[] params) throws RepositoryException{
        try {
            //find declared constuctor
            Constructor constructor = clazz.getDeclaredConstructor(params);
            if ( !isPublic(clazz, constructor) ) {
                //if constructor is not public, make it accessible
                constructor.setAccessible(true);
            }
            return constructor;
        } catch (NoSuchMethodException nme) {
            String msg = MessageFormat.format(
                    "Object class  \"{0}\" must declare constructor \"{1}\"",
                    new Object[] { clazz.getName(), buildConstructorDefinition(params) });
            throw new RepositoryException(msg);
        }        
    }    
    
    public static boolean isPublic(Class clazz, Member member) {
        return Modifier.isPublic( member.getModifiers() ) && Modifier.isPublic( clazz.getModifiers() );
    }
    
    /**
     * Builds String representation of constructor definition based on parameter classes.
     * 
     * @param params parameter classes
     * @return String representation of constructor
     */
    public static String buildConstructorDefinition(Class[] params){
        //builds String representation of constructor declaration
        //this is important for error messages
        StringBuffer result = new StringBuffer("(");
        for (int i = 0; i < params.length; i++) {
            if (i != 0) {
                //if not last parameters, then add ", "
                result.append(", ");
            }
            String className = params[i].getName();
            //substring only real class name, and add it
            className = className.substring(className.lastIndexOf('.')+1);
            result.append(className);
        }
        result.append(")");
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    public static Method getMethod(Class clazz, String _methodName, Class[] params) throws RepositoryException {
        
        Method[] methods = clazz.getDeclaredMethods();
        for (int i=0; i<methods.length; i++) {
            // only carry on if the method has no parameters
            if ( methods[i].getParameterTypes().length == params.length ) {
                String methodName = methods[i].getName();

                String testStdMethod = Introspector.decapitalize( methodName.substring(3) );
                String testOldMethod = methodName;
                if( testStdMethod.equals(_methodName) || testOldMethod.equals(_methodName) ) {
                    //check params
                    boolean match = true;
                    for (int j = 0; j < params.length ; j ++){
                        Class cl = params[j];
                        Class cl2 = methods[i].getParameterTypes()[j];
                        if (!cl2.isAssignableFrom(cl)){
                            match = false;
                        }
                    }
                    if (match) {
                        Method method = methods[i];
                        if (!isPublic(clazz, method)) {
                            method.setAccessible(true);
                        }
                        return method;
                    }

                }
            }
        }
        throw new RepositoryException("Method not found");
    }    

	
	public static RepositoryImpl createRepositoryObject() throws RepositoryException{
        //create repository
        Constructor constructor = ReflectHelper.getConstructor(RepositoryImpl.class, new Class[]{});
        try {
            return (RepositoryImpl) constructor.newInstance(new Object[]{});
        } catch (Exception e) {
            throw new RepositoryException("Error building Repository instance");
        }

    }
    
}


/*
 * $Log: ReflectHelper.java,v $
 * Revision 1.2  2009/02/05 10:00:40  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/13 10:03:44  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/11 15:47:11  dparhomenko
 * PTR#0144983 optimization
 *
 */