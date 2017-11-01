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

abstract class StandardModularizableConfigurator {
    static trait StandardModuleCreator implements ModularizableConfigurator.ModuleCreator<StandardModuleContext> {
        @Override
        StandardModuleContext createModuleContext() {
            return new StandardModuleContext()
        }
    }
    static class Generic extends ModularizableConfigurator.Generic<StandardModuleContext, Modularizable<StandardModuleContext>> implements StandardModuleCreator {}
    static class Single extends ModularizableConfigurator.Single<StandardModuleContext, Modularizable<StandardModuleContext>> implements StandardModuleCreator {}
    static class Multi extends ModularizableConfigurator.Multi <StandardModuleContext, Modularizable<StandardModuleContext>> implements StandardModuleCreator {}
}
