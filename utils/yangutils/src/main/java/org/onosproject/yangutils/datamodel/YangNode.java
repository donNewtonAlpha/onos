/*
 * Copyright 2016 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.yangutils.datamodel;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;

/**
 * Base class of a node in data model tree.
 */
public abstract class YangNode {

    /**
     * Type of node.
     */
    private YangNodeType nodeType;

    /**
     * Parent reference.
     */
    private YangNode parent;

    /**
     * First child reference.
     */
    private YangNode child;

    /**
     * Next sibling reference.
     */
    private YangNode nextSibling;

    /**
     * Previous sibling reference.
     */
    private YangNode previousSibling;

    /**
     * Get the nodes name.
     *
     * @return nodes name
     */
    public abstract String getName();

    /**
     * Set the nodes name.
     *
     * @param name nodes name
     */
    public abstract void setName(String name);

    /**
     * Default constructor is made private to ensure node type is always set.
     */
    @SuppressWarnings("unused")
    private YangNode() {

    }

    /**
     * Create a specific type of node.
     *
     * @param type of YANG node
     */
    protected YangNode(YangNodeType type) {
        setNodeType(type);
    }

    /**
     * Get the node type.
     *
     * @return node type
     */
    public YangNodeType getNodeType() {
        return nodeType;
    }

    /**
     * Set the node type.
     *
     * @param nodeType type of node
     */
    private void setNodeType(YangNodeType nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * Get the parent of node.
     *
     * @return parent of node
     */
    public YangNode getParent() {
        return parent;
    }

    /**
     * Set the parent of node.
     *
     * @param parent node
     */
    public void setParent(YangNode parent) {
        this.parent = parent;
    }

    /**
     * Get the first child of node.
     *
     * @return first child of node
     */
    public YangNode getChild() {
        return child;
    }

    /**
     * Set the first instance of a child node.
     *
     * @param child is only child to be set
     */
    public void setChild(YangNode child) {
        this.child = child;
    }

    /**
     * Get the next sibling of node.
     *
     * @return next sibling of node
     */
    public YangNode getNextSibling() {
        return nextSibling;
    }

    /**
     * Set the next sibling of node.
     *
     * @param sibling YANG node
     */
    public void setNextSibling(YangNode sibling) {
        nextSibling = sibling;
    }

    /**
     * Get the previous sibling.
     *
     * @return previous sibling node
     */
    public YangNode getPreviousSibling() {
        return previousSibling;
    }

    /**
     * Set the previous sibling.
     *
     * @param previousSibling points to predecessor sibling
     */
    public void setPreviousSibling(YangNode previousSibling) {
        this.previousSibling = previousSibling;
    }

    /**
     * Add a child node, the children sibling list will be sorted based on node
     * type.
     *
     * @param newChild refers to a child to be added
     * @throws DataModelException due to violation in data model rules
     */
    public void addChild(YangNode newChild) throws DataModelException {
        if (newChild.getNodeType() == null) {
            throw new DataModelException("Abstract node cannot be inserted into a tree");
        }

        if (newChild.getParent() == null) {
            newChild.setParent(this);
        } else if (newChild.getParent() != this) {
            throw new DataModelException("Node is already part of a tree");
        }

        if (newChild.getChild() != null) {
            throw new DataModelException("Child to be added is not atomic, it already has a child");
        }

        if (newChild.getNextSibling() != null) {
            throw new DataModelException("Child to be added is not atomic, it already has a next sibling");
        }

        if (newChild.getPreviousSibling() != null) {
            throw new DataModelException("Child to be added is not atomic, it already has a previous sibling");
        }

        /* First child to be added */
        if (getChild() == null) {
            setChild(newChild);
            return;
        }

        YangNode curNode;
        curNode = getChild();

        /*-
         *  If the new node needs to be the first child
        if (newChild.getNodeType().ordinal() < curNode.getNodeType().ordinal()) {
            newChild.setNextSibling(curNode);
            curNode.setPreviousSibling(newChild);
            setChild(newChild);
            return;
        }
         */

        /*
         * Get the predecessor child of new child
         */
        while (curNode.getNextSibling() != null
        /*
         * && newChild.getNodeType().ordinal() >=
         * curNode.getNextSibling().getNodeType().ordinal()
         */) {

            curNode = curNode.getNextSibling();
        }

        /* If the new node needs to be the last child */
        if (curNode.getNextSibling() == null) {
            curNode.setNextSibling(newChild);
            newChild.setPreviousSibling(curNode);
            return;
        }

        /*-
         *  Insert the new node in child node list sorted by type
        newChild.setNextSibling(curNode.getNextSibling());
        newChild.setPreviousSibling(curNode);
        curNode.getNextSibling().setPreviousSibling(newChild);
        curNode.setNextSibling(newChild);
        return;
         */
    }
}
