package com.scanify.app.domain.repository

interface UriResolver {
    suspend fun resolveUri(uriString: String): Pair<String, String>?
}