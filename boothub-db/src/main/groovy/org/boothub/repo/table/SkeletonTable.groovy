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

@InheritConstructors
class SkeletonTable extends RepoTable {
    final static DefaultTable TA_SKELETON = createTable("ta_skeleton")

    final static Field<String> COL_SKELETON_ID = TA_SKELETON.createField("skeleton_id", String.class)
    final static Field<String> COL_NAME = TA_SKELETON.createField("name", String.class)
    final static Field<String> COL_CAPTION = TA_SKELETON.createField("caption", String.class)
    final static Field<Integer> COL_USAGE_COUNT = TA_SKELETON.createField("usage_count", int.class)
    final static Field<Integer> COL_RATING_COUNT = TA_SKELETON.createField("rating_count", int.class)
    final static Field<Integer> COL_RATING_SUM = TA_SKELETON.createField("rating_sum", int.class)
    final static Field<Integer> COL_SORTING_WEIGHT = TA_SKELETON.createField("sorting_weight", int.class)

    @Override
    Table<Record> getTable() { TA_SKELETON }

    @Override
    void create() {
        dsl.createTable(TA_SKELETON)
                .column(COL_SKELETON_ID, COL_SKELETON_ID.getDataType().nullable(false))
                .column(COL_NAME, COL_NAME.getDataType().nullable(false))
                .column(COL_CAPTION)
                .column(COL_USAGE_COUNT, COL_USAGE_COUNT.getDataType().nullable(false).defaultValue(0))
                .column(COL_RATING_COUNT, COL_RATING_COUNT.getDataType().nullable(false).defaultValue(0))
                .column(COL_RATING_SUM, COL_RATING_SUM.getDataType().nullable(false).defaultValue(0))
                .column(COL_SORTING_WEIGHT, COL_SORTING_WEIGHT.getDataType().nullable(false).defaultValue(1))
                .constraints(
                    primaryKeyConstraint("pk_skeleton", TA_SKELETON, COL_SKELETON_ID)
                )
                .execute()
    }

    int addOrReplace(String skeletonId, String name, String caption) {
        String foundId = null
        dsl.select(COL_SKELETON_ID)
                .from(TA_SKELETON)
                .where(COL_SKELETON_ID.equal(skeletonId))
                .fetch().map{ r -> foundId = r.getValue(COL_SKELETON_ID)}
        if(foundId == null) {
            dsl.insertInto(TA_SKELETON)
                    .columns(COL_SKELETON_ID, COL_NAME, COL_CAPTION)
                    .values(skeletonId, name, caption)
                    .execute()
        } else {
            dsl.update(TA_SKELETON)
                    .set(COL_NAME, name as String)
                    .set(COL_CAPTION, caption as String)
                    .where(COL_SKELETON_ID.equal(skeletonId))
                    .execute()
        }
    }

    int delete(String skeletonId) {
        dsl.delete(TA_SKELETON)
                .where(COL_SKELETON_ID.equal(skeletonId))
        .execute()
    }

    int incrementUsageCounter(String skeletonId) {
        dsl.update(TA_SKELETON)
                .set(COL_USAGE_COUNT, COL_USAGE_COUNT.add(1))
                .where(COL_SKELETON_ID.equal(skeletonId))
                .execute()
    }

    int addRating(String skeletonId, long rating) {
        dsl.update(TA_SKELETON)
                .set(COL_RATING_COUNT, COL_RATING_COUNT.add(1))
                .set(COL_RATING_SUM, COL_RATING_SUM.add(rating))
                .where(COL_SKELETON_ID.equal(skeletonId))
                .execute()
    }
}
