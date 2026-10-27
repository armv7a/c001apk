package com.example.c001apk.compose.logic.update

import org.junit.Assert.*
import org.junit.Test

class ReleaseInfoTest {
    @Test
    fun comparesSemanticVersionAndIgnoresBuildIdentifiers() {
        assertTrue(AppVersion.parse("v1.10.0")!! > AppVersion.parse("1.9.9-release")!!)
        assertEquals(AppVersion.parse("1.3.5"), AppVersion.parse("1.3.5-miuix+abcdef"))
        assertEquals(AppVersion.parse("1.3.5+build1"), AppVersion.parse("v1.3.5+build2"))
        assertNull(AppVersion.parse("1.4.0-beta"))
        assertNull(AppVersion.parse("hash-miuix"))
        assertNull(AppVersion.parse("999999999999999999999999.2.3"))
    }

    private fun release(prerelease: Boolean = false, assets: String = "") =
        """{
      "draft":false,"prerelease":$prerelease,"tag_name":"v1.4.0","body":"# Notes\nFull notes",
      "published_at":"2026-10-24T00:00:00Z","html_url":"https://github.com/owner/miuix/releases/tag/v1.4.0",
      "assets":[$assets]
    }"""

    private fun asset(name: String, path: String = "owner/miuix") =
        """{"name":"$name","browser_download_url":"https://github.com/$path/releases/download/v1.4.0/$name"}"""

    @Test
    fun matchesAbiAndPreservesFullNotes() {
        val info =
            ReleaseInfo.parse(
                release(
                    assets =
                        listOf(
                                asset("app-x86_64.apk"),
                                asset("app-arm64-v8a.apk"),
                                asset("app-universal.apk"),
                            )
                            .joinToString(","),
                ),
                "owner/miuix",
                listOf("arm64-v8a"),
            )
        assertTrue(info.download!!.endsWith("app-arm64-v8a.apk"))
        assertEquals("# Notes\nFull notes", info.notes)
    }

    @Test
    fun fallsBackToPageForWrongAbiOrForeignRepository() {
        val info =
            ReleaseInfo.parse(
                release(
                    assets =
                        listOf(asset("app-x86_64.apk"), asset("app-arm64-v8a.apk", "other/project"))
                            .joinToString(","),
                ),
                "owner/miuix",
                listOf("arm64-v8a"),
            )
        assertNull(info.download)
    }

    @Test
    fun rejectsPrereleases() {
        assertTrue(
            runCatching {
                ReleaseInfo.parse(
                    release(prerelease = true),
                    "owner/miuix",
                    listOf("arm64-v8a"),
                )
            }
                .isFailure,
        )
    }
}
