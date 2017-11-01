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

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class GitHubUtilSpec extends Specification {
    def "isMaybeBinary(#file) should be #binary"() {
        expect:
        GitHubUtil.isMaybeBinary {getClass().getResourceAsStream(file)} == binary

        where:
        file                                                 | binary
        '/zip/test.zip'                                      | true
        '/definer/lib/my-lib.jar'                            | true
        '/definer/script/org/example/work/Company.groovy'    | false
        '/definer/script/org/example/work/tools/Tool.groovy' | false
    }
}
