package dev.alpas.pantry

import org.apache.commons.vfs2.*
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset

/**
 * An abstract class that represents a storage box for saving files and directories.
 * If this box is public, files and folders will be marked as public when saving
 * them. it is upto a concrete implementor to decide what public means.
 *
 * @param isPubliclyVisible Whether the box's contents are publicly accessible or not.
 * @param options FilesOptions to be applied to each box operations.
 */
abstract class Box(
    val isPubliclyVisible: Boolean = false,
    private val options: FileSystemOptions = FileSystemOptions()
) {

    /**
     * Check whether a file object at the given path exists or not.
     *
     * @param path The path of the file/folder to check for existence.
     *
     * @return True if a file object exists false if it doesn't.
     */
    open fun exists(path: String): Boolean {
        return resolve(path).exists()
    }

    /**
     * Read the contents of a file at the given path using the provided charset.
     *
     * @param path The path of file.
     * @param charset The charset to use while converting the file input bytes to a string.
     *
     * @return The content of a file.
     */
    open fun get(path: String, charset: Charset = Charsets.UTF_8): String? {
        return readStream(path)?.let {
            return String(it.readAllBytes(), charset)
        }
    }

    /**
     * Read the contents of a [FileObject] using the provided charset.
     *
     * @param file The [FileObject] to read contents from.
     * @param charset The charset to use while converting the file input bytes to a string.
     *
     * @return The content of a file.
     */
    open fun get(file: FileObject, charset: Charset = Charsets.UTF_8): String? {
        return readStream(file)?.let {
            return String(it.readAllBytes(), charset)
        }
    }

    /**
     * Read the file as a stream at the given path and return its input stream.
     *
     * @param path The path of the file to read.
     *
     * @return The contents of the file as an input stream.
     */
    open fun readStream(path: String): InputStream? {
        return resolve(path).content.inputStream
    }

    /**
     * Read the file as a stream and return its input stream.
     *
     * @param file The file object to read.
     *
     * @return The contents of the file as an input stream.
     */
    open fun readStream(file: FileObject): InputStream? {
        return file.content.inputStream
    }

    /**
     * Write the [contents] at [path] with the given [charset] and returns the location of the file.
     * If this box is public, the file created is marked as public as well.
     *
     * @param path The destination of the file.
     * @param contents The contents of the file.
     * @param charset The charset to use for writing. [Charsets.UTF_8] by default.
     *
     * @return The URI location of the file.
     */
    open fun put(path: String, contents: String, charset: Charset = Charsets.UTF_8): URI {
        val uri = resolve(path).use {
            write(it, contents, charset)
            it.url
        }.toURI()

        if (isPubliclyVisible) {
            makePublic(uri)
        }
        return uri
    }

    /**
     * Write the [stream] at [path] and returns the location of the file.
     * If this box is public, the file created is marked as public as well.
     *
     * @param path The destination of the file.
     * @param stream The input stream that will be copied to the destination path.
     *
     * @return The URI location of the file.
     */
    open fun put(path: String, stream: InputStream): URI {
        val uri = resolve(path).use {
            stream.transferTo(it.content.outputStream)
            it.url.toURI()
        }
        if (isPubliclyVisible) {
            makePublic(uri)
        }
        return uri
    }

    /**
     * Append [data] to a file at the [path] location using [charset].
     *
     * @param path The path of the file.
     * @param data The data to be appended.
     * @param charset The charset to use for writing. [Charsets.UTF_8] by default.
     *
     * @return The URI location of the file.
     */
    open fun append(
        path: String,
        data: String,
        charset: Charset = Charsets.UTF_8
    ): URI {
        return resolve(path).use {
            val contents = if (it.exists()) combine(get(it), data) else data
            write(it, contents, charset)
            it.url.toURI()
        }
    }

    /**
     * Prepend [data] to a file at the [path] location using [charset].
     *
     * @param path The path of the file.
     * @param data The data to be prepended.
     * @param charset The charset to use for writing. [Charsets.UTF_8] by default.
     *
     * @return The URI location of the file.
     */
    open fun prepend(
        path: String,
        data: String,
        charset: Charset = Charsets.UTF_8
    ): URI {
        return resolve(path).use {
            it.content.getOutputStream(true).write(data.toByteArray(charset))
            it.url.toURI()
        }
    }

    /**
     * If a file exists at the [path] then updated its last modification time to [System.currentTimeMillis()].
     * If the file doesn't exist then create it. Either way, return the location of the file.
     *
     * @param path The path of the file.
     *
     * @return The URI location of the file.
     */
    open fun touch(path: String): URI {
        return resolve(path).use {
            if (it.exists()) {
                it.content.lastModifiedTime = System.currentTimeMillis()
            } else {
                it.createFile()
            }
            it.url.toURI()
        }
    }

    /**
     * Delete files at the provided paths.
     */
    open fun delete(path: String, vararg paths: String) {
        resolve(path).delete()
        paths.forEach {
            resolve(it).delete()
        }
    }

    /**
     * Delete a folder and all its contents at [path].
     */
    open fun deleteFolder(path: String) {
        resolve(path).deleteAll()
    }

    /**
     * Copy files from one location to another location within this box.
     * By default all the files will be copied
     *
     * @param from The source path
     * @param to The destination path
     * @param fileSelector [FileSelector] to use for copying. ALl files are copied by default.
     *
     */
    open fun copy(
        from: String,
        to: String,
        fileSelector: FileSelector = AllFileSelector()
    ) {
        resolve(to).copyFrom(resolve(from), fileSelector)
    }

    /**
     * Move files from one location to another location within this box.
     *
     * @param from The source path
     * @param to The destination path
     *
     */
    open fun move(from: String, to: String) {
        resolve(from).moveTo(resolve(to))
    }

    /**
     * Resolve a file at [path] using the given [FileSystemOptions].
     *
     * @param path The partial path to be resolved.
     * @param options The options to be used while resolving a file.
     *
     * @return A file object representing a file at the given path.
     */
    open fun resolve(path: String, options: FileSystemOptions): FileObject {
        val fullPath = resolvePath(path)
        return VFS.getManager().resolveFile(fullPath, options)
    }

    /**
     * Resolve a file at [path].
     *
     * @param path The path to be resolved.
     *
     * @return A file object representing a file at the given path.
     */
    open fun resolve(path: String): FileObject {
        return resolve(path, options)
    }

    /**
     * Resolve the complete box-dependent path for the given partial path.
     * For an example, a partial path "screenshots/thumbnails/1.png" could be resolved to
     * "file:///Users/janedoe/project/screenshots/thumbnails/1.png" by a local file
     * box whose base path is set to "/Users/janedoe/project/"
     *
     * @param path The "partial" path in this box.
     *
     * @return The full path to the given path.
     */
    abstract fun resolvePath(path: String): String

    private fun combine(pre: String?, post: String?): String {
        return when {
            pre == null && post == null -> ""
            pre == null -> post
            post == null -> pre
            else -> "${pre}${System.lineSeparator()}${post}"
        } ?: ""
    }

    private fun write(file: FileObject, contents: String, charset: Charset = Charsets.UTF_8) {
        file.content.outputStream.write(contents.toByteArray(charset))
        file.close()
    }

    /**
     * Make a file object at the given [uri] public.
     *
     * @param uri The location of the file to be marked as "public".
     */
    open fun makePublic(uri: URI) {}
}


/**
 * A class that represents a movable file object for further chaining.
 */
data class MovableFileObject(val fileToMove: FileObject) {
    /**
     * Move this file to the destination box [destBox] at the given destination [destPath]. If
     * the destination box is public, the moved file will be marked as public too.
     *
     * @param destBox Destination box where this file will be moved to.
     * @param destPath Destination path where this file will be moved to.
     *
     * @return The location of the destination file.
     */
    fun toBox(destBox: Box, destPath: String): URI {
        val uri = destBox.resolve(destPath).use {
            fileToMove.moveTo(it)
            fileToMove.deleteAll()
            it.url.toURI()
        }
        if (destBox.isPubliclyVisible) {
            destBox.makePublic(uri)
        }
        return uri
    }

    /**
     * Move this file to the destination box [destBox] at the given destination [destPath]
     * and mark the moved file as public at the destination.
     *
     * @param destBox Destination box where this file will be moved to.
     * @param destPath Destination path where this file will be moved to.
     *
     * @return The location of the destination file.
     */
    fun asPubliclyToBox(destBox: Box, destPath: String): URI {
        val uri = toBox(destBox, destPath)
        // If the destBox wasn't public, we need to mark this file as public manually.
        if (!destBox.isPubliclyVisible) {
            destBox.makePublic(uri)
        }
        return uri
    }
}

/**
 * Makes a file at [srcPath] a [MovableFileObject] for further chaining.
 *
 * @param srcPath The source path.
 *
 * @return A movable file object.
 */
fun Box.move(srcPath: String): MovableFileObject {
    return MovableFileObject(resolve(srcPath))
}
