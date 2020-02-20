package dev.alpas.pantry

import dev.alpas.*
import dev.alpas.http.HttpCall

class PantryServiceProvider : ServiceProvider {
    override fun register(app: Application) {
        // Register the default PantryConfig if it is not already registered.
        app.bindIfMissing { PantryConfig(app.env) }
    }
}
