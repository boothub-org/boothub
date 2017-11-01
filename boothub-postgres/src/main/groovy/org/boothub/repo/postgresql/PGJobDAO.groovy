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
package org.boothub.repo.postgresql

import groovy.util.logging.Slf4j
import org.boothub.repo.*

import java.sql.ResultSet

@Slf4j
class PGJobDAO implements DBJobDAO, StatementApi {
    private static final String SQL_CREATE_SE_ENTRY = "create sequence if not exists se_entry start 1"
    private static final String SQL_CREATE_SE_OWNER = "create sequence if not exists se_owner start 1"
    private static final String SQL_CREATE_SE_TAG = "create sequence if not exists se_tag start 1"

    private static final String SQL_CREATE_TA_SKELETON = """
        create table if not exists ta_skeleton (
            skeleton_id text primary key,
            name text not null,
            caption text
        )
        """

    private static final String SQL_CREATE_TA_ENTRY = """
        create table if not exists ta_entry (
            id bigint primary key default nextval('se_entry'),
            skeleton_id text not null,
            version text not null,
            url text not null,
            size bigint not null,
            sha text not null,
            created_on timestamp not null default now(),
            updated_on timestamp not null default now(),
            usage_count bigint not null default 0,
            rating_count bigint not null default 0,
            rating_sum bigint not null default 0,
            constraint fk_entry_skeleton_id foreign key(skeleton_id) references ta_skeleton on delete cascade on update cascade,
            constraint uq_entry unique(skeleton_id, version)
        )
        """

    private static final String SQL_CREATE_TA_OWNER = """
        create table if not exists ta_owner (
            id bigint primary key default nextval('se_owner'),
            skeleton_id text not null,
            owner text not null,
            constraint fk_owner_skeleton_id foreign key(skeleton_id) references ta_skeleton on delete cascade on update cascade,
            constraint uq_owner unique(skeleton_id, owner)
        )
        """

    private static final String SQL_CREATE_TA_TAG = """
        create table if not exists ta_tag (
            id bigint primary key default nextval('se_tag'),
            skeleton_id text not null,
            tag text not null,
            constraint fk_tag_skeleton_id foreign key(skeleton_id) references ta_skeleton on delete cascade on update cascade,
            constraint uq_tag unique(skeleton_id, tag)
        )
        """

    private static final String SQL_INSERT_INTO_TA_SKELETON = """
        insert into ta_skeleton(skeleton_id, name, caption) values(?, ?, ?)
        on conflict(skeleton_id) do update set name=excluded.name, caption=excluded.caption
        """

    private static final String SQL_DELETE_TA_SKELETON = """
        delete from ta_entry where skeleton_id=?
        """

    private static final String SQL_INSERT_INTO_TA_ENTRY = """
        insert into ta_entry(skeleton_id, version, url, size, sha) values(?, ?, ?, ?, ?)
        on conflict on constraint uq_entry do update set url=excluded.url, size=excluded.size, sha=excluded.sha, updated_on=excluded.updated_on
        """

    private static final String SQL_DELETE_TA_ENTRY = """
        delete from ta_entry where skeleton_id=? and version=?
        """

    private static final String SQL_INSERT_INTO_TA_OWNER = """
        insert into ta_owner(skeleton_id, owner) values(?, ?)
        on conflict on constraint uq_owner do nothing
        """

    private static final String SQL_DELETE_TA_OWNER = """
        delete from ta_owner where skeleton_id=? and owner=?
        """

    private static final String SQL_INSERT_INTO_TA_TAG = """
        insert into ta_tag(skeleton_id, tag) values(?, ?)
        on conflict on constraint uq_tag do nothing
        """

    private static final String SQL_DELETE_TA_TAG = """
        delete from ta_tag 
        where skeleton_id = ? and tag=?
        """

    private static final String SQL_QUERY_SKELETONS = """
        select s.skeleton_id, s.name, s.caption, e.version, e.url, e.size, e.sha from ta_skeleton s
        join ta_entry e on s.skeleton_id = e.skeleton_id
        where 1=1
    """

    private static final String SQL_FILTER_SKELETONS_SKELETON_ID = "and s.skeleton_id = ?"
    private static final String SQL_FILTER_SKELETONS_VERSION = "and e.version = ?"
    private static final String SQL_FILTER_SKELETONS_OWNER = "and exists (select * from ta_owner o where o.skeleton_id = s.skeleton_id and o.owner = ?)"

    private static final String SQL_QUERY_OWNERS = "select o.owner from ta_owner o where o.skeleton_id=?"
    private static final String SQL_QUERY_TAGS = "select t.tag from ta_tag t where t.skeleton_id=?"


    @Override
    List<DBJob<Integer>> initTables() {
        [
            getUpdateJob(SQL_CREATE_SE_ENTRY),
            getUpdateJob(SQL_CREATE_SE_OWNER),
            getUpdateJob(SQL_CREATE_SE_TAG),

            getUpdateJob(SQL_CREATE_TA_SKELETON),
            getUpdateJob(SQL_CREATE_TA_ENTRY),
            getUpdateJob(SQL_CREATE_TA_OWNER),
            getUpdateJob(SQL_CREATE_TA_TAG),
        ]
    }

    @Override
    DBJob<Integer> addOrReplaceSkeleton(String skeletonId, String name, String caption) {
        new DBJob.PreparedUpdate(SQL_INSERT_INTO_TA_SKELETON)
                .configure {stmt -> stmt.setString(1, skeletonId)}
                .configure {stmt -> stmt.setString(2, name)}
                .configure {stmt -> stmt.setString(3, caption)}
    }

    @Override
    DBJob<Integer> deleteSkeleton(String skeletonId) {
        new DBJob.PreparedUpdate(SQL_DELETE_TA_SKELETON)
                .configure {stmt -> stmt.setString(1, skeletonId)}
    }

    @Override
    DBJob<Integer> addOrReplaceEntry(String skeletonId, String version, String url, long size, String sha) {
        new DBJob.PreparedUpdate(SQL_INSERT_INTO_TA_ENTRY)
                .configure {stmt -> stmt.setString(1, skeletonId)}
                .configure {stmt -> stmt.setString(2, version)}
                .configure {stmt -> stmt.setString(3, url)}
                .configure {stmt -> stmt.setLong(4, size)}
                .configure {stmt -> stmt.setString(5, sha)}
    }

    @Override
    DBJob<Integer> deleteEntry(String skeletonId, String version) {
        new DBJob.PreparedUpdate(SQL_DELETE_TA_ENTRY)
                .configure {stmt -> stmt.setString(1, skeletonId)}
                .configure {stmt -> stmt.setString(2, version)}
    }

    @Override
    DBJob<Integer> addOwner(String skeletonId, String ownerId) {
        new DBJob.PreparedUpdate(SQL_INSERT_INTO_TA_OWNER)
                .configure {stmt -> stmt.setString(1, skeletonId)}
                .configure {stmt -> stmt.setString(2, ownerId)}
    }

    @Override
    DBJob<Integer> deleteOwner(String skeletonId, String ownerId) {
        new DBJob.PreparedUpdate(SQL_DELETE_TA_OWNER)
                .configure {stmt -> stmt.setString(1, skeletonId)}
                .configure {stmt -> stmt.setString(2, ownerId)}
    }

    @Override
    DBJob<ResultSet> getOwnerIds(String skeletonId) {
        new DBJob.PreparedQuery(SQL_QUERY_OWNERS)
            .configure {stmt -> stmt.setString(1, skeletonId)}
    }

    @Override
    DBJob<Integer> addTag(String skeletonId, String tag) {
        new DBJob.PreparedUpdate(SQL_INSERT_INTO_TA_TAG)
                .configure {stmt -> stmt.setString(1, skeletonId)}
                .configure {stmt -> stmt.setString(2, tag)}
    }

    @Override
    DBJob<Integer> deleteTag(String skeletonId, String tag) {
        new DBJob.PreparedUpdate(SQL_DELETE_TA_TAG)
                .configure {stmt -> stmt.setString(1, skeletonId)}
                .configure {stmt -> stmt.setString(2, tag)}
    }

    @Override
    DBJob<ResultSet> getTags(String skeletonId) {
        new DBJob.PreparedQuery(SQL_QUERY_TAGS)
                .configure {stmt -> stmt.setString(1, skeletonId)}
    }

    @Override
    DBJob<ResultSet> getSkeletons(Map filterOptions = [:]) {
        def sql = SQL_QUERY_SKELETONS
        if(filterOptions.skeletonId) {
            sql += SQL_FILTER_SKELETONS_SKELETON_ID
        }
        if(filterOptions.ownerId) {
            sql += SQL_FILTER_SKELETONS_OWNER
        }
        if(filterOptions.version) {
            sql += SQL_FILTER_SKELETONS_VERSION
        }

        def query = new DBJob.PreparedQuery(sql)

        int prmIndex = 1
        if(filterOptions.skeletonId) {
            query.configure {stmt -> stmt.setString(prmIndex++, filterOptions.skeletonId)}
        }
        if(filterOptions.ownerId) {
            query.configure {stmt -> stmt.setString(prmIndex++, filterOptions.ownerId)}
        }
        if(filterOptions.version) {
            query.configure {stmt -> stmt.setString(prmIndex++, filterOptions.version)}
        }

        query
    }

    @Override
    RepoEntry getRepoEntry(ResultSet rsSkeletons) {
        RepoEntry entry = new RepoEntry()
        entry.id = rsSkeletons.getString('skeleton_id')
        entry.name = rsSkeletons.getString('name')
        entry.caption = rsSkeletons.getString('caption')
        entry.version = Version.fromString(rsSkeletons.getString('version'))
        entry.url = rsSkeletons.getString('url')
        entry.size = rsSkeletons.getLong('size')
        entry.sha = rsSkeletons.getString('sha')
        entry
    }
}
