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

abstract class StandardProjectContext extends ProjectContext implements Licensable, Versionable {
    static class Generic extends StandardProjectContext implements StandardMavenCompatible.Generic {}
    static class App extends StandardProjectContext implements StandardMavenCompatible.App {}
    static class Lib extends StandardProjectContext implements StandardMavenCompatible.Lib {}
    static class Multi extends StandardProjectContext implements StandardMavenCompatible.Multi {}
    static class AppMulti extends StandardProjectContext implements StandardMavenCompatible.AppMulti {}
    static class LibMulti extends StandardProjectContext implements StandardMavenCompatible.LibMulti {}
    static class Single extends StandardProjectContext implements StandardMavenCompatible.Single {}
    static class AppSingle extends StandardProjectContext implements StandardMavenCompatible.AppSingle {}
    static class LibSingle extends StandardProjectContext implements StandardMavenCompatible.LibSingle {}
}
