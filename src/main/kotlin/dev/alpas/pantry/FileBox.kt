package dev.alpas.pantry

import dev.alpas.Environment
import java.nio.file.Paths

/**
 * A box that represents a local file at the given [basePath].
 */
class FileBox(private val basePath: String) : Box() {
    /**
     * Construct a file box pointing to storage/app/web folder.
     */
    constructor(env: Environment) : this(env.storagePath("app", "web"))

    override fun resolvePath(path: String): String {
        return "file://${Paths.get(basePath, path).toAbsolutePath()}"
    }
}
