package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@Immutable
data class GitHubReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkAssetUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long
)

@Immutable
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val currentVersion: String) : UpdateUiState
    data class UpdateAvailable(val release: GitHubReleaseInfo) : UpdateUiState
    data class NeedsInstallPermission(val release: GitHubReleaseInfo) : UpdateUiState
    data class Downloading(
        val release: GitHubReleaseInfo,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateUiState
    data class ReadyToInstall(val release: GitHubReleaseInfo, val apkFile: File) : UpdateUiState
    data class Error(val message: String, val release: GitHubReleaseInfo? = null) : UpdateUiState
}

sealed interface UpdateCheckResult {
    data class Available(val release: GitHubReleaseInfo) : UpdateCheckResult
    data class AlreadyLatest(val currentVersion: String) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

object GitHubUpdater {
    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/HSG24/Focus-Lock/releases/latest"

    private const val PREFS_NAME = "github_updater_state"
    private const val KEY_PENDING_TAG = "pending_install_tag"
    private const val KEY_PENDING_PRE_UPDATE_TIME = "pending_pre_install_update_time"
    private const val KEY_CONFIRMED_TAG = "confirmed_installed_tag"
    private const val KEY_CONFIRMED_UPDATE_TIME = "confirmed_update_time"

    private val VERSION_TOKEN_REGEX = Regex("""\d+(?:\.\d+)*""")

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun readPackageInfo(context: Context): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        }.getOrNull()
    }

    /**
     * Resolves the currently running app version cleanly from PackageManager / BuildConfig,
     * and also reconciles an in-app update install whose PackageInfo.lastUpdateTime advanced
     * after the package installer was launched (protecting against unbumped APK metadata).
     */
    fun getInstalledVersion(context: Context): String {
        val pkgInfo = readPackageInfo(context)
        val rawPkgVersion = pkgInfo?.versionName?.trim().orEmpty().ifBlank { BuildConfig.VERSION_NAME }
        val normalizedPkgVersion = normalizeVersionString(rawPkgVersion)
        val lastUpdateTime = pkgInfo?.lastUpdateTime ?: 0L

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pendingTag = prefs.getString(KEY_PENDING_TAG, null)?.takeIf { it.isNotBlank() }
        val pendingPreTime = prefs.getLong(KEY_PENDING_PRE_UPDATE_TIME, -1L)

        if (pendingTag != null && lastUpdateTime > 0L && pendingPreTime in 1L until lastUpdateTime) {
            prefs.edit()
                .putString(KEY_CONFIRMED_TAG, pendingTag)
                .putLong(KEY_CONFIRMED_UPDATE_TIME, lastUpdateTime)
                .remove(KEY_PENDING_TAG)
                .remove(KEY_PENDING_PRE_UPDATE_TIME)
                .apply()
        }

        val confirmedTag = prefs.getString(KEY_CONFIRMED_TAG, null)?.takeIf { it.isNotBlank() }
        val confirmedTime = prefs.getLong(KEY_CONFIRMED_UPDATE_TIME, -1L)

        if (confirmedTag != null && lastUpdateTime > 0L) {
            if (confirmedTime == lastUpdateTime) {
                val normalizedConfirmed = normalizeVersionString(confirmedTag)
                return if (compareVersions(normalizedConfirmed, normalizedPkgVersion) > 0) {
                    normalizedConfirmed
                } else {
                    normalizedPkgVersion
                }
            } else if (lastUpdateTime > confirmedTime) {
                // Another manual APK install occurred outside our tracked update flow;
                // drop stale confirmed tag and trust the new APK's own PackageInfo.versionName.
                prefs.edit()
                    .remove(KEY_CONFIRMED_TAG)
                    .remove(KEY_CONFIRMED_UPDATE_TIME)
                    .apply()
            }
        }

        return normalizedPkgVersion
    }

    fun normalizeVersionString(raw: String): String {
        val trimmed = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
        if (trimmed.isBlank()) return "1.0"
        val match = VERSION_TOKEN_REGEX.find(trimmed)
        return match?.value ?: trimmed
    }

    suspend fun checkForUpdates(context: Context): UpdateCheckResult {
        val installedVersion = getInstalledVersion(context)
        return checkForUpdates(installedVersion)
    }

    suspend fun checkForUpdates(
        currentVersion: String = BuildConfig.VERSION_NAME
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val normalizedCurrent = normalizeVersionString(currentVersion)

        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "FocusLock-Android")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Failed("GitHub release check failed (${response.code})")
                }
                val body = response.body?.string()?.trim().orEmpty()
                if (body.isEmpty()) {
                    return@withContext UpdateCheckResult.Failed("Empty response from GitHub")
                }

                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "").trim()
                if (tagName.isBlank()) {
                    return@withContext UpdateCheckResult.AlreadyLatest(normalizedCurrent)
                }

                if (!isNewerVersion(remoteTag = tagName, localVersion = normalizedCurrent)) {
                    return@withContext UpdateCheckResult.AlreadyLatest(normalizedCurrent)
                }

                val releaseName = json.optString("name", "").trim().ifBlank { tagName }
                val releaseNotes = json.optString("body", "").trim()
                    .ifBlank { "Performance improvements, stability fixes, and polish." }

                val assets = json.optJSONArray("assets")
                var apkAssetUrl = ""
                var apkFileName = "update.apk"
                var apkSizeBytes = 0L

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        val contentType = asset.optString("content_type", "")
                        val browserUrl = asset.optString("browser_download_url", "").trim()
                        val fallbackUrl = asset.optString("url", "").trim()
                        val resolvedUrl = browserUrl.ifBlank { fallbackUrl }
                        if ((name.endsWith(".apk", ignoreCase = true) ||
                                contentType.equals("application/vnd.android.package-archive", ignoreCase = true)) &&
                            resolvedUrl.isNotBlank()
                        ) {
                            apkAssetUrl = resolvedUrl
                            apkFileName = name.ifBlank { "update.apk" }
                            apkSizeBytes = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (apkAssetUrl.isBlank()) {
                    return@withContext UpdateCheckResult.Failed("Release $tagName does not contain an APK asset.")
                }

                UpdateCheckResult.Available(
                    GitHubReleaseInfo(
                        tagName = tagName,
                        releaseName = releaseName,
                        releaseNotes = releaseNotes,
                        apkAssetUrl = apkAssetUrl,
                        apkFileName = apkFileName,
                        apkSizeBytes = apkSizeBytes
                    )
                )
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failed(e.message ?: "Unable to check for updates.")
        }
    }

    suspend fun downloadApk(
        context: Context,
        release: GitHubReleaseInfo,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(release.apkAssetUrl)
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "FocusLock-Android")
                .get()
                .build()

            val targetFile = File(context.cacheDir, "update.apk")
            if (targetFile.exists()) {
                targetFile.delete()
            }

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("APK download failed (${response.code})")
                }
                val body = response.body ?: error("Empty APK download stream")
                val reportedLength = body.contentLength()
                val totalBytes = when {
                    reportedLength > 0L -> reportedLength
                    release.apkSizeBytes > 0L -> release.apkSizeBytes
                    else -> -1L
                }

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0L
                        var lastReportedPct = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val progress = if (totalBytes > 0L) {
                                (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            val pct = (progress * 100).toInt()
                            if (pct != lastReportedPct || downloaded == totalBytes) {
                                lastReportedPct = pct
                                withContext(Dispatchers.Main) {
                                    onProgress(progress, downloaded, totalBytes)
                                }
                            }
                        }
                        output.flush()
                    }
                }
            }
            targetFile
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownAppSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun launchPackageInstaller(
        context: Context,
        apkFile: File,
        targetTag: String? = null
    ): Result<Unit> = runCatching {
        if (!targetTag.isNullOrBlank()) {
            val lastUpdateTime = readPackageInfo(context)?.lastUpdateTime ?: System.currentTimeMillis()
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PENDING_TAG, targetTag)
                .putLong(KEY_PENDING_PRE_UPDATE_TIME, lastUpdateTime)
                .apply()
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    internal fun parseVersionSegments(raw: String): List<Int> {
        val cleaned = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .trim()
        val token = VERSION_TOKEN_REGEX.find(cleaned)?.value ?: return listOf(0)
        return token.split(".").map { segment ->
            segment.toIntOrNull() ?: 0
        }
    }

    fun compareVersions(first: String, second: String): Int {
        val firstParts = parseVersionSegments(first)
        val secondParts = parseVersionSegments(second)
        val maxLen = maxOf(firstParts.size, secondParts.size)
        for (i in 0 until maxLen) {
            val a = firstParts.getOrElse(i) { 0 }
            val b = secondParts.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
        return compareVersions(remoteTag, localVersion) > 0
    }

    fun formatBytesReadable(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(java.util.Locale.US, "%.1f MB", mb)
        } else {
            val kb = bytes / 1024L
            "${kb} KB"
        }
    }
}
