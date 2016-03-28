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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.tojava.HasJavaFileInfo;
import org.onosproject.yangutils.translator.tojava.HasTempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPEDEF_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.ATTRIBUTES_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.CONSTRUCTOR_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EQUALS_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.GETTER_FOR_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.GETTER_FOR_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.HASH_CODE_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.SETTER_FOR_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.SETTER_FOR_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.TO_STRING_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getDataFromTempFileHandle;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.initiateJavaFileGeneration;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getConstructorStart;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethodOpen;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getHashCodeMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getHashCodeMethodOpen;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethodOpen;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.PRIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.PUBLIC;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.insertDataIntoJavaFile;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.partString;

/**
 * Generates java file.
 */
public final class JavaFileGenerator {

    /**
     * Default constructor.
     */
    private JavaFileGenerator() {
    }

    /**
     * Returns generated interface file for current node.
     *
     * @param file file
     * @param imports imports for the file
     * @param curNode current YANG node
     * @param isAttrPresent  if any attribute is present or not
     * @return interface file
     * @throws IOException when fails to write in file
     */
    public static File generateInterfaceFile(File file, List<String> imports, YangNode curNode, boolean isAttrPresent)
            throws IOException {

        JavaFileInfo javaFileInfo = ((HasJavaFileInfo) curNode).getJavaFileInfo();

        String className = getCaptialCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateJavaFileGeneration(file, className, INTERFACE_MASK, imports, path);
        if (isAttrPresent) {
            /**
             * Add getter methods to interface file.
             */
            try {
                /**
                 * Getter methods.
                 */
                insertDataIntoJavaFile(file, getDataFromTempFileHandle(GETTER_FOR_INTERFACE_MASK, curNode));
            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while interface file generation");
            }
        }
        return file;
    }

    /**
     * Return generated builder interface file for current node.
     *
     * @param file file
     * @param curNode current YANG node
     * @param isAttrPresent  if any attribute is present or not
     * @return builder interface file
     * @throws IOException when fails to write in file
     */
    public static File generateBuilderInterfaceFile(File file, YangNode curNode, boolean isAttrPresent)
            throws IOException {

        JavaFileInfo javaFileInfo = ((HasJavaFileInfo) curNode).getJavaFileInfo();

        String className = getCaptialCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateJavaFileGeneration(file, className, BUILDER_INTERFACE_MASK, null, path);
        List<String> methods = new ArrayList<>();
        if (isAttrPresent) {
            try {
                /**
                 * Getter methods.
                 */
                methods.add(FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(GETTER_FOR_INTERFACE_MASK, curNode));
                /**
                 * Setter methods.
                 */
                methods.add(NEW_LINE);
                methods.add(FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(SETTER_FOR_INTERFACE_MASK, curNode));
            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while builder interface file generation");
            }
        }
        /**
         * Add build method to builder interface file.
         */
        methods.add(
                ((HasTempJavaCodeFragmentFiles) curNode).getTempJavaCodeFragmentFiles().addBuildMethodForInterface());

        /**
         * Add getters and setters in builder interface.
         */
        for (String method : methods) {
            insertDataIntoJavaFile(file, method);
        }

        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);
        return file;
    }

    /**
     * Returns generated builder class file for current node.
     *
     * @param file file
     * @param imports imports for the file
     * @param curNode current YANG node
     * @param isAttrPresent  if any attribute is present or not
     * @return builder class file
     * @throws IOException when fails to write in file
     */
    public static File generateBuilderClassFile(File file, List<String> imports, YangNode curNode,
            boolean isAttrPresent) throws IOException {

        JavaFileInfo javaFileInfo = ((HasJavaFileInfo) curNode).getJavaFileInfo();

        String className = getCaptialCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateJavaFileGeneration(file, className, BUILDER_CLASS_MASK, imports, path);

        List<String> methods = new ArrayList<>();

        if (isAttrPresent) {
            /**
             * Add attribute strings.
             */
            try {
                insertDataIntoJavaFile(file,
                        NEW_LINE + FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(ATTRIBUTES_MASK, curNode));
            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while builder class file generation");
            }

            try {
                /**
                 * Getter methods.
                 */
                methods.add(getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK, curNode));
                /**
                 * Setter methods.
                 */
                methods.add(getDataFromTempFileHandle(SETTER_FOR_CLASS_MASK, curNode) + NEW_LINE);
            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while builder class file generation");
            }
        } else {
            insertDataIntoJavaFile(file, NEW_LINE);
        }
        /**
         * Add default constructor and build method impl.
         */
        methods.add(((HasTempJavaCodeFragmentFiles) curNode).getTempJavaCodeFragmentFiles().addBuildMethodImpl());
        methods.add(((HasTempJavaCodeFragmentFiles) curNode).getTempJavaCodeFragmentFiles()
                .addDefaultConstructor(PUBLIC, BUILDER));

        /**
         * Add methods in builder class.
         */
        for (String method : methods) {
            insertDataIntoJavaFile(file, method);
        }
        return file;
    }

    /**
     * Returns generated impl class file for current node.
     *
     * @param file file
     * @param curNode current YANG node
     * @param isAttrPresent if any attribute is present or not
     * @return impl class file
     * @throws IOException when fails to write in file
     */
    public static File generateImplClassFile(File file, YangNode curNode, boolean isAttrPresent)
            throws IOException {

        JavaFileInfo javaFileInfo = ((HasJavaFileInfo) curNode).getJavaFileInfo();

        String className = getCaptialCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateJavaFileGeneration(file, className, IMPL_CLASS_MASK, null, path);

        List<String> methods = new ArrayList<>();
        if (isAttrPresent) {
            /**
             * Add attribute strings.
             */
            try {
                insertDataIntoJavaFile(file,
                        NEW_LINE + FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(ATTRIBUTES_MASK, curNode));
            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while impl class file generation");
            }

            insertDataIntoJavaFile(file, NEW_LINE);
            try {
                /**
                 * Getter methods.
                 */
                methods.add(getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK, curNode));

                /**
                 * Hash code method.
                 */
                methods.add(getHashCodeMethodClose(getHashCodeMethodOpen() + partString(
                        getDataFromTempFileHandle(HASH_CODE_IMPL_MASK, curNode).replace(NEW_LINE, EMPTY_STRING))));
                /**
                 * Equals method.
                 */
                methods.add(getEqualsMethodClose(
                        getEqualsMethodOpen(className + IMPL) + getDataFromTempFileHandle(EQUALS_IMPL_MASK, curNode)));
                /**
                 * To string method.
                 */
                methods.add(getToStringMethodOpen() + getDataFromTempFileHandle(TO_STRING_IMPL_MASK, curNode)
                        + getToStringMethodClose());

            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while impl class file generation");
            }
        } else {
            insertDataIntoJavaFile(file, NEW_LINE);
        }
        try {
            /**
             * Constructor.
             */
            methods.add(getConstructorStart(className) + getDataFromTempFileHandle(CONSTRUCTOR_IMPL_MASK, curNode)
                    + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET);
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while impl class file generation");
        }
        /**
         * Add methods in impl class.
         */
        for (String method : methods) {
            insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + method + NEW_LINE);
        }
        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);

        return file;
    }

    /**
     * Generate class file for type def.
     *
     * @param file generated file
     * @param curNode current YANG node
     * @param imports imports for file
     * @return type def class file
     * @throws IOException when fails to generate class file
    */
    public static File generateTypeDefClassFile(File file, YangNode curNode, List<String> imports) throws IOException {

        JavaFileInfo javaFileInfo = ((HasJavaFileInfo) curNode).getJavaFileInfo();

        String className = getCaptialCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateJavaFileGeneration(file, className, GENERATE_TYPEDEF_CLASS, imports, path);

        List<String> methods = new ArrayList<>();

        /**
         * Add attribute strings.
         */
        try {
            insertDataIntoJavaFile(file,
                    NEW_LINE + FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(ATTRIBUTES_MASK, curNode));
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while type def class file generation");
        }

        /**
         * Default constructor.
         */
        methods.add(((HasTempJavaCodeFragmentFiles) curNode).getTempJavaCodeFragmentFiles()
                .addDefaultConstructor(PRIVATE, EMPTY_STRING));

        /**
         * Constructor.
         */
        methods.add(((HasTempJavaCodeFragmentFiles) curNode).getTempJavaCodeFragmentFiles()
                .addTypeDefConstructor());

        /**
         * Of method.
         */
        methods.add(((HasTempJavaCodeFragmentFiles) curNode).getTempJavaCodeFragmentFiles().addOfMethod());
        try {

            /**
             * Getter method.
             */
            methods.add(getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK, curNode));

            /**
             * Setter method.
             */
            methods.add(((HasTempJavaCodeFragmentFiles) curNode).getTempJavaCodeFragmentFiles().addTypeDefsSetter());

            /**
             * Hash code method.
             */
            methods.add(getHashCodeMethodClose(getHashCodeMethodOpen() + partString(
                    getDataFromTempFileHandle(HASH_CODE_IMPL_MASK, curNode).replace(NEW_LINE, EMPTY_STRING))));

            /**
             * Equals method.
             */
            methods.add(getEqualsMethodClose(getEqualsMethodOpen(className + EMPTY_STRING)
                    + getDataFromTempFileHandle(EQUALS_IMPL_MASK, curNode)));

            /**
             * To string method.
             */
            methods.add(getToStringMethodOpen() + getDataFromTempFileHandle(TO_STRING_IMPL_MASK, curNode)
                    + getToStringMethodClose());

        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while tyoe def class file generation");
        }

        for (String method : methods) {
            insertDataIntoJavaFile(file, method);
        }
        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);

        return file;
    }
}
