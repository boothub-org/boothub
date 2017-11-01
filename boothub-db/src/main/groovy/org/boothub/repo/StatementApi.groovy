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

import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

trait StatementApi {
    public DBJob<Integer> getUpdateJob(String updateSql) {
        { Connection connection ->
            Statement stmt = connection.createStatement()
            stmt.executeUpdate(updateSql)
        }
    }

    public DBJob<ResultSet> getQueryJob(String querySql) {
        { Connection connection ->
            Statement stmt = connection.createStatement()
            stmt.executeQuery(querySql)
        }
    }

}
