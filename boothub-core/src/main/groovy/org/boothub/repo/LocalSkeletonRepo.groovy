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

import org.boothub.Result

class LocalSkeletonRepo implements SkeletonRepo {
    private final String baseRepoDir
    private final Map<String, SkeletonGroup> allSkeletons = [:]

    LocalSkeletonRepo(String baseRepoDir = '.') {
        this.baseRepoDir = baseRepoDir
        [
                'template-simple-java',
                'template-simple-groovy',
                'template-simple-kotlin',
                'template-java-groovy',
                'meta-template'
        ].each { addSkeleton(it) }
    }

    private final void addSkeleton(String name) {
        def dir = new File(baseRepoDir, "../boothub-$name/skeleton")
        if(!dir.directory) throw new IllegalArgumentException("Directory not found: $dir.absolutePath")
        def repoEntry = RepoEntry.fromDir(dir)

        allSkeletons[repoEntry.id] = new SkeletonGroup(name: repoEntry.name, caption: repoEntry.caption, entries: [(repoEntry.version): repoEntry])
    }

    @Override
    Result<Map<String, SkeletonGroup>> getSkeletons(SkeletonSearchOptions options) {
        def skeletons = allSkeletons.findAll {
            def skeletonId = it.key
            def group = it.value
            if (options.skeletonId && skeletonId != options.skeletonId) return false
            if (options.version && group.entries[0].version != options.version) return false
            true
        }
        if(options.searchPattern) {
            skeletons.values().each { group -> group.keepOnlyMatches(options.searchPattern) }
        }
        def sortedSkeletons = skeletons.sort{a, b -> a.value.name <=> b.value.name}
        new Result(type: Result.Type.SUCCESS, value: sortedSkeletons)
    }
}
