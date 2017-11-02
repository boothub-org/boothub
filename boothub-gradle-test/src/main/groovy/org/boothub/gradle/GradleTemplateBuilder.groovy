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

import org.boothub.Initializr
import org.boothub.context.ProjectContext
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

import java.nio.file.Path
import java.util.jar.JarFile

import static org.boothub.Constants.TEMPLATE_DIR
import static LogUtil.logPrint

class GradleTemplateBuilder {
    final String templateDir
    ProjectContext context
    List<String> gradleOptions = ['--info', '--stacktrace', '-Dorg.gradle.daemon=false ']
    boolean inProcess = false
    EnumSet<TaskOutcome> loggedOutcomes = EnumSet.of(TaskOutcome.FAILED, TaskOutcome.SKIPPED, TaskOutcome.UP_TO_DATE)
    String gradleVersion = null

    GradleTemplateBuilder(String templateDir = TEMPLATE_DIR) {
        this.templateDir = templateDir ?: TEMPLATE_DIR
    }

    GradleTemplateBuilder withContext(ProjectContext context) {
        this.context = context
        this
    }

    GradleTemplateBuilder withContextFile(String contextFile) {
        this.context = new Initializr(templateDir).createContext(contextFile)
        this
    }

    GradleTemplateBuilder withLoggedOutcomes(EnumSet<TaskOutcome> loggedOutcomes) {
        this.loggedOutcomes = loggedOutcomes
        this
    }

    GradleTemplateBuilder withLoggedOutcomes(TaskOutcome... outcomes) {
        this.loggedOutcomes = EnumSet.copyOf(outcomes.toList())
        this
    }

    GradleTemplateBuilder withGradleVersion(String gradleVersion) {
        this.gradleVersion = gradleVersion
        this
    }

    GradleTemplateBuilder withGradleOptions(String... options ) {
        this.gradleOptions = options as List
        this
    }
    GradleTemplateBuilder withGradleOptions(List<String> options ) {
        this.gradleOptions = options
        this
    }

    GradleTemplateBuilder withInProcessBuild(boolean inProcess) {
        this.inProcess = inProcess
        this
    }

    GradleResult runGradle(String... buildArguments) {
        def projectPath = new Initializr(templateDir).generateWithContext(context).toAbsolutePath()
        def args = buildArguments ? (buildArguments as List) : ['build']
        args = gradleOptions + args
        def gradleRunner = GradleRunner.create()
        if(gradleVersion) gradleRunner.withGradleVersion(gradleVersion)
        BuildResult buildResult = gradleRunner
                .withProjectDir(projectPath.toFile())
                .withArguments(args)
                .withDebug(inProcess)
                .build()
        new GradleResult(projectPath, buildResult)
    }

    GradleBuildResult runGradleBuild(String module = null) {
        def taskName = getQualifiedTaskName(module, "build")
        def gradleResult = runGradle(taskName)
        checkTask(taskName, gradleResult)
        def modulePath = getModulePath(module, gradleResult.projectPath)
        def libsPath = modulePath.resolve('build/libs').toFile().absolutePath
        def artifacts = new FileNameFinder().getFileNames(libsPath, "*.jar").collect{new JarFile(it)}
        logPrint("artifacts created by $taskName: $artifacts.name")
        new GradleBuildResult(
                projectPath: gradleResult.projectPath,
                buildResult: gradleResult.buildResult,
                modulePath: modulePath,
                artifacts: getArtifactsMap(artifacts))
    }

    File runGradleInstallDist(String module = null) {
        def taskName = getQualifiedTaskName(module, "installDist")
        def gradleResult = runGradle(taskName)
        checkTask(taskName, gradleResult)
        def modulePath = getModulePath(module, gradleResult.projectPath)
        def dirs = modulePath.resolve('build/install').toFile().listFiles({f -> f.isDirectory()} as FileFilter)
        assert dirs.length == 1
        dirs[0]
    }

    boolean checkTask(String taskName, GradleResult gradleResult) {
        def task = gradleResult.buildResult.task(taskName)
        if(!task || loggedOutcomes.contains(task.outcome)) {
            logPrint "Task $taskName ${task?.outcome}"
            logPrint("Gradle output:")
            logPrint(gradleResult.buildResult.output)
        }
        assert task : "Task '$taskName' not found"
        assert task.outcome != TaskOutcome.FAILED
        true
    }

    Process executeMainApplication(String module = null, String appName = null, String arguments = null) {
        def installDir = runGradleInstallDist(module)
        def scriptName = appName ?: installDir.name
        if (System.properties['os.name'].toLowerCase().contains('windows')) {
            scriptName += ".bat"
        }
        def scriptPath = "$installDir.absolutePath/bin/$scriptName"
        assert new File(scriptPath).isFile()
        def cmd = scriptPath
        if(arguments) {
            cmd += " $arguments"
        }
        logPrint "Executing: $cmd"
        cmd.execute()
    }

    static String getQualifiedTaskName(String module, String taskName) {
        String name = taskName
        if(!name.startsWith(':')) {
            name = ':' + name
        }
        if(module) {
            name = module + name
            if(!name.startsWith(':')) {
                name = ':' + name
            }
        }
        name
    }

    static Path getModulePath(String module, Path rootPath) {
        if(!module) return rootPath
        String relPath = module.replaceAll(':', '/')
        if(relPath.startsWith('/')) {
            relPath = relPath.substring(1)
        }
        rootPath.resolve(relPath)
    }

    static Map<String, List<JarFile>> getArtifactsMap(List<JarFile> artifacts) {
        def map = [jar: [], sources: [], javadoc: [], groovydoc: [], fat: []]
        artifacts.each { artifact ->
            def type = 'jar'
            if(artifact.name.endsWith('-sources.jar')) type = 'sources'
            else if(artifact.name.endsWith('-javadoc.jar')) type = 'javadoc'
            else if(artifact.name.endsWith('-groovydoc.jar')) type = 'groovydoc'
            else if(artifact.name.endsWith('-all.jar')) type = 'fat'
            map[type] << artifact
        }
        map
    }
}
