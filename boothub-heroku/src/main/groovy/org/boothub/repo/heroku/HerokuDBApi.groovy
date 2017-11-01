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
package org.boothub.repo.heroku

import com.heroku.sdk.jdbc.DatabaseUrl
import groovy.transform.Memoized
import org.boothub.repo.DBApi

import java.sql.Connection

class HerokuDBApi implements DBApi {
    @Memoized
    boolean isLocal() {
        String herokuStack = System.getenv("STACK")
        (herokuStack == null) || herokuStack.isEmpty()
    }
    Connection createConnection() {
        return DatabaseUrl.extract(local).getConnection()
    }
}
