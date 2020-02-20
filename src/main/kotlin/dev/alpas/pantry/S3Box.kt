package dev.alpas.pantry

import com.amazonaws.auth.AWSCredentialsProvider
import com.github.vfss3.S3FileObject
import com.github.vfss3.S3FileSystemConfigBuilder
import com.github.vfss3.operations.Acl
import com.github.vfss3.shaded.com.amazonaws.auth.BasicAWSCredentials
import dev.alpas.Environment
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemOptions
import java.net.URI

/**
 * A box that points to an S3 bucket. If [url] is set, it overrides [region] and [bucket].
 *
 * @param key The API Key
 * @param secret The API secret
 * @param region The region to be used. Set to null by default.
 * @param bucket The bucket to be used. Set to null by default.
 * @param url The url to be used. Set to null by default. If set, it overrides other values.
 * @param isPubliclyVisibility Whether this box's contents should be public or not.
 */
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

    /**
     * Construct an S3 box using values from [env].
     */
    constructor(env: Environment) : this(
        key = env("AWS_ACCESS_KEY_ID")!!,
        secret = env("AWS_SECRET_ACCESS_KEY")!!,
        region = env("AWS_DEFAULT_REGION"),
        bucket = env("AWS_BUCKET"),
        url = env("AWS_URL"),
        isPubliclyVisibility = env("AWS_DEFAULT_VISIBILITY", false)
    )

    /**
     * Construct an S3 box using [key], [secret], the [url].
     */
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

    /**
     * Make a file object at the given URL public using an ACL.
     * The file is applied an [Acl.Permission.READ] permission for [Acl.Group.EVERYONE].
     */
    override fun makePublic(uri: URI) {
        val fileObject = resolve(uri.path, FileSystemOptions())
        (fileObject as S3FileObject).acl = Acl().apply {
            allow(
                Acl.Group.EVERYONE,
                Acl.Permission.READ
            )
        }
    }
}
