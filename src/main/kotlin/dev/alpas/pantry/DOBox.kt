package dev.alpas.pantry

import dev.alpas.Environment

open class DOBox(
    private val key: String,
    private val secret: String,
    private val region: String? = null,
    private val bucket: String? = null,
    private val url: String? = null,
    private val isPubliclyVisibility: Boolean = false
) : S3Box(key, secret, region, bucket, url, isPubliclyVisibility) {
    constructor(env: Environment) : this(
        key = env("DO_ACCESS_KEY_ID")!!,
        secret = env("DO_SECRET_ACCESS_KEY")!!,
        region = env("DO_DEFAULT_REGION"),
        bucket = env("DO_BUCKET"),
        url = env("DO_URL"),
        isPubliclyVisibility = env("DO_DEFAULT_VISIBILITY", false)
    )

    override fun resolvePath(path: String): String {
        if (url != null) {
            return "s3://$url/$path"
        }
        return "s3://${bucket}.${region}.digitaloceanspaces.com/$path"
    }
}
