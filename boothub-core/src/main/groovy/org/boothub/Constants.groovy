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
package org.boothub

class Constants {
    static final String TEMPLATE_DIR = 'template'
    static final String TEMPLATE_DIR_FILES = 'files'
    static final String TEMPLATE_DIR_FILES_SRC = 'files-src'
    static final String TEMPLATE_DIR_LICENSES = 'licenses'
    static final String TEMPLATE_DIR_SCRIPT = 'script'
    static final String LICENSE_YAML_FILE = 'license.yml'
    static final String LICENSES_ZIP_RESOURCE_PATH = '/licenses.zip'
    static final String SRC_CONTEXT_TEMPLATE = 'sources.yml'
    static final String FILES_CONTEXT_TEMPLATE = 'files.yml'
    static final String CONFIG_YAML_FILE = 'config.yml'

    static final String CTX_EXT_TEMPLATE_DIR = 'boothub:template-dir'
    static final String CTX_EXT_TEXT_IO = 'boothub:text-io'
    static final String CTX_EXT_GITHUB_API = 'boothub:github-api'
    static final String CTX_EXT_GITHUB_PASS = 'boothub:github-pass'
    static final String CTX_EXT_GIT_HTTP_URL = 'boothub:git-http-url'
}
