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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * The Class ExecutionRootGoal.
 */
@Mojo(name = "execution-root", defaultPhase = LifecyclePhase.INITIALIZE, requiresProject = true, threadSafe = true)
public class ExecutionRootGoal extends AbstractDirectoryGoal {

    /** The Constant EXEC_ROOT_CONTEXT_KEY. */
    protected static final String EXEC_ROOT_CONTEXT_KEY = "directories.execRoot";

    @Override
    protected File findDirectory() {
        return Path.of(session.getExecutionRootDirectory()).toFile();
    }

    @Override
    protected String getContextKey() {
        return EXEC_ROOT_CONTEXT_KEY;
    }

    @Override
    protected String getLogLabel() {
        return "Execution-Root";
    }

}
