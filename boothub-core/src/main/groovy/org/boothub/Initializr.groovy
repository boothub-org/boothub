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

import org.beryx.textio.TextIO
import org.beryx.textio.TextIoFactory
import org.boothub.context.ProjectContext
import org.boothub.repo.*
import org.yaml.snakeyaml.Yaml

import java.nio.file.Path
import java.nio.file.Paths

import static org.boothub.Constants.TEMPLATE_DIR

class Initializr {
    final String projectTemplateDir
    private String outputDir

    static Initializr ofUrl(String url) {
        ofRepoKey(new NullRepoKey(url), new NullRepoCache())
    }

    static Initializr ofRepoKey(RepoKey repoKey, RepoCache repoCache) {
        def destPath = Util.createTempDirWithDeleteOnExit()
        def urlSpec = repoKey.url
        def url = new URL(urlSpec)
        Path zipFilePath = null
        if(url.protocol == 'file') {
            def path = Paths.get(url.toURI())
            if(path.toFile().isFile()) {
                zipFilePath = path
            } else {
                def fileName = path.fileName.toString()
                if(fileName != TEMPLATE_DIR) {
                    path = path.resolve(TEMPLATE_DIR)
                }
                if(path.toFile().isDirectory()) {
                    new AntBuilder().copy(todir: destPath.toAbsolutePath().toString()) {
                        fileset(dir: path.toAbsolutePath().toString())
                    }
                } else {
                    throw new IOException("Directory $TEMPLATE_DIR not found in $urlSpec")
                }
            }
        } else {
            long expectedSize = -1
            String expectedSha = null
            if(repoKey instanceof RepoEntry) {
                def repoEntry = repoKey as RepoEntry
                expectedSize = repoEntry.size
                expectedSha = repoEntry.sha
            }
            zipFilePath = repoCache.get(repoKey, expectedSize, expectedSha)
        }
        if(zipFilePath) {
            Util.unzipStream(new FileInputStream(zipFilePath.toFile()), destPath, TEMPLATE_DIR)
        }
        new Initializr(destPath.toFile().absolutePath)
    }

    Initializr(String projectTemplateDir) {
        this.projectTemplateDir = projectTemplateDir
    }

    Initializr withOutputDir(String outputDir) {
        this.outputDir = outputDir
        this
    }

    Path generateWithContext(String contextFile) {
        ProjectContext ctx = createContext(contextFile)
        generateWithContext(ctx)
    }

    Path generateWithContext(ProjectContext ctx) {
        def outDir = outputDir ?: Util.createTempDirWithDeleteOnExit().toFile().absolutePath
        def projectTemplatePath = Paths.get(projectTemplateDir).toRealPath()
        SkeletonBuilder builder = new SkeletonBuilder(projectTemplatePath)
        Path outputPath = Paths.get(outDir)
        builder.generate(ctx, outputPath)
        outputPath
    }

    ProjectContext createContext(String contextFile) {
        ProjectContext ctx = null
        GroovyClassDefiner.ofTemplateDir(projectTemplateDir).withCustomClasses {
            ctx = new Yaml().load(new FileInputStream(contextFile))
        }
        ctx
    }

    Configuration createConfiguration(String configFile = null) {
        Configuration.fromYaml(projectTemplateDir, configFile)
    }

    static void main(String[] args) {
        if(args.length > 0 && args.length != 3) {
            println "Arguments: <skeleton-url> <context-file> <output-dir>"
            System.exit(-1)
        }
        String url, contextFile, outputDir
        if(args.length == 3) {
            url = args[0]
            contextFile = args[1]
            outputDir = args[2]
        } else {
            TextIO textIO = TextIoFactory.textIO
            try {
                url = textIO.newStringInputReader().read("Skeleton jar URL")
                contextFile = textIO.newStringInputReader().read("YAML context file")
                outputDir = textIO.newStringInputReader().read("Output dir")
            } catch (Exception e) {
                println "Program aborted"
                System.exit(-2)
            } finally {
                textIO.dispose()
            }
        }
        def initializr = ofUrl(url)
        initializr.withOutputDir(outputDir).generateWithContext(contextFile)
        println "\nProgram terminated. Output written to $outputDir"
    }
}
