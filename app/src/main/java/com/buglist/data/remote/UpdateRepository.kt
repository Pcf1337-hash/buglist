package com.buglist.data.remote

import com.buglist.util.UpdateConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor() {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        engine {
            connectTimeout = 10_000
            socketTimeout = 10_000
        }
    }

    suspend fun fetchLatestRelease(): Result<GithubRelease> = runCatching {
        val response: GithubRelease = client.get(UpdateConfig.API_URL) {
            headers {
                append("Accept", "application/vnd.github.v3+json")
                append("User-Agent", "BugList-Android")
                if (UpdateConfig.GITHUB_TOKEN.isNotEmpty()) {
                    append("Authorization", "Bearer ${UpdateConfig.GITHUB_TOKEN}")
                }
            }
        }.body()

        if (response.draft || response.prerelease) {
            throw IllegalStateException("Latest release is draft or prerelease")
        }
        response
    }

    fun normalizeVersion(tag: String): String = tag.trimStart('v', 'V')

    fun isNewerVersion(localVersion: String, remoteVersion: String): Boolean {
        val local = normalizeVersion(localVersion).split(".").map { it.toIntOrNull() ?: 0 }
        val remote = normalizeVersion(remoteVersion).split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(local.size, remote.size)
        for (i in 0 until maxLen) {
            val l = local.getOrElse(i) { 0 }
            val r = remote.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    fun findApkAsset(release: GithubRelease): GithubAsset? =
        release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
}
