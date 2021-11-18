/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.lock;

/**
 * Lock token
 */
class LockToken {

    /**
     * UUID of node holding lock
     */
    public final Long nodeId;

    /**
     * Create a new instance of this class. Used when creating new locks upon
     * a request.
     * @param uuid uuid
     */
    public LockToken(Long nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Parse a lock token string representation and return a lock token instance.
     * @param s string representation of lock token
     * @throws IllegalArgumentException if some field is illegal
     */
    public static LockToken parse(String s)
            throws IllegalArgumentException {

        /*int sep = s.lastIndexOf('-');
        if (sep == -1 || sep == s.length() - 1) {
            throw new IllegalArgumentException("Separator not found.");
        }
        String uuid = s.substring(0, sep);
        if (getCheckDigit(uuid) != s.charAt(s.length() - 1)) {
            throw new IllegalArgumentException("Bad check digit.");
        }
        return new LockToken(uuid);
        throw new UnsupportedOperationException();*/
        return new LockToken(new Long(s));
    }

    /**
     * Return the string representation of a lock token
     * @return string representation
     * @see #toString
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(nodeId);
        //buf.append('-');
        //buf.append(getCheckDigit(nodeId));
        return buf.toString();
    }

    /**
     * Return the check digit for a lock token, given by its UUID
     * @param uuid uuid
     * @return check digit
     */
/*    private static char getCheckDigit(String uuid) {
        int result = 0;

        int multiplier = 36;
        for (int i = 0; i < uuid.length(); i++) {
            char c = uuid.charAt(i);
            if (c >= '0' && c <= '9') {
                int num = c - '0';
                result += multiplier * num;
                multiplier--;
            } else if (c >= 'A' && c <= 'F') {
                int num = c - 'A' + 10;
                result += multiplier * num;
                multiplier--;
            } else if (c >= 'a' && c <= 'f') {
                int num = c - 'a' + 10;
                result += multiplier * num;
                multiplier--;
            }
        }

        int rem = result % 37;
        if (rem != 0) {
            rem = 37 - rem;
        }
        if (rem >= 0 && rem <= 9) {
            return (char) ('0' + rem);
        } else if (rem >= 10 && rem <= 35) {
            return (char) ('A' + rem - 10);
        } else {
            return '+';
        }
    }*/
}
