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
package org.boothub.gradle

import java.nio.file.Path
import java.util.jar.JarFile

class BuildChecker {
    final String module
    final String pathInModule
    final Path modulePath
    final Map<String, List<JarFile>> artifacts
    List<String> srcDirSets = ["java", "groovy", "scala", "kotlin"]

    BuildChecker(GradleTemplateBuilder builder, String module, String pathInModule) {
        this.module = module
        this.pathInModule = pathInModule
        def result = builder.runGradleBuild(module.replaceAll('_', '-'))
        this.modulePath = result.modulePath
        this.artifacts = result.artifacts
    }

    boolean checkClassesAndJars(String artifactType, List<String> extensions,
                                List<String> fileNames, List<String> forbiddenFileNames,
                                List<String> testFileNames, List<String> forbiddenTestFileNames) {
        checkClasses(fileNames, forbiddenFileNames, testFileNames, forbiddenTestFileNames)
        checkJars(artifactType, extensions, fileNames, forbiddenFileNames)
        true
    }

    boolean checkJars(String artifactType, List<String> extensions, List<String> fileNames, List<String> forbiddenFileNames) {
        List<JarFile> jars = artifacts[artifactType]
        assert jars.size() == 1
        def jar = jars[0]
        fileNames.each { fileName ->
            def entries = extensions.collect { ext ->
                String entryName = "$pathInModule/${fileName}.${ext}"
                jar.getEntry(entryName)}.findAll { it }
            assert entries.size() == 1 : "Not available in $jar.name: $pathInModule/${fileName}"
        }
        forbiddenFileNames.each { fileName ->
            def entries = extensions.collect { ext -> jar.getEntry("$pathInModule/${fileName}.${ext}")}.findAll { it }
            assert entries.size() == 0 : "Forbidden file available in $jar.name: $pathInModule/${fileName}"
        }
        true
    }

    boolean checkClasses(List<String> fileNames, List<String> forbiddenFileNames,
                         List<String> testFileNames, List<String> forbiddenTestFileNames) {
        Path classesPath = modulePath.resolve("build/classes")
        checkRequiredClassFiles(classesPath, "main", fileNames)
        checkForbiddenClassFiles(classesPath, "main", forbiddenFileNames)

        checkRequiredClassFiles(classesPath, "test", testFileNames)
        checkForbiddenClassFiles(classesPath, "test", forbiddenTestFileNames)

        true
    }

    boolean checkRequiredClassFiles(Path classesPath, String sourceSet, List<String> fileNames) {
        fileNames.each { fileName ->
            if(!classesPath.resolve("$sourceSet/$pathInModule/${fileName}.class").toFile().isFile()) {
                assert srcDirSets.any { srcDir ->
                    classesPath.resolve("$srcDir/$sourceSet/$pathInModule/${fileName}.class").toFile().isFile()
                } : "File not found: ${fileName}.class"
            }
        }
        true
    }

    boolean checkForbiddenClassFiles(Path classesPath, String sourceSet, List<String> fileNames) {
        fileNames.each { fileName ->
            assert srcDirSets.every { srcDir ->
                !classesPath.resolve("$srcDir/$sourceSet/$pathInModule/${fileName}.class").toFile().isFile()
            } : "Forbidden file exists: ${fileName}.class"
        }
        true
    }
}
