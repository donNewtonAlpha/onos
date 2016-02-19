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
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.ParsableDataType;

/*-
 * Reference RFC 6020.
 *
 * The "uses" statement is used to reference a "grouping" definition. It takes
 * one argument, which is the name of the grouping.
 *
 * The effect of a "uses" reference to a grouping is that the nodes defined by
 * the grouping are copied into the current schema tree, and then updated
 * according to the "refine" and "augment" statements.
 *
 * The identifiers defined in the grouping are not bound to a namespace until
 * the contents of the grouping are added to the schema tree via a "uses"
 * statement that does not appear inside a "grouping" statement, at which point
 * they are bound to the namespace of the current module.
 *
 * The uses's sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | augment      | 7.15    | 0..1        | -child nodes     |
 *                | description  | 7.19.3  | 0..1        | -string          |
 *                | if-feature   | 7.18.2  | 0..n        | -TODO            |
 *                | refine       | 7.12.2  | 0..1        | -TODO            |
 *                | reference    | 7.19.4  | 0..1        | -string          |
 *                | status       | 7.19.2  | 0..1        | -YangStatus      |
 *                | when         | 7.19.5  | 0..1        | -TODO            |
 *                +--------------+---------+-------------+------------------+
 */
/**
 * Data model node to maintain information defined in YANG uses.
 *
 */
public class YangUses extends YangNode implements YangCommonInfo, Parsable {

    /**
     * Name.
     */
    private String name;

    /**
     * referred group.
     */
    private YangGrouping refGroup;

    /**
     * description.
     */
    private String description;

    /**
     * YANG reference.
     */
    private String reference;

    /**
     * Status.
     */
    private YangStatusType status;

    /**
     * Create an YANG uses node.
     */
    public YangUses() {
        super(YangNodeType.USES_NODE);
    }

    /**
     * Get the name.
     *
     * @return the name.
     */
    public String getRefGroupingName() {
        return name;
    }

    /**
     * Set the name.
     *
     * @param refGroupingName the referred grouping name to set
     */
    public void setRefGroupingName(String refGroupingName) {
        name = refGroupingName;
    }

    /**
     * Get the referred group.
     *
     * @return the referred group.
     */
    public YangGrouping getRefGroup() {
        return refGroup;
    }

    /**
     * Set the referred group.
     *
     * @param refGroup the referred group.
     */
    public void setRefGroup(YangGrouping refGroup) {
        this.refGroup = refGroup;
    }

    /**
     * Get the description.
     *
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description.
     *
     * @param description set the description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the textual reference.
     *
     * @return the reference.
     */
    public String getReference() {
        return reference;
    }

    /**
     * Set the textual reference.
     *
     * @param reference the reference to set.
     */
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Get the status.
     *
     * @return the status.
     */
    public YangStatusType getStatus() {
        return status;
    }

    /**
     * Set the status.
     *
     * @param status the status to set.
     */
    public void setStatus(YangStatusType status) {
        this.status = status;
    }

    /**
     * Returns the type of the data.
     *
     * @return returns USES_DATA.
     */
    public ParsableDataType getParsableDataType() {
        return ParsableDataType.USES_DATA;
    }

    /**
     * Validate the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules.
     */
    public void validateDataOnEntry() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validate the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules.
     */
    public void validateDataOnExit() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.translator.CodeGenerator#generateJavaCodeEntry()
     */
    public void generateJavaCodeEntry() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.translator.CodeGenerator#generateJavaCodeExit()
     */
    public void generateJavaCodeExit() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#getPackage()
     */
    @Override
    public String getPackage() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#setPackage(java.lang.String)
     */
    @Override
    public void setPackage(String pkg) {
        // TODO Auto-generated method stub

    }
}
