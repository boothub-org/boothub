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

import org.beryx.textio.web.TextIoApp

import java.awt.*
import java.util.function.Consumer

/**
 * Allows executing code in a {@link org.beryx.textio.web.WebTextTerminal}
 * by configuring and initializing a {@link TextIoApp}.
 */
class WebTextIoExecutor {
    private Integer port
    private boolean browserAutoStart

    WebTextIoExecutor withPort(int port) {
        this.port = port
        return this
    }

    WebTextIoExecutor withBrowserAutoStart(boolean browserAutoStart) {
        this.browserAutoStart = browserAutoStart
        return this
    }

    void execute(TextIoApp app) {
        Consumer<String> stopServer = {sessionId ->
//                Executors.newSingleThreadScheduledExecutor().schedule({ ->
//                        System.exit(0)}, 2, TimeUnit.SECONDS)
        }

        app.withOnDispose(stopServer)
                .withOnAbort(stopServer)
                .withPort(port)
                .withMaxInactiveSeconds(600)
                .withStaticFilesLocation("static")
                .init()

        if(browserAutoStart) {
            String url = "http://localhost:$app.port/index.html"
            boolean browserStarted = false
            if(Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(new URI(url))
                    browserStarted = true
                } catch (Exception e) {
                    e.printStackTrace()
                }
            }
            if(!browserStarted) {
                System.out.println("Please open the following link in your browser: " + url)
            }
        }
    }
}
