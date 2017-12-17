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

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromAbstractTypeMethods

enum Helpers implements Helper {
    ENCLOSE_IF ("encloseIf", { condition, options ->
        Options.Buffer buffer = options.buffer()
        CharSequence body = options.fn()
        if (options.isFalsy(condition)) {
            buffer.append(body)
        } else {
            String prefix = options.hash("prefix", "")
            String suffix = options.hash("suffix", "")
            int indent = options.hash("indent", "4") as int
            buffer.append(prefix)
            if(indent <= 0) buffer.append(body)
            else {
                def padding = ' ' * indent
                body.eachLine { line ->
                    buffer.append(padding).append(line).append('\n')
                }
            }
            buffer.append(suffix)
        }
        buffer

    }),

    BADGE_URL ("badgeUrl", { subject, options ->
        Options.Buffer buffer = options.buffer()
        subject = asBadgePart(from(subject))
        def status = asBadgePart(options.prm(0))
        def color = asBadgePart(options.prm(1))
        def url = "https://img.shields.io/badge/$subject-$status-${color}.svg"
        buffer.append(url)
        buffer
    }),


    final String helperName
    private final Closure<CharSequence> closure;

    Helpers(String helperName, @ClosureParams(value=FromAbstractTypeMethods, options="com.github.jknack.handlebars.Helper") Closure<CharSequence> closure) {
        this.closure = closure
        this.helperName = helperName
    }

    @Override
    public CharSequence apply(context, Options options) throws IOException {
        closure.call(context, options)
    }

    static void register(Handlebars handlebars) {
        Helpers.values().each { helper -> handlebars.registerHelper(helper.helperName, helper)}
    }

    private static from(param) {
        if(param instanceof Options.NativeBuffer) {
            param.writer.toString()
        } else {
            param
        }
    }

    private static String asBadgePart(String s) {
        s = s.replaceAll('-', '--')
            .replaceAll('_', '__')
            .replaceAll(' ', '_')
        def uri = new URI(null, null, s, null, null)
        uri.toString()
    }
}
