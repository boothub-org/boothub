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

import org.codehaus.groovy.runtime.IOGroovyMethods

import java.sql.Connection

//import ratpack.exec.Blocking
//import ratpack.exec.Promise
import java.sql.ResultSet

trait DBApi extends StatementApi {
    abstract Connection createConnection()

//    public <V> Promise<V> dbExecuteBlocking(DBJob<V> dbJob) {
//        return Blocking.get{ -> dbExecute(dbJob) }
//    }

    public <V> V dbExecute(DBJob<V> dbJob) {
        IOGroovyMethods.withCloseable(createConnection() as Closeable) {
            connection -> return dbJob.execute(connection)
        }
    }

    public void dbExecuteAll(List<DBJob> dbJobs) {
        IOGroovyMethods.withCloseable(createConnection() as Closeable) { connection ->
            dbJobs.each {dbJob -> dbJob.execute(connection)}
        }
    }

    public int executeUpdate(String updateSql) {
        dbExecute getUpdateJob(updateSql)
    }

//    public Promise<Integer> executeUpdateBlocking(String updateSql) {
//        dbExecuteBlocking getUpdateJob(updateSql)
//    }

    public ResultSet executeQuery(String updateSql) {
        dbExecute getQueryJob(updateSql)
    }

//    public Promise<ResultSet> executeQueryBlocking(String updateSql) {
//        dbExecuteBlocking getQueryJob(updateSql)
//    }
}
