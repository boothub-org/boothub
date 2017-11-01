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

import org.boothub.context.ProjectContext

import java.util.concurrent.TimeUnit

import static org.boothub.Util.stripAll

class ProjectExecutor {
    final ProjectContext context
    String templateDir = null
    String moduleName = null
    long timeoutSeconds = 10
    String appName = null
    String arguments = null

    ProjectExecutor(ProjectContext context) {
        this.context = context
    }

    ProjectExecutor withTemplateDir(String templateDir) {
        this.templateDir = templateDir
        this
    }

    ProjectExecutor withModuleName(String moduleName) {
        this.moduleName = moduleName
        this
    }

    ProjectExecutor withTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds
        this
    }

    ProjectExecutor withAppName(String appName) {
        this.appName = appName
        this
    }

    ProjectExecutor withArguments(String arguments) {
        this.arguments = arguments
        this
    }

    String execute() {
        def builder = new GradleTemplateBuilder(templateDir).withContext(context)
        String modName = (context.isMultiModule() && moduleName == null) ? context.appModule.artifact : moduleName
        execute(builder, modName, appName, arguments, timeoutSeconds)
    }


    static String execute(GradleTemplateBuilder builder, String moduleName = null, String appName = null, String arguments = null, long timeoutSeconds = 10) {
        def process = builder.executeMainApplication(moduleName, appName, arguments)
        process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        stripAll(process.text)
    }

}
