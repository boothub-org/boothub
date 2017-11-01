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
package org.boothub

import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.beryx.textio.TextIO
import org.boothub.context.HierarchicalConfigurator
import org.boothub.context.ProjectContext
import org.boothub.repo.RepoCache
import org.boothub.repo.RepoKey
import org.boothub.repo.SkeletonRepo
import org.kohsuke.github.GitHub

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.boothub.context.ExtUtil.*

@Slf4j
abstract class BootHub implements HierarchicalConfigurator<ProjectContext> {
    static final String ZIP_FILE_PREFIX = "boothub-prj-"
    static final String ZIP_FILE_SUFFIX = ".zip"

    final TextIO textIO
    final SkeletonRepo skeletonRepo
    final RepoCache repoCache

    String outputDirBasePath
    String zipFilesBasePath
    boolean autoChooseOutputDir
    boolean zipOutput

    abstract RepoKey getRepoKey()
    abstract void configureGhApi(ProjectContext ctx, TextIO textIO)

    @Canonical
    private static class GenerationResult {
        GenerationResultData resultData
        boolean done
    }

    BootHub(TextIO textIO, SkeletonRepo skeletonRepo, RepoCache repoCache) {
        this.textIO = textIO
        this.skeletonRepo = skeletonRepo
        this.repoCache = repoCache
    }

    BootHub withOutputDirBasePath(String outputDirBasePath) {
        this.outputDirBasePath = outputDirBasePath
        this
    }

    BootHub withZipFilesBasePath(String zipFilesBasePath) {
        this.zipFilesBasePath = zipFilesBasePath
        this
    }

    BootHub withAutoChooseOutputDir(boolean autoChooseOutputDir) {
        this.autoChooseOutputDir = autoChooseOutputDir
        this
    }

    BootHub withZipOutput(Boolean zipOutput) {
        this.zipOutput = zipOutput
        this
    }

    GenerationResultData execute() {
        ProjectContext ctx = null
        try {
            Initializr initializr
            try {
                initializr = Initializr.ofRepoKey(repoKey, repoCache)
            } catch (Exception e) {
                throw new Exception("Cannot initialize skeleton from $repoKey.url: $e.message")
            }
            def config = initializr.createConfiguration()
            log.debug "contextClass: $config.contextClass"

            ctx = config.createProjectContext()
            setTextIO(ctx, textIO)
            setTemplateDir(ctx, initializr.projectTemplateDir)

            configureGhApi(ctx, textIO)

            configureInheritedTraitsOfConfigurableTrait(ctx)

            textIO.textTerminal.println "\nGenerating project. Please wait..."

            generateProject(ctx, initializr)
        } catch (Exception e) {
            log.error("Errors occurred.", e)
            new GenerationResultData(ghProjectId: ctx?.ghProjectId, errorMessage: "Errors occurred: $e.message")
        }
    }

    private GenerationResultData generateProject(ProjectContext ctx, Initializr initializr) {
        GitHub gitHub = getGitHubApi(ctx)
        while (true) {
            def onGitHub = ctx.ghApiUsed && gitHub && gitHub.isCredentialValid()
            GenerationResult result = onGitHub ? generateOnGitHub(initializr, ctx) : generateLocally(initializr, ctx)
            if(result.done) {
                return result.resultData
            }
        }
    }

    private GenerationResult generateOnGitHub(Initializr initializr, ProjectContext ctx) {
        try {
            String workingCopyPath = Util.createTempDirWithDeleteOnExit().toAbsolutePath().toString()
            initializr.withOutputDir(workingCopyPath).generateWithContext(ctx)
            def repo = GitHubUtil.createRepo(ctx)
            GitHubUtil.addContent(ctx, repo, workingCopyPath)
            new GenerationResult(done: true, resultData: new GenerationResultData(ghProjectId: ctx.ghProjectId, gitHubRepoLink: "https://github.com/$ctx.ghProjectOwner/$ctx.ghProjectId"))
        } catch (Exception e) {
            log.warn("Error generating GitHub project", e)
            textIO.textTerminal.println("An error occurred: $e" as String)
            def option = textIO.newStringInputReader()
                    .withPromptAdjustments(false)
                    .withInlinePossibleValues("t", "z", "a")
                    .withIgnoreCase()
                    .read("(T)ry again, generate (z)ip file or (a)bort? ")
            switch (option) {
                case "t": return new GenerationResult(done: false)
                case "z": ctx.ghApiUsed = false; return new GenerationResult(done: false)
                default: return new GenerationResult(done: true, resultData: new GenerationResultData(ghProjectId: ctx.ghProjectId, errorMessage: "Project generation aborted"))
            }
        }
    }

    private GenerationResult generateLocally(Initializr initializr, ProjectContext ctx) {
        try {
            Path basePath = outputDirBasePath ? Paths.get(outputDirBasePath) : null
            String outputDir
            if(autoChooseOutputDir) {
                def prefix = "boothub-out-"
                def outDirPath = basePath ? Files.createTempDirectory(basePath, prefix) : Files.createTempDirectory(prefix)
                outputDir = outDirPath.toAbsolutePath().toFile().absolutePath
                log.debug("outputPath: $outputDir")
            } else {
                while (true) {
                    outputDir = textIO.newStringInputReader().read("Output dir")
                    if (createDir(outputDir)) break
                }
            }
            initializr.withOutputDir(outputDir).generateWithContext(ctx)
//            textIO.textTerminal.println("Output written to $outputPath" as String)
            def outputPath = outputDir
            if(zipOutput) {
                def zipBasePath = basePath
                if(zipFilesBasePath) {
                    zipBasePath = Paths.get(zipFilesBasePath)
                }
                def zipPath = zipBasePath ? Files.createTempFile(zipBasePath, ZIP_FILE_PREFIX, ZIP_FILE_SUFFIX) : Files.createTempFile(ZIP_FILE_PREFIX, ZIP_FILE_SUFFIX)
                zipPath.toFile().delete()
                outputPath = zipPath.toAbsolutePath().toFile().absolutePath
                def ant = new AntBuilder()
                ant.zip(destfile: outputPath, basedir: outputDir)
            }
            new GenerationResult(done: true, resultData: new GenerationResultData(ghProjectId: ctx.ghProjectId, outputPath: outputPath))
        } catch(Exception e) {
            log.warn("Error generating project", e)
            textIO.textTerminal.println("An error occurred: $e" as String)
            new GenerationResult(done: !textIO.newBooleanInputReader().read("Retry?"))
        }
    }

    private boolean createDir(String path) {
        File dir = new File(path)
        if(dir.isFile()) {
            if(!textIO.newBooleanInputReader().read("File $path already exists. Delete?")) return false
            if(!dir.delete()) {
                textIO.textTerminal.println("Cannot delete file $path" as String)
                return false
            }
        }
        if(dir.list()) {
            if(!textIO.newBooleanInputReader().read("Directory $path already exists and is not empty. Delete?")) return false
            if(!dir.deleteDir()) {
                textIO.textTerminal.println("Cannot delete directory $path" as String)
                return false
            }
        }
        if(!dir.isDirectory()) dir.mkdirs()
        if(!dir.isDirectory()) {
            textIO.textTerminal.println("Cannot create the directory $path" as String)
            return false
        }
        return true
    }
}
