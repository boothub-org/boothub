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
import org.jooq.DSLContext

import static org.boothub.Result.Type.ERROR
import static org.boothub.Result.Type.SUCCESS

@Slf4j
class DBRepoManager extends DBSkeletonRepo implements RepoManager {
    DBRepoManager(DSLContext dsl, RepoCache repoCache) {
        super(dsl, repoCache)
    }

    @Override
    Result addEntry(RepoEntry repoEntry, String userId) {
        try {
            def skeletonId = repoEntry.id
            def version = repoEntry.version.toString()

            def ownersResult = getOwners(skeletonId)
            if (!ownersResult.successful) return new Result(type: ownersResult.type, message: ownersResult.message, value: repoEntry)
            def owners = ownersResult.value
            if (owners && !(userId in owners)) return new Result(type: ERROR, message: "$userId is not an owner of $skeletonId", value: repoEntry)
            def newestEntryResult = getNewestEntry(skeletonId)
            if (!newestEntryResult.successful) return new Result(type: newestEntryResult.type, message: newestEntryResult.message, value: repoEntry)
            def newestEntry = newestEntryResult.value
            if (!newestEntry || newestEntry.version < repoEntry.version) {
                skeletonTable.addOrReplace(skeletonId, repoEntry.name, repoEntry.caption)
            }
            if (!newestEntry) {
                def addResult = addOwner(skeletonId, userId)
                if (!addResult.successful) return new Result(type: addResult.type, message: addResult.message, value: repoEntry)
            }
            entryTable.addOrReplace(skeletonId, version, repoEntry.url, repoEntry.size, repoEntry.sha)
            repoEntry.tags.each { tag -> tagTable.add(skeletonId, tag) }
        } catch (Exception e) {
            log.error("Failed to insert $repoEntry.id", e)
            return new Result(type: ERROR, message: e.message, value: repoEntry)
        }
        return new Result(type: SUCCESS, value: repoEntry)
    }

    @Override
    Result<Integer> deleteSkeleton(String skeletonId) {
        Result.onFailure("Cannot delete skeleton $skeletonId") {
            skeletonTable.delete(skeletonId)
        }
    }

    @Override
    Result<Integer> deleteEntry(String skeletonId, String version) {
        Result.onFailure("Cannot delete skeleton $skeletonId version $version") {
            entryTable.delete(skeletonId, version)
        }
    }

    @Override
    Result<Integer> incrementUsageCounter(String skeletonId) {
        Result.onFailure("Cannot increment the usage counter of skeleton $skeletonId") {
            skeletonTable.incrementUsageCounter(skeletonId)
        }
    }

    @Override
    Result<Integer> addRating(String skeletonId, long rating) {
        Result.onFailure("Cannot add rating $rating to skeleton $skeletonId") {
            skeletonTable.addRating(skeletonId, rating)
        }
    }

    @Override
    Result<Integer> addOwner(String skeletonId, String ownerId) {
        Result.onFailure("Cannot add owner $ownerId to skeleton $skeletonId") {
            ownerTable.add(skeletonId, ownerId)
        }
    }

    @Override
    Result<Integer> deleteOwner(String skeletonId, String ownerId) {
        Result.onFailure("Cannot delete owner $ownerId of skeleton $skeletonId") {
            ownerTable.delete(skeletonId, ownerId)
        }
    }

    @Override
    Result<List<String>> getOwners(String skeletonId) {
        Result.onFailure("Cannot retrieve the owners of skeleton $skeletonId") {
            ownerTable.getOwnerIds(skeletonId)
        }
    }

    @Override
    Result<Integer> addTag(String skeletonId, String tag) {
        Result.onFailure("Cannot add tag $tag to skeleton $skeletonId") {
            tagTable.add(skeletonId, tag)
        }
    }

    @Override
    Result<Integer> deleteTag(String skeletonId, String tag) {
        Result.onFailure("Cannot delete tag $tag of skeleton $skeletonId") {
            tagTable.delete(skeletonId, tag)
        }
    }

    @Override
    Result<List<String>> getTags(String skeletonId) {
        Result.onFailure("Cannot retrieve the tags of skeleton $skeletonId") {
            tagTable.getTags(skeletonId)
        }
    }
}
