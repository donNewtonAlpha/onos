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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangType;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.onosproject.yangutils.datamodel.YangDataTypes.BOOLEAN;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT32;
import static org.onosproject.yangutils.datamodel.YangDataTypes.STRING;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT8;
import static org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType.getJavaDataType;
import static org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType.getJavaImportClass;
import static org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType.getJavaImportPackage;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_LANG;

/**
 * Unit test case for attribute java data type.
 */
public class AttributesJavaDataTypeTest {

    private static final YangDataTypes TYPE1 = STRING;
    private static final YangDataTypes TYPE2 = INT32;
    private static final YangDataTypes TYPE3 = BOOLEAN;
    private static final YangDataTypes TYPE4 = UINT8;
    private static final String CLASS_INFO1 = "String";
    private static final String CLASS_INFO2 = "int";
    private static final String CLASS_INFO3 = "boolean";
    private static final String CLASS_INFO4 = "short";
    private static final String CLASS_INFO5 = "Integer";
    private static String test = "";

    /**
     * Unit test for private constructor.
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

        Class<?>[] classesToConstruct = {AttributesJavaDataType.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance());
        }
    }

    /**
     * Unit test for java class info method test.
     */
    @Test
    public void testgetJavaClassInfo() {

        test = getJavaImportClass(getStubYangType(TYPE1), false);
        assertThat(true, is(test.equals(CLASS_INFO1)));

        test = getJavaImportClass(getStubYangType(TYPE2), true);
        assertThat(true, is(test.equals(CLASS_INFO5)));

        test = getJavaImportClass(getStubYangType(TYPE3), false);
        assertThat(null, is(test));

        test = getJavaImportClass(getStubYangType(TYPE4), false);
        assertThat(null, is(test));
    }

    /**
     * Unit test for java data type method.
     */
    @Test
    public void testgetJavaDataType() {

        test = getJavaDataType(getStubYangType(TYPE1));
        assertThat(true, is(test.equals(CLASS_INFO1)));

        test = getJavaDataType(getStubYangType(TYPE2));
        assertThat(true, is(test.equals(CLASS_INFO2)));

        test = getJavaDataType(getStubYangType(TYPE3));
        assertThat(true, is(test.equals(CLASS_INFO3)));

        test = getJavaDataType(getStubYangType(TYPE4));
        assertThat(true, is(test.equals(CLASS_INFO4)));
    }

    /**
     * Unit test for java package info method.
     */
    @Test
    public void testgetJavaPkgInfo() {

        test = getJavaImportPackage(getStubYangType(TYPE1), false, CLASS_INFO1);
        assertThat(true, is(test.equals(JAVA_LANG)));

        test = getJavaImportPackage(getStubYangType(TYPE2), true, CLASS_INFO5);
        assertThat(true, is(test.equals(JAVA_LANG)));

        test = getJavaImportPackage(getStubYangType(TYPE3), false, CLASS_INFO3);
        assertThat(null, is(test));

        test = getJavaImportPackage(getStubYangType(TYPE4), false, CLASS_INFO4);
        assertThat(null, is(test));
    }

    /**
     * Returns stub YANG type for test.
     *
     * @param dataTypes YANG data types
     * @return YANG type
     */
    private YangType<?> getStubYangType(YangDataTypes dataTypes) {

        YangType<?> type = new YangType();
        type.setDataType(dataTypes);
        return type;
    }
}
