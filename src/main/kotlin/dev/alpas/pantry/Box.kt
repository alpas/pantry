package dev.alpas.pantry

import org.apache.commons.vfs2.*
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset

abstract class Box {
    open fun exists(path: String, options: FileSystemOptions? = null): Boolean {
        return resolve(path, options).exists()
    }

    open fun get(path: String, charset: Charset = Charsets.UTF_8, options: FileSystemOptions? = null): String? {
        return readStream(path, options)?.let {
            return String(it.readAllBytes(), charset)
        }
    }

    open fun get(file: FileObject, charset: Charset = Charsets.UTF_8): String? {
        return readStream(file)?.let {
            return String(it.readAllBytes(), charset)
        }
    }

    open fun readStream(path: String, options: FileSystemOptions? = null): InputStream? {
        return resolve(path, options).content.inputStream
    }

    open fun readStream(file: FileObject): InputStream? {
        return file.content.inputStream
    }

    open fun put(
        path: String,
        contents: String,
        charset: Charset = Charsets.UTF_8,
        options: FileSystemOptions? = null
    ): URL {
        return resolve(path, options).let {
            put(it, contents, charset)
            it.url
        }
    }

    open fun put(path: String, stream: InputStream, options: FileSystemOptions? = null): URL {
        return resolve(path, options).let {
            stream.transferTo(it.content.outputStream)
            it.close()
            it.url
        }
    }

    open fun append(
        path: String,
        data: String,
        charset: Charset = Charsets.UTF_8,
        options: FileSystemOptions? = null
    ): URL {
        return resolve(path, options).let {
            val contents = if (it.exists()) combine(get(it), data) else data
            put(it, contents, charset)
            it.url
        }
    }

    open fun prepend(
        path: String,
        data: String,
        charset: Charset = Charsets.UTF_8,
        options: FileSystemOptions? = null
    ): URL {
        return resolve(path, options).let {
            it.content.getOutputStream(true).write(data.toByteArray(charset))
            it.close()
            it.url
        }
    }

    open fun touch(path: String, options: FileSystemOptions? = null): URL {
        return resolve(path, options).let {
            if (it.exists()) {
                it.content.lastModifiedTime = System.currentTimeMillis()
            } else {
                it.createFile()
            }
            it.close()
            it.url
        }
    }

    open fun resolve(path: String, options: FileSystemOptions? = null): FileObject {
        val manager = VFS.getManager()
        val fullPath = resolvePath(path)

        return options?.let {
            manager.resolveFile(fullPath, it)
        } ?: manager.resolveFile(fullPath)
    }

    open fun delete(path: String, vararg paths: String, options: FileSystemOptions? = null) {
        resolve(path, options).delete()
        paths.forEach {
            resolve(it, options).delete()
        }
    }

    open fun deleteFolder(path: String, options: FileSystemOptions? = null) {
        resolve(path, options).deleteAll()
    }

    open fun copy(
        from: String,
        to: String,
        fileSelector: FileSelector = AllFileSelector(),
        options: FileSystemOptions? = null
    ) {
        resolve(to, options).copyFrom(resolve(from, options), fileSelector)
    }

    open fun move(from: String, to: String, options: FileSystemOptions? = null) {
        resolve(from, options).moveTo(resolve(to, options))
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

    private fun put(file: FileObject, contents: String, charset: Charset = Charsets.UTF_8) {
        file.content.outputStream.write(contents.toByteArray(charset))
        file.close()
    }
}
