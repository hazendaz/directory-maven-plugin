/*
 *    Copyright 2011-2024 the original author or authors.
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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * The Class DirectoryOfGoal.
 */
@Mojo(name = "directory-of", defaultPhase = LifecyclePhase.INITIALIZE, requiresProject = true, threadSafe = true)
public class DirectoryOfGoal extends AbstractDirectoryGoal {

    /** The Constant DIR_OF_CONTEXT_KEY. */
    protected static final String DIR_OF_CONTEXT_KEY = "directories.directoryOf-";

    /** The project. */
    @Parameter
    private ProjectRef project;

    /** The projects. */
    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    protected List<MavenProject> projects;

    @Override
    protected File findDirectory() throws MojoExecutionException {
        File dir = null;

        final Deque<MavenProject> toCheck = new ArrayDeque<>(projects);
        while (!toCheck.isEmpty()) {
            final MavenProject mavenProject = toCheck.pop();
            if (project.matches(mavenProject)) {
                dir = mavenProject.getBasedir();
                break;
            }

            if (mavenProject.getParent() != null) {
                toCheck.add(mavenProject.getParent());
            }
        }

        if (dir == null) {
            throw new MojoExecutionException("Cannot find directory for project: " + project);
        }

        return dir;
    }

    @Override
    protected String getContextKey() {
        return DIR_OF_CONTEXT_KEY + project;
    }

    @Override
    protected String getLogLabel() {
        return "Directory of " + project;
    }

}
