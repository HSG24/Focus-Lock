package com.example

import android.content.Context
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import com.example.data.GitHubUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GitHubUpdaterTest {

    @Test
    fun isNewerVersion_handlesSemanticTagsAndPrefixes() {
        assertTrue(GitHubUpdater.isNewerVersion("v1.0.15", "1.0"))
        assertTrue(GitHubUpdater.isNewerVersion("v1.1.0", "1.0.9"))
        assertTrue(GitHubUpdater.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(GitHubUpdater.isNewerVersion("V1.0.1", "v1.0.0"))
        assertTrue(GitHubUpdater.isNewerVersion("release-v1.0.16", "1.0.15"))

        assertFalse(GitHubUpdater.isNewerVersion("v1.0.15", "1.0.15"))
        assertFalse(GitHubUpdater.isNewerVersion("1.0.15", "v1.0.15"))
        assertFalse(GitHubUpdater.isNewerVersion("v1.0.0", "1.0"))
        assertFalse(GitHubUpdater.isNewerVersion("v1.0", "1.0.0"))
        assertFalse(GitHubUpdater.isNewerVersion("v0.9.9", "1.0"))
        assertFalse(GitHubUpdater.isNewerVersion("v1.0.14", "1.0.15"))
        assertFalse(GitHubUpdater.isNewerVersion("v1.0.15-release", "1.0.15"))
    }

    @Test
    fun parseTokenPayload_handlesQuotedJsonStringAndObject() {
        assertEquals("ghp_abc123", GitHubUpdater.parseTokenPayload("\"ghp_abc123\""))
        assertEquals("ghp_xyz789", GitHubUpdater.parseTokenPayload("{\"token\":\"ghp_xyz789\"}"))
        assertEquals("ghp_raw", GitHubUpdater.parseTokenPayload("ghp_raw"))
    }

    @Test
    fun getInstalledVersion_reflectsPackageInfoVersionName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPm = Shadows.shadowOf(context.packageManager)
        val pkgInfo = PackageInfo().apply {
            packageName = context.packageName
            versionName = "1.0.18"
            lastUpdateTime = 1_700_000_000_000L
        }
        shadowPm.installPackage(pkgInfo)

        assertEquals("1.0.18", GitHubUpdater.getInstalledVersion(context))
        assertFalse(GitHubUpdater.isNewerVersion("v1.0.18", GitHubUpdater.getInstalledVersion(context)))
        assertTrue(GitHubUpdater.isNewerVersion("v1.0.19", GitHubUpdater.getInstalledVersion(context)))
    }
}
