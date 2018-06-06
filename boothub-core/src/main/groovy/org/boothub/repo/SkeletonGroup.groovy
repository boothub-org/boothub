/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.boothub.repo

import groovy.transform.Canonical
import org.boothub.Version

@Canonical
class SkeletonGroup {
    String name
    String caption
    int usageCount
    int ratingCount
    int ratingSum
    int sortingWeight = 1
    TreeMap<Version, RepoEntry> entries = new TreeMap<>({ v1, v2 -> v2 <=> v1})

    void addRepoEntry(RepoEntry entry) {
        entries[entry.version] = entry
        def lastEntry = entries.lastEntry().value
        name = lastEntry.name
        caption = lastEntry.caption
    }

    void keepOnlyLastEntry() {
        if(entries.size() > 1) {
            def lastEntry = entries.firstEntry()
            entries.clear()
            entries[lastEntry.key] = lastEntry.value
        }
    }

    void keepOnlyMatches(String searchPattern) {
        // TODO - implement
    }
}
