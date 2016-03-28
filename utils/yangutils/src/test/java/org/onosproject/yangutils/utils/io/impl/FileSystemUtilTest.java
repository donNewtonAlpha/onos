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

package org.onosproject.yangutils.utils.io.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;
import org.onosproject.yangutils.utils.UtilConstants;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the file handle utilities.
 */
public final class FileSystemUtilTest {

    private static final String BASE_DIR_PKG = "target.UnitTestCase.";
    private static final String PKG_INFO_CONTENT = "testGeneration6";
    private static final String BASE_PKG = "target/UnitTestCase";
    private static final String TEST_DATA_1 = "This is to append a text to the file first1\n";
    private static final String TEST_DATA_2 = "This is next second line\n";
    private static final String TEST_DATA_3 = "This is next third line in the file";

    /**
     * A private constructor is tested.
     *
     * @throws SecurityException if any security violation is observed
     * @throws NoSuchMethodException if when the method is not found
     * @throws IllegalArgumentException if there is illegal argument found
     * @throws InstantiationException if instantiation is provoked for the private constructor
     * @throws IllegalAccessException if instance is provoked or a method is provoked
     * @throws InvocationTargetException when an exception occurs by the method or constructor
     */
    @Test
    public void callPrivateConstructors() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<?>[] classesToConstruct = {FileSystemUtil.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance());
        }
    }

    /**
     * This test case checks the contents to be written in the file.
     *
     * @throws IOException when fails to create a test file
     */
    @Test
    public void updateFileHandleTest() throws IOException {

        File dir = new File(BASE_PKG + File.separator + "File1");
        dir.mkdirs();
        File createFile = new File(dir + "testFile");
        createFile.createNewFile();
        File createSourceFile = new File(dir + "sourceTestFile");
        createSourceFile.createNewFile();
        FileSystemUtil.updateFileHandle(createFile, TEST_DATA_1, false);
        FileSystemUtil.updateFileHandle(createFile, TEST_DATA_2, false);
        FileSystemUtil.updateFileHandle(createFile, TEST_DATA_3, false);
        FileSystemUtil.appendFileContents(createFile, createSourceFile);
        FileSystemUtil.updateFileHandle(createFile, null, true);
    }

    /**
     * This test  case checks whether the package is existing.
     *
     * @throws IOException when failed to create a test file
     */
    @Test
    public void packageExistTest() throws IOException {

        String dirPath = "exist1.exist2.exist3";
        String strPath = BASE_DIR_PKG + dirPath;
        File createDir = new File(strPath.replace(UtilConstants.PERIOD, UtilConstants.SLASH));
        createDir.mkdirs();
        File createFile = new File(createDir + File.separator + "package-info.java");
        createFile.createNewFile();
        assertTrue(FileSystemUtil.doesPackageExist(strPath));
        FileSystemUtil.createPackage(strPath, PKG_INFO_CONTENT);
        createDir.delete();
    }

}
