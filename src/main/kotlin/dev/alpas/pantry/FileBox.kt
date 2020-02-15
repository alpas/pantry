package dev.alpas.pantry

import java.nio.file.Paths

class FileBox(private val basePath: String) : Box() {
    override fun resolvePath(path: String): String {
        return "file://${Paths.get(basePath, path).toAbsolutePath()}"
    }
}
