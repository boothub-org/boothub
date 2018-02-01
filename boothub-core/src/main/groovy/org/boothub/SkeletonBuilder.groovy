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

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import groovy.io.FileType
import groovy.util.logging.Slf4j
import org.boothub.context.*

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

import static org.boothub.Constants.*

@Slf4j
class SkeletonBuilder {
    final Path baseProjectTemplatePath
    final noMergePatterns = ['**/img/*', '**/*.jar', '**/*.zip']
    Path workPath

    SkeletonBuilder(Path baseProjectTemplatePath) {
        this.baseProjectTemplatePath = baseProjectTemplatePath
    }

    SkeletonBuilder withNoMergePatterns(String... patterns) {
        patterns.each { noMergePatterns << it }
        this
    }

    SkeletonBuilder clearNoMergePatterns() {
        noMergePatterns.clear()
        this
    }

    SkeletonBuilder withWorkPath(Path workPath) {
        this.workPath = workPath
        this
    }

    def generate(ProjectContext projectContext, Path outputPath) {
        createWorkPath()
        copyCoreTemplates(projectContext)
        handleLicenses(projectContext)
        copySourceTemplates(projectContext)
        Util.createDirForPath(outputPath, true)
        mergeTemplates(projectContext, outputPath)
    }

    def createWorkPath() {
        if(workPath == null) workPath = Util.createTempDirWithDeleteOnExit()
        else Util.createDirForPath(workPath)
        workPath
    }

    def copyCoreTemplates(ProjectContext projectContext) {
        def fileContextReader = new FileContextReader(baseProjectTemplatePath)
        Map<String, FileContext> fileContexts = fileContextReader.getFileContexts(projectContext)
        def srcPath = baseProjectTemplatePath.resolve(TEMPLATE_DIR_FILES).toAbsolutePath().toRealPath()
        if(!srcPath.toFile().isDirectory()) throw new IOException("Files directory not found: $srcPath")
        Handlebars handlebars = Util.createHandlebars(srcPath)
        List<Path> disabledPaths = []
        srcPath.toFile().eachFileRecurse { f ->
            def relPath = srcPath.relativize(f.toPath().toAbsolutePath().toRealPath())
            if(disabledPaths.every {!relPath.startsWith(it)}) {
                def relFilePath = relPath.toString()
                FileContext ctx = fileContexts[relFilePath]
                if(!ctx) {
                    Files.copy(f.toPath(), workPath.resolve(relPath))
                } else {
                    if(f.directory) {
                        if(ctx.enabled) Files.copy(f.toPath(), workPath.resolve(relPath))
                        else disabledPaths << relPath
                    } else if(ctx.enabled) {
                        Template template = handlebars.compile(relFilePath)
                        def mergedContent = template.apply(projectContext);
                        def relTargetPath = ctx.targetPath ?: relFilePath
                        def targetFilePath = workPath.resolve(relTargetPath)
                        targetFilePath.write(mergedContent)
                    }
                }
            }
        }
    }

    def handleLicenses(ProjectContext projectContext) {
        if(projectContext instanceof Licensable) {
            def tmpPath = createTempLicensesPath(projectContext.license)
            mergeLicensesFrom(tmpPath, projectContext)
        }
    }

    def createTempLicensesPath(String licenseName) {
        Path tmpPath = Util.createTempDirWithDeleteOnExit()
        def zstream = this.getClass().getResourceAsStream(LICENSES_ZIP_RESOURCE_PATH)
        if(zstream == null) throw new IOException("Cannot find resource $LICENSES_ZIP_RESOURCE_PATH")
        Util.unzipStream(zstream, tmpPath, licenseName)

        def templateLicensesPath = baseProjectTemplatePath.resolve("$TEMPLATE_DIR_LICENSES/$licenseName")
        def templateLicensesDir = templateLicensesPath.toFile()
        if (templateLicensesDir.isDirectory()) {
            templateLicensesDir.eachFileRecurse { f ->
                def relPath = templateLicensesPath.relativize(f.toPath())
                Files.copy(f.toPath(), tmpPath.resolve(relPath), StandardCopyOption.REPLACE_EXISTING)
            }
        }
        tmpPath
    }

    def mergeLicensesFrom(Path srcPath, ProjectContext projectContext) {
        def srcDir = srcPath.toFile()
        if(srcDir.isDirectory()) {
            Handlebars handlebars = Util.createHandlebars(srcPath)
            srcDir.eachFileRecurse { f ->
                if(f.name == LICENSE_YAML_FILE) {
                    projectContext.addExtraPropertiesFromYamlStream(f.newInputStream())
                } else {
                    def relPath = srcPath.relativize(f.toPath())
                    Template template = handlebars.compile(relPath.toString())
                    def mergedContent = template.apply(projectContext);
                    workPath.resolve(relPath).write(mergedContent)
                }
            }
        }
    }

    String getMergedContent(Path filePath, ProjectContext projectContext) {
        def file = filePath.toFile()
        if(!file.isFile()) return null
        Handlebars handlebars = Util.createHandlebars(filePath.parent)
        Template template = handlebars.compile(filePath.fileName.toString())
        template.apply(projectContext)
    }

    def copySourceTemplates(ProjectContext projectContext) {
        if(projectContext instanceof MavenCompatible) {
            def srcContextReader = new SourceContextReader(baseProjectTemplatePath)
            Map<String, Map<String, SourceFileContext>> sourceFileContexts = srcContextReader.getSourceFileContexts(projectContext)
            boolean multiModule = projectContext.isMultiModule()
            sourceFileContexts.each { artifact, map ->
                copyArtifactSourceTemplates(artifact, map, multiModule)
            }
        }
    }

    def copyArtifactSourceTemplates(String artifact, Map<String, SourceFileContext> srcFileContexts, boolean multiModule) {
        Path artifactPath = createArtifactSourceDir(artifact, multiModule)
        Path srcTemplateDir = baseProjectTemplatePath.resolve(TEMPLATE_DIR_FILES_SRC).toAbsolutePath().toRealPath()
        if(!srcTemplateDir.toFile().isDirectory()) throw new IOException("Source directory not found: $srcTemplateDir")
        Handlebars handlebars = Util.createHandlebars(srcTemplateDir)
        srcTemplateDir.eachFile { Path fPath ->
            def fName = fPath.toFile().name
            SourceFileContext ctx = srcFileContexts[fName]
            if(ctx && ctx.enabled) {
                Path packagePath = Util.getPackageAsPath(ctx.targetPackage)
                File targetDir = artifactPath.resolve(ctx.targetBaseDir).resolve(packagePath).toFile()
                targetDir.mkdirs()
                if(!targetDir.isDirectory()) throw new IOException("Cannot  create target directory '$targetDir'")
                String targetFileName = ctx.targetFileClass ? (ctx.targetFileClass + Util.getFileExtension(fName)) : fName
                Path targetFilePath = new File(targetDir, targetFileName).toPath()
                def relPath = srcTemplateDir.relativize(fPath.toAbsolutePath().toRealPath())
                Template template = handlebars.compile(relPath.toString())
                def mergedContent = template.apply(ctx);
                targetFilePath.write(mergedContent)
            }
        }
    }

    Path createArtifactSourceDir(String artifact, boolean multiModule) {
        if(!multiModule) return workPath
        Path artifactPath = workPath.resolve(artifact)
        Util.createDirForPath(artifactPath, true)
        artifactPath
    }

    void mergeTemplates(ProjectContext projectContext, Path outputPath) {
        Handlebars handlebars = Util.createHandlebars(workPath)
        def absTemplatePath = workPath.toFile().canonicalFile.toPath().toAbsolutePath().toRealPath()
        def mergeables = getMergeableFileNames()
        allFileNames.each { fName ->
            def path = Paths.get(fName).toAbsolutePath().toRealPath()
            def relPath = absTemplatePath.relativize(path)
            log.debug "processing $relPath..."
            def currOutputPath = Paths.get(outputPath.toFile().absolutePath, relPath.toString())
            def parent = currOutputPath.getParent()?.toFile()
            if(parent) parent.mkdirs()
            if(mergeables.contains(fName)) {
                Template template = handlebars.compile(relPath.toString())
                def mergedContent = template.apply(projectContext);
                currOutputPath.write(mergedContent)
            } else {
                Files.copy(path, currOutputPath)
            }
        }
    }

    List<String> getAllFileNames() {
        def fls = []
        workPath.eachFileRecurse(FileType.FILES) {Path path -> fls << path.toFile().absolutePath}
        fls
    }

    List<String> getMergeableFileNames() {
        def args = [
                dir: workPath.toString(),
                excludes: noMergePatterns.join(','),
                defaultExcludes: false
        ]
        def ant = new AntBuilder()
        def scanner = ant.fileScanner {
            fileset(args)
        }
        def fls = []
        for (f in scanner) {
            fls << f.getAbsolutePath()
        }
        fls
    }
}
