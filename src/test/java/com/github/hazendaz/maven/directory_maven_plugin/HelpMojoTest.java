/*
 *    Copyright 2011-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.github.hazendaz.maven.directory_maven_plugin;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.SilentLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Tests for {@link HelpMojo}.
 */
public class HelpMojoTest {

    /**
     * execute() should parse plugin-help and run successfully even when lineLength/indentSize are non-positive,
     * covering default-value fallback branches.
     */
    @Test
    void executeUsesDefaultLineLengthAndIndentWhenNonPositive() throws Exception {
        HelpMojo mojo = new HelpMojo();
        mojo.setLog(new SilentLog());
        setField(mojo, "lineLength", 0);
        setField(mojo, "indentSize", 0);

        Assertions.assertDoesNotThrow(() -> mojo.execute());

        Assertions.assertEquals(80, getIntField(mojo, "lineLength"));
        Assertions.assertEquals(2, getIntField(mojo, "indentSize"));
    }

    /**
     * execute() should render detailed help for a selected goal without failure.
     */
    @Test
    void executeWithDetailAndGoalFilterRunsSuccessfully() throws Exception {
        HelpMojo mojo = new HelpMojo();
        mojo.setLog(new SilentLog());
        setField(mojo, "detail", true);
        setField(mojo, "goal", "help");
        setField(mojo, "lineLength", 120);
        setField(mojo, "indentSize", 4);

        Assertions.assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * execute() should handle an unknown goal by producing no goal details but still completing.
     */
    @Test
    void executeWithUnknownGoalRunsSuccessfully() throws Exception {
        HelpMojo mojo = new HelpMojo();
        mojo.setLog(new SilentLog());
        setField(mojo, "detail", true);
        setField(mojo, "goal", "unknown-goal");
        setField(mojo, "lineLength", 100);
        setField(mojo, "indentSize", 2);

        Assertions.assertDoesNotThrow(() -> mojo.execute());
    }

    /**
     * getSingleChild() should throw when the element is missing.
     */
    @Test
    void getSingleChildThrowsWhenElementMissing() throws Exception {
        Document document = parse("<root><a>v</a></root>");

        MojoExecutionException exception = Assertions.assertThrows(MojoExecutionException.class,
                () -> invokeStatic("getSingleChild", new Class<?>[] { Node.class, String.class },
                        document.getDocumentElement(), "missing"));
        Assertions.assertTrue(exception.getMessage().contains("Could not find missing"));
    }

    /**
     * getSingleChild() should throw when multiple elements exist.
     */
    @Test
    void getSingleChildThrowsWhenMultipleElementsExist() throws Exception {
        Document document = parse("<root><a>one</a><a>two</a></root>");

        MojoExecutionException exception = Assertions.assertThrows(MojoExecutionException.class,
                () -> invokeStatic("getSingleChild", new Class<?>[] { Node.class, String.class },
                        document.getDocumentElement(), "a"));
        Assertions.assertTrue(exception.getMessage().contains("Multiple a"));
    }

    /**
     * findSingleChild() should return null when no matching element exists.
     */
    @Test
    void findSingleChildReturnsNullWhenElementMissing() throws Exception {
        Document document = parse("<root><a>v</a></root>");

        Object result = invokeStatic("findSingleChild", new Class<?>[] { Node.class, String.class },
                document.getDocumentElement(), "b");

        Assertions.assertNull(result);
    }

    /**
     * getPropertyFromExpression() should extract simple placeholders and reject other expressions.
     */
    @Test
    void getPropertyFromExpressionHandlesSupportedAndUnsupportedExpressions() throws Exception {
        String extracted = (String) invokeStatic("getPropertyFromExpression", new Class<?>[] { String.class },
                "${directory.skip}");
        String nested = (String) invokeStatic("getPropertyFromExpression", new Class<?>[] { String.class },
                "${outer${inner}}");
        String nonPlaceholder = (String) invokeStatic("getPropertyFromExpression", new Class<?>[] { String.class },
                "literal-value");

        Assertions.assertEquals("directory.skip", extracted);
        Assertions.assertNull(nested);
        Assertions.assertNull(nonPlaceholder);
    }

    /**
     * toLines() should wrap long lines and normalize non-breaking spaces.
     */
    @SuppressWarnings("unchecked")
    @Test
    void toLinesWrapsAndNormalizesSpecialWhitespace() throws Exception {
        List<String> lines = (List<String>) invokeStatic("toLines",
                new Class<?>[] { String.class, int.class, int.class, int.class },
                "\talpha\u00A0beta gamma delta epsilon", 1, 2, 14);

        Assertions.assertTrue(lines.size() > 1, "Expected wrapped output");
        Assertions.assertFalse(lines.stream().anyMatch(line -> line.contains("\u00A0")),
                "Non-breaking spaces should be normalized to regular spaces");
    }

    private static Document parse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static Object invokeStatic(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = HelpMojo.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = HelpMojo.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static int getIntField(Object target, String name) throws Exception {
        Field field = HelpMojo.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

}
