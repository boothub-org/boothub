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
import org.boothub.Util
import org.boothub.Version

import java.nio.file.Files

import static org.boothub.Result.Type.ERROR

@Slf4j
abstract trait RepoManager implements SkeletonRepo {
    abstract RepoCache getRepoCache()

    abstract Result addEntry(RepoEntry repoEntry, String userId)
    Result<RepoEntry> addSkeleton(String url, String userId) {
        if(!userId) return new Result(type: ERROR, message: "userId not set")
        try {
            def path = Files.createTempFile("boothub-", ".zip")
            def file = path.toFile()
            try {
                Util.downloadFile(url, path)
                RepoEntry repoEntry = RepoEntry.fromZipFile(file)
                repoEntry.url = url
                return addEntry(repoEntry, userId)
            } finally {
                file.delete()
            }
        } catch(Exception e) {
            def errMsg = "Failed to add skeleton from url '$url'"
            log.error(errMsg, e)
            return new Result(type: ERROR, message: errMsg)
        }
    }


    abstract Result<Integer> deleteSkeleton(String skeletonId)

    abstract Result<Integer> deleteEntry(String skeletonId, String version)
    Result<Integer> deleteEntry(String skeletonId, Version version) {
        deleteEntry(skeletonId, version.toString())
    }

    abstract Result<Integer> incrementUsageCounter(String skeletonId, String version)
    abstract Result<Integer> addRating(String skeletonId, String version, long rating)

    abstract Result<Integer> addOwner(String skeletonId, String ownerId)
    abstract Result<Integer> deleteOwner(String skeletonId, String ownerId)
    abstract Result<List<String>> getOwners(String skeletonId)

    abstract Result<Integer> addTag(String skeletonId, String tag)
    abstract Result<Integer> deleteTag(String skeletonId, String tag)
    abstract Result<List<String>> getTags(String skeletonId)

    Result<RepoEntry> getNewestEntry(String skeletonId) {
        Result<Map<String, SkeletonGroup>> result = getSkeletons(new SkeletonSearchOptions(skeletonId: skeletonId, compact: true, lastVersionOnly: true))
        result.mapValueIfNotNullAndSuccess {map -> map[skeletonId]?.entries?.lastEntry()?.value}
    }
}
