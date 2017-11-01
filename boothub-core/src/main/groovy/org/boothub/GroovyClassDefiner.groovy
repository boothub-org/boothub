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

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.tools.GroovyClass

import java.nio.file.Path
import java.nio.file.Paths

import static Constants.TEMPLATE_DIR_SCRIPT

@Slf4j
public class GroovyClassDefiner {
    final ClassLoader originalClassLoader
    final GroovyClassLoader customClassLoader
    final List<Path> sourceDirPaths = []

    static GroovyClassDefiner ofTemplateDir(String projectTemplateDir) {
        ofTemplateDirPath(Paths.get(projectTemplateDir))
    }

    static GroovyClassDefiner ofTemplateDirPath(Path projectTemplateDirPath) {
        def scriptPath = projectTemplateDirPath.toRealPath().resolve(TEMPLATE_DIR_SCRIPT)
        new GroovyClassDefiner().withSourceDirPath(scriptPath)
    }

    GroovyClassDefiner(ClassLoader originalClassLoader = null) {
        this.originalClassLoader = originalClassLoader ?: Thread.currentThread().contextClassLoader
        this.customClassLoader = new GroovyClassLoader(this.originalClassLoader)
    }

    GroovyClassDefiner withSourceDirPath(Path sourceDirPath) {
        sourceDirPaths << sourceDirPath
        this
    }

    GroovyClassDefiner withLibPath(Path libPath) {
        customClassLoader.addClasspath(libPath.toRealPath().toString())
        this
    }

    GroovyClassDefiner withLibDirPath(Path libDirPath) {
        libDirPath.eachFileRecurse { Path libPath ->
            def file = libPath.toFile()
            if(file.isFile() && file.name.endsWith('.jar')) {
                customClassLoader.addClasspath(libPath.toRealPath().toString())
            }
        }
        this
    }

    List<GroovyClass> compileClasses() {
        final CompilerConfiguration conf = new CompilerConfiguration()
        Path targetDir = Util.createTempDirWithDeleteOnExit()
        conf.setTargetDirectory(targetDir.toFile())
        def compUnit = new CompilationUnit(conf)
        sourceDirPaths.each { Path srcPath ->
            if(srcPath.toFile().isDirectory()) {
                srcPath.eachFileRecurse { Path filePath ->
                    def file = filePath.toFile()
                    if(file.isFile() && file.name.endsWith('.groovy')) {
                        compUnit.addSource(file)
                    }
                }
            }
        }
        compUnit.compile()
        compUnit.getClasses()
    }

    List<GroovyClass> defineClasses() {
        withCustomClasses(null)
    }

    List<GroovyClass> withCustomClasses(@ClosureParams(value=SimpleType, options="List<Class>") Closure<Void> closure) {
        setCustomContextClassLoader()
        try {
            def classes = compileClasses()
            classes.each { cls ->
                log.debug "Defining groovy class ${cls.name}..."
                customClassLoader.defineClass(cls.name, cls.bytes, 0, cls.bytes.length)
            }
            if(closure) closure.call(classes)
            classes
        } finally {
            setOriginalContextClassLoader()
        }
    }

    void setOriginalContextClassLoader() {
        Thread.currentThread().contextClassLoader = originalClassLoader
    }
    void setCustomContextClassLoader() {
        Thread.currentThread().contextClassLoader = customClassLoader
    }

    Class forName(String className) {
        return Class.forName(className, true, customClassLoader)
    }
}
