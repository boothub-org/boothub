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

class StandardMavenCompatible {
    @ConfiguredBy(value = StandardMavenCompatibleConfigurator.Generic, inheritConfigurators = false)
    static trait Generic implements MavenCompatible<StandardModuleContext>, StandardModularizable.Generic {}

    @ConfiguredBy(value = StandardMavenCompatibleConfigurator.Generic, inheritConfigurators = false)
    static trait Multi implements MavenCompatible<StandardModuleContext>, StandardModularizable.Multi {}

    @ConfiguredBy(value = StandardMavenCompatibleConfigurator.Generic, inheritConfigurators = false)
    static trait Single implements MavenCompatible<StandardModuleContext>, StandardModularizable.Single {}

    @ConfiguredBy(value = StandardMavenCompatibleConfigurator.App, inheritConfigurators = false)
    static trait App implements MavenCompatible<StandardModuleContext>, StandardModularizable.Generic {}

    @ConfiguredBy(value = StandardMavenCompatibleConfigurator.App, inheritConfigurators = false)
    static trait AppMulti implements MavenCompatible<StandardModuleContext>, StandardModularizable.Multi {}

    @ConfiguredBy(value = StandardMavenCompatibleConfigurator.App, inheritConfigurators = false)
    static trait AppSingle implements MavenCompatible<StandardModuleContext>, StandardModularizable.Single {}

    @ConfiguredBy(value = StandardMavenCompatibleConfigurator.Lib, inheritConfigurators = false)
    static trait Lib implements MavenCompatible<StandardModuleContext>, StandardModularizable.Generic {}

    @ConfiguredBy(value = StandardMavenCompatibleConfigurator.Lib, inheritConfigurators = false)
    static trait LibMulti implements MavenCompatible<StandardModuleContext>, StandardModularizable.Multi {}

    @ConfiguredBy(value = StandardMavenCompatibleConfigurator.Lib, inheritConfigurators = false)
    static trait LibSingle implements MavenCompatible<StandardModuleContext>, StandardModularizable.Single {}
}
