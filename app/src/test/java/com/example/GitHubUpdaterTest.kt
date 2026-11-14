package com.example

import com.example.data.GitHubUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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

        assertFalse(GitHubUpdater.isNewerVersion("v1.0.0", "1.0"))
        assertFalse(GitHubUpdater.isNewerVersion("v1.0", "1.0.0"))
        assertFalse(GitHubUpdater.isNewerVersion("v0.9.9", "1.0"))
        assertFalse(GitHubUpdater.isNewerVersion("v1.0.14", "1.0.15"))
    }

    @Test
    fun parseTokenPayload_handlesQuotedJsonStringAndObject() {
        assertEquals("ghp_abc123", GitHubUpdater.parseTokenPayload("\"ghp_abc123\""))
        assertEquals("ghp_xyz789", GitHubUpdater.parseTokenPayload("{\"token\":\"ghp_xyz789\"}"))
        assertEquals("ghp_raw", GitHubUpdater.parseTokenPayload("ghp_raw"))
    }
}
