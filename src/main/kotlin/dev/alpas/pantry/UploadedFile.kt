package dev.alpas.pantry

import dev.alpas.secureRandomString
import org.apache.tika.Tika
import java.io.InputStream
import java.net.URL

data class UploadedFile(
    val contentStream: InputStream,
    val contentType: String,
    val filename: String,
    val size: Long,
    val config: PantryConfig
) {
    val guessedMime by lazy {
        Tika().detect(contentStream)
    }

    val guessedExtensions by lazy {
        guessedMime.substringAfterLast("/")
    }

    fun store(path: String, filename: String? = null): URL {
        return storeIn(null, path, filename)
    }

    fun storeIn(boxName: String?, path: String, filename: String? = null): URL {
        val name = filename ?: secureRandomString(32)
        return config.box(boxName).put("$path/$name.$guessedExtensions", contentStream)
    }
}
