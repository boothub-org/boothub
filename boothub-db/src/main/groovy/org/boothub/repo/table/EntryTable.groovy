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
package org.boothub.repo.table

import groovy.transform.InheritConstructors
import org.jooq.*
import java.sql.Timestamp
import org.jooq.impl.DSL

@InheritConstructors
class EntryTable extends RepoTable {
    final static DefaultTable TA_ENTRY = createTable("ta_entry")

    final static Field<Integer> COL_ID = TA_ENTRY.createField("id", int.class)
    final static Field<String> COL_SKELETON_ID = TA_ENTRY.createField("skeleton_id", String.class)
    final static Field<String> COL_VERSION = TA_ENTRY.createField("version", String.class)
    final static Field<String> COL_URL = TA_ENTRY.createField("url", String.class)
    final static Field<String> COL_SIZE = TA_ENTRY.createField("size", long.class)
    final static Field<String> COL_SHA = TA_ENTRY.createField("sha", String.class)
    final static Field<Timestamp> COL_CREATED_ON = TA_ENTRY.createField("created_on", Timestamp.class)
    final static Field<Timestamp> COL_UPDATED_ON = TA_ENTRY.createField("updated_on", Timestamp.class)

    final static Sequence<Integer> SE_ENTRY = DSL.sequence(DSL.name("se_entry"), int.class)

    @Override
    Table<Record> getTable() { TA_ENTRY }

    @Override
    void create() {
        dsl.createTable(TA_ENTRY)
                .column(COL_ID, COL_ID.getDataType().nullable(false))
                .column(COL_SKELETON_ID, COL_SKELETON_ID.getDataType().nullable(false))
                .column(COL_VERSION, COL_VERSION.getDataType().nullable(false))
                .column(COL_URL, COL_URL.getDataType().nullable(false))
                .column(COL_SIZE, COL_SIZE.getDataType().nullable(false))
                .column(COL_SHA, COL_SHA.getDataType().nullable(false))
                .column(COL_CREATED_ON, COL_CREATED_ON.getDataType().nullable(false).defaultValue(DSL.now()))
                .column(COL_UPDATED_ON, COL_UPDATED_ON.getDataType().nullable(false).defaultValue(DSL.now()))
                .execute()

        dsl.alterTable(TA_ENTRY).add(primaryKeyConstraint("pk_entry", TA_ENTRY, COL_ID)).execute()

        dsl.alterTable(TA_ENTRY)
                .add(DSL.constraint(DSL.name("fk_entry_skeleton_id"))
                .foreignKey(COL_SKELETON_ID)
                .references(SkeletonTable.TA_SKELETON, SkeletonTable.COL_SKELETON_ID)
                .onDeleteCascade()
                .onUpdateCascade()
        ).execute()

        dsl.alterTable(TA_ENTRY)
                .add(DSL.constraint(DSL.name("uq_entry"))
                .unique(COL_SKELETON_ID, COL_VERSION))
                .execute()

        dsl.dropSequenceIfExists(SE_ENTRY).execute()
        dsl.createSequence(SE_ENTRY).execute()
    }

    int addOrReplace(String skeletonId, String version, String url, long size, String sha) {
        def colId = -1
        dsl.select(COL_ID)
                .from(TA_ENTRY)
                .where(COL_SKELETON_ID.equal(skeletonId).and(COL_VERSION.equal(version)))
            .fetch()
            .map { r -> colId = r.getValue(COL_ID)}
        if(colId < 0) {
            dsl.insertInto(TA_ENTRY)
                    .columns(COL_ID, COL_SKELETON_ID, COL_VERSION, COL_URL, COL_SIZE, COL_SHA)
                    .values(dsl.nextval(SE_ENTRY), skeletonId, version, url, size, sha)
                    .execute()
        } else {
            dsl.update(TA_ENTRY)
                    .set(COL_URL, url)
                    .set(COL_SIZE, size)
                    .set(COL_SHA, sha)
                    .set(COL_UPDATED_ON, DSL.now())
                    .where(COL_SKELETON_ID.equal(skeletonId).and(COL_VERSION.equal(version)))
                    .execute()
        }
    }

    int delete(String skeletonId, String version) {
        dsl.delete(TA_ENTRY)
            .where(COL_SKELETON_ID.equal(skeletonId))
            .and(COL_VERSION.equal(version))
        .execute()
    }
}
