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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The Class ProjectRefTest.
 */
public class ProjectRefTest {

    /** The project ref. */
    ProjectRef projectRef;

    /**
     * Setup.
     */
    @BeforeEach
    void setup() {
        projectRef = new ProjectRef();
    }

    /**
     * Group artifact test.
     */
    @Test
    void groupArtifactTest() {
        projectRef.setGroupId("groupId");
        projectRef.setArtifactId("artifactId");
        Assertions.assertEquals("groupId:artifactId", projectRef.toString());
    }

    /**
     * Validate succeeds when both groupId and artifactId are set.
     */
    @Test
    void validateSucceedsWhenBothFieldsAreSet() throws MojoExecutionException {
        projectRef.setGroupId("com.example");
        projectRef.setArtifactId("my-artifact");
        Assertions.assertDoesNotThrow(() -> projectRef.validate());
    }

    /**
     * Validate fails when groupId is null.
     */
    @Test
    void validateFailsWhenGroupIdIsNull() {
        projectRef.setArtifactId("my-artifact");
        Assertions.assertThrows(MojoExecutionException.class, () -> projectRef.validate());
    }

    /**
     * Validate fails when groupId is empty.
     */
    @Test
    void validateFailsWhenGroupIdIsEmpty() {
        projectRef.setGroupId("");
        projectRef.setArtifactId("my-artifact");
        Assertions.assertThrows(MojoExecutionException.class, () -> projectRef.validate());
    }

    /**
     * Validate fails when groupId is blank (whitespace only).
     */
    @Test
    void validateFailsWhenGroupIdIsBlank() {
        projectRef.setGroupId("   ");
        projectRef.setArtifactId("my-artifact");
        Assertions.assertThrows(MojoExecutionException.class, () -> projectRef.validate());
    }

    /**
     * Validate fails when artifactId is null.
     */
    @Test
    void validateFailsWhenArtifactIdIsNull() {
        projectRef.setGroupId("com.example");
        Assertions.assertThrows(MojoExecutionException.class, () -> projectRef.validate());
    }

    /**
     * Validate fails when artifactId is empty.
     */
    @Test
    void validateFailsWhenArtifactIdIsEmpty() {
        projectRef.setGroupId("com.example");
        projectRef.setArtifactId("");
        Assertions.assertThrows(MojoExecutionException.class, () -> projectRef.validate());
    }

    /**
     * Matches returns true when groupId and artifactId both match.
     */
    @Test
    void matchesReturnsTrueForMatchingProject() {
        projectRef.setGroupId("com.example");
        projectRef.setArtifactId("my-artifact");

        MavenProjectStub project = new MavenProjectStub();
        project.setGroupId("com.example");
        project.setArtifactId("my-artifact");

        Assertions.assertTrue(projectRef.matches(project));
    }

    /**
     * Matches returns false when groupId does not match.
     */
    @Test
    void matchesReturnsFalseForWrongGroupId() {
        projectRef.setGroupId("com.example");
        projectRef.setArtifactId("my-artifact");

        MavenProjectStub project = new MavenProjectStub();
        project.setGroupId("org.other");
        project.setArtifactId("my-artifact");

        Assertions.assertFalse(projectRef.matches(project));
    }

    /**
     * Matches returns false when artifactId does not match.
     */
    @Test
    void matchesReturnsFalseForWrongArtifactId() {
        projectRef.setGroupId("com.example");
        projectRef.setArtifactId("my-artifact");

        MavenProjectStub project = new MavenProjectStub();
        project.setGroupId("com.example");
        project.setArtifactId("other-artifact");

        Assertions.assertFalse(projectRef.matches(project));
    }

    /**
     * Test get and set groupId.
     */
    @Test
    void testGetSetGroupId() {
        projectRef.setGroupId("com.example");
        Assertions.assertEquals("com.example", projectRef.getGroupId());
    }

    /**
     * Test get and set artifactId.
     */
    @Test
    void testGetSetArtifactId() {
        projectRef.setArtifactId("my-artifact");
        Assertions.assertEquals("my-artifact", projectRef.getArtifactId());
    }

}
