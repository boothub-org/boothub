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

abstract trait RepoManager implements SkeletonRepo {
    abstract RepoCache getRepoCache()

    abstract Result<RepoEntry> addSkeleton(String url, String userId)
    abstract Result<Integer> deleteSkeleton(String skeletonId)

    abstract Result<Integer> deleteEntry(String skeletonId, String version)
    Result<Integer> deleteEntry(String skeletonId, Version version) {
        deleteEntry(skeletonId, version.toString())
    }

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
