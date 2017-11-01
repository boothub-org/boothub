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

import org.beryx.textio.TextIO
import org.boothub.Util

import java.nio.file.Paths

import static ExtUtil.getTemplateDir

class LicensableConfigurator extends TextIOConfigurator {
    @Override
    void configureWithTextIO(ProjectContext context, TextIO textIO) {
        def ctx = context as Licensable
        def templateDir = getTemplateDir(context)
        if(!templateDir) throw new IllegalArgumentException("templateDir not set")
        def licenses = Util.getLicenses(Paths.get(templateDir))
        ctx.license = textIO.newStringInputReader()
                .withNumberedPossibleValues(licenses.keySet() as List)
                .withValueFormatter{id -> licenses[id]}
                .read("License")
    }
}
