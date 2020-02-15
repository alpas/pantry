package dev.alpas.pantry

import dev.alpas.Environment
import java.nio.file.Paths

class FileBox(private val basePath: String) : Box() {
    constructor(env: Environment) : this(env.storagePath("app", "web"))
    override fun resolvePath(path: String): String {
        return "file://${Paths.get(basePath, path).toAbsolutePath()}"
    }
}
