package dev.alpas.pantry

import org.apache.commons.vfs2.*
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset


abstract class Box(
    val isPubliclyVisible: Boolean = false,
    private val options: FileSystemOptions = FileSystemOptions()
) {
    open fun exists(path: String): Boolean {
        return resolve(path).exists()
    }

    open fun get(path: String, charset: Charset = Charsets.UTF_8): String? {
        return readStream(path)?.let {
            return String(it.readAllBytes(), charset)
        }
    }

    open fun get(file: FileObject, charset: Charset = Charsets.UTF_8): String? {
        return readStream(file)?.let {
            return String(it.readAllBytes(), charset)
        }
    }

    open fun readStream(path: String): InputStream? {
        return resolve(path).content.inputStream
    }

    open fun readStream(file: FileObject): InputStream? {
        return file.content.inputStream
    }

    open fun put(
        path: String,
        contents: String,
        charset: Charset = Charsets.UTF_8
    ): URL {
        val url = resolve(path).use {
            write(it, contents, charset)
            it.url
        }

        if (isPubliclyVisible) {
            makePublic(url)
        }
        return url
    }

    open fun put(path: String, stream: InputStream): URL {
        val url = resolve(path).use {
            stream.transferTo(it.content.outputStream)
            it.url
        }
        if (isPubliclyVisible) {
            makePublic(url)
        }
        return url
    }

    open fun append(
        path: String,
        data: String,
        charset: Charset = Charsets.UTF_8
    ): URL {
        return resolve(path).use {
            val contents = if (it.exists()) combine(get(it), data) else data
            write(it, contents, charset)
            it.url
        }
    }

    open fun prepend(
        path: String,
        data: String,
        charset: Charset = Charsets.UTF_8
    ): URL {
        return resolve(path).use {
            it.content.getOutputStream(true).write(data.toByteArray(charset))
            it.url
        }
    }

    open fun touch(path: String): URL {
        return resolve(path).use {
            if (it.exists()) {
                it.content.lastModifiedTime = System.currentTimeMillis()
            } else {
                it.createFile()
            }
            it.url
        }
    }

    open fun delete(path: String, vararg paths: String) {
        resolve(path).delete()
        paths.forEach {
            resolve(it).delete()
        }
    }

    open fun deleteFolder(path: String) {
        resolve(path).deleteAll()
    }

    open fun copy(
        from: String,
        to: String,
        fileSelector: FileSelector = AllFileSelector()
    ) {
        resolve(to).copyFrom(resolve(from), fileSelector)
    }

    open fun move(from: String, to: String) {
        resolve(from).moveTo(resolve(to))
    }

    open fun resolve(path: String, options: FileSystemOptions): FileObject {
        val fullPath = resolvePath(path)
        return VFS.getManager().resolveFile(fullPath, options)
    }

    open fun resolve(path: String): FileObject {
        return resolve(path, options)
    }

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

    open fun makePublic(url: URL) {}
}
