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

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j

import java.sql.ResultSet

@Slf4j
abstract trait DBJobDAO {
    abstract List<DBJob<Integer>> initTables()
    abstract DBJob<Integer> addOrReplaceSkeleton(String skeletonId, String name, String caption)
    abstract DBJob<Integer> deleteSkeleton(String skeletonId)

    abstract DBJob<Integer> addOrReplaceEntry(String skeletonId, String version, String url, long size, String sha)
    abstract DBJob<Integer> deleteEntry(String skeletonId, String version)

    abstract DBJob<Integer> incrementUsageCounter(String skeletonId, String version)
    abstract DBJob<Integer> addRating(String skeletonId, String version, long rating)

    abstract DBJob<Integer> addOwner(String skeletonId, String ownerId)
    abstract DBJob<Integer> deleteOwner(String skeletonId, String ownerId)
    abstract DBJob<ResultSet> getOwnerIds(String skeletonId)

    abstract DBJob<Integer> addTag(String skeletonId, String tag)
    abstract DBJob<Integer> deleteTag(String skeletonId, String tag)
    abstract DBJob<ResultSet> getTags(String skeletonId)

    /**
     * Supported filterOptions keys: skeletonId, ownerId, version
     */
    abstract DBJob<ResultSet> getSkeletons(Map filterOptions)

    /**
     * @param rsSkeletons a ResultSet obtained by executing the DBJob returned by a call to {@link #getSkeletons(java.util.Map)}
     * @return the RepoEntry associated with the row at the current position in {@code rsSkeletons}
     */
    abstract RepoEntry getRepoEntry(ResultSet rsSkeletons)

    /**
     * @param rsSkeletons a ResultSet obtained by executing the DBJob returned by a call to {@link #getSkeletons(java.util.Map)}
     */
    List<RepoEntry> getRepoEntries(ResultSet rsSkeletons) {
        getValues(rsSkeletons) {getRepoEntry(it)}
    }

    public <V> List<V> getValues(ResultSet rs, @ClosureParams(value=SimpleType, options="java.sql.ResultSet") Closure<V> valueProvider) {
        List<V> values = []
        while(rs.next()) {
            try {
                values << valueProvider.call(rs)
            } catch (Exception e) {
                log.error("Failed to create RepoEntry from resultSet", e)
            }
        }
        values

    }

    List<String> getStrings(ResultSet rs, int columnIndex = 1) {
        getValues(rs) {rs.getString(columnIndex)}
    }
}
