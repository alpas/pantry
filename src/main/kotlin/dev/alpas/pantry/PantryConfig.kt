package dev.alpas.pantry

import dev.alpas.Config
import dev.alpas.Environment

open class PantryConfig(private val env: Environment) : Config {
    private val defaultBoxName = env("PANTRY_DEFAULT_BOX", "local")
    private val boxes = mutableMapOf<String, Lazy<Box>>()

    init {
        addBox(defaultBoxName, lazy { FileBox(env.storagePath("app", "web")) })
    }

    fun addBox(name: String, box: Lazy<Box>) {
        boxes[name] = box
    }

    fun box(name: String? = null): Box {
        val boxName = name ?: defaultBoxName
        return boxes[boxName]?.value
            ?: throw IllegalArgumentException("Pantry box '$name' doesn't exist")
    }
}
