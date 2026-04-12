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

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link DirectoryOfGoal}.
 */
public class DirectoryOfGoalTest {

    /** Temporary directory provided by JUnit 5 for creating project base directories. */
    @TempDir
    File tempDir;

    /**
     * {@link DirectoryOfGoal#getContextKey()} must include the project coordinates.
     */
    @Test
    void getContextKeyContainsProjectCoordinates() throws Exception {
        DirectoryOfGoal mojo = createMojoWithProject("com.example", "my-artifact");
        String key = mojo.getContextKey();
        Assertions.assertTrue(key.startsWith(DirectoryOfGoal.DIR_OF_CONTEXT_KEY),
                "Context key must start with the well-known prefix");
        Assertions.assertTrue(key.contains("com.example"), "Context key must contain groupId");
        Assertions.assertTrue(key.contains("my-artifact"), "Context key must contain artifactId");
    }

    /**
     * {@link DirectoryOfGoal#getLogLabel()} must include the project coordinates.
     */
    @Test
    void getLogLabelContainsProjectCoordinates() throws Exception {
        DirectoryOfGoal mojo = createMojoWithProject("com.example", "my-artifact");
        String label = mojo.getLogLabel();
        Assertions.assertTrue(label.contains("com.example"), "Log label must contain groupId");
        Assertions.assertTrue(label.contains("my-artifact"), "Log label must contain artifactId");
    }

    /**
     * {@link DirectoryOfGoal#findDirectory()} must return the basedir of the matched project.
     */
    @Test
    void findDirectoryReturnsBasedirOfMatchedProject() throws Exception {
        File projectDir = new File(tempDir, "my-project");
        projectDir.mkdirs();

        TestMavenProject target = new TestMavenProject("com.example", "target-artifact", projectDir);
        TestMavenProject other = new TestMavenProject("com.example", "other-artifact", tempDir);

        DirectoryOfGoal mojo = createMojoWithProject("com.example", "target-artifact");
        setField(mojo, "projects", Arrays.asList(other, target));

        File result = mojo.findDirectory();

        Assertions.assertEquals(projectDir, result);
    }

    /**
     * {@link DirectoryOfGoal#findDirectory()} must traverse parent projects when the direct match is not in the reactor
     * list itself but is reachable via parent links.
     */
    @Test
    void findDirectoryFindsProjectViaParentChain() throws Exception {
        File parentDir = new File(tempDir, "parent");
        parentDir.mkdirs();

        TestMavenProject parentProject = new TestMavenProject("com.example", "parent-artifact", parentDir);

        // A child project whose parent is the one we are looking for
        TestMavenProject childProject = new TestMavenProject("com.example", "child-artifact",
                new File(tempDir, "child"));
        childProject.setParent(parentProject);

        DirectoryOfGoal mojo = createMojoWithProject("com.example", "parent-artifact");
        setField(mojo, "projects", List.of(childProject));

        File result = mojo.findDirectory();

        Assertions.assertEquals(parentDir, result);
    }

    /**
     * {@link DirectoryOfGoal#findDirectory()} must throw when no project matches.
     */
    @Test
    void findDirectoryThrowsWhenProjectNotFound() throws Exception {
        TestMavenProject project = new TestMavenProject("com.example", "different-artifact", tempDir);

        DirectoryOfGoal mojo = createMojoWithProject("com.example", "nonexistent-artifact");
        setField(mojo, "projects", List.of(project));

        Assertions.assertThrows(MojoExecutionException.class, () -> mojo.findDirectory());
    }

    /**
     * execute() sets the project property to the matched directory when everything is configured.
     */
    @Test
    void executeSetsMavenProjectProperty() throws Exception {
        File projectDir = new File(tempDir, "my-project");
        projectDir.mkdirs();

        TestMavenProject reactor = new TestMavenProject("com.example", "target-artifact", projectDir);
        TestMavenProject currentProject = new TestMavenProject("test.group", "test-current", tempDir);

        DirectoryOfGoal mojo = createMojoWithProject("com.example", "target-artifact");
        setField(mojo, "projects", List.of(reactor));
        setField(mojo, "currentProject", currentProject);

        mojo.execute();

        Assertions.assertEquals(projectDir.getAbsolutePath(),
                currentProject.getProperties().getProperty("testDirOfProperty"),
                "Property must be set to the matched project basedir");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link DirectoryOfGoal} configured with a target project reference.
     */
    private DirectoryOfGoal createMojoWithProject(String groupId, String artifactId) throws Exception {
        ProjectRef ref = new ProjectRef();
        ref.setGroupId(groupId);
        ref.setArtifactId(artifactId);

        DirectoryOfGoal mojo = new DirectoryOfGoal();
        mojo.setLog(new SilentLog());
        mojo.setPluginContext(new HashMap<>());
        setField(mojo, "project", ref);
        setField(mojo, "property", "testDirOfProperty");
        setField(mojo, "quiet", false);
        setField(mojo, "systemProperty", false);
        setField(mojo, "skip", false);

        TestMavenProject currentProject = new TestMavenProject("test.group", "test-current", tempDir);
        setField(mojo, "currentProject", currentProject);

        return mojo;
    }

    /**
     * A {@link MavenProjectStub} with stable {@link Properties} and configurable basedir.
     */
    static class TestMavenProject extends MavenProjectStub {
        private final Properties properties = new Properties();
        private final File basedir;

        TestMavenProject(String groupId, String artifactId, File basedir) {
            setGroupId(groupId);
            setArtifactId(artifactId);
            this.basedir = basedir;
        }

        @Override
        public Properties getProperties() {
            return properties;
        }

        @Override
        public File getBasedir() {
            return basedir;
        }
    }

    /**
     * Sets the value of a (possibly non-public) field declared anywhere in the class hierarchy.
     */
    private static void setField(Object target, String name, Object value) throws Exception {
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

}
