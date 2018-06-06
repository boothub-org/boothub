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

@InheritConstructors
class OwnerTable extends RepoTable {
    final static DefaultTable TA_OWNER = createTable("ta_owner").withIdentity(COL_ID)

    final static Field<Integer> COL_ID = TA_OWNER.createField("id", int.class)
    final static Field<String> COL_SKELETON_ID = TA_OWNER.createField("skeleton_id", String.class)
    final static Field<String> COL_OWNER = TA_OWNER.createField("owner", String.class)

    final static Sequence<Integer> SE_OWNER = DSL.sequence(DSL.name("se_owner"), int.class)

    static {
        TA_OWNER.withIdentity(COL_ID)
    }

    @Override
    Table<Record> getTable() { TA_OWNER }

    @Override
    void create() {
        dsl.createTable(TA_OWNER)
                .column(COL_ID, COL_ID.getDataType().nullable(false))
                .column(COL_SKELETON_ID, COL_SKELETON_ID.getDataType().nullable(false))
                .column(COL_OWNER, COL_OWNER.getDataType().nullable(false))
                .execute()

        dsl.alterTable(TA_OWNER).add(primaryKeyConstraint("pk_owner", TA_OWNER, COL_SKELETON_ID, COL_OWNER)).execute()

        dsl.alterTable(TA_OWNER).add(
                DSL.constraint(DSL.name("fk_owner_skeleton_id"))
                        .foreignKey(COL_SKELETON_ID)
                        .references(SkeletonTable.TA_SKELETON, SkeletonTable.COL_SKELETON_ID)
                        .onDeleteCascade()
                        .onUpdateCascade()
        ).execute()

        dsl.dropSequenceIfExists(SE_OWNER).execute()
        dsl.createSequence(SE_OWNER).execute()
    }

    int add(String skeletonId, String ownerId) {
        def colId = -1
        dsl.select(COL_ID)
                .from(TA_OWNER)
                .where(COL_SKELETON_ID.equal(skeletonId).and(COL_OWNER.equal(ownerId)))
                .fetch()
                .map { r -> colId = r.getValue(COL_ID)}
        if(colId < 0) {
            dsl.insertInto(TA_OWNER)
                    .columns(COL_ID, COL_SKELETON_ID, COL_OWNER)
                    .values(dsl.nextval(SE_OWNER), skeletonId, ownerId)
                    .execute()
        } else 0
    }

    int delete(String skeletonId, String ownerId) {
        dsl.delete(TA_OWNER)
                .where(COL_SKELETON_ID.equal(skeletonId))
                .and(COL_OWNER.equal(ownerId))
                .execute()
    }

    List<String> getOwnerIds(String skeletonId) {
        dsl.select(COL_OWNER)
            .from(TA_OWNER)
            .where(COL_SKELETON_ID.equal(skeletonId))
        .fetch(COL_OWNER)
    }
}
