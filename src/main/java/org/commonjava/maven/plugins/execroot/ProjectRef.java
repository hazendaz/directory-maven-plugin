/*
 *    Copyright 2011-2023 the original author or authors.
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
import org.apache.maven.project.MavenProject;

/**
 * The Class ProjectRef.
 */
public class ProjectRef {

    /** The group id. */
    private String groupId;

    /** The artifact id. */
    private String artifactId;

    /**
     * Gets the group id.
     *
     * @return the group id
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Sets the group id.
     *
     * @param groupId
     *            the new group id
     */
    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    /**
     * Gets the artifact id.
     *
     * @return the artifact id
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Sets the artifact id.
     *
     * @param artifactId
     *            the new artifact id
     */
    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Validate.
     *
     * @throws MojoExecutionException
     *             the mojo execution exception
     */
    public void validate() throws MojoExecutionException {
        if (empty(groupId) || empty(artifactId)) {
            throw new MojoExecutionException("Project references must contain groupId AND artifactId.");
        }
    }

    /**
     * Empty.
     *
     * @param str
     *            the str
     *
     * @return true, if successful
     */
    private boolean empty(final String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * Matches.
     *
     * @param project
     *            the project
     *
     * @return true, if successful
     */
    public boolean matches(final MavenProject project) {
        return project.getGroupId().equals(groupId) && project.getArtifactId().equals(artifactId);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId;
    }

}
