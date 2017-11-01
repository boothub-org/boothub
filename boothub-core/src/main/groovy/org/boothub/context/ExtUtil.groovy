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

import org.beryx.textio.TextIO
import org.beryx.textio.TextIoFactory
import org.boothub.Constants
import org.kohsuke.github.GitHub

class ExtUtil {
    static TextIO getTextIO(ProjectContext ctx) {
        def io = ctx.ext[Constants.CTX_EXT_TEXT_IO]
        return (io instanceof TextIO) ? io : TextIoFactory.textIO
    }
    static void setTextIO(ProjectContext ctx, TextIO textIO) {
        ctx.ext[Constants.CTX_EXT_TEXT_IO] = textIO
    }

    static String getTemplateDir(ProjectContext ctx) {
        ctx.ext[Constants.CTX_EXT_TEMPLATE_DIR]
    }
    static void setTemplateDir(ProjectContext ctx, String dir) {
        ctx.ext[Constants.CTX_EXT_TEMPLATE_DIR] = dir
    }

    static GitHub getGitHubApi(ProjectContext ctx) {
        ctx.ext[Constants.CTX_EXT_GITHUB_API]
    }
    static void setGitHubApi(ProjectContext ctx, GitHub gitHubApi) {
        ctx.ext[Constants.CTX_EXT_GITHUB_API] = gitHubApi
    }

    static String getGitHubPassword(ProjectContext ctx) {
        ctx.ext[Constants.CTX_EXT_GITHUB_PASS]
    }
    static void setGitHubPassword(ProjectContext ctx, String gitHubPass) {
        ctx.ext[Constants.CTX_EXT_GITHUB_PASS] = gitHubPass
    }

    static String getGitHttpUrl(ProjectContext ctx) {
        ctx.ext[Constants.CTX_EXT_GIT_HTTP_URL]
    }
    static void setGitHttpUrl(ProjectContext ctx, String gitHttpUrl) {
        ctx.ext[Constants.CTX_EXT_GIT_HTTP_URL] = gitHttpUrl
    }
}
