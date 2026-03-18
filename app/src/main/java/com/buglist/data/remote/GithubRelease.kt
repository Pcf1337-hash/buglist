package com.buglist.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String,
    @SerialName("body") val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("assets") val assets: List<GithubAsset> = emptyList(),
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("draft") val draft: Boolean = false
)

@Serializable
data class GithubAsset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("size") val size: Long
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class UpdateAvailable(
        val currentVersion: String,
        val newVersion: String,
        val releaseNotes: String?,
        val downloadUrl: String,
        val releasePageUrl: String
    ) : UpdateState()
    object NoConnection : UpdateState()
    data class Error(val message: String) : UpdateState()
}
