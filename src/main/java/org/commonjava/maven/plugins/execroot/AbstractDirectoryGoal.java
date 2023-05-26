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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * The Class AbstractDirectoryGoal.
 */
public abstract class AbstractDirectoryGoal extends AbstractMojo {

    /** The property. */
    @Parameter(defaultValue = "dirProperty", required = true)
    protected String property;

    /** The current project. */
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject currentProject;

    /** The session. */
    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    /** The quiet. */
    @Parameter(defaultValue = "false")
    protected boolean quiet;

    /** The system property. */
    @Parameter(defaultValue = "false")
    protected boolean systemProperty;

    /**
     * Skip run of plugin.
     * 
     * @since 1.1.0
     */
    @Parameter(defaultValue = "false", property = "directory.skip")
    private boolean skip;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        // Check if plugin run should be skipped
        if (this.skip) {
            getLog().info("Directory Plugin is skipped");
            return;
        }

        File execRoot;
        synchronized (session) {
            final String key = getContextKey();
            execRoot = (File) getPluginContext().get(key);
            if (execRoot == null) {
                execRoot = findDirectory();
                getPluginContext().put(key, execRoot);
            }
        }

        if (!quiet) {
            getLog().info(getLogLabel() + " set to: " + execRoot);
        }

        currentProject.getProperties().setProperty(property, execRoot.getAbsolutePath());

        if (systemProperty) {
            String existingValue = System.getProperty(property);
            if (existingValue == null) {
                System.setProperty(property, execRoot.getAbsolutePath());
            }
        }

        if (getLog().isDebugEnabled()) {
            final StringWriter str = new StringWriter();
            currentProject.getProperties().list(new PrintWriter(str));

            getLog().debug("After setting property '" + property + "', project properties are:\n\n" + str);
        }
    }

    /**
     * Gets the log label.
     *
     * @return the log label
     */
    protected abstract String getLogLabel();

    /**
     * Find directory.
     *
     * @return the file
     *
     * @throws MojoExecutionException
     *             the mojo execution exception
     */
    protected abstract File findDirectory() throws MojoExecutionException;

    /**
     * Gets the context key.
     *
     * @return the context key
     */
    protected abstract String getContextKey();

}
