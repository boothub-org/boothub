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

import org.boothub.Version

trait SkeletonKey implements Comparable<SkeletonKey> {
    abstract String getId()
    abstract Version getVersion()

    @Override
    int compareTo(SkeletonKey sk) {
        int res = id <=> sk.id
        if(res) return res
        return version <=> sk.version
    }

    @Override
    boolean equals(Object obj) {
        def sk = obj as SkeletonKey
        int res = id <=> sk.id
        if(res) return res
        return version <=> sk.version
    }

    @Override
    int hashCode() {
        int res = 13
        res += 37 * (id ? id.hashCode() : 0)
        res += 37 * (version ? version.hashCode() : 0)
    }

    String toStringKey() {
        "$id-$version"
    }
}
