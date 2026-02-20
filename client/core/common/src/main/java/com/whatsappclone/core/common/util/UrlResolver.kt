package com.whatsappclone.core.common.util

import java.net.URI

/**
 * Resolves relative media/avatar URLs into absolute URLs using the
 * configured server origin. Thread-safe via volatile field.
 */
object UrlResolver {

    @Volatile
    var serverOrigin: String = ""

    /**
     * Initialise from a full base URL like `https://host.example/api/v1/`.
     * Extracts and stores the scheme + authority (origin) portion.
     */
    fun init(baseUrl: String) {
        serverOrigin = try {
            val uri = URI(baseUrl)
            "${uri.scheme}://${uri.authority}"
        } catch (_: Exception) {
            baseUrl.trimEnd('/')
        }
    }

    /**
     * Returns an absolute URL. Already-absolute URLs pass through unchanged.
     * Relative paths (starting with `/`) are prepended with the server origin.
     */
    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        if (serverOrigin.isBlank()) return url
        val path = if (url.startsWith("/")) url else "/$url"
        return "${serverOrigin}$path"
    }
}
