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

import groovy.util.logging.Slf4j

@Slf4j
class LogUtil {
    private static Closure<Void> logMethod = {s -> log.warn(s)}

    static void setLogPrintAsDebug() {
        logMethod = {s -> log.debug(s)}
    }
    static void setLogPrintAsInfo() {
        logMethod = {s -> log.info(s)}
    }
    static void setLogPrintAsWarn() {
        logMethod = {s -> log.warn(s)}
    }
    static void setLogPrintAsError() {
        logMethod = {s -> log.error(s)}
    }

    static void logPrint(s) {
        logMethod(s)
    }
}
