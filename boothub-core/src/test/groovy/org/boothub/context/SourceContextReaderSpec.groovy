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
package org.boothub.context

import org.boothub.Constants
import org.boothub.Util
import org.yaml.snakeyaml.Yaml
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path

@Unroll
class SourceContextReaderSpec extends Specification {
    static { String.metaClass.stripAll = { Util.stripAll(delegate) } }

    static class JVMProjectContext extends StandardProjectContext.Generic {
        boolean useJava
        boolean useGroovy
    }

    def projectContextYaml = '''
        --- !!org.boothub.context.SourceContextReaderSpec$JVMProjectContext
        modules:
          - !!org.boothub.context.StandardModuleContext &mod-strange-lib
            artifact: strange-lib
            basePackage: org.bizarre_soft.strange_lib
          - !!org.boothub.context.StandardModuleContext &mod-spooky-ui
            artifact: spooky-ui
            basePackage: org.bizarre_soft.spooky_ui
        useJava: true
    '''.stripAll()

    def sourcesYaml = '''
          {{#each modules}}
          --- !!org.boothub.context.SourceContext
          artifact: {{artifact}}
          fileContexts:
              - !!org.boothub.context.SourceFileContext
                fileName: JavaMain.java
                targetBaseDir: src/main/java
                targetPackage: {{basePackage}}
                targetFileClass: {{artifactAsClassName}}JavaMain
                enabled: {{useJava}}
              - !!org.boothub.context.SourceFileContext
                fileName: GroovyMain.groovy
                targetBaseDir: src/main/groovy
                targetPackage: {{basePackage}}
                targetFileClass: {{artifactAsClassName}}GroovyMain
                enabled: {{default useGroovy false}}
          {{/each}}
    '''.stripAll()

    def "should correctly create the srcContext of artifact #artifact and source file #fName"() {
        given:
        JVMProjectContext context = new Yaml().load(projectContextYaml)

        Path templateDirPath = Util.createTempDirWithDeleteOnExit()
        def srcYamlFile = Files.createFile(templateDirPath.resolve(Constants.SRC_CONTEXT_TEMPLATE))
        srcYamlFile << sourcesYaml

        SourceContextReader<JVMProjectContext> reader = new SourceContextReader<>(templateDirPath)

        when:
        Map<String, Map<SourceFileContext>> srcContexts = reader.getSourceFileContexts(context)

        then:
        srcContexts.size() == 2

        srcContexts[artifact][fName].targetPackage == targetPackage
        srcContexts[artifact][fName].targetFileClass == targetFileClass
        srcContexts[artifact][fName].enabled == enabled

        where:
        artifact      | fName               | targetPackage                  | targetFileClass        | enabled
        'strange-lib' | 'JavaMain.java'     | 'org.bizarre_soft.strange_lib' | 'StrangeLibJavaMain'   | true
        'strange-lib' | 'GroovyMain.groovy' | 'org.bizarre_soft.strange_lib' | 'StrangeLibGroovyMain' | false
        'spooky-ui'   | 'JavaMain.java'     | 'org.bizarre_soft.spooky_ui'   | 'SpookyUiJavaMain'     | true
        'spooky-ui'   | 'GroovyMain.groovy' | 'org.bizarre_soft.spooky_ui'   | 'SpookyUiGroovyMain'   | false
    }
}
