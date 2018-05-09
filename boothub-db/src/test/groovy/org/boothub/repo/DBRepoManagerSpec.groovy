/*
 * Copyright 2018 the original author or authors.
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

import org.boothub.Result
import org.boothub.Util
import org.boothub.Version
import org.hsqldb.Server
import spock.lang.Specification

class DBRepoManagerSpec  extends Specification {
    Server server
    DBRepoManager repoManager

    def setup() {
        repoManager = new HSQLDBRepoManager.Builder()
                .withDbPort(9137)
                .withDbDir(Util.createTempDirWithDeleteOnExit().toString())
                .create()
        server = repoManager.server
    }

    def cleanup() {
        server.shutdown()
    }

    private RepoEntry defaultEntry() {
        def entry = new RepoEntry()

        entry.id = 'org.example.mytemplate'
        entry.version = new Version(2, 3, 4)
        entry.name = 'My Template'
        entry.caption = 'A very useful template'
        entry.minimumRequiredBootHub = new Version(1, 2, 3)
        entry.homepage = 'http://mytemplate.example.org'
        entry.authors = ['jsmith', 'mbrown']
        entry.tags = ['good', 'bad', 'ugly']
        entry.description = 'No description'

        entry.url = 'http://example.org/entries/1'
        entry.size = 555
        entry.sha= 'ccbbaa0099887766'
        entry.createdOn = new Date(1667700077661L)
        entry.updatedOn = new Date(1667789877661L)
        entry.ratingCount = 111
        entry.usageCount = 222
        entry.ratingSum = 333
        entry
    }

    def "should correctly handle skeleton entries"() {
        when: "adding a skeleton entry"
        def entry1 = defaultEntry()
        def result1 = repoManager.addEntry(entry1, 'jsmith')

        and: "adding another entry with the same id, but a different version and a changed name"
        def entry2 = defaultEntry()
        entry2.name = "My Changed Template"
        entry2.version.minor = entry1.version.minor + 1
        def result2 = repoManager.addEntry(entry2, 'jsmith')

        then: "two entries have been added and the skeleton name has changed"
        result1.type == Result.Type.SUCCESS
        result2.type == Result.Type.SUCCESS
        def skeletons = repoManager.getSkeletons()
        skeletons.size() == 2
        skeletons[0] == entry1
        skeletons[0].name == "My Changed Template"
        skeletons[1] == entry2
        skeletons[1].name == "My Changed Template"

        when: "deleting the first entry"
        def deleteResult = repoManager.deleteEntry(entry1.id, entry1.version)
        skeletons = repoManager.getSkeletons()

        then: "only the second entry remains"
        deleteResult.type == Result.Type.SUCCESS
        skeletons.size() == 1
        skeletons[0] == entry2
        skeletons[0].name == "My Changed Template"

        when: "trying to add an entry with the same id and version as the existing one"
        def entry3 = defaultEntry()
        entry3.version = entry2.version
        entry3.description = "Same id and version as entry2"
        def result3 = repoManager.addEntry(entry3, 'jsmith')

        then: "the addEntry should fail"
        result3.type == Result.Type.ERROR
    }

    def "should check ownership"() {
        when: "adding en entry with jsmith as owner"
        def entry1 = defaultEntry()
        def result1 = repoManager.addEntry(entry1, 'jsmith')

        and: "ading another etry with the same id, a different version and a different owner"
        def entry2 = defaultEntry()
        entry2.version.minor = entry1.version.minor + 1
        entry2.name = "My Changed Template"
        def result2 = repoManager.addEntry(entry2, 'mjohnson')

        then: "the second addEntry should fail"
        result1.type == Result.Type.SUCCESS
        result2.type == Result.Type.ERROR
    }

    def "deleteSkeleton should delete all related entries"() {
        when: "adding two entries with the same id but different versions and an entry with a different id"
        def entry1 = defaultEntry()
        def result1 = repoManager.addEntry(entry1, 'jsmith')

        def entry2 = defaultEntry()
        entry2.version.minor = entry1.version.minor + 1
        def result2 = repoManager.addEntry(entry2, 'jsmith')

        def entry3 = defaultEntry()
        entry3.id = entry1.id + ".other"
        def result3 = repoManager.addEntry(entry3, 'jsmith')

        then: "three entries have been added"
        result1.type == Result.Type.SUCCESS
        result2.type == Result.Type.SUCCESS
        result3.type == Result.Type.SUCCESS

        when: "deleteing the skeleton having the id of the first two entries"
        repoManager.deleteSkeleton(entry1.id)

        then: "only the third entry remains"
        def skeletons = repoManager.getSkeletons()
        skeletons.size() == 1
        skeletons[0].id == entry3.id
    }

    def "should correctly handle owners"() {
        when: "adding an entry and three additional owners, where one of them already exists"
        def entry1 = defaultEntry()
        def res = repoManager.addEntry(entry1, 'jsmith')
        def res1 = repoManager.addOwner(entry1.id, 'mjohnson')
        def res2 = repoManager.addOwner(entry1.id, 'jsmith')
        def res3 = repoManager.addOwner(entry1.id, 'dbcooper')
        def owners = repoManager.getOwners(entry1.id)

        then: "all operations have been successful"
        res.type == Result.Type.SUCCESS
        res1.type == Result.Type.SUCCESS
        res2.type == Result.Type.SUCCESS
        res3.type == Result.Type.SUCCESS
        owners.type == Result.Type.SUCCESS

        and: "only the owners that no yet exists have been added"
        res1.value == 1
        res2.value == 0
        res3.value == 1

        and: "the skeleton has now 3 owners, with the previously provided names"
        owners.value.size() == 3
        owners.value as Set == ['jsmith', 'mjohnson', 'dbcooper'] as Set

        when: "deleting the owner with name 'mjohnson'"
        res = repoManager.deleteOwner(entry1.id, 'mjohnson')

        then: "the operation succeeds and only 2 owners remain"
        res.type == Result.Type.SUCCESS
        res.value == 1
        repoManager.getOwners(entry1.id).value.size() == 2

        when: "deleting again the owner with name 'mjohnson'"
        res = repoManager.deleteOwner(entry1.id, 'mjohnson')

        then: "the operation succeeds but there are still 2 owners"
        res.type == Result.Type.SUCCESS
        res.value == 0
        repoManager.getOwners(entry1.id).value.size() == 2
    }

    def "should correctly handle tags"() {
        when: "adding an entry and three additional tags, where one of them already exists"
        def entry1 = defaultEntry()
        entry1.tags = ['good', 'bad', 'ugly']
        def res = repoManager.addEntry(entry1, 'jsmith')
        def res1 = repoManager.addTag(entry1.id, 'big')
        def res2 = repoManager.addTag(entry1.id, 'bad')
        def res3 = repoManager.addTag(entry1.id, 'scary')
        def tags = repoManager.getTags(entry1.id)

        then: "all operations have been successful"
        res.type == Result.Type.SUCCESS
        res1.type == Result.Type.SUCCESS
        res2.type == Result.Type.SUCCESS
        res3.type == Result.Type.SUCCESS
        tags.type == Result.Type.SUCCESS

        and: "only the tgas that no yet exists have been added"
        res1.value == 1
        res2.value == 0
        res3.value == 1

        and: "the skeleton has now 5 tags, with the previously provided names"
        tags.value.size() == 5
        tags.value as Set == ['good', 'bad', 'ugly', 'big', 'scary'] as Set

        when: "deleting the 'ugly' tag"
        res = repoManager.deleteTag(entry1.id, 'ugly')

        then: "the operation succeeds and only 4 tags remain"
        res.type == Result.Type.SUCCESS
        res.value == 1
        repoManager.getTags(entry1.id).value.size() == 4

        when: "deleting again the 'ugly' tag"
        res = repoManager.deleteTag(entry1.id, 'ugly')

        then: "the operation succeeds but there are still 4 tags"
        res.type == Result.Type.SUCCESS
        res.value == 0
        repoManager.getTags(entry1.id).value.size() == 4
    }
}
