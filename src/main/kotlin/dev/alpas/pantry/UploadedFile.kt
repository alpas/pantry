package dev.alpas.pantry

import dev.alpas.secureRandomString
import org.apache.tika.Tika
import java.io.InputStream
import java.net.URI

/**
 * A class that represents an uploaded file, which is ready to be stored in a Pantry [Box].
 *
 * @param contentStream The input stream of the file.
 * @param contentType The content type of the file.
 * @param filename The name of this file.
 * @param size The size of this file.
 * @param config A [PantryConfig] for storing it in a proper Pantry [Box].
 */
data class UploadedFile(
    val contentStream: InputStream,
    val contentType: String,
    val filename: String,
    val size: Long,
    val config: PantryConfig
) {
    /**
     * The mime of this file as detected by the [Tika] library.
     */
    val detectedMime by lazy {
        Tika().detect(contentStream)
    }

    /**
     * The guessed extension of this file based on its mime. The extension is derived from its mime after slashing
     * everything before the last /. For an example, if the mime is "image/png" the extension becomes "png".
     */
    val guessedExtensions by lazy {
        detectedMime.substringAfterLast("/")
    }

    /**
     * Store a file at [path] as [filename]. If the name is
     * left out or null, a secure random name will be generated.
     *
     * @param path The destination to store the file.
     * @param filename The name to be used for storing. If left out, a random name will be generated.
     *
     * @return The destination of the file stored.
     */
    fun store(path: String, filename: String? = null): URI {
        return storeIn(null, path, filename)
    }

    /**
     * Store a file publicly at [path] as [filename]. If the name is
     * left out or null, a secure random name will be generated.
     *
     * @param path The destination to store the file.
     * @param filename The name to be used for storing. If left out, a random name will be generated.
     *
     * @return The destination of the file stored.
     */
    fun storePublicly(path: String, filename: String? = null): URI {
        return storePubliclyIn(null, path, filename)
    }

    /**
     * Store a file at [path] in the given [boxName] as [filename]. The box is fetched from [config].
     * If the box name is null, the default box will be used. If the name is left out or null,
     * a secure random name will be generated.
     *
     * @param boxName The name of the box for storing the file.
     * @param path The destination to store the file.
     * @param filename The name to be used for storing. If left out, a random name will be generated.
     * @param withoutGuessedExtension If the file should be saved without the guessed extension. Default is false
     *
     * @return The destination of the file stored.
     */
    fun storeIn(boxName: String?, path: String = "", filename: String? = null, withoutGuessedExtension: Boolean = false): URI {
        val name = filename ?: secureRandomString(32)
        val extension = if(withoutGuessedExtension) "" else ".$guessedExtensions"
        return config.box(boxName).put("$path/$name$extension", contentStream)
    }

    /**
     * Store a file publicly at [path] in the given [boxName] as [filename]. The box is fetched
     * from [config]. If the box name is null, the default box will be used. If the name is
     * left out or null, a secure random name will be generated.
     *
     * @param boxName The name of the box for storing the file.
     * @param path The destination to store the file.
     * @param filename The name to be used for storing. If left out, a random name will be generated.
     *
     * @return The destination of the file stored.
     */
    fun storePubliclyIn(boxName: String?, path: String, filename: String? = null): URI {
        return storeIn(boxName, path, filename).let {
            val box = config.box(boxName)
            // If the box is publicly visible, this file should have been made public already
            if (!box.isPubliclyVisible) {
                box.makePublic(it)
            }
            it
        }
    }

    /**
     * Store a file from [fromPath] at [fromBoxName] to [toPath] at [toBoxName].
     *
     * @param fromBoxName The source box name.
     * @param toBoxName The destination box name.
     * @param fromPath The source path.
     * @param toPath The destination path.
     *
     * @return The location of the destination file.
     */
    fun move(fromBoxName: String, toBoxName: String, fromPath: String, toPath: String): URI {
        val fromFile = config.box(fromBoxName).resolve("$fromPath/$filename")
        val toBox = config.box(toBoxName)
        return toBox.put("$toPath/$filename", fromFile.content.inputStream)
    }
}
