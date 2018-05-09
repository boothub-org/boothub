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

class HerokuPostgresDBRepoManager extends DBRepoManager {
    final boolean local

    private HerokuPostgresDBRepoManager(DSLContext dsl, RepoCache repoCache, boolean local) {
        super(dsl, repoCache)
        this.local = local
    }

    static class Builder {
        String herokuDbUrl = System.getenv('DATABASE_URL')
        boolean local = System.getenv("STACK") ? true : false
        RepoCache repoCache

        Builder withHerokuDbUrl(String herokuDbUrl) {
            this.herokuDbUrl = herokuDbUrl
            this
        }

        Builder withLocal(boolean local) {
            this.local = local
            this
        }

        Builder withRepoCache(RepoCache repoCache) {
            this.repoCache = repoCache
            this
        }

        HerokuPostgresDBRepoManager create() {
            Class.forName('org.postgresql.Driver').getConstructor().newInstance()
            if(!herokuDbUrl) throw new IllegalStateException("Missing environment variable 'DATABASE_URL'")
            def dbUri = new URI(herokuDbUrl)
            def userTokens = dbUri.getUserInfo().split(':')
            String username = userTokens[0]
            String password = userTokens[1]
            String dbUrl = "jdbc:postgresql://$dbUri.host:$dbUri.port$dbUri.path";
            if(!local) {
                dbUrl += '?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory'
            }
            def dsl = DSL.using(dbUrl, username, password)
            new HerokuPostgresDBRepoManager(dsl, repoCache ?: new DefaultRepoCache(), local)
        }
    }
}
