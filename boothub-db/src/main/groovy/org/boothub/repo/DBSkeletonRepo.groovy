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

import groovy.util.logging.Slf4j
import org.boothub.Result

import static org.boothub.Result.Type.ERROR
import static org.boothub.Result.Type.SUCCESS

@Slf4j
class DBSkeletonRepo implements SkeletonRepo {
    final DBApi dbApi
    final DBJobDAO dbJobDAO
    final RepoCache repoCache

    DBSkeletonRepo(DBApi dbApi, DBJobDAO dbJobDAO, RepoCache repoCache) {
        this.dbApi = dbApi
        this.dbJobDAO = dbJobDAO
        this.repoCache = repoCache
        initDB()
    }

    private final void initDB() {
        dbApi.dbExecuteAll(dbJobDAO.initTables())
    }

    Result<Map<String, SkeletonGroup>> getSkeletons(SkeletonSearchOptions options) {
        Map<String, SkeletonGroup> skeletons = [:]

        try {
            def rs = dbApi.dbExecute(dbJobDAO.getSkeletons(skeletonId: options.skeletonId, ownerId: options.ownerId, version: options.version))
            dbJobDAO.getRepoEntries(rs).each { repoEntry ->
                def group = skeletons.computeIfAbsent(repoEntry.id, {id -> new SkeletonGroup()})
                group.addRepoEntry(repoEntry)
            }
            if(options.lastVersionOnly) {
                skeletons.values().each { group -> group.keepOnlyLastEntry() }
            }
            if(options.searchPattern) {
                skeletons.values().each { group -> group.keepOnlyMatches(options.searchPattern) }
            }
            if(!options.compact) {
                skeletons.values().each { group ->
                    group.entries.each { entry ->
                        def repoEntry = entry.value
                        try {
                            def entryPath = repoCache.get(repoEntry, repoEntry.size, repoEntry.sha)
                            entry.value = RepoEntry.fromZipFile(entryPath.toFile())
                            entry.value.url = repoEntry.url
                        } catch (Exception e) {
                            log.error("Cannot retrieve skeleton from $repoEntry.url", e)
                        }
                    }
                }
            }
            def sortedSkeletons = skeletons.sort{a, b -> a.value.name <=> b.value.name}
            new Result(type: SUCCESS, value: sortedSkeletons)
        } catch (Exception e) {
            new Result(type: ERROR, message: "Cannot retrieve skeletons", value: skeletons)
        }
    }
}
