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
import org.codehaus.groovy.runtime.IOGroovyMethods
import org.gradle.testkit.runner.TaskOutcome

import java.util.concurrent.TimeUnit

import static LogUtil.logPrint
import static org.boothub.Util.stripAll

class OutputChecker {
    final String templateDir
    final ProjectContext context
    final String moduleName

    String gradleVersion
    List<String> gradleOptions
    EnumSet<TaskOutcome> loggedOutcomes
    long timeoutSeconds = 10

    OutputChecker(String templateDir, ProjectContext context, String moduleName = null) {
        this.templateDir = templateDir
        this.context = context
        this.moduleName = moduleName
    }

    OutputChecker withLoggedOutcomes(TaskOutcome... outcomes) {
        this.loggedOutcomes = EnumSet.copyOf(outcomes.toList())
        this
    }

    OutputChecker withGradleVersion(String gradleVersion) {
        this.gradleVersion = gradleVersion
        this
    }

    OutputChecker withGradleOptions(String... options ) {
        this.gradleOptions = options as List
        this
    }

    OutputChecker withTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds
        this
    }

    boolean checkOutput(String expectedOutput) {
        if(context.respondsTo("dumpFlags")) {
            logPrint "Checking context: ${context.dumpFlags()}"
        }
        def builder = new GradleTemplateBuilder(templateDir)
        if(gradleVersion) builder.withGradleVersion(gradleVersion)
        if(gradleOptions) builder.withGradleOptions(gradleOptions)
        if(loggedOutcomes) builder.withLoggedOutcomes(loggedOutcomes)
        def process = builder
                .withContext(context)
                .executeMainApplication(moduleName)
        process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if(process.exitValue() != 0) {
            logPrint "\tProcess terminated with exit code ${process.exitValue()}"
            String errText = IOGroovyMethods.getText(new BufferedReader(new InputStreamReader(process.err)))
            logPrint "\tError message: $errText"
        }
        def output = process.text
        logPrint "\toutput: $output"
        assert stripAll(output) == expectedOutput
        true
    }

}
