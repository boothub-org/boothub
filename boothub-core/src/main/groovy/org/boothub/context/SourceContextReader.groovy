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
class SourceContextReader<P extends ProjectContext> {
    final Path baseProjectTemplatePath
    final Path srcContextTemplatePath
    final GroovyClassDefiner definer

    SourceContextReader(Path baseProjectTemplatePath) {
        this.baseProjectTemplatePath = baseProjectTemplatePath
        this.srcContextTemplatePath = baseProjectTemplatePath.resolve(Constants.SRC_CONTEXT_TEMPLATE)
        this.definer = GroovyClassDefiner.ofTemplateDirPath(baseProjectTemplatePath)
    }

    /**
     * @param context the project context
     * @return a map that associates artifacts with their corresponding maps of {@link SourceFileContext}s indexed by their filePath
     */
    Map<String, Map<String, SourceFileContext>> getSourceFileContexts(P context) {
        Map<String, Map<String, SourceFileContext>> srcFileContexts = [:]
        if(srcContextTemplatePath.toFile().isFile()) {
            getSourceContexts(context).each { sourceContext ->
                Map<String, SourceFileContext> ctxs = [:]
                srcFileContexts[sourceContext.artifact] = ctxs
                sourceContext.fileContexts.each { fileCtx -> ctxs[fileCtx.fileName] = fileCtx }
            }
        }
        srcFileContexts
    }

    List<SourceContext> getSourceContexts(P context) {
        Handlebars handlebars = Util.createHandlebars(srcContextTemplatePath.parent)
        Template template = handlebars.compile(srcContextTemplatePath.toFile().name)

        def mergedSrcYaml = template.apply(context)
        log.debug "merged:\n$mergedSrcYaml"

        List<SourceContext> srcContexts = null
        definer.withCustomClasses {
            srcContexts = new Yaml().loadAll(mergedSrcYaml).asList()
        }
		log.debug "srcContexts: $srcContexts"
        srcContexts
    }
}
