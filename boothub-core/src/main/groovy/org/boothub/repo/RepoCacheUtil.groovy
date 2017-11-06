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

import java.nio.file.Path

import static org.boothub.repo.RepoCache.EXPECTED_SIZE_INVALIDATE

@Slf4j
trait RepoCacheUtil {
    String getValidationError(Path path, RepoKey key, long expectedSize, String expectedSha) {
        if(!path) return "Path is null"
        def file = path.toFile()
        if(!file.isFile()) return "Not a file"
        if(expectedSize == EXPECTED_SIZE_INVALIDATE) return "Cache invalidation requested"
        if(expectedSize > 0 && file.length() != expectedSize) return "Invalid length of $key.url. Expected: $expectedSize. Actual: ${file.length()}"
        def sha = Util.getSha256(file)
        if(expectedSha && sha != expectedSha) return "Invalid SHA of $key.url. Expected: $expectedSha. Actual: $sha"
        null
    }

    void downloadFile(RepoKey key, Path path, long expectedSize, String expectedSha) {
        Util.downloadFile(key.url, path)
        if(expectedSize != EXPECTED_SIZE_INVALIDATE) {
            def err = getValidationError(path, key, expectedSize, expectedSha)
            if (err) {
                path.toFile().delete()
                throw new IOException(err)
            }
        }
    }
}
