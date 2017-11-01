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

import org.boothub.context.SourceFileContext
import org.boothub.context.StandardProjectContext
import org.yaml.snakeyaml.Yaml
import spock.lang.Specification

import java.nio.file.Path

import static org.boothub.Constants.TEMPLATE_DIR_FILES_SRC

class SkeletonBuilderSpec extends Specification {
    static { String.metaClass.stripAll = { Util.stripAll(delegate) } }

    def contextYaml = '''
        --- !!org.boothub.context.StandardProjectContext$Generic
        modules:
          - !!org.boothub.context.StandardModuleContext &mod-strange-lib
            artifact: strange-lib
            basePackage: org.bizarre_soft.strange_lib
          - !!org.boothub.context.StandardModuleContext &mod-spooky-ui
            artifact: spooky-ui
            basePackage: org.bizarre_soft.spooky_ui
          - !!org.boothub.context.StandardModuleContext &mod-weird-app
            artifact: weird-app
            basePackage: org.bizarre_soft.weird_app

        appModule: *mod-weird-app
        appMainClass: WeirdApp
    '''.stripAll()

    StandardProjectContext context = (StandardProjectContext)new Yaml().load(contextYaml)

    static Path createSrcTemplates(List<String> srcFileNames) {
        Path baseProjectTemplatePath = Util.createTempDirWithDeleteOnExit()
        Path path = baseProjectTemplatePath.resolve(TEMPLATE_DIR_FILES_SRC)
        path.toFile().mkdirs()
        srcFileNames.each { fName ->
            path.resolve(fName).toFile() << "This is $fName"
        }
        baseProjectTemplatePath
    }
    def "should correctly copy artifact source templates"() {
        given:
        def baseProjectTemplatePath = createSrcTemplates(['A.java', 'ATest.java', 'B.groovy', 'BSpec.groovy', 'C.java', 'D.groovy'])
        def srcCtxList = [
                new SourceFileContext(fileName: 'A.java', targetBaseDir: 'src/main/java', targetPackage: 'org.boothub.mylib', targetFileClass: 'MainApp', enabled: true),
                new SourceFileContext(fileName: 'ATest.java', targetBaseDir: 'src/test/java', targetPackage: 'org.boothub.mylib', targetFileClass: 'MainAppTest', enabled: true),
                new SourceFileContext(fileName: 'B.groovy', targetBaseDir: 'src/main/groovy', targetPackage: 'org.boothub.mylib', targetFileClass: 'Lib', enabled: true),
                new SourceFileContext(fileName: 'BSpec.groovy', targetBaseDir: 'src/test/groovy', targetPackage: 'org.boothub.mylib', targetFileClass: 'LibSpec', enabled: true),
                new SourceFileContext(fileName: 'C.java', targetBaseDir: 'src/main/java', targetPackage: 'org.boothub.mylib', targetFileClass: 'Disabled', enabled: false),
                new SourceFileContext(fileName: 'E.java', targetBaseDir: 'src/main/java', targetPackage: 'org.boothub.mylib', targetFileClass: 'MissingContext', enabled: true),
        ]
        def ctxMap = [:]
        srcCtxList.each { ctx -> ctxMap[ctx.fileName] = ctx }
        def artifact = 'my-lib'
        def builder = new SkeletonBuilder(baseProjectTemplatePath)
        def workPath = builder.createWorkPath()
        def srcPath = baseProjectTemplatePath.resolve(TEMPLATE_DIR_FILES_SRC)
        def artifactPath = workPath.resolve(artifact)

        when:
        builder.copyArtifactSourceTemplates(artifact, ctxMap, context.isMultiModule())

        then:
        srcPath.resolve('A.java').text == artifactPath.resolve('src/main/java/org/boothub/mylib/MainApp.java').text
        srcPath.resolve('ATest.java').text == artifactPath.resolve('src/test/java/org/boothub/mylib/MainAppTest.java').text
        srcPath.resolve('B.groovy').text == artifactPath.resolve('src/main/groovy/org/boothub/mylib/Lib.groovy').text
        srcPath.resolve('BSpec.groovy').text == artifactPath.resolve('src/test/groovy/org/boothub/mylib/LibSpec.groovy').text
        !artifactPath.resolve('src/main/java/org/boothub/mylib/Disabled.java').toFile().exists()
        !artifactPath.resolve('src/main/java/org/boothub/mylib/MissingContext.java').toFile().exists()
    }
}
