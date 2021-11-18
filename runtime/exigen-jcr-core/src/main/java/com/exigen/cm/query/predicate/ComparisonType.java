/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.text.MessageFormat;

/**
 * Declares comparison type constaints
 */
public class ComparisonType {
    
    /**
     * Binary comparison types.
     */
    public enum BINARY {
        LIKE          (" LIKE ", " NOT LIKE ")
      , CONTAINS      (" CONTAINS ", " NOT CONTAINS ")        
      , EQUALS        ("=", "<>")
      , NOT_EQUALS    ("!=", "<>", "=")
      , GT            (">", "<")
      , LT            ("<", ">")
      , ELT           ("<=", ">")
      , EGT           (">=", "<");
        
        
      private final String xPathCmp;
      private final String sqlCmp;
      private final String negationSQL;
      
      private BINARY(String xPathCmp, String sqlStr, String negation){
          this.xPathCmp=xPathCmp;
          this.sqlCmp = sqlStr;
          negationSQL = negation;
      }
      
      private BINARY(String xPathCmp, String negationSQL){
          this(xPathCmp,xPathCmp,negationSQL);
          
      }
      
      /**
       * Returns SQL comparison or negated SQL comparison depending on
       * <code>negation</code> flag value.
       * @param negation
       * @return
       */
      public String toSQL(boolean negation){
          return negation?negationSQL:sqlCmp;
      }
      
      
      
      /**
       * Returns comparison ComparisonType by XPath name.
       * @param strCmp
       * @return
       */
      public static BINARY getTypeByXPathName(String xPathName){
          switch(xPathName.length()){
              case 1:
                  switch(xPathName.charAt(0)){
                      case '=': return EQUALS;
                      case '>': return GT;
                      case '<': return LT;
                  }
              case 2:
                  switch(xPathName.charAt(0)){
                      case '!': return NOT_EQUALS;
                      case '>': return EGT;
                      case '<': return ELT;
                  }
             default:
                 for(BINARY c: values())
                     if(c.xPathCmp.equalsIgnoreCase(xPathName))
                         return c;
          }
          
//          for(BINARY c: values())
//              if(c.xPathCmp.equalsIgnoreCase(xPathName))
//                  return c;
    
          throw new IllegalArgumentException(MessageFormat.format("Unsupported comparison {0}", xPathName));
      }   
    }    


    /**
     * Declares possible FTS comparisons
     */
    public enum FTS {PHRASE, WORD};
    
    /**
     * Unary comparison types.
     */
    public enum UNARY {
        IS_NULL       ("IS NULL", "IS NOT NULL")
      , IS_NOT_NULL   ("IS NOT NULL", "IS NULL");
            
        private String sql, negationSQL;
          private UNARY(String sql, String negationSQL){
              this.sql=sql;
              this.negationSQL=negationSQL;
          }
          
          /**
           * Returns SQL comparison or negated SQL comparison depending on
           * <code>negation</code> flag value.
           * @param negation
           * @return
           */
          public String toSQL(boolean negation){
              return negation?negationSQL:sql;
          }
    };
    

}

/*
 * $Log: ComparisonType.java,v $
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/21 12:33:17  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.2  2006/11/22 16:35:37  maksims
 * #1802721 Log category performance added
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */