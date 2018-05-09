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

    @Override
    Table<Record> getTable() { TA_SKELETON }

    @Override
    void create() {
        dsl.createTable(TA_SKELETON)
                .column(COL_SKELETON_ID, COL_SKELETON_ID.getDataType().nullable(false))
                .column(COL_NAME, COL_NAME.getDataType().nullable(false))
                .column(COL_CAPTION)
                .constraints(
                    primaryKeyConstraint("pk_skeleton", TA_SKELETON, COL_SKELETON_ID)
                )
                .execute()
    }

    int addOrReplace(String skeletonId, String name, String caption) {
        dsl.insertInto(TA_SKELETON)
            .columns(COL_SKELETON_ID, COL_NAME, COL_CAPTION)
            .values(skeletonId, name, caption)
            .onDuplicateKeyUpdate()
                .set(COL_NAME, name as String)
                .set(COL_CAPTION, caption as String)
        .execute()
    }

    int delete(String skeletonId) {
        dsl.delete(TA_SKELETON)
                .where(COL_SKELETON_ID.equal(skeletonId))
        .execute()
    }
}
