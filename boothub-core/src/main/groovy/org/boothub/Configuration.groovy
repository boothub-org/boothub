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

import org.boothub.context.ProjectContext
import org.yaml.snakeyaml.Yaml

class Configuration {
    final Class<? extends ProjectContext> contextClass
    final noMergePatterns = []

    Configuration(Class<? extends ProjectContext> contextClass) {
        this.contextClass = contextClass
    }

    static Configuration fromYaml(String templateDir, String configFile = null) {
        if(configFile == null) {
            configFile = "$templateDir/$Constants.CONFIG_YAML_FILE"
        }
        def props = new Yaml().load(new FileInputStream(configFile))
        def definer = GroovyClassDefiner.ofTemplateDir(templateDir)
        definer.defineClasses()
        def contextClass = definer.forName(props.contextClass)
        def cfg = new Configuration(contextClass)
        if(props.noMergePatterns) {
            cfg.noMergePatterns.addAll(props.noMergePatterns)
        }
        cfg
    }

    def <T extends ProjectContext> T createProjectContext() {
        T ctx = contextClass.newInstance()
        ctx.noMergePatterns.addAll(noMergePatterns)
        ctx
    }
}
