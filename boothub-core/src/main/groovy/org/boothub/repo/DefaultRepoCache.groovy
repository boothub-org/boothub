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
package org.boothub.repo

import groovy.util.logging.Slf4j
import org.boothub.Util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
class DefaultRepoCache implements RepoCache, RepoCacheUtil {
    final Path cachePath
    boolean checkCachedLength
    boolean checkCachedSha

    DefaultRepoCache(Path cachePath = null) {
        if(cachePath) {
            this.cachePath = cachePath
            Util.ensureDirExists(cachePath)
        } else {
            this.cachePath = Files.createTempDirectory("boothub-cache-")
        }
    }

    static DefaultRepoCache ofCurrentUser() {
        Path path = Paths.get(System.properties.getProperty('user.home'), '.boothub', 'cache')
        new DefaultRepoCache(path)
    }

    DefaultRepoCache withCheckCachedLength(boolean checkCachedLength) {
        this.checkCachedLength = checkCachedLength
        this
    }

    DefaultRepoCache withCheckCachedSha(boolean checkCachedSha) {
        this.checkCachedSha = checkCachedSha
        this
    }

    Path get(RepoKey key, long expectedSize = EXPECTED_SIZE_UNKNOWN, String expectedSha = null) {
        Path path
        if(key.id == NullRepoKey.NO_ID) {
            path = Util.createTempFileWithDeleteOnExit(".zip")
            downloadFile(key, path, expectedSize, expectedSha)
        } else {
            path = getPath(key)
            synchronized(path.toString().intern()) {
                long cachedLength = (checkCachedLength ? expectedSize : EXPECTED_SIZE_UNKNOWN)
                String cachedSha =  (checkCachedSha ? expectedSha : null)
                def cachedErr = getValidationError(path, key, cachedLength, cachedSha)
                if(cachedErr) {
                    log.debug "Invalid cache entry $path: $cachedErr"
                    downloadFile(key, path, expectedSize, expectedSha)
                }
            }
        }
        path
    }

    protected Path getPath(SkeletonKey key) {
        def tokens = key.id.split('\\.')
        Paths.get(cachePath.toString(), tokens)
                .resolve(key.version.toString())
                .resolve(tokens[-1] + ".zip")
    }
}
