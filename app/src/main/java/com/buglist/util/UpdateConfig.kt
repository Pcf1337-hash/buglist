package com.buglist.util

object UpdateConfig {
    const val GITHUB_OWNER = "Pcf1337-hash"
    const val GITHUB_REPO = "buglist"
    const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    // Public repo → kein Token nötig
    const val GITHUB_TOKEN = ""

    // Max 1x pro 24h checken (GitHub Rate Limit schonen)
    const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

    const val PREF_LAST_UPDATE_CHECK = "last_update_check"
    const val PREF_SKIPPED_VERSION = "skipped_version"
}
