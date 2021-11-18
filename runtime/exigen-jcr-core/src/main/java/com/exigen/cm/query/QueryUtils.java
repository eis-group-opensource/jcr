/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.jcr.PropertyType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Holds set of utility methods.
 */
public class QueryUtils {
    private static final Log log = LogFactory.getLog(QueryUtils.class);
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    
    static{
        dateFormat.setLenient(false);
    }

    
    private QueryUtils(){}
    
    
    /**
     * Tries to convert JCR date string to date with no error logging.
     * @param date
     * @return
     */
    public static Date stringToDate(String date){
        return stringToDate(date, false);
    }
    /**
     * Tryes to convert string to Date.
     * @param date
     * @param logError
     * @return
     */
    public static Date stringToDate(String date, boolean logError){
        try{

//            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
//            dateFormat.setLenient(true);

//            if(date.charAt(0) == '\'') // remove enclosing ' '
//                date = date.substring(1, date.length()-1);
            
            
            if(date.charAt(date.length()-1) == 'Z') // in case short form of zero time shift is used
                date = new StringBuffer().append(date.substring(0, date.length()-1)).append("+0000").toString();
            else
                if(date.lastIndexOf(':') != -1)
                    date = new StringBuffer(date).deleteCharAt(date.lastIndexOf(':')).toString();
                else
                    return null;
    
//          It looks that dateFormat isn't thread safe
            synchronized(dateFormat){
                return dateFormat.parse(date);                
            }
        }catch(ParseException ex){
            
            String message = MessageFormat.format("Date parameter ''{0}'' is not applicable for pattern ''{1}'' See JSR-170 chapter: 6.2.5.1 Date",
                    date, "sYYYY-MM-DDThh:mm:ss.sssTZD");
            
            if(logError){
                if(log.isDebugEnabled())
                    log.error(message, ex);
                else
                    log.error(message);
            }
            return null;
//            throw new RuntimeException(message, ex);
        }
    }
    
    /**
     * Converts date to JCR date string.
     * @param date
     * @return
     */
    public static String dateToString(Date date){
        String result=null;
        synchronized(dateFormat){
            result = dateFormat.format(date);                
        }
        
        return new StringBuilder(result).insert(result.length()-2, ':').toString();
    }

    /**
     * appends dot to alias.
     * @param alias
     * @return
     */
    public static StringBuilder asPrefix(String alias){
        return new StringBuilder(alias.length()+1).append(alias).append('.');
    }

    public static StringBuilder asPrefix(StringBuilder alias){
        return new StringBuilder(alias.length()+1).append(alias).append('.');
    }

    
    /**
     * Strip quotes from string value if value is quoted.
     * Returns value unchanged otherwise.
     * @param src
     * @return
     */
    public static String stripQuotes(String src){
        if(src.charAt(0) == '\'' || src.charAt(0) == '"')
            return src.substring(1, src.length()-1);

        return src;
    }

    
    /**
     * Guesses JCR Property type by object class name ...
     * @param value
     * @return
     */
    public static int getValueType(Object value){
        if(value == null)
            return PropertyType.UNDEFINED;
        
        if(value instanceof Boolean)
            return PropertyType.BOOLEAN;
        
        if(value instanceof Date)
            return PropertyType.DATE;

        if(value instanceof Double)
            return PropertyType.DOUBLE;

        if(value instanceof Long || value instanceof Integer)                    
            return PropertyType.LONG;

        if(value instanceof String){
//          May return string which actually is a Date string ...
//          Agreed that values which are parseable to Date are searched as Dates ...
//          Moved to GeneralComparison impl ... to allow simultaneous value() update ...
//            Date date = stringToDate((String)value, false);
//            if(date != null)
//                return PropertyType.DATE;
            
            return PropertyType.STRING;
        }

        return PropertyType.UNDEFINED;
    }
    
    
    /**
     * Decodes entity name according to JSR-170 requirements: 6.4.3
     * @param name
     * @return
     */
    public static String decodeEntity(String name){
        if(name == null || name.length() < 7) // _xHHHH_
            return name;

        
        StringBuilder res = new StringBuilder(name.length());
        char[] codeBuffer = new char[4];

        int i=0;
        for(; i<name.length()-5; i++){
            char c = name.charAt(i);
            if(c == '_' && name.charAt(i+1) == 'x' && name.charAt(i+6)=='_'){
                name.getChars(i+2, i+6, codeBuffer, 0);
                String codeStr = new StringBuilder("0x").append(codeBuffer).toString();
                int code = Integer.decode(codeStr);
                res.append(Character.toChars(code));
                i+=6;
            }else
                res.append(c);
        }
        
//        ab_xHHHH_cd
        if(i<name.length())
            res.append(name.substring(i));
        
        return res.toString();
    }

    /**
     * Add dialectized parameter into params list.
     * @param param
     * @param context
     * @param paramsList
     */
    public static void addParameter(Object param, BuildingContext context, List<Object> paramsList){
        if(param instanceof Boolean)
            addParameter((Boolean)param, context, paramsList);
        else
        if(param instanceof Date)
            addParameter((Date)param, context, paramsList);
        else
        if(param instanceof Double)
            addParameter((Double)param, context, paramsList);
        else            
        if(param instanceof Integer)
            addParameter((Integer)param, context, paramsList);
        else            
        if(param instanceof Long)
            addParameter((Long)param, context, paramsList);
        else            
        if(param instanceof String)
            addParameter((String)param, context, paramsList);
        else
            if(param instanceof FixedParameter)
                paramsList.add( ((FixedParameter)param).getValue());
            else{
                String message = MessageFormat.format("Instances of class {0} are not supported as parameters.",
                        param.getClass().getName());
                throw new RuntimeException(message);
            }
        
    }
    
    public static void addParameter(String param, BuildingContext context, List<Object> paramsList){
        paramsList.add((Object)context.getDialect().convertStringToSQL(param));
    }

    public static void addParameter(boolean param, BuildingContext context, List<Object> paramsList){
        paramsList.add((Object)context.getDialect().convertToDBBoolean(param));
    }

    public static void addParameter(Boolean param, BuildingContext context, List<Object> paramsList){
        paramsList.add((Object)context.getDialect().convertToDBBoolean(param));
    }

    public static void addParameter(Date param, BuildingContext context, List<Object> paramsList){
        paramsList.add((Object)context.getDialect().convertToDBDate(param));
    }
    
    public static void addParameter(Double param, BuildingContext context, List<Object> paramsList){
        paramsList.add((Object)context.getDialect().convertToDBDouble(param));
    }

    public static void addParameter(Long param, BuildingContext context, List<Object> paramsList){
        paramsList.add((Object)context.getDialect().convertToDBLong(param));
    }

    public static void addParameter(Integer param, BuildingContext context, List<Object> paramsList){
        paramsList.add((Object)context.getDialect().convertToDBInteger(param));   
    }    

    /**
     * Parameter can be wrapped by this class instance
     * to preserve modification by Dialect.
     */
    public static class FixedParameter{
        final Object value;
        public FixedParameter(Object value){
            this.value=value;
        }
        
        public Object getValue(){
            return value;
        }
    }    
}

/*
 * $Log: QueryUtils.java,v $
 * Revision 1.1  2007/04/26 08:59:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2007/03/21 10:27:55  maksims
 * #1804130 fixed jcr:contains grammar
 *
 * Revision 1.2  2007/02/06 10:18:10  maksims
 * #1803814 lenient date parse disabled
 *
 * Revision 1.1  2006/12/15 13:13:29  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.3  2006/12/05 15:52:22  maksims
 * #1803540 Added ability to search by uuid
 *
 * Revision 1.2  2006/11/20 16:15:47  maksims
 * #0149156 String conversion for columns fixed
 *
 * Revision 1.1  2006/11/02 17:28:06  maksims
 * #1801897 Query2 addition
 *
 */