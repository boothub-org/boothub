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

import org.beryx.textio.InputReader
import org.beryx.textio.TextIO
import org.boothub.Util

abstract class ModularizableConfigurator<MC extends ModuleContext<MC>, M extends Modularizable<MC>> extends TextIOConfigurator implements ModuleCreator<MC> {
    static interface ModuleCreator<MC extends ModuleContext<MC>> {
        MC createModuleContext()
    }
    abstract static class Generic<MC extends ModuleContext<MC>, M extends Modularizable<MC>> extends ModularizableConfigurator<MC, M> {
        @Override
        void configureWithTextIO(ProjectContext context, TextIO textIO) {
            def ctx = context as M
            configureModuleNames(ctx, textIO)
            configureModuleDependencies(ctx, textIO)
        }
    }
    abstract static class Single<MC extends ModuleContext<MC>, M extends Modularizable<MC>> extends ModularizableConfigurator<MC, M> {
        @Override
        void configureWithTextIO(ProjectContext context, TextIO textIO) {
            def ctx = context as M
            configureSingleModuleName(ctx, textIO)
        }
    }
    abstract static class Multi<MC extends ModuleContext<MC>, M extends Modularizable<MC>> extends ModularizableConfigurator<MC, M> {
        @Override
        void configureWithTextIO(ProjectContext context, TextIO textIO) {
            def ctx = context as M
            configureMultiModuleNames(ctx, textIO)
            configureModuleDependencies(ctx, textIO)
        }
    }

    static void configureModuleDependencies(ProjectContext context, TextIO textIO) {
        def ctx = context as M
        if(ctx.modules.size() > 1) {
            textIO.textTerminal.println("Module interdependencies")
            ctx.modules.each { MC module ->
                def possibleDeps = getPossibleDependenciesOf(module, ctx.modules)
                if(possibleDeps) {
                    module.dependsOn = textIO.newGenericInputReader(null)
                            .withNumberedPossibleValues(possibleDeps)
                            .readList("Modules on which $module.artifact depends")
                }
            }
        }

    }

    void configureModuleNames(ProjectContext ctx, TextIO textIO) {
        boolean multiModule = textIO.newBooleanInputReader()
                .read("Is this a multi-module project?")

        if(multiModule) {
            configureMultiModuleNames(ctx, textIO)
        } else {
            configureSingleModuleName(ctx, textIO)
        }
    }

    private static String getDefaultBasePackage(ProjectContext context) {
        def ctx = context as MavenCompatible
        ctx.group ? ctx.group.split("\\.").collect { Util.asJavaId(it) }.join('.') : null
    }

    private static String getDefaultBasePackage(ProjectContext ctx, String artifact, boolean compact) {
        String pkg = getDefaultBasePackage(ctx)
        String artf = Util.asJavaId(artifact)
        String prjId = ctx.ghProjectId.toLowerCase()
        if(artf.toLowerCase().startsWith(prjId)) {
            int pos = prjId.length()
            while(pos < artf.length()) {
                if(Character.isJavaIdentifierStart(artf.charAt(pos))) break
                pos++
            }
            if(pos < artf.length()) {
                artf = artf[pos].toLowerCase() + artf.substring(pos+1)
            }
        }
        if(compact) {
            String lastPkgPart = pkg ? (pkg.substring(pkg.lastIndexOf('.') + 1)) : ''
            if(lastPkgPart && (lastPkgPart.toLowerCase() == artf.toLowerCase())) {
                return pkg
            }
        }
        return pkg ? (pkg + '.' + artf) : artf
    }

    void configureSingleModuleName(ProjectContext context, TextIO textIO) {
        def ctx = context as M
        def mod = createModuleContext()
        mod.artifact = textIO.newStringInputReader()
                .withDefaultValue(context.ghProjectId)
                .withValueChecker(Util.mavenIdChecker)
                .read("Artifact ID")

        mod.basePackage = textIO.newStringInputReader()
                .withDefaultValue(getDefaultBasePackage(ctx, mod.artifact, true))
                .withValueChecker(Util.packageNameChecker)
                .read("Base package")

        ctx.modules << mod
    }

    static void configureMultiModuleNames(ProjectContext context, TextIO textIO) {
        def ctx = context as M
        boolean firstModule = true
        while (true) {
            if (!firstModule) {
                if (!textIO.newBooleanInputReader()
                        .read("More modules?")) {
                    break
                }
            }
            InputReader.ValueChecker<String> duplicateModuleChecker  = { id, prop ->
                ctx.modules*.artifact.contains(id) ? ["Duplicate module: $id" as String] : null}

            firstModule = false
            def mod = new StandardModuleContext()
            mod.artifact = textIO.newStringInputReader()
                    .withValueChecker(duplicateModuleChecker)
                    .withValueChecker(Util.mavenIdChecker)
                    .read("Module name / Artifact ID")

            mod.basePackage = textIO.newStringInputReader()
                    .withDefaultValue(getDefaultBasePackage(ctx, mod.artifact, false))
                    .withValueChecker(Util.packageNameChecker)
                    .read("Base package")

            ctx.modules << mod
        }
    }


    static List<ModuleContext> getPossibleDependenciesOf(ModuleContext module, List<? extends ModuleContext> allModules) {
        def deps = new ArrayList(allModules)
        deps.remove(module)
        removeDependingOn(module, new ArrayList(deps), deps)
        deps
    }

    private static boolean removeDependingOn(ModuleContext module, List<ModuleContext> modulesToCheck, List<ModuleContext> result) {
        boolean depFound = false
        for(ModuleContext m : modulesToCheck) {
            boolean found = removeDependingOn(module, m.dependsOn, result)
            if(found) {
                result.remove(m)
            }
            if(found || m == module) {
                depFound = true
            }
            if(!result) break
        }
        return depFound
    }
}
