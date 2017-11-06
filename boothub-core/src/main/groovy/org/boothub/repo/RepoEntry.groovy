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

import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.boothub.Util
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

import java.util.zip.ZipFile

@TupleConstructor
@ToString(includeSuper = true)
class RepoEntry extends SkeletonInfo implements RepoKey {
    String url
    long size = -1
    String sha
    Date createdOn
    Date updatedOn
    int ratingCount
    int usageCount
    int ratingSum
    String validationError

    /**
     * This method does not set the {@code size} and {@code sha} attributes. It is your responsibility to subsequently set them.
     */
    static RepoEntry fromDir(File dir) {
        if(!dir.directory) throw new IllegalArgumentException("Directory not found: $dir.absolutePath")
        def infoUrl = new File(dir, "info.yml").toURI().toURL()
        def repoEntry = (RepoEntry)new Yaml(new Constructor(RepoEntry)).load(infoUrl.openStream())
        repoEntry.url = dir.toURI().toURL().toString()
        repoEntry.description = new File(dir, "description.md").text
        repoEntry
    }

    /**
     * This method does not set the {@code url}. It is your responsibility to subsequently set it.
     */
    static RepoEntry fromZipFile(File file) {
        ZipFile zipFile = new ZipFile(file)
        if(!zipFile) throw new IllegalArgumentException("zipFile is null")
        def infoEntry = zipFile.getEntry("info.yml")
        if(!infoEntry) throw new IllegalArgumentException("Cannot find 'info.yml' inside of $zipFile")
        def repoEntry = (RepoEntry)new Yaml(new Constructor(RepoEntry)).load(zipFile.getInputStream(infoEntry))

        def descrEntry = zipFile.getEntry("description.md")
        if(!descrEntry) throw new IllegalArgumentException("Cannot find 'description.md' inside of $zipFile")
        repoEntry.description = zipFile.getInputStream(descrEntry).text

        repoEntry.size = file.length()
        repoEntry.sha = Util.getSha256(file)

        repoEntry
    }
}
