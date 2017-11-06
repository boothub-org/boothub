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

import groovy.io.FileType
import groovy.util.logging.Slf4j
import org.ajoberstar.grgit.Credentials
import org.ajoberstar.grgit.Grgit
import org.boothub.context.ProjectContext
import org.codehaus.groovy.runtime.IOGroovyMethods
import org.kohsuke.github.*

import static org.boothub.context.ExtUtil.*

@Slf4j
class GitHubUtil {
    static executableExtensions = ['.bat', '.cmd', '.pl', '.py', '.sh', '.tcl']
    static GHRepository createRepo(ProjectContext ctx) {
        if(!ctx.ghApiUsed) throw new IllegalArgumentException("GitHub usage not enabled")
        GitHub github = getGitHubApi(ctx)
        if(!github) throw new IllegalArgumentException("GitHub client not set")
        if(!github.credentialValid) throw new IllegalArgumentException("Invalid credentials")

        String repoName = ctx.ghProjectId
        def repoBuilder = (ctx.ghUserId == ctx.ghProjectOwner) ?
                github.createRepository(repoName) :
                github.getOrganization(ctx.ghProjectOwner).createRepository(repoName)

        def repo = repoBuilder
                    .homepage(ctx.projectPageUrl)
                    .description(ctx.projectName)
                    .create()

        setGitHttpUrl(ctx, repo.gitHttpTransportUrl())

        repo
    }

    static void addContent(ProjectContext ctx, GHRepository repo, String workingCopyPath) {
        String password = getGitHubPassword(ctx)
        if(password) {
            addContentWithGrgit(ctx, workingCopyPath)
        } else {
            addContentWithGitHubApi(repo, workingCopyPath)
        }
    }

    static void addContentWithGrgit(ProjectContext ctx, String workingCopyPath) {
        Grgit.init(dir: workingCopyPath)
        def credentials = new Credentials(ctx.ghUserId, getGitHubPassword(ctx))
        Grgit grgit = Grgit.open(dir: workingCopyPath, creds: credentials)
        grgit.remote.add(name: 'origin', url: getGitHttpUrl(ctx))
        grgit.add(patterns: ['.'], update: false)
        grgit.commit(message: 'initial commit by BootHub')
        grgit.push()
    }

    static void addContentWithGitHubApi(GHRepository repo, String workingCopyPath) {
        repo.createContent("Repository created by BootHub", "create repository with BootHub", "README.md")

        GHRef masterRef = repo.getRef("heads/master")
        String masterTreeSha = repo
                .getTreeRecursive("master", 1)
                .sha

        GHTreeBuilder treeBuilder = new GHTreeBuilder(repo).baseTree(masterTreeSha)

        def workingCopyDir = new File(workingCopyPath)
        def workingCopyUri = workingCopyDir.toURI()
        workingCopyDir.eachFileRecurse(FileType.FILES) { f ->
            def relPath = workingCopyUri.relativize(f.toURI()).path
            boolean executable = isMaybeExecutable(f.name)
            if(isMaybeBinary {f.newInputStream()}) {
                String sha = new GHBlobBuilder(repo)
                        .binaryContent(f.bytes)
                        .create()
                        .sha
                treeBuilder.shaEntry(relPath, sha, executable)
            } else {
                treeBuilder.textEntry(relPath, f.text, executable)
            }
        }
        String treeSha = treeBuilder.create().sha
        String commitSha = new GHCommitBuilder(repo)
                .message("Initial commit by BootHub")
                .tree(treeSha)
                .parent(masterRef.getObject().getSha())
                .create()
                .SHA1
        masterRef.updateTo(commitSha)
    }

    static boolean updateContent(GHRepository repo, String content, String commitMessage, String path, boolean executable) {
        String currentContent = repo.getFileContent(path).read().text
        if(currentContent?.trim() == content?.trim()) {
            log.debug("No content change. Skipping update of $path")
            return false
        }
        GHRef masterRef = repo.getRef("heads/master")
        String masterTreeSha = repo
                .getTreeRecursive("master", 1)
                .sha
        GHTreeBuilder treeBuilder = new GHTreeBuilder(repo).baseTree(masterTreeSha)
        treeBuilder.textEntry(path, content, executable)
        String treeSha = treeBuilder.create().sha
        String commitSha = new GHCommitBuilder(repo)
                .message(commitMessage)
                .tree(treeSha)
                .parent(masterRef.getObject().getSha())
                .create()
                .SHA1
        masterRef.updateTo(commitSha)
        true
    }

    static boolean isMaybeExecutable(String fileName) {
        executableExtensions.any { ext -> fileName.endsWith(ext) }
    }

    static boolean isMaybeBinary(Closure<InputStream> streamProvider) {
        IOGroovyMethods.withCloseable(streamProvider.call()) { istream ->
            istream.any { byte b -> (b != 9) && (b != 10) && (b != 13) && (((byte)(b + 1)) <= (byte)32) }
        }
    }
}
