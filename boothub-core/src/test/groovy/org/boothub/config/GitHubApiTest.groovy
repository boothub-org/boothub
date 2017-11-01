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
package org.boothub.config

import org.beryx.textio.TextIO
import org.beryx.textio.TextIoFactory
import org.kohsuke.github.GitHub

class GitHubApiTest {

    public static void main(String[] args) {
        TextIO textIO = TextIoFactory.textIO
        String user = textIO.newStringInputReader().read("GitHub user")
        String pass = textIO.newStringInputReader().withInputMasking(true).read("password")

        GitHub github = GitHub.connectUsingPassword(user, pass)
        def organization = textIO.newStringInputReader().read("organization")
        def repoName = textIO.newStringInputReader().read("repository")
        textIO.dispose()

        def repo = github.getOrganization(organization)
                    .createRepository(repoName)
                    .homepage("http://www.my-repo.com")
                    .description("Bla bla bla")
                    .create()

        repo.createContent("This is my content", "my first commit ever", "doc/draft/content1.txt")

//        GHRepository repo = github.createRepository(
//                "new-repository","this is my new repository",
//                "http://www.kohsuke.org/",true/*public*/);
//        repo.addCollaborators(github.getUser("abayer"),github.getUser("rtyler"));
//        repo.delete();
        println "I am " + github.getMyself().name
    }
}
