/*
 * Copyright 2018 the original author or authors.
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

class ReadOnlyJsonRepoManager extends JsonSkeletonRepo implements RepoManager {
    final RepoCache repoCache

    ReadOnlyJsonRepoManager(String repoJsonUrl = DEFAULT_REPO_JSON_URL, RepoCache repoCache = new DefaultRepoCache()) {
        super(repoJsonUrl)
        this.repoCache = repoCache
    }

    @Override
    Result addEntry(RepoEntry.Extended repoEntry, String userId) {
        throw new UnsupportedOperationException()
    }

    @Override
    Result<Integer> deleteSkeleton(String skeletonId) {
        throw new UnsupportedOperationException()
    }

    @Override
    Result<Integer> deleteEntry(String skeletonId, String version) {
        throw new UnsupportedOperationException()
    }

    @Override
    Result<Integer> incrementUsageCounter(String skeletonId) {
        throw new UnsupportedOperationException()
    }

    @Override
    Result<Integer> addRating(String skeletonId, long rating) {
        throw new UnsupportedOperationException()
    }

    @Override
    Result<Integer> addOwner(String skeletonId, String ownerId) {
        throw new UnsupportedOperationException()
    }

    @Override
    Result<Integer> deleteOwner(String skeletonId, String ownerId) {
        throw new UnsupportedOperationException()
    }

    @Override
    Result<List<String>> getOwners(String skeletonId) {
        throw new UnsupportedOperationException()
    }

    @Override
    Result<Integer> addTag(String skeletonId, String tag) {
        throw new UnsupportedOperationException()
    }

    @Override
    Result<Integer> deleteTag(String skeletonId, String tag) {
        throw new UnsupportedOperationException()
    }

    @Override
    Result<List<String>> getTags(String skeletonId) {
        throw new UnsupportedOperationException()
    }
}
