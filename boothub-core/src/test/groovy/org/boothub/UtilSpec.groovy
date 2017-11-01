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

import java.nio.file.Path
import java.nio.file.Paths

@Unroll
class UtilSpec extends Specification {
    def "should convert package '#pkg' to path #parts"() {
        expect:
        Util.getPackageAsPath(pkg) == Paths.get(*parts)

        where:
        pkg                | parts
        'org.boothub' | ['org', 'boothub']
        'example'          | ['example']
        ''                 | ['']
    }

    def "should retrieve extension '#ext' from file name '#fName'"() {
        expect:
        Util.getFileExtension(fName) == ext

        where:
        fName         | ext
        ''            | ''
        'a'           | ''
        'a.txt'       | '.txt'
        'a.'          | '.'
        '.gitignore'  | ''
        'aa.bb.cc.dd' | '.dd'
    }

    Path createTempDir(List<String> dirPaths, List<String> filePaths) {
        Path path = Util.createTempDirWithDeleteOnExit()
        dirPaths.each { dir -> path.resolve(dir).toFile().mkdirs() }
        filePaths.each { fName -> path.resolve(fName).toFile() << "some text here" }
        path
    }

    def "should correctly handle the creation of '#newDirName'"() {
        given:
        Path basePath = createTempDir(dirs, files)

        when:
        Path newPath = basePath.resolve(newDirName)
        File newDir = newPath.toFile()
        Class excType = null
        try {
            Util.createDirForPath(newPath, delIfExists)
        } catch (Exception e) {
            excType = e.getClass()
        }

        then:
        excType == exception
        excType || (newDir.isDirectory() && !newDir.list())

        where:
        newDirName  | delIfExists | dirs          | files               | exception
        'new-dir-1' | false       | []            | []                  | null
        'new-dir-2' | true        | []            | []                  | null
        'new-dir-3' | false       | ['new-dir-3'] | []                  | null
        'new-dir-4' | true        | ['new-dir-4'] | []                  | null
        'new-dir-5' | false       | ['new-dir-5'] | ['new-dir-5/a.txt'] | IOException
        'new-dir-6' | true        | ['new-dir-6'] | ['new-dir-6/a.txt'] | null
        'new-dir-7' | false       | []            | ['new-dir-7']       | IOException
        'new-dir-8' | true        | []            | ['new-dir-8']       | null
    }

    def "should convert '#text' to javaId '#id'"() {
        expect:
        Util.asJavaId(text) == id
        Util.asJavaId(text, true, false) == id
        Util.asJavaId(text, true, true) == idTT
        Util.asJavaId(text, false, true) == idFT
        Util.asJavaId(text, false, false) == idFF

        where:
        text                                              | id                | idTT                | idFT                | idFF
        ""                                                | ''                | ''                  | ''                  | ''
        "Acme Corporation"                                | 'acmeCorporation' | 'acme_Corporation'  | 'acme_corporation'  | 'acmecorporation'
        "Hello, world!"                                   | 'helloWorld'      | 'hello_World'       | 'hello_world'       | 'helloworld'
        "3.14'"                                           | '_314'            | '_3_14'             | '_3_14'             | '_314'
        "   Should be trimmed   "                         | 'shouldBeTrimmed' | 'should_Be_Trimmed' | 'should_be_trimmed' | 'shouldbetrimmed'
        "###...-- *** This is a warning !!! *** --...###" | 'thisIsAWarning'  | 'this_Is_A_Warning' | 'this_is_a_warning' | 'thisisawarning'
        "!!! 6 x 9 = 42"                                  | '_6X942'          | '_6_X_9_42'         | '_6_x_9_42'         | '_6x942'
    }

    def "should convert '#text' to className '#cls'"() {
        expect:
        Util.asJavaClassName(text) == cls
        Util.asJavaClassName(text, true, false) == cls
        Util.asJavaClassName(text, true, true) == clsTT
        Util.asJavaClassName(text, false, true) == clsFT
        Util.asJavaClassName(text, false, false) == clsFF

        where:
        text                                              | cls               | clsTT               | clsFT               | clsFF
        ""                                                | ''                | ''                  | ''                  | ''
        "_"                                               | '_'               | '_'                 | '_'                 | '_'
        "Acme Corporation"                                | 'AcmeCorporation' | 'Acme_Corporation'  | 'Acme_corporation'  | 'Acmecorporation'
        "Hello, world!"                                   | 'HelloWorld'      | 'Hello_World'       | 'Hello_world'       | 'Helloworld'
        "3.14'"                                           | '_314'            | '_3_14'             | '_3_14'             | '_314'
        "   Should be trimmed   "                         | 'ShouldBeTrimmed' | 'Should_Be_Trimmed' | 'Should_be_trimmed' | 'Shouldbetrimmed'
        "###...-- *** This is a warning !!! *** --...###" | 'ThisIsAWarning'  | 'This_Is_A_Warning' | 'This_is_a_warning' | 'Thisisawarning'
        "!!! 6 x 9 = 42"                                  | '_6X942'          | '_6_X_9_42'         | '_6_x_9_42'         | '_6x942'
    }

    def "should correctly unzip a resource without subDir"() {
        given:
        Path destPath = Util.createTempDirWithDeleteOnExit()
        def zstream = this.getClass().getResourceAsStream("/zip/test.zip")

        when:
        Util.unzipStream(zstream, destPath)

        then:
        Util.stripAll(destPath.resolve('a.txt').text) == 'aaa'
        Util.stripAll(destPath.resolve('d1/b1.txt').text) == 'b1b1b1'
        Util.stripAll(destPath.resolve('d2/b2.txt').text) == 'b2b2b2'
    }

    def "should correctly unzip a resource with subDir"() {
        given:
        Path destPath = Util.createTempDirWithDeleteOnExit()
        def zstream = this.getClass().getResourceAsStream("/zip/test.zip")

        when:
        Util.unzipStream(zstream, destPath, 'd2')

        then:
        !destPath.resolve('a.txt').toFile().exists()
        !destPath.resolve('d1/b1.txt').toFile().exists()
        !destPath.resolve('b1.txt').toFile().exists()
        Util.stripAll(destPath.resolve('b2.txt').text) == 'b2b2b2'
    }

    def "should correctly create a map of licenses"() {
        given:
        Path destPath = Util.createTempDirWithDeleteOnExit()
        def zstream = this.getClass().getResourceAsStream("/zip/test.zip")
        Util.unzipStream(zstream, destPath)

        when:
        def licenses = Util.getLicenses(destPath)

        then:
        licenses['Apache-2.0'] == 'The Apache Software License, Version 2.0'
        licenses['MIT'] == 'The Overloaded MIT License'
        licenses['MyTestLicense'] == 'My Test License Version 9.99'
    }
}
