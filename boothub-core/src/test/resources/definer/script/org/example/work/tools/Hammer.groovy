package org.example.work.tools

import groovy.transform.Canonical
import org.example.work.material.Material

@Canonical
class Hammer implements Tool {
    final String name = 'hammer'

    @Override
    void transform(Material material) {
        material.weight *= 0.95
        material.length *= 1.05
        material.width *= 1.8
    }
}
