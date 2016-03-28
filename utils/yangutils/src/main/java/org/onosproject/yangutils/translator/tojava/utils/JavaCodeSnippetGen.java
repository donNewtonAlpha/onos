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

package org.onosproject.yangutils.translator.tojava.utils;

import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;

import static org.onosproject.yangutils.translator.tojava.utils.ClassDefinitionGenerator.generateClassDefinition;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_CLOSE_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_OPEN_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.LIST;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PRIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;

/**
 * Utility class to generate the java snippet.
 */
public final class JavaCodeSnippetGen {

    /**
     * Default constructor.
     */
    private JavaCodeSnippetGen() {
    }

    /**
     * Get the java file header comment.
     *
     * @return the java file header comment
     */
    public static String getFileHeaderComment() {

        /**
         * TODO return the file header.
         */
        return null;
    }

    /**
     * Get the textual java code information corresponding to the import list.
     *
     * @param importInfo import info
     * @return the textual java code information corresponding to the import
     *         list
     */
    public static String getImportText(JavaQualifiedTypeInfo importInfo) {

        return IMPORT + importInfo.getPkgInfo() + PERIOD + importInfo.getClassInfo() + SEMI_COLAN + NEW_LINE;
    }

    /**
     * Based on the file type and the YANG name of the file, generate the class
     * / interface definition start.
     *
     * @param genFileTypes type of file being generated
     * @param yangName YANG name
     * @return corresponding textual java code information
     */
    public static String getJavaClassDefStart(int genFileTypes, String yangName) {

        /*
         * get the camel case name for java class / interface.
         */
        yangName = getCamelCase(yangName);
        return generateClassDefinition(genFileTypes, yangName);
    }

    /**
     * Get the textual java code for attribute definition in class.
     *
     * @param javaAttributeTypePkg Package of the attribute type
     * @param javaAttributeType java attribute type
     * @param javaAttributeName name of the attribute
     * @param isList is list attribute
     * @return the textual java code for attribute definition in class
     */
    public static String getJavaAttributeDefination(String javaAttributeTypePkg, String javaAttributeType,
            String javaAttributeName, boolean isList) {

        String attributeDefination = PRIVATE + SPACE;

        if (!isList) {
            if (javaAttributeTypePkg != null) {
                attributeDefination = attributeDefination + javaAttributeTypePkg + PERIOD;
            }

            attributeDefination = attributeDefination + javaAttributeType + SPACE + javaAttributeName + SEMI_COLAN
                    + NEW_LINE;
        } else {
            attributeDefination = attributeDefination + LIST + DIAMOND_OPEN_BRACKET;
            if (javaAttributeTypePkg != null) {
                attributeDefination = attributeDefination + javaAttributeTypePkg + PERIOD;
            }

            attributeDefination = attributeDefination + javaAttributeType + DIAMOND_CLOSE_BRACKET + SPACE
                    + javaAttributeName + SEMI_COLAN + NEW_LINE;
        }
        return attributeDefination;
    }

    /**
     * Returns list attribute string.
     *
     * @param type attribute type
     * @return list attribute string
     */
    public static String getListAttribute(String type) {

        return LIST + DIAMOND_OPEN_BRACKET + type + DIAMOND_CLOSE_BRACKET;
    }

    /**
     * Based on the file type and the YANG name of the file, generate the class
     * / interface definition close.
     *
     * @return corresponding textual java code information
     */
    public static String getJavaClassDefClose() {

        return CLOSE_CURLY_BRACKET;
    }
}
