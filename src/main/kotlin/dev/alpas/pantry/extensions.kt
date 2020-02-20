package dev.alpas.pantry

import dev.alpas.Container
import dev.alpas.http.HttpCall
import dev.alpas.make
import java.net.URI

/**
 * Fetch a pantry box of the given [name]. If the [name] is null, the default box will be returned.
 *
 * @param name The name of the box.
 *
 * @return A pantry box of [name].
 */
fun Container.pantry(name: String? = null): Box {
    return make<PantryConfig>().box(name)
}

/**
 * Return the first file of the given [name] if this call has multipart form data otherwise return null.
 *
 * @param name The name of the file.
 *
 * @return An [UploadedFile] if this call has multipart form data otherwise null.
 */
fun HttpCall.file(name: String): UploadedFile? {
    return if (isMultipartFormData) files(name).first() else null
}

/**
 * Return all the files of the given [name] if this call has multipart form data otherwise return an empty list.
 *
 * @param name The name of the file.
 *
 * @return A list of [UploadedFile] if this call has multipart form data otherwise an empty list.
 */
fun HttpCall.files(name: String): List<UploadedFile> {
    return multipartFiles.filter { it.name == name }.map {
        UploadedFile(it.inputStream, it.contentType, it.submittedFileName, it.size, make())
    }
}

/**
 * Store all the uploaded files at the [path] in the default box using [nameCallback] to determine the name of each
 * file. If [nameCallback] is null, a random name will be generated for each uploaded files while storing.
 *
 * @param path The path where all the files will be saved.
 * @param nameCallback The callback to use for determining the name of each uploaded file.
 *
 * @return A list of the locations of all the uploaded file in a pantry box.
 */
fun List<UploadedFile>.storeAll(path: String, nameCallback: (() -> String?)? = null): List<URI> {
    return storeIn(null, path, nameCallback)
}

/**
 * Store all the uploaded files publicly at [path] in the default box using [nameCallback] to determine the name of
 * each file. If [nameCallback] is null, a random name will be generated for each uploaded files while storing.
 *
 * @param path The path where all the files will be saved publicly.
 * @param nameCallback The callback to use for determining the name of each uploaded file.
 *
 * @return A list of the locations of all the uploaded file in a pantry box.
 */
fun List<UploadedFile>.storeAllPublicly(path: String, nameCallback: (() -> String?)? = null): List<URI> {
    return storePubliclyIn(null, path, nameCallback)
}

/**
 * Store all the uploaded files publicly at the given [path] in pantry box of [boxName]
 * using [nameCallback] to determine the name of each file. If [nameCallback] is null,
 * a random name will be generated for each uploaded files while storing.
 *
 * @param boxName The name of the pantry box to store the files.
 * @param path The path where all the files will be saved.
 * @param nameCallback The callback to use for determining the name of each uploaded file.
 *
 * @return A list of the locations of all the uploaded file in a pantry box.
 */
fun List<UploadedFile>.storeIn(boxName: String?, path: String, nameCallback: (() -> String?)? = null): List<URI> {
    return map {
        it.storeIn(boxName, path, nameCallback?.invoke())
    }
}

/**
 * Store all the uploaded files publicly at the given [path] in pantry box of [boxName]
 * [nameCallback] to determine the name of each file. If [nameCallback] is null,
 * a random name will be generated for each uploaded files while storing.
 *
 * @param path The path where all the files will be saved publicly.
 * @param nameCallback The callback to use for determining the name of each uploaded file.
 *
 * @return A list of the locations of all the uploaded file in a pantry box.
 */
fun List<UploadedFile>.storePubliclyIn(
    boxName: String?,
    path: String,
    nameCallback: (() -> String?)? = null
): List<URI> {
    return map {
        it.storePubliclyIn(boxName, path, nameCallback?.invoke())
    }
}
