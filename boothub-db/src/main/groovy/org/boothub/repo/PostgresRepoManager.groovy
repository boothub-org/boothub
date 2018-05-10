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

import org.jooq.DSLContext
import org.jooq.impl.DSL

class PostgresRepoManager extends DBRepoManager {
    private PostgresRepoManager(DSLContext dsl, RepoCache repoCache) {
        super(dsl, repoCache)
    }

    static class Builder {
        String dbUrl = System.getenv('BOOTHUB_DB_URL')
        String username
        String password
        RepoCache repoCache

        Builder withDbUrl(String dbUrl) {
            this.dbUrl = dbUrl
            this
        }

        Builder withUsername(String username) {
            this.username = username
            this
        }

        Builder withPassword(String password) {
            this.password = password
            this
        }

        Builder withRepoCache(RepoCache repoCache) {
            this.repoCache = repoCache
            this
        }

        PostgresRepoManager create() {
            Class.forName('org.postgresql.Driver').getConstructor().newInstance()
            if(!dbUrl) throw new IllegalStateException("Missing database URL")
            def dsl = DSL.using(dbUrl, username, password)
            new PostgresRepoManager(dsl, repoCache ?: new DefaultRepoCache())
        }
    }
}
