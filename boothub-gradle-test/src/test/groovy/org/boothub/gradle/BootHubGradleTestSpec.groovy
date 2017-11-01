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
package org.boothub.gradle

import org.boothub.Initializr
import spock.lang.Specification

import java.util.stream.Collectors

class BootHubGradleTestSpec extends Specification {
    private static final TEMPLATE_DIR = 'src/test/resources/template'
    private static final CONTEXT_FILE = 'src/test/resources/context.yml'

    private static final APP_MAIN_CLASS = 'WeirdAppMain'
    private static final APP_MAIN_CLASS_PATH = "org/bizarre_soft/weird_app/${APP_MAIN_CLASS}.class"

    GradleTemplateBuilder builder = new GradleTemplateBuilder(TEMPLATE_DIR).withContextFile(CONTEXT_FILE)

    def "should create a valid artifact"() {
        when:
        def artifacts = builder.runGradleBuild().artifacts
        def jars = artifacts['jar']

        then:
        jars.size() == 1
        jars[0].getEntry(APP_MAIN_CLASS_PATH) != null
    }

    def "should create a valid application (run without arguments)"() {
        when:
        def output = ProjectExecutor.execute(builder)

        then:
        output == "XBox multiplayer strategy game: Hello from $APP_MAIN_CLASS!"
    }

    def "should create a valid application (run with arguments)"() {
        when:
        def output = ProjectExecutor.execute(builder, null, null, "Jane Mary Jim")

        then:
        output == "XBox multiplayer strategy game: Hello from Jane and Mary and Jim!"
    }

    def "should provide a stream of ProjectContexts"() {
        given:
        List<String> variants = new ProjectContextStreamBuilder({ -> new Initializr(TEMPLATE_DIR).createContext(CONTEXT_FILE)})
                .withFlagNames("releaseBuild", "genre", "multiplayer")
                .withFlag("platform", ["Wii", "PlayStation", "XBox"])
                .withExclusion {ctx -> !ctx.releaseBuild && ctx.genre.name() == "STRATEGY"}
                .withExclusion {ctx -> ctx.multiplayer && ctx.platform == "XBox"}
                .stream()*.dumpFlags()

        expect:
        variants.size() == (2 * 4 - 1) * (2 * 3 - 1)
        variants.unique() == variants
        variants.any {variant -> variant.contains("releaseBuild: false, genre: STRATEGY")} == false
        variants.findAll {variant -> variant.contains("releaseBuild: false, genre: ACTION")}.size() == 2 * 3 - 1
        variants.any {variant -> variant.contains("multiplayer: true, platform: XBox")} == false
        variants.findAll {variant -> variant.contains("multiplayer: true, platform: Wii")}.size() == 2 * 4 - 1
    }

    def "should create valid applications for all combinations of flags"() {
        given:
        List outputs = new ProjectContextStreamBuilder({ -> new Initializr(TEMPLATE_DIR).createContext(CONTEXT_FILE)})
                .withFlagName("multiplayer")
                .withFlag("platform", ["PlayStation", "XBox"])
                .withExclusion {ctx -> ctx.multiplayer && ctx.platform == "XBox"}
                .stream()
                .map {ctx -> new ProjectExecutor(ctx).withTemplateDir(TEMPLATE_DIR).execute()}
                .collect(Collectors.toList())
                .sort()

        expect:
        outputs == [
                "PlayStation multiplayer strategy game: Hello from WeirdAppMain!",
                "PlayStation single-player strategy game: Hello from WeirdAppMain!",
                "XBox single-player strategy game: Hello from WeirdAppMain!"
        ]
    }
}
