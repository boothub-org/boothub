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

import groovy.util.logging.Slf4j
import org.beryx.textio.TextIO
import org.beryx.textio.TextIoFactory
import org.boothub.context.ProjectContext
import org.boothub.repo.*
import org.kohsuke.github.GitHub

import static org.boothub.context.ExtUtil.*

@Slf4j
class BootHubCli extends BootHub {

    BootHubCli(TextIO textIO, SkeletonRepo skeletonRepo, RepoCache repoCache) {
        super(textIO, skeletonRepo, repoCache)
    }

    @Override
    RepoKey getRepoKey() {
        def skeletons = skeletonRepo.getSkeletons(new SkeletonSearchOptions(lastVersionOnly: true, compact: true)).valueOrThrow()
        String skeletonName = textIO.newStringInputReader()
                .withNumberedPossibleValues(skeletons.keySet() as List)
                .withValueFormatter{id -> "${skeletons[id].name} ($id)" as String}
                .read("Project type")
        skeletons[skeletonName].entries.lastEntry().value
    }

    void configureGhApi(ProjectContext ctx, TextIO textIO) {
        ctx.ghUserId = textIO.newStringInputReader().read("GitHub username")
        ctx.ghApiUsed = textIO.newBooleanInputReader()
                .withDefaultValue(true)
                .read("Add the generated project to GitHub?")
        if(ctx.ghApiUsed) {
            passLoop:
            while(true) {
                String pass = textIO.newStringInputReader()
                        .withInputMasking(true)
                        .read("GitHub password")
                setGitHubPassword(ctx, pass)
                String errMsg
                try {
                    GitHub gitHubApi = GitHubUtil.connectUsingPassword(ctx.ghUserId, pass)
                    if(gitHubApi.credentialValid) {
                        setGitHubApi(ctx, gitHubApi)
                        break
                    } else {
                        errMsg = "Invalid credentials"
                    }
                } catch (IOException e) {
                    errMsg = e.message
                }
                textIO.textTerminal.println("Cannot connect to GitHub: $errMsg" as String)
                def option = textIO.newStringInputReader()
                        .withPromptAdjustments(false)
                        .withInlinePossibleValues("p", "u", "w")
                        .withIgnoreCase()
                        .read("Try another (p)assword, change GitHub (u)sername or continue (w)ithout adding the project to GitHub? ")
                switch (option) {
                    case "p": break
                    case "u": ctx.ghUserId = textIO.newStringInputReader().read("GitHub username"); break
                    default: ctx.ghApiUsed = false; break passLoop
                }
            }
        }
        def gitHubApi = getGitHubApi(ctx)
        if(gitHubApi) textIO.textTerminal.println("Logged as $gitHubApi.myself.login" as String)
    }

    GenerationResultData executeCLI() {
        def resultData = execute()
        if(resultData) {
            if(resultData.errorMessage) {
                textIO.textTerminal.println(resultData.errorMessage)
            } else {
                if(resultData.gitHubRepoLink) {
                    textIO.textTerminal.println("Your project is now available on GitHub: $resultData.gitHubRepoLink" as String)
                }
                if(resultData.outputPath) {
                    textIO.textTerminal.println("Output written to $resultData.outputPath" as String)
                }
            }
        }
        textIO.newStringInputReader().withMinLength(0).read("\nPress enter to terminate...")
        textIO.dispose()
        resultData
    }


    static void main(String[] args) {
        def textIO = TextIoFactory.textIO

//        def repoClass = textIO.<Class<? extends SkeletonRepo>> newGenericInputReader(null)
//                .withNumberedPossibleValues([LocalSkeletonRepo, JsonSkeletonRepo])
//                .withDefaultValue(LocalSkeletonRepo)
//                .read("Repo type")
        def repoClass = Class.forName(System.properties['boothubRepoClass'] ?: JsonSkeletonRepo.name)
        def repo = repoClass.newInstance()

        new BootHubCli(textIO, repo, DefaultRepoCache.ofCurrentUser()).executeCLI()
    }

}
