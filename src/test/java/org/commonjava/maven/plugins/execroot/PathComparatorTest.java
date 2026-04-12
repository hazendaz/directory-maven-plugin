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

import org.commonjava.maven.plugins.execroot.HighestBasedirGoal.PathComparator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HighestBasedirGoal.PathComparator}.
 */
public class PathComparatorTest {

    /** The comparator under test. */
    private final PathComparator comparator = new PathComparator();

    /**
     * Equal paths compare as zero.
     */
    @Test
    void compareEqualPathsReturnsZero() {
        File file = new File("/some/path/dir");
        Assertions.assertEquals(0, comparator.compare(file, file));
    }

    /**
     * A parent directory (shorter path) sorts before a child directory alphabetically.
     */
    @Test
    void compareParentDirectorySortsBeforeChild() {
        File parent = new File("/some/path");
        File child = new File("/some/path/child");
        // parent path is alphabetically less than child path
        Assertions.assertTrue(comparator.compare(parent, child) < 0);
    }

    /**
     * A child directory sorts after its parent.
     */
    @Test
    void compareChildDirectorySortsAfterParent() {
        File parent = new File("/some/path");
        File child = new File("/some/path/child");
        Assertions.assertTrue(comparator.compare(child, parent) > 0);
    }

    /**
     * Alphabetically earlier path sorts first.
     */
    @Test
    void compareAlphabeticalOrdering() {
        File aDir = new File("/project/a-module");
        File bDir = new File("/project/b-module");
        Assertions.assertTrue(comparator.compare(aDir, bDir) < 0);
        Assertions.assertTrue(comparator.compare(bDir, aDir) > 0);
    }

}
