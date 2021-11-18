/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;

/**
 * Generic path map that associates information with the individual path elements
 * of a path.
 */
public class PathMap {

    /**
     * Root element
     */
    private final Element root = new Element(Path.ROOT.getNameElement());

    /**
     * Map a path to a child. If <code>exact</code> is <code>false</code>,
     * returns the last available item along the path that is stored in the map.
     * @param path path to map
     * @param exact flag indicating whether an exact match is required
     * @return child, maybe <code>null</code> if <code>exact</code> is
     *         <code>true</code>
     */
    public Element map(Path path, boolean exact) {
        Path.PathElement[] elements = path.getElements();
        Element current = root;

        for (int i = 1; i < elements.length; i++) {
            Element next = current.getChild(elements[i]);
            if (next == null) {
                if (exact) {
                    return null;
                }
                break;
            }
            current = next;
        }
        return current;
    }

    /**
     * Create an element given by its path. The path map will create any necessary
     * intermediate elements.
     * @param path path to child
     * @param obj object to store at destination
     */
    public Element put(Path path, Object obj) {
        Element element = put(path);
        element.obj = obj;
        return element;
    }

    /**
     * Put an element given by its path. The path map will create any necessary
     * intermediate elements.
     * @param path path to child
     * @param element element to store at destination
     */
    public void put(Path path, Element element) {
        Path.PathElement[] elements = path.getElements();
        Element current = root;

        for (int i = 1; i < elements.length - 1; i++) {
            Element next = current.getChild(elements[i]);
            if (next == null) {
                next = current.createChild(elements[i]);
            }
            current = next;
        }
        current.put(path.getNameElement(), element);
    }

    /**
     * Create an empty child given by its path.
     * @param path path to child
     */
    public Element put(Path path) {
        Path.PathElement[] elements = path.getElements();
        Element current = root;

        for (int i = 1; i < elements.length; i++) {
            Element next = current.getChild(elements[i]);
            if (next == null) {
                next = current.createChild(elements[i]);
            }
            current = next;
        }
        return current;
    }

    /**
     * Traverse the path map and call back requester.
     * @param includeEmpty if <code>true</code> invoke call back on every child
     *                     regardless, whether the associated object is empty
     *                     or not; otherwise call back on non-empty children
     *                     only
     */
    public void traverse(ElementVisitor visitor, boolean includeEmpty) {
        root.traverse(visitor, includeEmpty);
    }

    /**
     * Internal class holding the object associated with a certain
     * path element.
     */
    public static class Element {

        /**
         * Parent element
         */
        private Element parent;

        /**
         * Map of immediate children
         */
        private Map<QName, ArrayList<Element>> children;

        /**
         * Number of non-empty children
         */
        private int childrenCount;

        /**
         * Object associated with this element
         */
        private Object obj;

        /**
         * QName associated with this element
         */
        private QName name;

        /**
         * 1-based index associated with this element where index=0 is
         * equivalent to index=1.
         */
        private int index;

        /**
         * Create a new instance of this class with a path element.
         * @param nameIndex path element of this child
         */
        private Element(Path.PathElement nameIndex) {
            this.name = nameIndex.getName();
            this.index = nameIndex.getIndex();
        }

        /**
         * Create a child of this node inside the path map.
         * @param nameIndex position where child is created
         * @return child
         */
        private Element createChild(Path.PathElement nameIndex) {
            Element element = new Element(nameIndex);
            put(nameIndex, element);
            return element;
        }

        /**
         * Insert an empty child. Will shift all children having an index
         * greater than or equal to the child inserted to the right.
         * @param nameIndex position where child is inserted
         */
        public void insert(Path.PathElement nameIndex) {
            // convert 1-based index value to 0-base value
            int index = getOneBasedIndex(nameIndex) - 1;
            if (children != null) {
                ArrayList<Element> list = children.get(nameIndex.getName());
                if (list != null && list.size() > index) {
                    for (int i = index; i < list.size(); i++) {
                        Element element = (Element) list.get(i);
                        if (element != null) {
                            element.index = element.getNormalizedIndex() + 1;
                        }
                    }
                    list.add(index, null);
                }
            }
        }

        /**
         * Return an element matching a name and index.
         * @param nameIndex position where child is located
         * @return element matching <code>nameIndex</code> or <code>null</code> if
         *         none exists.
         */
        private Element getChild(Path.PathElement nameIndex) {
            // convert 1-based index value to 0-base value
            int index = getOneBasedIndex(nameIndex) - 1;
            Element element = null;

            if (children != null) {
                ArrayList<Element> list = children.get(nameIndex.getName());
                if (list != null && list.size() > index) {
                    element = (Element) list.get(index);
                }
            }
            return element;
        }

        /**
         * Link a child of this node. Position is given by <code>nameIndex</code>.
         * @param nameIndex position where child should be located
         * @param element element to add
         */
        public void put(Path.PathElement nameIndex, Element element) {
            // convert 1-based index value to 0-base value
            int index = getOneBasedIndex(nameIndex) - 1;
            if (children == null) {
                children = new HashMap<QName, ArrayList<Element>>();
            }
            ArrayList<Element> list = children.get(nameIndex.getName());
            if (list == null) {
                list = new ArrayList<Element>();
                children.put(nameIndex.getName(), list);
            }
            while (list.size() < index) {
                list.add(null);
            }
            if (list.size() == index) {
                list.add(element);
            } else {
                list.set(index, element);
            }

            element.parent = this;
            element.name = nameIndex.getName();
            element.index = nameIndex.getIndex();

            childrenCount++;
        }

        /**
         * Remove a child. Will shift all children having an index greater than
         * the child removed to the left. If there are no more children left in
         * this element and no object is associated with this element, the
         * element itself gets removed.
         *
         * @param nameIndex child's path element
         * @return removed child, may be <code>null</code>
         */
        public Element remove(Path.PathElement nameIndex) {
            return remove(nameIndex, true);
        }

        /**
         * Remove a child. If <code>shift</code> is set to <code>true</code>,
         * will shift all children having an index greater than the child
         * removed to the left. If there are no more children left in
         * this element and no object is associated with this element, the
         * element itself gets removed.
         *
         * @param nameIndex child's path element
         * @param shift whether to shift same name siblings having a greater
         *              index to the left
         * @return removed child, may be <code>null</code>
         */
        private Element remove(Path.PathElement nameIndex, boolean shift) {
            // convert 1-based index value to 0-base value
            int index = getOneBasedIndex(nameIndex) - 1;
            if (children == null) {
                return null;
            }
            ArrayList<Element> list = children.get(nameIndex.getName());
            if (list == null || list.size() <= index) {
                return null;
            }
            Element element = (Element) list.set(index, null);
            if (shift) {
                for (int i = index + 1; i < list.size(); i++) {
                    Element sibling = (Element) list.get(i);
                    if (sibling != null) {
                        sibling.index--;
                    }
                }
                list.remove(index);
            }
            if (element != null) {
                element.parent = null;
                childrenCount--;
            }
            if (childrenCount == 0 && obj == null && parent != null) {
                parent.remove(getPathElement(), shift);
            }
            return element;
        }

        /**
         * Remove this element. Delegates the call to the parent item.
         */
        public void remove() {
            if (parent != null) {
                parent.remove(getPathElement());
            }
        }

        /**
         * Remove all children of this element. Removes this element itself
         * if this element does not contain associated information.
         */
        public void removeAll() {
            children = null;
            childrenCount = 0;

            if (obj == null && parent != null) {
                parent.remove(getPathElement(), false);
            }
        }

        /**
         * Return the object associated with this element
         * @return object associated with this element
         */
        public Object get() {
            return obj;
        }

        /**
         * Set the object associated with this element
         * @param obj object associated with this element
         */
        public void set(Object obj) {
            this.obj = obj;

            if (obj == null && childrenCount == 0 && parent != null) {
                parent.remove(getPathElement(), false);
            }
        }

        /**
         * Return the name of this element
         * @return name
         */
        public QName getName() {
            return name;
        }

        /**
         * Return the non-normalized 1-based index of this element. Note that
         * this method can return a value of 0 which should be treated as 1.
         * @return index
         * @see #getNormalizedIndex()
         */
        public int getIndex() {
            return index;
        }

        /**
         * Return the 1-based index of this element.
         * Same as {@link #getIndex()} except that an index value of 0
         * is automatically converted to 1.
         * @return 1-based index
         */
        public int getNormalizedIndex() {
            if (index == 0) {
                return 1;
            } else {
                return index;
            }
        }

        /**
         * Return a path element pointing to this element
         * @return path element
         */
        public Path.PathElement getPathElement() {
            return Path.create(name, index).getNameElement();
        }

        /**
         * Return the path of this element.
         * @return path
         * @throws MalformedPathException if building the path fails
         */
        public Path getPath() throws MalformedPathException {
            if (parent == null) {
                return Path.ROOT;
            }

            Path.PathBuilder builder = new Path.PathBuilder();
            getPath(builder);
            return builder.getPath();
        }

        /**
         * Internal implementation of {@link #getPath()} that populates entries
         * in a builder. On exit, <code>builder</code> contains the path
         * of this element
         */
        private void getPath(Path.PathBuilder builder) {
            if (parent == null) {
                builder.addRoot();
                return;
            }
            parent.getPath(builder);
            if (index == 0 || index == 1) {
                builder.addLast(name);
            } else {
                builder.addLast(name, index);
            }
        }

        /**
         * Checks whether this element has the specified path. Introduced to
         * avoid catching a <code>MalformedPathException</code> for simple
         * path comparisons.
         * @param path path to compare to
         * @return <code>true</code> if this child has the path
         *         <code>path</code>, <code>false</code> otherwise
         */
        public boolean hasPath(Path path) {
            return hasPath(path.getElements(), path.getLength());
        }

        /**
         * Checks whether this element has the specified path, given by
         * path elements.
         * @param elements path elements to compare to
         * @param len number of elements to compare to
         * @return <code>true</code> if this element has the path given;
         *         otherwise <code>false</code>
         */
        private boolean hasPath(Path.PathElement[] elements, int len) {
            if (getPathElement().equals(elements[len - 1])) {
                if (parent != null) {
                    return parent.hasPath(elements, len - 1);
                }
                return true;
            }
            return false;
        }

        /**
         * Return 1-based index of a path element.
         */
        private static int getOneBasedIndex(Path.PathElement nameIndex) {
            int index = nameIndex.getIndex();
            if (index == 0) {
                return 1;
            } else {
                return index;
            }
        }

        /**
         * Recursively invoked traversal method.
         * @param visitor visitor to invoke
         * @param includeEmpty if <code>true</code> invoke call back on every
         *        element regardless, whether the associated object is empty
         *        or not; otherwise call back on non-empty children only
         */
        public void traverse(ElementVisitor visitor, boolean includeEmpty) {
            if (children != null) {
                Iterator<ArrayList<Element>> iter = children.values().iterator();
                while (iter.hasNext()) {
                    ArrayList<Element> list = iter.next();
                    for (int i = 0; i < list.size(); i++) {
                        Element element = (Element) list.get(i);
                        if (element != null) {
                            element.traverse(visitor, includeEmpty);
                        }
                    }
                }
            }
            if (includeEmpty || obj != null) {
                visitor.elementVisited(this);
            }
        }

        /**
         * Return the depth of this element. Defined to be <code>0</code> for the
         * root element and <code>n + 1</code> for some element if the depth of
         * its parent is <code>n</code>.
         */
        public int getDepth() {
            if (parent != null) {
                return parent.getDepth() + 1;
            }
            return 0;
        }

        /**
         * Return a flag indicating whether the specified node is a
         * child of this node.
         * @param other node to check
         */
        public boolean isAncestorOf(Element other) {
            Element parent = other.parent;
            while (parent != null) {
                if (parent == this) {
                    return true;
                }
                parent = parent.parent;
            }
            return false;
        }

        /**
         * Return the parent of this element
         * @return parent or <code>null</code> if this is the root element
         */
        public Element getParent() {
            return parent;
        }

        /**
         * Return the children count of this element
         * @return children count
         */
        public int getChildrenCount() {
            return childrenCount;
        }

        /**
         * Return an iterator over all of this element's children. Every
         * element returned by this iterator is of type {@link #Element}.
         */
        public Iterator<Element> getChildren() {
            ArrayList<Element> result = new ArrayList<Element>();

            if (children != null) {
                Iterator<ArrayList<Element>> iter = children.values().iterator();
                while (iter.hasNext()) {
                    ArrayList<Element> list = iter.next();
                    for (int i = 0; i < list.size(); i++) {
                        Element element = list.get(i);
                        if (element != null) {
                            result.add(element);
                        }
                    }
                }
            }
            return result.iterator();
        }
    }

    /**
     * Element visitor used in {@link PathMap#traverse}
     */
    public interface ElementVisitor {

        /**
         * Invoked for every element visited on a tree traversal
         * @param element element visited
         */
        void elementVisited(Element element);
    }
}
