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

import org.jooq.*
import org.jooq.impl.*

abstract class RepoTable {
    private static class DefaultTable extends TableImpl<Record> {
        UniqueKey<Record> primaryKey
        Identity<Record, Long> identity

        DefaultTable(Name name) {
            super(name)
        }

        DefaultTable withPrimaryKey(String keyName, TableField<Record,?>... fields) {
            this.primaryKey = Internal.createUniqueKey(this, keyName, fields)
            this
        }

        DefaultTable withIdentity(TableField<Record,?> field) {
            this.identity = Internal.createIdentity(this, field)
            this
        }

        Identity<Record, Long> getIdentity() {
            return identity
        }

        def <T> TableField<Record, T> createField(String name, Class<T> type) {
            createField(name, DSL.getDataType(type))
        }
    }

    final DSLContext dsl
    private UniqueKey<Record> primaryKey

    abstract void create()
    abstract Table<Record> getTable()

    RepoTable(DSLContext dsl) {
        this.dsl = dsl
    }

    boolean exists() {
        dsl.meta().getTables().any {it.name == table.name}
    }

    void createIfNotExists() {
        if(!exists()) create()
    }

    void createDropIfExists() {
        dsl.dropTableIfExists(table)
        create()
    }

    static final DefaultTable createTable(String tableName) {
        return new DefaultTable(DSL.name(tableName))
    }

    static <T>ConstraintFinalStep primaryKeyConstraint(String keyName, DefaultTable table, Field<T>... fields) {
        table.withPrimaryKey(keyName, (TableField[])fields)
        return DSL.constraint(DSL.name(keyName)).primaryKey(fields)
    }
}
