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
package org.boothub

import com.google.gson.*
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

import java.lang.reflect.Type

@TupleConstructor
@EqualsAndHashCode
class Version implements Comparable<Version>{
    int major
    int minor
    int patch
    String label

    static final Version BOOTHUB_CURRENT = new Version(
            major: VersionInfo.MAJOR,
            minor: VersionInfo.MINOR,
            patch: VersionInfo.PATCH,
            label: VersionInfo.LABEL
    )

    static class GsonDeserializer implements JsonDeserializer<Version> {
        Version deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                Version.fromString(json.getAsString());
            } catch (Exception e) {
                throw new JsonParseException(e)
            }
        }
    }

    static class GsonSerializer implements JsonSerializer<Version> {
        @Override
        JsonElement serialize(Version version, Type typeOfSrc, JsonSerializationContext context) {
            new JsonPrimitive(version.toString())
        }
    }

    static Version fromString(String s) {
        def version = new Version()
        def tokens = s.split('\\.', 3)
        if(tokens.length != 3) throw new IllegalArgumentException("Invalid version: $s")
        version.major = tokens[0] as int
        version.minor = tokens[1] as int
        tokens = tokens[2].split('-', 2)
        version.patch = tokens[0] as int
        if(tokens.length > 1) {
            version.label = tokens[1]
        }
        version
    }

    @Override
    String toString() {
        return "${major}.${minor}.${patch}" + (label ? "-$label" : "")
    }

    @Override
    int compareTo(Version v) {
        int res = major <=> v.major
        if(res) return res
        res = minor <=> v.minor
        if(res) return res
        res = patch <=> v.patch
        if(res) return res
        if(!label) return v.label ? 1 : 0
        if(!v.label) return -1
        return label <=> v.label
    }
}
