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
package org.commonjava.maven.plugins.execroot;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Properties;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link ExecutionRootGoal}.
 */
@MojoTest
@ExtendWith(MojoExtension.class)
public class ExecutionRootGoalTest {

    /**
     * {@link ExecutionRootGoal#getContextKey()} must return the well-known constant.
     */
    @Test
    void getContextKeyReturnsCorrectValue() {
        ExecutionRootGoal mojo = new ExecutionRootGoal();
        Assertions.assertEquals(ExecutionRootGoal.EXEC_ROOT_CONTEXT_KEY, mojo.getContextKey());
    }

    /**
     * {@link ExecutionRootGoal#getLogLabel()} must return a human-readable label.
     */
    @Test
    void getLogLabelReturnsCorrectValue() {
        ExecutionRootGoal mojo = new ExecutionRootGoal();
        Assertions.assertEquals("Execution-Root", mojo.getLogLabel());
    }

    /**
     * {@link ExecutionRootGoal#findDirectory()} must return the directory reported by the session.
     */
    @Test
    void findDirectoryReturnsSessionExecutionRootDirectory() throws Exception {
        ExecutionRootGoal mojo = new ExecutionRootGoal();
        MavenSession session = mock(MavenSession.class);
        when(session.getExecutionRootDirectory()).thenReturn(System.getProperty("java.io.tmpdir"));
        setField(mojo, "session", session);

        File result = mojo.findDirectory();

        Assertions.assertEquals(new File(System.getProperty("java.io.tmpdir")), result);
    }

    /**
     * When {@code skip=true} the mojo must return without setting any project property.
     */
    @Test
    void executeWithSkipTrueDoesNotSetProperty() throws Exception {
        TestMavenProject project = new TestMavenProject();
        ExecutionRootGoal mojo = createConfiguredMojo(project, false, false, true);

        mojo.execute();

        Assertions.assertNull(project.getProperties().getProperty("testProperty"));
    }

    /**
     * Normal execution must set the project property to the execution root path.
     */
    @Test
    void executeNormalSetsProjectProperty() throws Exception {
        TestMavenProject project = new TestMavenProject();
        ExecutionRootGoal mojo = createConfiguredMojo(project, false, false, false);

        mojo.execute();

        String value = project.getProperties().getProperty("testProperty");
        Assertions.assertNotNull(value, "Project property must be set after execute()");
        Assertions.assertEquals(new File(System.getProperty("java.io.tmpdir")).getAbsolutePath(), value);
    }

    /**
     * When {@code quiet=true} the property must still be set.
     */
    @Test
    void executeWithQuietStillSetsProperty() throws Exception {
        TestMavenProject project = new TestMavenProject();
        ExecutionRootGoal mojo = createConfiguredMojo(project, true, false, false);

        mojo.execute();

        Assertions.assertNotNull(project.getProperties().getProperty("testProperty"),
                "Property must be set even in quiet mode");
    }

    /**
     * When {@code systemProperty=true} the system property must be set after execution.
     */
    @Test
    void executeWithSystemPropertySetsSystemProperty() throws Exception {
        String propertyName = "test.execRoot.sysprop." + System.nanoTime();
        System.clearProperty(propertyName);
        try {
            TestMavenProject project = new TestMavenProject();
            ExecutionRootGoal mojo = createConfiguredMojo(project, false, true, false);
            setField(mojo, "property", propertyName);

            mojo.execute();

            Assertions.assertNotNull(System.getProperty(propertyName),
                    "System property must be set when systemProperty=true");
        } finally {
            System.clearProperty(propertyName);
        }
    }

    /**
     * When the system property already exists it must not be overridden.
     */
    @Test
    void executeWithSystemPropertyDoesNotOverrideExistingValue() throws Exception {
        String propertyName = "test.execRoot.existing." + System.nanoTime();
        System.setProperty(propertyName, "originalValue");
        try {
            TestMavenProject project = new TestMavenProject();
            ExecutionRootGoal mojo = createConfiguredMojo(project, false, true, false);
            setField(mojo, "property", propertyName);

            mojo.execute();

            Assertions.assertEquals("originalValue", System.getProperty(propertyName),
                    "Pre-existing system property must not be overridden");
        } finally {
            System.clearProperty(propertyName);
        }
    }

    /**
     * The result of findDirectory is cached in the plugin context so that subsequent calls use the cached value without
     * invoking findDirectory again.
     */
    @Test
    void executeUsesPluginContextCacheOnSubsequentCalls() throws Exception {
        File cachedDir = new File(System.getProperty("java.io.tmpdir"));
        TestMavenProject project = new TestMavenProject();
        ExecutionRootGoal mojo = createConfiguredMojo(project, false, false, false);

        // Pre-populate the context with a pre-computed directory
        ((HashMap<String, Object>) getField(mojo, "pluginContext")).put(ExecutionRootGoal.EXEC_ROOT_CONTEXT_KEY,
                cachedDir);

        mojo.execute();

        Assertions.assertEquals(cachedDir.getAbsolutePath(), project.getProperties().getProperty("testProperty"),
                "Cached directory must be used");
    }

    /**
     * The mojo can be looked up by goal name via the harness, verifying that the plugin descriptor is correctly
     * generated and accessible on the test classpath.
     */
    @Test
    @InjectMojo(goal = "execution-root", pom = "src/test/resources/unit/execution-root/pom.xml")
    void mojoCanBeLocatedByGoalNameViaHarness(ExecutionRootGoal mojo) {
        Assertions.assertNotNull(mojo, "Mojo must be found by the harness");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a fully-wired {@link ExecutionRootGoal} ready for {@code execute()}.
     */
    private ExecutionRootGoal createConfiguredMojo(TestMavenProject project, boolean quiet, boolean systemProperty,
            boolean skip) throws Exception {
        ExecutionRootGoal mojo = new ExecutionRootGoal();
        mojo.setLog(new SilentLog());
        mojo.setPluginContext(new HashMap<>());

        setField(mojo, "currentProject", project);
        setField(mojo, "property", "testProperty");
        setField(mojo, "quiet", quiet);
        setField(mojo, "systemProperty", systemProperty);
        setField(mojo, "skip", skip);

        MavenSession session = mock(MavenSession.class);
        when(session.getExecutionRootDirectory()).thenReturn(System.getProperty("java.io.tmpdir"));
        setField(mojo, "session", session);

        return mojo;
    }

    /**
     * A {@link MavenProjectStub} with a stable {@link Properties} instance.
     */
    private static class TestMavenProject extends MavenProjectStub {
        private final Properties properties = new Properties();

        @Override
        public Properties getProperties() {
            return properties;
        }
    }

    /**
     * Sets the value of a (possibly non-public) field declared anywhere in the class hierarchy.
     */
    static void setField(Object target, String name, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + name + "' not found in hierarchy of " + target.getClass());
    }

    /**
     * Gets the value of a (possibly non-public) field declared anywhere in the class hierarchy.
     */
    static Object getField(Object target, String name) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + name + "' not found in hierarchy of " + target.getClass());
    }

}
