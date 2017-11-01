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
import org.codehaus.groovy.runtime.IOGroovyMethods

import java.nio.file.Files
import java.sql.Connection

import static org.boothub.Result.Type.ERROR
import static org.boothub.Result.Type.SUCCESS

@Slf4j
class DBRepoManager extends DBSkeletonRepo implements RepoManager {
    DBRepoManager(DBApi dbApi, DBJobDAO dbJobDAO, RepoCache repoCache) {
        super(dbApi, dbJobDAO, repoCache)
    }

    @Override
    Result<RepoEntry> addSkeleton(String url, String userId) {
        if(!userId) return new Result(type: ERROR, message: "userId not set")
        try {
            def path = Files.createTempFile("boothub-", ".zip")
            def file = path.toFile()
            IOGroovyMethods.withCloseable(dbApi.createConnection() as Closeable) { closeable ->
                def connection = closeable as Connection
                connection.setAutoCommit(false)
                try {
                    Util.downloadFile(url, path)
                    RepoEntry repoEntry = RepoEntry.fromZipFile(file)
                    def skeletonId = repoEntry.id
                    def version = repoEntry.version.toString()

                    def ownersResult = getOwners(skeletonId)
                    if(!ownersResult.successful) return new Result(type: ownersResult.type, message: ownersResult.message, value: repoEntry)
                    def owners = ownersResult.value
                    if (owners && !(userId in owners)) return new Result(type: ERROR, message: "$userId is not an owner of $skeletonId", value: repoEntry)
                    def newestEntryResult = getNewestEntry(skeletonId)
                    if(!newestEntryResult.successful) return new Result(type: newestEntryResult.type, message: newestEntryResult.message, value: repoEntry)
                    def newestEntry = newestEntryResult.value
                    if (!newestEntry || newestEntry.version < repoEntry.version) {
                        dbApi.dbExecute(dbJobDAO.addOrReplaceSkeleton(skeletonId, repoEntry.name, repoEntry.caption))
                    }
                    if (!newestEntry) {
                        def addResult = addOwner(skeletonId, userId)
                        if(!addResult.successful) return new Result(type: addResult.type, message: addResult.message, value: repoEntry)
                    }
                    dbApi.dbExecute(dbJobDAO.addOrReplaceEntry(skeletonId, version, url, repoEntry.size, repoEntry.sha))
                    repoEntry.tags.each {tag -> dbApi.dbExecute(dbJobDAO.addTag(skeletonId, tag))}
                    connection.commit()
                    return new Result(type: SUCCESS, value: repoEntry)
                } catch(Exception e) {
                    connection.rollback()
                    throw e
                } finally {
                    file.delete()
                }
            }
        } catch (Exception e) {
            def errMsg = "Failed to add skeleton from url '$url'"
            log.error(errMsg, e)
            return new Result(type: ERROR, message: errMsg)
        }
    }

    @Override
    Result<Integer> deleteSkeleton(String skeletonId) {
        Result.onFailure("Cannot delete skeleton $skeletonId") {
            dbApi.dbExecute(dbJobDAO.deleteSkeleton(skeletonId))
        }
    }

    @Override
    Result<Integer> deleteEntry(String skeletonId, String version) {
        Result.onFailure("Cannot delete skeleton $skeletonId version $version") {
            dbApi.dbExecute(dbJobDAO.deleteEntry(skeletonId, version))
        }
    }

    @Override
    Result<Integer> addOwner(String skeletonId, String ownerId) {
        Result.onFailure("Cannot add owner $ownerId to skeleton $skeletonId") {
            dbApi.dbExecute(dbJobDAO.addOwner(skeletonId, ownerId))
        }
    }

    @Override
    Result<Integer> deleteOwner(String skeletonId, String ownerId) {
        Result.onFailure("Cannot delete owner $ownerId of skeleton $skeletonId") {
            dbApi.dbExecute(dbJobDAO.deleteOwner(skeletonId, ownerId))
        }
    }

    @Override
    Result<List<String>> getOwners(String skeletonId) {
        Result.onFailure("Cannot retrieve the owners of skeleton $skeletonId") {
            def rs = dbApi.dbExecute(dbJobDAO.getOwnerIds(skeletonId))
            dbJobDAO.getStrings(rs)
        }
    }

    @Override
    Result<Integer> addTag(String skeletonId, String tag) {
        Result.onFailure("Cannot add tag $tag to skeleton $skeletonId") {
            dbApi.dbExecute(dbJobDAO.addTag(skeletonId, tag))
        }
    }

    @Override
    Result<Integer> deleteTag(String skeletonId, String tag) {
        Result.onFailure("Cannot delete tag $tag of skeleton $skeletonId") {
            dbApi.dbExecute(dbJobDAO.deleteTag(skeletonId, tag))
        }
    }

    @Override
    Result<List<String>> getTags(String skeletonId) {
        Result.onFailure("Cannot retrieve the tags of skeleton $skeletonId") {
            def rs = dbApi.dbExecute(dbJobDAO.getTags(skeletonId))
            dbJobDAO.getStrings(rs)
        }
    }
}
