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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link HighestBasedirGoal}.
 */
public class HighestBasedirGoalTest {

    /** Temporary directory for creating project structures. */
    @TempDir
    File tempDir;

    /**
     * {@link HighestBasedirGoal#getContextKey()} must return the well-known constant.
     */
    @Test
    void getContextKeyReturnsCorrectValue() {
        HighestBasedirGoal mojo = new HighestBasedirGoal();
        Assertions.assertEquals(HighestBasedirGoal.HIGHEST_DIR_CONTEXT_KEY, mojo.getContextKey());
    }

    /**
     * {@link HighestBasedirGoal#getLogLabel()} must return a human-readable label.
     */
    @Test
    void getLogLabelReturnsCorrectValue() {
        HighestBasedirGoal mojo = new HighestBasedirGoal();
        Assertions.assertEquals("Highest basedir", mojo.getLogLabel());
    }

    /**
     * With a single project the returned directory must equal that project's basedir.
     */
    @Test
    void findDirectoryWithSingleProjectReturnsItsBasedir() throws Exception {
        File projectDir = new File(tempDir, "project");
        projectDir.mkdirs();

        TestMavenProject project = new TestMavenProject("com.example", "artifact", projectDir);
        HighestBasedirGoal mojo = createMojo(List.of(project), new File(tempDir, "localrepo"));

        File result = mojo.findDirectory();

        Assertions.assertEquals(projectDir, result);
    }

    /**
     * With a parent directory that contains a child, the parent (alphabetically first path) must be returned.
     */
    @Test
    void findDirectoryWithParentChildHierarchyReturnsParent() throws Exception {
        File parentDir = new File(tempDir, "parent");
        File childDir = new File(parentDir, "child");
        parentDir.mkdirs();
        childDir.mkdirs();

        TestMavenProject parent = new TestMavenProject("com.example", "parent", parentDir);
        TestMavenProject child = new TestMavenProject("com.example", "child", childDir);

        HighestBasedirGoal mojo = createMojo(Arrays.asList(parent, child), new File(tempDir, "localrepo"));

        File result = mojo.findDirectory();

        Assertions.assertEquals(parentDir, result);
    }

    /**
     * When all projects are located inside the local repository they are all filtered out, and a
     * {@link MojoExecutionException} must be thrown.
     */
    @Test
    void findDirectoryThrowsWhenAllProjectsAreInsideLocalRepository() throws Exception {
        File localRepo = new File(tempDir, "localrepo");
        // Project lives inside the local repository
        File insideRepoDir = new File(localRepo, "com/example/artifact/1.0");
        insideRepoDir.mkdirs();

        TestMavenProject project = new TestMavenProject("com.example", "artifact", insideRepoDir);
        HighestBasedirGoal mojo = createMojo(List.of(project), localRepo);

        Assertions.assertThrows(MojoExecutionException.class, () -> mojo.findDirectory());
    }

    /**
     * When two projects are in sibling directories that do not share a common root, a {@link MojoExecutionException}
     * must be thrown.
     */
    @Test
    void findDirectoryThrowsWhenProjectsDoNotShareCommonRoot() throws Exception {
        File rootA = new File(tempDir, "rootA");
        File rootB = new File(tempDir, "rootB");
        rootA.mkdirs();
        rootB.mkdirs();

        TestMavenProject projectA = new TestMavenProject("com.example", "a", rootA);
        TestMavenProject projectB = new TestMavenProject("com.example", "b", rootB);

        HighestBasedirGoal mojo = createMojo(Arrays.asList(projectA, projectB), new File(tempDir, "localrepo"));

        Assertions.assertThrows(MojoExecutionException.class, () -> mojo.findDirectory());
    }

    /**
     * Projects whose basedir starts with the local repository path must be ignored, and only the project outside the
     * repository is returned.
     */
    @Test
    void findDirectorySkipsProjectsInsideLocalRepository() throws Exception {
        File localRepo = new File(tempDir, "localrepo");
        File projectInsideRepo = new File(localRepo, "com/example/artifact/1.0");
        projectInsideRepo.mkdirs();

        File realProjectDir = new File(tempDir, "realproject");
        realProjectDir.mkdirs();

        TestMavenProject insideRepo = new TestMavenProject("com.example", "inside-repo", projectInsideRepo);
        TestMavenProject realProject = new TestMavenProject("com.example", "real", realProjectDir);

        HighestBasedirGoal mojo = createMojo(Arrays.asList(insideRepo, realProject), localRepo);

        File result = mojo.findDirectory();

        Assertions.assertEquals(realProjectDir, result);
    }

    /**
     * execute() must set the project property to the highest basedir path.
     */
    @Test
    void executeSetsMavenProjectProperty() throws Exception {
        File projectDir = new File(tempDir, "project");
        projectDir.mkdirs();

        TestMavenProject reactor = new TestMavenProject("com.example", "artifact", projectDir);
        TestMavenProject currentProject = new TestMavenProject("test", "current", tempDir);

        HighestBasedirGoal mojo = createMojo(List.of(reactor), new File(tempDir, "localrepo"));
        setField(mojo, "currentProject", currentProject);

        mojo.execute();

        Assertions.assertEquals(projectDir.getAbsolutePath(),
                currentProject.getProperties().getProperty("testHighestBasedirProperty"),
                "Project property must be set to the highest basedir");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a fully-wired {@link HighestBasedirGoal} with the given reactor projects and local repository base
     * directory.
     */
    private HighestBasedirGoal createMojo(List<TestMavenProject> projects, File localRepoDir) throws Exception {
        HighestBasedirGoal mojo = new HighestBasedirGoal();
        mojo.setLog(new SilentLog());
        mojo.setPluginContext(new HashMap<>());
        setField(mojo, "property", "testHighestBasedirProperty");
        setField(mojo, "quiet", false);
        setField(mojo, "systemProperty", false);
        setField(mojo, "skip", false);
        setField(mojo, "projects", projects);

        TestMavenProject currentProject = projects.isEmpty() ? new TestMavenProject("test", "current", tempDir)
                : projects.get(0);
        setField(mojo, "currentProject", currentProject);

        // Mock the MavenSession → RepositorySystemSession → LocalRepository chain
        LocalRepository localRepository = new LocalRepository(localRepoDir);
        RepositorySystemSession repositorySession = mock(RepositorySystemSession.class);
        when(repositorySession.getLocalRepository()).thenReturn(localRepository);
        MavenSession session = mock(MavenSession.class);
        when(session.getRepositorySession()).thenReturn(repositorySession);
        setField(mojo, "session", session);

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
