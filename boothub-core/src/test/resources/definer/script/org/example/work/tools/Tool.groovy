package org.example.work.tools

import org.example.work.material.Material

interface Tool {
    String getName()

    void transform(Material material)
}
