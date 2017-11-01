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
import org.yaml.snakeyaml.TypeDescription
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path

@Unroll
class FileContextReaderSpec extends Specification {
    static { String.metaClass.stripAll = { Util.stripAll(delegate) } }

    def projectContextYaml = '''
        --- !!org.boothub.context.StandardProjectContext$Generic
        ghProjectId: myProject
        appMainClass: MyApp
        modules:
          - !!org.boothub.context.StandardModuleContext &mod-strange-lib
            artifact: strange-lib
            basePackage: org.bizarre_soft.strange_lib
    '''.stripAll()

    def filesYaml = '''
          --- !!org.boothub.context.FileContext
          filePath: settings.gradle
          enabled: {{multiModule}}
          --- !!org.boothub.context.FileContext
          filePath: run.sh
          targetPath: dist/bin/run{{appMainClass}}.sh
          enabled: {{asBoolean appMainClass}}
          --- !!org.boothub.context.FileContext
          filePath: license.txt
          targetPath: license-{{ghProjectId}}.txt
          enabled: true
          --- !!org.boothub.context.FileContext
          filePath: README
          targetPath: doc/README
    '''.stripAll()

    def "should correctly create the fileContext of file #filePath"() {
        given:

        Constructor constructor = new Constructor(StandardProjectContext.Generic);
        TypeDescription prjDescription = new TypeDescription(StandardProjectContext);
        prjDescription.putMapPropertyType("appMainClass", String, Object);
        constructor.addTypeDescription(prjDescription);
        StandardProjectContext context = new Yaml(constructor).load(projectContextYaml)


//        ProjectContext context = new Yaml().load(projectContextYaml)

        Path templateDirPath = Util.createTempDirWithDeleteOnExit()
        def yamlFile = Files.createFile(templateDirPath.resolve(Constants.FILES_CONTEXT_TEMPLATE))
        yamlFile << filesYaml

        FileContextReader<ProjectContext> reader = new FileContextReader<>(templateDirPath)

        when:
        Map<String, FileContext> fileContexts = reader.getFileContexts(context)

        then:
        fileContexts.size() == 4

        fileContexts[filePath].filePath == filePath
        fileContexts[filePath].targetPath == targetPath
        fileContexts[filePath].enabled == enabled

        where:
        filePath          | targetPath              | enabled
        'settings.gradle' | null                    | false
        'run.sh'          | 'dist/bin/runMyApp.sh'  | true
        'license.txt'     | 'license-myProject.txt' | true
        'README'          | 'doc/README'            | true
    }
}
