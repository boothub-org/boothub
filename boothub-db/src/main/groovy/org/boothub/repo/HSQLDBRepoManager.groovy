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
package org.boothub.repo

import groovy.util.logging.Slf4j
import org.hsqldb.Server
import org.hsqldb.persist.HsqlProperties
import org.jooq.DSLContext
import org.jooq.impl.DSL

@Slf4j
class HSQLDBRepoManager extends DBRepoManager {
    final Server server

    private HSQLDBRepoManager(DSLContext dsl, RepoCache repoCache, Server server) {
        super(dsl, repoCache)
        this.server = server
    }

    class Builder {
        private String dbHost = 'localhost'
        private int dbPort = 9001
        private String dbName = 'mydb'
        private String dbDir = defaultDbDir
        private boolean serverStart = true
        private boolean serverShutdownOnExit = true
        private RepoCache repoCache

        Builder withDbHost(String dbHost) {
            this.dbHost = dbHost
            this
        }

        Builder withDbPort(int dbPort) {
            this.dbPort = dbPort
            this
        }

        Builder withDbName(String dbName) {
            this.dbName = dbName
            this
        }

        Builder withDbDir(String dbDir) {
            this.dbDir = dbDir
            this
        }

        Builder withServerStart(boolean serverStart) {
            this.serverStart = serverStart
            this
        }

        Builder withServerShutdownOnExit(boolean serverShutdownOnExit) {
            this.serverShutdownOnExit = serverShutdownOnExit
            this
        }

        Builder withRepoCache(String repoCache) {
            this.repoCache = repoCache
            this
        }

        HSQLDBRepoManager create() {
            Server server = null
            if(serverStart) {
                server = startServer()
                if(serverShutdownOnExit) {
                    addShutdownHook {
                        server.shutdown()
                    }
                }
            }
            new HSQLDBRepoManager(getDSLContext(), repoCache ?: new DefaultRepoCache(), server)
        }

        private Server startServer() {
            def props = new HsqlProperties()
            props.setProperty("server.database.0", "file:$dbDir/$dbName;")
            props.setProperty("server.dbname.0", dbName)
            props.setProperty("server.port", "$dbPort");
            def server = new Server()
            server.setProperties(props)
            server.start()
            server
        }

        private DSLContext getDSLContext() {
            DSL.using("jdbc:hsqldb:hsql://$dbHost:$dbPort/$dbName")
        }

        private static String getDefaultDbDir() {
            def baseDir = System.getProperty("user.home", ".")
            "$baseDir/boothub-db"
        }
    }

}
