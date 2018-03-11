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

import groovy.util.logging.Slf4j
import org.beryx.textio.TextIO
import org.boothub.Util
import org.boothub.Version
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub

import static ExtUtil.getGitHubApi

@Slf4j
class ProjectInfoConfigurator extends TextIOConfigurator {
    @Override
    void configureWithTextIO(ProjectContext context, TextIO textIO) {
        def ctx = context as ProjectInfo
        ctx.boothubVersion = Version.BOOTHUB_CURRENT.toString()
        def gitHubApi = getGitHubApi(ctx)
        def organizations = getOrganizations(gitHubApi)
        if(organizations != null) {
            if(organizations.empty) {
                ctx.ghProjectOwner = ctx.ghUserId
            } else {
                def ownerOptions = [ctx.ghUserId]
                ownerOptions.addAll(new ArrayList<>(organizations).sort())
                ctx.ghProjectOwner = textIO.newStringInputReader()
                        .withDefaultValue(ctx.ghUserId)
                        .withNumberedPossibleValues(ownerOptions)
                        .read("Project owner")
            }
        } else {
            ctx.ghProjectOwner = textIO.newStringInputReader()
                    .withDefaultValue(ctx.ghUserId)
                    .withPattern(Util.GITHUB_USERNAME_REGEX)
                    .read("Project owner (GitHub user or organization)")
        }

        List<GHRepository> repos = getExistingRepos(gitHubApi, ctx)
        def projectIdReader = textIO.newStringInputReader()
        if(repos) {
            List<String> repoNames = repos.collect {repo -> repo.name.toLowerCase()}
            projectIdReader.withValueChecker{prjId, prop -> repoNames.contains(prjId.toLowerCase()) ? ['A repository with this name already exists.'] : null}
        }
        ctx.ghProjectId = projectIdReader
                .withValueChecker(Util.getMavenIdChecker('Not a valid project ID'))
                .withValueChecker{val, prop -> (val == '.' || val == '..') ? ["The repository name $val is reserved."] : null}
                .read("Repository name")

        ctx.projectName = textIO.newStringInputReader()
                .withDefaultValue(ctx.ghProjectId)
                .withMaxLength(100)
                .read("Project name")

        ctx.projectPageUrl = textIO.newStringInputReader()
                .withDefaultValue("https://github.com/$ctx.ghProjectOwner/$ctx.ghProjectId" as String)
                .read("Project page URL")
    }

    private Set<String> getOrganizations(GitHub gitHubApi) {
        if(gitHubApi) {
            try {
                return gitHubApi.getMyOrganizations().keySet()
            } catch (Exception e) {
                log.error("Cannot get the list of organizations", e)
            }
        }
        null
    }

    private List<GHRepository> getExistingRepos(GitHub gitHubApi, ProjectContext ctx) {
        List<GHRepository> repos = null
        try {
            if (gitHubApi) {
                if (ctx.ghProjectOwner == ctx.ghUserId) {
                    repos = gitHubApi.myself.listRepositories().asList()
                } else {
                    repos = gitHubApi.getOrganization(ctx.ghProjectOwner).listRepositories().asList()
                }
            }
        } catch (Exception e) {
            log.error("Cannot get the repositories of $ctx.ghProjectOwner", e)
            repos = null
        }
        repos
    }
}
