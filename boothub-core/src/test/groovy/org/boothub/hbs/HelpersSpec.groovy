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
package org.boothub.hbs

import spock.lang.Specification
import spock.lang.Unroll

import static org.boothub.Util.stripAll

@Unroll
class HelpersSpec extends Specification implements HandlebarsUtil {
    def template = stripAll('''
            {{#each modules}}
            {{#def 'projectBlockPrefix'}}project('{{artifact}}') { {{~/def}}
            {{#encloseIf multiModule prefix=projectBlockPrefix suffix="}"}}
            {{#ifb (compare artifact '==' appModule.artifact)}}apply plugin: 'application'{{/ifb}}
            {{~#if multiModule}}
            dependencies {
                {{~#each dependsOn}}
                compile project(':{{artifact}}')
                {{~/each}}
            }
            {{~/if}}
            {{/encloseIf}}
            {{/each}}
        ''')

    def contextSingle = stripAll('''
        multiModule: false
        modules:
          - !!org.boothub.context.StandardModuleContext &mod-weird-app
            artifact: weird-app
            basePackage: org.bizarre_soft.weird_app
        appModule: *mod-weird-app
        appMainClass: WeirdAppMain
        ''')

    def "should not enclose single module"() {
        when:
        def merged = merge(template, contextSingle)

        then:
        merged == "apply plugin: 'application'"
    }


    def contextMulti = stripAll('''
        multiModule: true
        modules:
          - !!org.boothub.context.StandardModuleContext &mod-strange-lib
            artifact: strange-lib
            basePackage: org.bizarre_soft.strange_lib
          - !!org.boothub.context.StandardModuleContext &mod-spooky-ui
            artifact: spooky-ui
            basePackage: org.bizarre_soft.spooky_ui
            dependsOn:
              - *mod-strange-lib
          - !!org.boothub.context.StandardModuleContext &mod-weird-app
            artifact: weird-app
            basePackage: org.bizarre_soft.weird_app
            dependsOn:
              - *mod-strange-lib
              - *mod-spooky-ui
        appModule: *mod-weird-app
        appMainClass: WeirdAppMain
        ''')

    def "should enclose multi-module"() {
        when:
        def merged = merge(template, contextMulti)
        println "merged:\n$merged"

        then:
        merged == stripAll('''
            project('strange-lib') {
                dependencies {
                }
            }
            project('spooky-ui') {
                dependencies {
                    compile project(':strange-lib')
                }
            }
            project('weird-app') {
                apply plugin: 'application\'
                dependencies {
                    compile project(':strange-lib')
                    compile project(':spooky-ui')
                }
            }
        ''')
    }
}
