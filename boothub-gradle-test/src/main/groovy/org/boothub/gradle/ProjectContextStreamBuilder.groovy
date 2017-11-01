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

import org.beryx.streamplify.product.CartesianProduct
import org.boothub.context.ProjectContext

import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Stream

public class ProjectContextStreamBuilder<T extends ProjectContext> {
    final Supplier<T> baseContextSupplier
    final List<String> flagNames = []
    final Map<String, List> flagValues = [:]
    final List<Predicate<T>>  exclusions = []

    ProjectContextStreamBuilder(Supplier<T> baseContextSupplier) {
        this.baseContextSupplier = baseContextSupplier
    }

    ProjectContextStreamBuilder withFlag(String flagName, List values) {
        this.flagNames << flagName
        flagValues[flagName] = values
        this
    }

    ProjectContextStreamBuilder withFlagName(String flagName) {
        this.flagNames << flagName
        this
    }

    ProjectContextStreamBuilder withFlagNames(String... flagNames) {
        this.flagNames.addAll(flagNames)
        this
    }

    ProjectContextStreamBuilder withExclusion(Predicate<T> exclusion) {
        this.exclusions << exclusion
        this
    }

    List getValues(String flagName, T context) {
        List values = flagValues[flagName]
        if(!values) {
            Class type = context.hasProperty(flagName)?.type
            if(!type) throw new IllegalArgumentException("Flag with unknown type: $flagName")
            def valMethod = type.methods.findAll{m -> m.name == 'values' && (m.modifiers & java.lang.reflect.Modifier.STATIC) != 0}?.find()
            if(valMethod) {
                values = valMethod.invoke(null)
            } else if(type == Boolean || type == boolean) {
                values = [false, true]
            } else {
                throw new IllegalArgumentException("Flag with unknown possible values: $flagName")
                // values = [context."$flagName"]
            }
        }
        values
    }

    int[] getFlagDimensions(T context = null) {
        if(!context) context = baseContextSupplier.get()
        int[] dimensions = new int[flagNames.size()]
        flagNames.eachWithIndex { flagName, idx  -> dimensions[idx] = getValues(flagName, context).size() }
        dimensions
    }

    Stream<ProjectContext> stream() {
        new CartesianProduct(getFlagDimensions())
                .stream()
                .map {prod ->
                    def context = baseContextSupplier.get()
                    flagNames.eachWithIndex { flagName, idx  -> context."$flagName" = getValues(flagName, context)[prod[idx]] }
                    context.metaClass.dumpFlags = { toString(delegate) }
                    context
                }
                .filter {ctx -> !exclusions.any { it.test(ctx)} }
    }

    Stream<ProjectContext> parallelStream() {
        stream().parallel()
    }

    String toString(ProjectContext context) {
        flagNames.collect {flagName -> "$flagName: " + context."$flagName"}.join(", ")
    }
}
