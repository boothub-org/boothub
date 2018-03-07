package org.example.work

import groovy.transform.Canonical
import org.example.work.material.Material
import org.example.work.tools.Tool

@Canonical
class Company {
    String name
    List<Person> employees = []
    List<Tool> tools = []

    void useAllToolsOn(Material material) {
        println "Material before: $material"
        tools.each { t -> t.transform(material) }
        println "Material after: $material"
    }
}
