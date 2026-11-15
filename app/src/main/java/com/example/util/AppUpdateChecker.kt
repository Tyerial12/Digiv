package com.example.util

import com.example.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class GameUpdateCheckResult(
    val isUpdateAvailable: Boolean,
    val isNewVersionFirstLaunch: Boolean = false,
    val installedVersionName: String,
    val installedVersionCode: Int,
    val latestVersionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val checkedSuccessfully: Boolean = true
)

object AppUpdateChecker {
    const val GITHUB_RELEASES_PAGE_URL = "https://github.com/Tyerial12/Digiv/releases/latest"
    private const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/Tyerial12/Digiv/releases/latest"
    private const val GITHUB_RAW_BUILD_GRADLE_URL = "https://raw.githubusercontent.com/Tyerial12/Digiv/main/app/build.gradle.kts"
    private const val GITHUB_RAW_README_URL = "https://raw.githubusercontent.com/Tyerial12/Digiv/main/README.md"

    suspend fun checkForUpdateOnLaunch(isFirstLaunchOfThisVersion: Boolean = false): GameUpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVersionName = BuildConfig.VERSION_NAME.removePrefix("v").trim()
        val currentVersionCode = BuildConfig.VERSION_CODE
        val currentNumeric = parseNumericVersion(currentVersionName, currentVersionCode)

        // 1. Check GitHub Latest Release API
        val releaseCheck = runCatching {
            fetchFromGitHubReleasesApi(currentVersionName, currentVersionCode, currentNumeric)
        }.getOrNull()
        if (releaseCheck != null && releaseCheck.isUpdateAvailable) {
            return@withContext releaseCheck
        }

        // 2. Check main branch app/build.gradle.kts
        val gradleCheck = runCatching {
            fetchFromRawBuildGradle(currentVersionName, currentVersionCode, currentNumeric)
        }.getOrNull()
        if (gradleCheck != null && gradleCheck.isUpdateAvailable) {
            return@withContext gradleCheck
        }

        // 3. Check main branch README.md version header
        val readmeCheck = runCatching {
            fetchFromRawReadme(currentVersionName, currentVersionCode, currentNumeric)
        }.getOrNull()
        if (readmeCheck != null && readmeCheck.isUpdateAvailable) {
            return@withContext readmeCheck
        }

        val baseResult = releaseCheck ?: gradleCheck ?: readmeCheck ?: GameUpdateCheckResult(
            isUpdateAvailable = false,
            installedVersionName = currentVersionName,
            installedVersionCode = currentVersionCode,
            latestVersionName = currentVersionName,
            releaseTitle = "Digi Verse v$currentVersionName",
            releaseNotes = "• Rookie-Only Starter Monster Selection enforced\n• Real-Player Chat & Trade (All NPCs removed)\n• Automatic Game Launch Update Alert system\n• 24h Offline Auto-Training (100 DigiCoins) & Monster Parts Salvage (2 / 5 DigiCoins) + 1 HP 5-Min Recovery",
            downloadUrl = GITHUB_RELEASES_PAGE_URL,
            checkedSuccessfully = false
        )

        baseResult.copy(isNewVersionFirstLaunch = isFirstLaunchOfThisVersion)
    }

    private fun fetchFromGitHubReleasesApi(
        currentVersionName: String,
        currentVersionCode: Int,
        currentNumeric: Double
    ): GameUpdateCheckResult? {
        val conn = (URL(GITHUB_LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6000
            readTimeout = 6000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "DigiVerse-Android-UpdateChecker/${BuildConfig.VERSION_NAME}")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            val bodyText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(bodyText)
            val rawTag = json.optString("tag_name", "").removePrefix("v").trim()
            if (rawTag.isBlank()) return null

            val releaseName = json.optString("name", "Digi Verse v$rawTag").ifBlank { "Digi Verse v$rawTag" }
            val releaseBody = json.optString("body", "").ifBlank {
                "A new update (v$rawTag) is available for Digi Verse with the latest features and improvements."
            }
            val htmlUrl = json.optString("html_url", GITHUB_RELEASES_PAGE_URL).ifBlank { GITHUB_RELEASES_PAGE_URL }

            var apkDownloadUrl = htmlUrl
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val browserUrl = asset.optString("browser_download_url", "")
                    if (browserUrl.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = browserUrl
                        break
                    }
                }
            }

            val remoteNumeric = parseNumericVersion(rawTag, 0)
            val updateAvailable = isRemoteVersionNewer(
                installedVersionName = currentVersionName,
                installedNumeric = currentNumeric,
                remoteVersionName = rawTag,
                remoteNumeric = remoteNumeric
            )

            GameUpdateCheckResult(
                isUpdateAvailable = updateAvailable,
                installedVersionName = currentVersionName,
                installedVersionCode = currentVersionCode,
                latestVersionName = if (updateAvailable) rawTag else currentVersionName,
                releaseTitle = releaseName,
                releaseNotes = releaseBody,
                downloadUrl = apkDownloadUrl,
                checkedSuccessfully = true
            )
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchFromRawBuildGradle(
        currentVersionName: String,
        currentVersionCode: Int,
        currentNumeric: Double
    ): GameUpdateCheckResult? {
        val conn = (URL(GITHUB_RAW_BUILD_GRADLE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6000
            readTimeout = 6000
            setRequestProperty("User-Agent", "DigiVerse-Android-UpdateChecker/${BuildConfig.VERSION_NAME}")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            val content = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val codeMatch = Regex("""versionCode\s*=\s*(\d+)""").find(content)?.groupValues?.getOrNull(1)?.toIntOrNull()
            val nameMatch = Regex("""versionName\s*=\s*"([^"]+)"""").find(content)?.groupValues?.getOrNull(1)?.removePrefix("v")?.trim()
            if (nameMatch.isNullOrBlank() && codeMatch == null) return null

            val remoteVersionName = nameMatch ?: "${codeMatch ?: currentVersionCode}.0"
            val remoteNumeric = parseNumericVersion(remoteVersionName, codeMatch ?: 0)
            val updateAvailable = (codeMatch != null && codeMatch > currentVersionCode) ||
                isRemoteVersionNewer(
                    installedVersionName = currentVersionName,
                    installedNumeric = currentNumeric,
                    remoteVersionName = remoteVersionName,
                    remoteNumeric = remoteNumeric
                )

            val directApkUrl = "https://github.com/Tyerial12/Digiv/releases/download/v$remoteVersionName/DigiVerse-v$remoteVersionName.apk"
            GameUpdateCheckResult(
                isUpdateAvailable = updateAvailable,
                installedVersionName = currentVersionName,
                installedVersionCode = currentVersionCode,
                latestVersionName = if (updateAvailable) remoteVersionName else currentVersionName,
                releaseTitle = "Digi Verse v$remoteVersionName Update",
                releaseNotes = "• Rookie-Only Starter Monster Selection enforced\n• Real-Player Chat & Trade (All NPCs removed)\n• Automatic Game Launch Update Alert system\n• 24h Offline Auto-Training (100 DigiCoins) & Monster Parts Salvage",
                downloadUrl = if (updateAvailable) directApkUrl else GITHUB_RELEASES_PAGE_URL,
                checkedSuccessfully = true
            )
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchFromRawReadme(
        currentVersionName: String,
        currentVersionCode: Int,
        currentNumeric: Double
    ): GameUpdateCheckResult? {
        val conn = (URL(GITHUB_RAW_README_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("User-Agent", "DigiVerse-Android-UpdateChecker/${BuildConfig.VERSION_NAME}")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            val content = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val versionMatch = Regex("""\(v([0-9]+(?:\.[0-9]+)?)\)""").find(content)?.groupValues?.getOrNull(1)?.trim()
                ?: return null
            val remoteNumeric = parseNumericVersion(versionMatch, 0)
            val updateAvailable = isRemoteVersionNewer(
                installedVersionName = currentVersionName,
                installedNumeric = currentNumeric,
                remoteVersionName = versionMatch,
                remoteNumeric = remoteNumeric
            )
            if (!updateAvailable) return null
            val directApkUrl = "https://github.com/Tyerial12/Digiv/releases/download/v$versionMatch/DigiVerse-v$versionMatch.apk"
            GameUpdateCheckResult(
                isUpdateAvailable = true,
                installedVersionName = currentVersionName,
                installedVersionCode = currentVersionCode,
                latestVersionName = versionMatch,
                releaseTitle = "Digi Verse v$versionMatch Update Available!",
                releaseNotes = "A newer version (v$versionMatch) was detected on GitHub! Download the latest APK release now.",
                downloadUrl = directApkUrl,
                checkedSuccessfully = true
            )
        } finally {
            conn.disconnect()
        }
    }

    fun parseNumericVersion(versionName: String, fallbackCode: Int): Double {
        val cleaned = versionName.removePrefix("v").trim()
        val parsed = cleaned.toDoubleOrNull()
        if (parsed != null) return parsed
        val parts = cleaned.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.isNotEmpty()) {
            val major = parts.getOrElse(0) { fallbackCode }
            val minor = parts.getOrElse(1) { 0 }
            return major + (minor / 100.0)
        }
        return fallbackCode.toDouble()
    }

    fun isRemoteVersionNewer(
        installedVersionName: String,
        installedNumeric: Double,
        remoteVersionName: String,
        remoteNumeric: Double
    ): Boolean {
        if (remoteVersionName.equals(installedVersionName, ignoreCase = true)) return false
        return remoteNumeric > installedNumeric + 0.0001
    }
}
