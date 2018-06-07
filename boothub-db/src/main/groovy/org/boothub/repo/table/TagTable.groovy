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
import org.jooq.impl.DSL

import java.time.Year

@InheritConstructors
class TagTable extends RepoTable {
    final static DefaultTable TA_TAG = createTable("ta_tag")

    final static Field<Integer> COL_ID = TA_TAG.createField("id", int.class)
    final static Field<String> COL_SKELETON_ID = TA_TAG.createField("skeleton_id", String.class)
    final static Field<String> COL_TAG = TA_TAG.createField("tag", String.class)

    final static Sequence<Integer> SE_TAG = DSL.sequence(DSL.name("se_tag"), int.class)

    static {
        TA_TAG.withIdentity(COL_ID)
    }

    @Override
    Table<Record> getTable() { TA_TAG }

    @Override
    void create() {
        dsl.createTable(TA_TAG)
                .column(COL_ID, COL_ID.getDataType().nullable(false))
                .column(COL_SKELETON_ID, COL_SKELETON_ID.getDataType().nullable(false))
                .column(COL_TAG, COL_TAG.getDataType().nullable(false))
                .execute()

        dsl.alterTable(TA_TAG).add(primaryKeyConstraint("pk_tag", TA_TAG, COL_SKELETON_ID, COL_TAG)).execute()

        dsl.alterTable(TA_TAG).add(
                DSL.constraint(DSL.name("fk_tag_skeleton_id"))
                        .foreignKey(COL_SKELETON_ID)
                        .references(SkeletonTable.TA_SKELETON, SkeletonTable.COL_SKELETON_ID)
                        .onDeleteCascade()
                        .onUpdateCascade()
        ).execute()

        dsl.dropSequenceIfExists(SE_TAG).execute()
        dsl.createSequence(SE_TAG).execute()
    }

    int add(String skeletonId, String tag) {
        def colId = -1
        dsl.select(COL_ID)
                .from(TA_TAG)
                .where(COL_SKELETON_ID.equal(skeletonId).and(COL_TAG.equal(tag)))
                .fetch()
                .map { r -> colId = r.getValue(COL_ID)}
        if(colId < 0) {
            dsl.insertInto(TA_TAG)
                    .columns(COL_ID, COL_SKELETON_ID, COL_TAG)
                    .values(dsl.nextval(SE_TAG), skeletonId, tag)
                    .execute()
        } else 0
    }

    int delete(String skeletonId, String tag) {
        dsl.delete(TA_TAG)
            .where(COL_SKELETON_ID.equal(skeletonId))
            .and(COL_TAG.equal(tag))
        .execute()
    }

    void update(String skeletonId, String[] currTags) {
        def oldTags = getTags(skeletonId)
        oldTags.each { oldTag ->
            if(!currTags.contains(oldTag)) {
                delete(skeletonId, oldTag)
            }
        }
        currTags.each { currTag ->
            if(!oldTags.contains(currTag)) {
                add(skeletonId, currTag)
            }
        }
    }

    List<String> getTags(String skeletonId) {
        dsl.select(COL_TAG)
            .from(TA_TAG)
            .where(COL_SKELETON_ID.equal(skeletonId))
        .fetch(COL_TAG)
    }
}
