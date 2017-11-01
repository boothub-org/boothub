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
import org.kohsuke.github.GitHub

import static ExtUtil.getGitHubApi

@Slf4j
class PomContextConfigurator extends TextIOConfigurator {
    @Override
    void configureWithTextIO(ProjectContext context, TextIO textIO) {
        def ctx = context as PomContext
        textIO.textTerminal.println("Developers")

        GitHub gitHub = getGitHubApi(context)
        String myName = null
        String myId = null
        String myEmail = null
        if(gitHub) {
            try {
                def myself = gitHub.getMyself()
                myId = myself.login
                myName = myself.name
                myEmail = myself.email
            } catch (Exception e) {
                log.warn("Cannot retrieve GitHub self info", e)
            }
        }
        boolean firstDev = true
        while(true) {
            if(!firstDev) {
                if(!textIO.newBooleanInputReader()
                        .withDefaultValue(false)
                        .read("More developers?")) {
                    break
                }
            }
            PomContext.Developer dev = new PomContext.Developer()
            ctx.developers << dev

            dev.id = textIO.newStringInputReader()
                    .withDefaultValue(firstDev ? ctx.ghUserId : null)
                    .withPattern(Util.MAVEN_ID_REGEX)
                    .read("Developer ID")

            String defaultName = (dev.id == myId) ? myName : null
            dev.name = textIO.newStringInputReader()
                    .withDefaultValue(defaultName)
                    .read("Developer name")

            String defaultEmail = (dev.id == myId) ? myEmail : null
            dev.email = textIO.newStringInputReader()
                    .withMinLength(0)
                    .withDefaultValue(defaultEmail)
                    .read("Developer email" + (defaultEmail ? "" : " (optional)"))

            firstDev = false
        }

    }
}
