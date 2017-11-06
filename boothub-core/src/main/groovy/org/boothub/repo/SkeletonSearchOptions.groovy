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

import groovy.transform.Canonical

@Canonical
class SkeletonSearchOptions {
    static final SkeletonSearchOptions ALL = new SkeletonSearchOptions()
    static final SkeletonSearchOptions ALL_COMPACT = new SkeletonSearchOptions(compact: true)

    String skeletonId
    String ownerId
    String version

    boolean compact
    boolean includeInvalidEntries
    boolean lastVersionOnly
    String searchPattern

    static SkeletonSearchOptions fromParameterMap(Map<String,String> params) {
        def searchOpts = new SkeletonSearchOptions()
        params.each {k,v ->
            if(searchOpts.hasProperty(k)) {
                def propType = searchOpts.metaClass.getMetaProperty(k).type
                if(boolean.equals(propType)) {
                    v = Boolean.valueOf(v)
                }
                searchOpts[k] = v
            }
        }
        searchOpts
    }
}
