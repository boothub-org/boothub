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
import org.boothub.Version
import org.boothub.repo.table.EntryTable
import org.boothub.repo.table.OwnerTable
import org.boothub.repo.table.SkeletonTable
import org.boothub.repo.table.TagTable
import org.jooq.DSLContext
import org.jooq.impl.TrueCondition

import static org.boothub.Result.Type.ERROR
import static org.boothub.Result.Type.SUCCESS
import static org.jooq.impl.DSL.exists
import static org.jooq.impl.DSL.selectOne

@Slf4j
class DBSkeletonRepo implements SkeletonRepo {
    final DSLContext dsl
    final RepoCache repoCache

    final SkeletonTable skeletonTable
    final EntryTable entryTable
    final OwnerTable ownerTable
    final TagTable tagTable

    DBSkeletonRepo(DSLContext dsl, RepoCache repoCache) {
        this.dsl = dsl
        this.repoCache = repoCache

        this.skeletonTable = new SkeletonTable(dsl)
        this.entryTable = new EntryTable(dsl)
        this.ownerTable = new OwnerTable(dsl)
        this.tagTable = new TagTable(dsl)

        initDB()
    }

    private final void initDB() {
        [skeletonTable, entryTable, ownerTable, tagTable].each {it.createIfNotExists()}
    }

    Result<Map<String, SkeletonGroup>> getSkeletons(SkeletonSearchOptions options) {
        Map<String, SkeletonGroup> skeletons = [:]

        try {
            def entries = getSkeletons(skeletonId: options.skeletonId, ownerId: options.ownerId, version: options.version)
            entries.each { repoEntry ->
                def group = skeletons.computeIfAbsent(repoEntry.id, {id ->
                    def newGroup = new SkeletonGroup()
                    newGroup.usageCount = repoEntry.usageCount
                    newGroup.ratingCount = repoEntry.ratingCount
                    newGroup.ratingSum = repoEntry.ratingSum
                    newGroup.sortingWeight = repoEntry.sortingWeight
                    newGroup.tags = repoEntry.tags
                    newGroup.authors = repoEntry.authors
                    newGroup
                })
                group.addRepoEntry(repoEntry)
            }
            if(options.lastVersionOnly) {
                skeletons.values().each { group -> group.keepOnlyLastEntry() }
            }
            if(options.searchPattern) {
                skeletons.values().each { group -> group.keepOnlyMatches(options.searchPattern) }
            }
            if(!options.compact) {
                skeletons.values().removeAll{ group ->
                    group.entries.entrySet().removeAll{ entry ->
                        def repoEntry = entry.value
                        try {
                            def entryPath = repoCache.get(repoEntry, repoEntry.size, repoEntry.sha)
                            def repoEntryExt = RepoEntry.fromZipFile(entryPath.toFile())
                            entry.value = RepoEntry.copyOf(repoEntryExt)
                            entry.value.url = repoEntry.url
                            if(repoEntry.id != entry.value.id) {
                                throw new IOException("Invalid skeleton id in $repoEntry.url: $entry.value.id. Expected: $repoEntry.id")
                            }
                            if(repoEntry.version != entry.value.version) {
                                throw new IOException("Invalid version in $repoEntry.url: $entry.value.version. Expected: $repoEntry.version")
                            }
                            return false
                        } catch (Exception e) {
                            log.error("Cannot retrieve skeleton $repoEntry.id", e)
                            if(options.includeInvalidEntries) {
                                entry.value.validationError = e.getMessage()
                                return false
                            }
                            return true
                        }
                    }
                    return group.entries.isEmpty()
                }
            }
            def sortedSkeletons = skeletons.sort{a, b -> b.value.sortingWeight <=> a.value.sortingWeight ?: a.value.name <=> b.value.name}
            new Result(type: SUCCESS, value: sortedSkeletons)
        } catch (Exception e) {
            log.error("getSkeletons($options) failed", e)
            new Result(type: ERROR, message: "Cannot retrieve skeletons", value: skeletons)
        }
    }
    List<RepoEntry.Extended> getSkeletons(Map filterOptions = [:]) {
        getSkeletonsResult(filterOptions).map{ r ->
            def entry = new RepoEntry.Extended()
            entry.id = r.getValue(SkeletonTable.COL_SKELETON_ID)
            entry.name = r.getValue(SkeletonTable.COL_NAME)
            entry.caption = r.getValue(SkeletonTable.COL_CAPTION)
            entry.version = Version.fromString(r.getValue(EntryTable.COL_VERSION))
            entry.url = r.getValue(EntryTable.COL_URL)
            entry.size = r.getValue(EntryTable.COL_SIZE)
            entry.sha = r.getValue(EntryTable.COL_SHA)
            entry.usageCount = r.getValue(SkeletonTable.COL_USAGE_COUNT)
            entry.ratingCount = r.getValue(SkeletonTable.COL_RATING_COUNT)
            entry.ratingSum = r.getValue(SkeletonTable.COL_RATING_SUM)
            entry.sortingWeight = r.getValue(SkeletonTable.COL_SORTING_WEIGHT)
            try {
                entry.tags = tagTable.getTags(entry.id)
                entry.authors = ownerTable.getOwnerIds(entry.id)
            } catch (Exception e) {
                log.error("Cannot retrieve tags and authors of ${entry.id}", e)
            }
            entry
        }
    }

    private org.jooq.Result getSkeletonsResult(Map filterOptions = [:]) {
        def query = dsl.select(SkeletonTable.COL_SKELETON_ID, SkeletonTable.COL_NAME, SkeletonTable.COL_CAPTION,
                SkeletonTable.COL_USAGE_COUNT, SkeletonTable.COL_RATING_COUNT, SkeletonTable.COL_RATING_SUM, SkeletonTable.COL_SORTING_WEIGHT,
                EntryTable.COL_VERSION, EntryTable.COL_URL, EntryTable.COL_SIZE, EntryTable.COL_SHA)
                .from(SkeletonTable.TA_SKELETON)
                .join(EntryTable.TA_ENTRY)
                .on(SkeletonTable.COL_SKELETON_ID.equal(EntryTable.COL_SKELETON_ID))
                .where(TrueCondition.INSTANCE)

        if(filterOptions.skeletonId) {
            query = query.and(SkeletonTable.COL_SKELETON_ID.equal(filterOptions.skeletonId))
        }
        if(filterOptions.ownerId) {
            query = query.and(exists(selectOne()
                    .from(OwnerTable.TA_OWNER)
                    .where(OwnerTable.COL_SKELETON_ID.equal(SkeletonTable.COL_SKELETON_ID))
                    .and(OwnerTable.COL_OWNER.equal(filterOptions.ownerId))
            ))
        }
        if(filterOptions.version) {
            query = query.and(EntryTable.COL_VERSION.equal(filterOptions.version))
        }
        def result = query.fetch()
        result
    }
}
