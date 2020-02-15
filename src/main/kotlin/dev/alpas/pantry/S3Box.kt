package dev.alpas.pantry

import com.amazonaws.auth.AWSCredentialsProvider
import com.github.vfss3.S3FileObject
import com.github.vfss3.S3FileSystemConfigBuilder
import com.github.vfss3.operations.Acl
import com.github.vfss3.shaded.com.amazonaws.auth.BasicAWSCredentials
import dev.alpas.Environment
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemOptions
import java.net.URL

open class S3Box(
    private val key: String,
    private val secret: String,
    private val region: String? = null,
    private val bucket: String? = null,
    private val url: String? = null,
    private val isPubliclyVisibility: Boolean = false
) : Box(isPubliclyVisibility, FileSystemOptions()) {

    private val credentialsProvider by lazy {
        object : AWSCredentialsProvider {
            override fun getCredentials() = BasicAWSCredentials(key, secret)
            override fun refresh() {}
        }
    }

    constructor(env: Environment) : this(
        key = env("AWS_ACCESS_KEY_ID")!!,
        secret = env("AWS_SECRET_ACCESS_KEY")!!,
        region = env("AWS_DEFAULT_REGION"),
        bucket = env("AWS_BUCKET"),
        url = env("AWS_URL"),
        isPubliclyVisibility = env("AWS_DEFAULT_VISIBILITY", false)
    )

    constructor(key: String, secret: String, url: String) : this(
        key = key,
        secret = secret,
        region = null,
        bucket = null,
        url = url
    )

    override fun resolvePath(path: String): String {
        return if (url == null) {
            "s3://s3.${region}.amazonaws.com/$bucket/$path"
        } else {
            "s3://$url/$path"
        }
    }

    override fun resolve(path: String, options: FileSystemOptions): FileObject {
        S3FileSystemConfigBuilder.getInstance().setCredentialsProvider(options, credentialsProvider)
        return super.resolve(path, options)
    }

    override fun makePublic(url: URL) {
        val fileObject = resolve(url.toURI().path, FileSystemOptions())
        (fileObject as S3FileObject).acl = Acl().apply {
            allow(
                Acl.Group.EVERYONE,
                Acl.Permission.READ
            )
        }
    }
}
