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

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import groovy.util.logging.Slf4j
import org.boothub.Constants
import org.boothub.GroovyClassDefiner
import org.boothub.Util
import org.yaml.snakeyaml.Yaml

import java.nio.file.Path

@Slf4j
class FileContextReader<P extends ProjectContext> {
    final Path baseProjectTemplatePath
    final Path fileContextTemplatePath
    final GroovyClassDefiner definer

    FileContextReader(Path baseProjectTemplatePath) {
        this.baseProjectTemplatePath = baseProjectTemplatePath
        this.fileContextTemplatePath = baseProjectTemplatePath.resolve(Constants.FILES_CONTEXT_TEMPLATE)
        this.definer = GroovyClassDefiner.ofTemplateDirPath(baseProjectTemplatePath)
    }

    /**
     * @param context the project context
     * @return a map that associates artifacts with their corresponding maps of {@link SourceFileContext}s indexed by their filePath
     */
    Map<String, FileContext> getFileContexts(P context) {
        Map<String, FileContext> fileContexts = [:]
        if(fileContextTemplatePath.toFile().isFile()) {
            getFileContextList(context).each { sourceContext ->
                fileContexts[sourceContext.filePath] = sourceContext
            }
        }
        fileContexts
    }

    List<FileContext> getFileContextList(P context) {
        Handlebars handlebars = Util.createHandlebars(fileContextTemplatePath.parent)
        Template template = handlebars.compile(fileContextTemplatePath.toFile().name)

        def mergedFileYaml = template.apply(context)
        log.debug "merged:\n$mergedFileYaml"

        List<FileContext> fileContexts = null
        definer.withCustomClasses {
            fileContexts = new Yaml().loadAll(mergedFileYaml).asList()
        }
		log.debug "fileContexts: $fileContexts"
        fileContexts
    }
}
