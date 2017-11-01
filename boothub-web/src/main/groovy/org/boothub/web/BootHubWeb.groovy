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
package org.boothub.web

import com.google.gson.Gson
import groovy.util.logging.Slf4j
import org.beryx.textio.TextIO
import org.beryx.textio.web.RunnerData
import org.boothub.BootHub
import org.boothub.context.ProjectContext
import org.boothub.repo.*
import org.kohsuke.github.GitHub

import static org.boothub.context.ExtUtil.setGitHubApi

@Slf4j
class BootHubWeb extends BootHub {
    final InitData initData
    final Map<String, String> sessionData

    BootHubWeb(TextIO textIO, SkeletonRepo skeletonRepo, RepoCache repoCache, RunnerData runnerData) {
        super(textIO, skeletonRepo, repoCache)
        this
            .withAutoChooseOutputDir(true)
            .withZipOutput(true)

        this.sessionData = runnerData.sessionData

        this.initData = new Gson().fromJson(runnerData.initData, InitData);
        println("##### initData: $initData")
    }

    @Override
    RepoKey getRepoKey() {
        if(initData.id && initData.version) {
            try {
                return new RepoEntry(url: initData.url, id: initData.id, version: Version.fromString(initData.version))
            } catch (Exception e) {
                log.warn("Invalid version: $initData.version")
            }
        }
        return new NullRepoKey(initData.url)
    }

    private void waitForSessionDataCompletion(long maxMillis) {
        long startTime = System.currentTimeMillis();
        long exitTime = startTime + maxMillis;
        while(System.currentTimeMillis() <= exitTime) {
            if(sessionData.completed) break
            sleep(50)
        }
        log.trace("Exiting waitForSessionDataCompletion after ${System.currentTimeMillis() - startTime} millis.")
    }

    void configureGhApi(ProjectContext ctx, TextIO textIO) {
        waitForSessionDataCompletion(10000);
        println "##### Starting configureGhApi() with accessToken $sessionData.accessToken"

        ctx.ghUserId = sessionData.ghUserId ?: initData.ghUserId ?: textIO.newStringInputReader().read("GitHub username")
        GitHub gitHubApi
        if(sessionData.accessToken) {
            try {
                gitHubApi = GitHub.connectUsingOAuth(sessionData.accessToken)
            } catch (Exception e) {
                textIO.textTerminal.println("\n### Failed to connect to GitHub. The project will be generated offline.")
                gitHubApi = null
            }
            if(gitHubApi) {
                setGitHubApi(ctx, gitHubApi)
                ctx.ghApiUsed = textIO.newBooleanInputReader()
                        .withDefaultValue(true)
                        .read("Add the generated project to GitHub?")
            }
        }
//        if(gitHubApi) textIO.textTerminal.println "Logged as $gitHubApi.myself.login"
    }
}
