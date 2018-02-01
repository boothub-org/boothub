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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.boothub.Result
import org.boothub.Version
import org.codehaus.groovy.runtime.IOGroovyMethods

import java.lang.reflect.Type

class JsonSkeletonRepo implements SkeletonRepo {
    private static final String DEFAULT_REPO_JSON_URL = "https://raw.githubusercontent.com/boothub-org/boothub-repo/master/repo.json"

    final String repoJsonUrl
    private TreeMap<String, SkeletonGroup> allSkeletons

    JsonSkeletonRepo(String repoJsonUrl = DEFAULT_REPO_JSON_URL) {
        this.repoJsonUrl = repoJsonUrl
        reloadSkeletons()
    }

    private final JsonSkeletonRepo reloadSkeletons() {
        TreeMap<String, SkeletonGroup> newAllSkeletons
        Type type = new TypeToken<TreeMap<String, SkeletonGroup>>(){}.getType();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Version, new Version.GsonDeserializer())
                .create()
        IOGroovyMethods.withCloseable(new InputStreamReader(new URL(repoJsonUrl).openStream())) { reader ->
            newAllSkeletons = (TreeMap<String, TreeMap<Version,RepoEntry>>)gson.fromJson(reader, type);
        }
        this.allSkeletons = newAllSkeletons

        this
    }

    @Override
    Result<Map<String, SkeletonGroup>> getSkeletons(SkeletonSearchOptions options) {
        return new Result(type: Result.Type.SUCCESS, value: allSkeletons)
    }

    public static void main(String[] args) {
        println new JsonSkeletonRepo().allSkeletons
    }
}
