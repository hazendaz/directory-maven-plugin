/*
 *    Copyright 2011-2025 the original author or authors.
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
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * The Class HighestBasedirGoal.
 */
@Mojo(name = "highest-basedir", defaultPhase = LifecyclePhase.INITIALIZE, requiresProject = true, threadSafe = true)
public class HighestBasedirGoal extends AbstractDirectoryGoal {

    /**
     * The Class PathComparator.
     */
    public static final class PathComparator implements Comparator<File> {
        @Override
        public int compare(final File first, final File second) {
            if (System.getProperty("os.name").startsWith("Windows")) {
                return first.getAbsolutePath().compareToIgnoreCase(second.getAbsolutePath());
            }
            return first.getAbsolutePath().compareTo(second.getAbsolutePath());
        }
    }

    /** The Constant HIGHEST_DIR_CONTEXT_KEY. */
    protected static final String HIGHEST_DIR_CONTEXT_KEY = "directories.highestDir";

    /** The projects. */
    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    protected List<MavenProject> projects;

    @Override
    protected File findDirectory() throws MojoExecutionException {
        final Deque<MavenProject> toCheck = new ArrayDeque<>(projects);
        // exclude projects loaded directly from the local repository (super-pom's etc)
        String localRepoBaseDir = session.getRepositorySession().getLocalRepository().getBasedir().getAbsolutePath();

        final List<File> files = new ArrayList<>();
        while (!toCheck.isEmpty()) {
            final MavenProject mavenProject = toCheck.pop();
            if (mavenProject.getBasedir() == null
                    || mavenProject.getBasedir().toString().startsWith(localRepoBaseDir)) {
                // we've hit a parent that was resolved. Don't bother going higher up the hierarchy.
                continue;
            }

            Path path = Path.of(mavenProject.getBasedir().toURI()).normalize();

            if (!files.contains(path.toFile())) {
                // add to zero to pre-sort the paths...the shortest (parent) paths should end up near the top.
                files.add(0, path.toFile());
            }

            if (mavenProject.getParent() != null) {
                toCheck.add(mavenProject.getParent());
            }
        }

        if (files.isEmpty()) {
            throw new MojoExecutionException("No project base directories found! Are you sure you're "
                    + "executing this on a valid Maven project?");
        }

        Collections.sort(files, new PathComparator());
        final File dir = files.get(0);

        if (files.size() > 1) {
            final File next = files.get(1);
            String dirPath = dir.getAbsolutePath();
            String nextPath = next.getAbsolutePath();
            if (System.getProperty("os.name").startsWith("Windows")) {
                dirPath = dirPath.toLowerCase(Locale.ENGLISH);
                nextPath = nextPath.toLowerCase(Locale.ENGLISH);
            }
            if (!nextPath.startsWith(dirPath)) {
                getLog().error("Candidate 1: " + dirPath);
                getLog().error("Candidate 2: " + nextPath);
                throw new MojoExecutionException("Cannot find a single highest directory for this project set. "
                        + "First two candidates directories don't share a common root." + " Candidate 1: " + dirPath
                        + " Candidate 2: " + nextPath);
            }
        }

        return dir;
    }

    @Override
    protected String getContextKey() {
        return HIGHEST_DIR_CONTEXT_KEY;
    }

    @Override
    protected String getLogLabel() {
        return "Highest basedir";
    }

}
