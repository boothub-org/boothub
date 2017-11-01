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

abstract class MavenCompatibleConfigurator<MV extends MavenCompatible> extends TextIOConfigurator {
    abstract void configureAppModule(MV ctx, TextIO textIO)

    abstract static class Generic<MV extends MavenCompatible> extends MavenCompatibleConfigurator<MV> {
        @Override
        void configureAppModule(MV ctx, TextIO textIO) {
            doConfigureAppModule(ctx, textIO, false)
        }
    }
    abstract static class App<MV extends MavenCompatible> extends MavenCompatibleConfigurator<MV> {
        @Override
        void configureAppModule(MV ctx, TextIO textIO) {
            doConfigureAppModule(ctx, textIO, true)
        }
    }
    abstract static class Lib<MV extends MavenCompatible> extends MavenCompatibleConfigurator<MV> {
        @Override
        void configureAppModule(MV ctx, TextIO textIO) {
            // nothing to configure
        }
    }

    @Override
    void configureWithTextIO(ProjectContext context, TextIO textIO) {
        def ctx = context as MV
        ctx.group = textIO.newStringInputReader()
                .withPattern('[a-zA-Z_][a-zA-Z0-9_.]{1,99}')
                .read("Maven groupId")
        configureInheritedTraitsOfConfigurableTrait(context, Modularizable, [MavenCompatible])
        configureAppModule(ctx, textIO)
        configureTrait(context, PomContext)
    }

    void doConfigureAppModule(MV ctx, TextIO textIO, boolean mandatory) {
        if(!ctx.modules) return
        ctx.appModule = ctx.modules[0]
        if(mandatory || textIO.newBooleanInputReader()
                .read("Do you want to designate a main application class for your project?")) {
            if(ctx.modules.size() > 1) {
                ctx.appModule = textIO.newGenericInputReader{artifact ->
                    ModuleContext mod = ctx.modules.find {ModuleContext m -> m.artifact == artifact}
                    return mod ? new InputReader.ParseResult(mod) : new InputReader.ParseResult(mod, "Unknown module: " + artifact)
                }
                .withNumberedPossibleValues(ctx.modules)
                        .read("Module containing the main class")
            }

            String defaultAppMainClass = Util.asJavaClassName(ctx.ghProjectId) + "Main"
            ctx.appMainClass = textIO.newStringInputReader()
                    .withDefaultValue(defaultAppMainClass)
                    .withValueChecker(Util.classNameChecker)
                    .read("Application main class name")
        }
    }
}
