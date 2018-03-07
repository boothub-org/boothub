package org.example.work

import org.example.work.material.Material
import org.example.work.tools.Hammer
import org.mylib.util.MyUtil

class MyTest {
    static Material transformMaterial() {
        Person p1 = new Person('Jane', 'Smith', 33)
        Person p2 = new Person('Jim', 'Brown', 24)
        Company comp = new Company(name: 'Acme Corporation')
        comp.employees << p1
        comp.employees << p2
        Hammer hammer = new Hammer()
        comp.tools << hammer
        String materialName = MyUtil.alternateCase('my big iron plate')
        Material material = new Material(name: materialName, length: 10, width: 2, weight: 4)
        comp.useAllToolsOn(material)
        material
    }
}
