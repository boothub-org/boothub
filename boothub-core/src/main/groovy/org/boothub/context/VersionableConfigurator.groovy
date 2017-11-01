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

class VersionableConfigurator extends TextIOConfigurator {
    @Override
    void configureWithTextIO(ProjectContext context, TextIO textIO) {
        def ctx = context as Versionable
        ctx.versionMajor = textIO.newIntInputReader()
                .withDefaultValue(1)
                .withMinVal(0)
                .read("Major version number")

        ctx.versionMinor = textIO.newIntInputReader()
                .withDefaultValue(0)
                .withMinVal(0)
                .read("Minor version number")

        ctx.versionPatch = textIO.newIntInputReader()
                .withDefaultValue(0)
                .withMinVal(0)
                .read("Patch version number")
    }
}
