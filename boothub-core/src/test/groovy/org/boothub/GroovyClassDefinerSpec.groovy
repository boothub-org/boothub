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

import java.nio.file.Path
import java.nio.file.Paths

import static spock.util.matcher.HamcrestMatchers.closeTo
import static spock.util.matcher.HamcrestSupport.expect

class GroovyClassDefinerSpec extends Specification {
    def "should define classes on runtime"() {
        given:
        def definerUrl = getClass().getResource("/definer")
        assert definerUrl
        Path basePath = Paths.get(definerUrl.toURI())
        def definer = new GroovyClassDefiner()
                .withSourceDirPath(basePath.resolve('script'))
                .withLibDirPath(basePath.resolve('lib'))

        when:
        definer.defineClasses()
        Class myTest = definer.forName('org.example.work.MyTest')
        def material = myTest.transformMaterial()

        then:
        material.name == 'mY BiG IrOn pLaTe'
        expect material.length, closeTo(10.5, 0.0001)
        expect material.width, closeTo(3.6, 0.0001)
        expect material.weight, closeTo(3.8, 0.0001)
    }
}
