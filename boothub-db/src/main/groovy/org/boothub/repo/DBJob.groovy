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

import groovy.transform.InheritConstructors
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

@FunctionalInterface
interface DBJob<V> {
    V execute(Connection connection) throws Exception


    @InheritConstructors
    static class PreparedUpdate extends Prepared<Integer> {
        @Override
        Integer executeStatement(PreparedStatement stmt) {
            return stmt.executeUpdate()
        }
    }

    @InheritConstructors
    static class PreparedQuery extends Prepared<ResultSet> {
        @Override
        ResultSet executeStatement(PreparedStatement stmt) {
            return stmt.executeQuery()
        }
    }

    static abstract class Prepared<V> implements DBJob<V> {
        final String sql
        final List<Closure<Void>> configurators = []

        abstract V executeStatement(PreparedStatement stmt)

        Prepared(String sql) {
            this.sql = sql
        }

        Prepared<V> configure(@ClosureParams(value=SimpleType, options="java.sql.PreparedStatement") Closure<Void> stmtConfigurator) {
            configurators << stmtConfigurator
            this
        }

        V execute(Connection connection) throws Exception {
            PreparedStatement stmt = connection.prepareStatement(sql)
            configurators.each {cfg -> cfg.call(stmt)}
            executeStatement(stmt)
        }
    }

}
