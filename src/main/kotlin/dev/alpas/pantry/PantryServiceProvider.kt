package dev.alpas.pantry

import dev.alpas.*
import dev.alpas.http.HttpCall

class PantryServiceProvider : ServiceProvider {
    override fun register(app: Application) {
        // Register the default PantryConfig if it is not already registered.
        app.bindIfMissing { PantryConfig(app.env) }
    }
}

fun Container.pantry(name: String? = null): Box {
    return make<PantryConfig>().box(name)
}

fun HttpCall.file(name: String): UploadedFile? {
    return if (isMultipartFormData) {
        files(name).first()
    } else {
        null
    }
}

fun HttpCall.files(name: String): List<UploadedFile> {
    return multipartFiles.filter { it.name == name }.map {
        UploadedFile(it.inputStream, it.contentType, it.submittedFileName, it.size, make())
    }
}
