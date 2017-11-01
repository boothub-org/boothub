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

import groovy.transform.ToString
import org.boothub.Util

import java.nio.file.Path

@ToString
class ModuleContext<M extends ModuleContext> {
    String artifact
    String basePackage
    List<M> dependsOn = []

    String getArtifactAsClassName() {
        Util.asJavaClassName(artifact)
    }

    String getArtifactAsJavaId() {
        Util.asJavaId(artifact)
    }

    Path getBasePackageAsPath() {
        Util.getPackageAsPath(basePackage)
    }

    @Override
    String toString() {
        artifact
    }
}
