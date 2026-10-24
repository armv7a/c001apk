package com.example.c001apk.compose.logic.update

import com.google.gson.JsonParser
import java.net.URI

/** Compare semantic versions, never ABI version codes, timestamps, hashes or build metadata. */
data class AppVersion(val major: Long, val minor: Long, val patch: Long) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion) =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString() = "$major.$minor.$patch"

    companion object {
        fun parse(value: String): AppVersion? {
            val match =
                Regex(
                        "^v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-(?:release|miuix))?(?:\\+[A-Za-z0-9.-]+)?$",
                    )
                    .matchEntire(value.trim()) ?: return null
            return runCatching {
                AppVersion(
                    match.groupValues[1].toLong(),
                    match.groupValues[2].toLong(),
                    match.groupValues[3].toLong(),
                )
            }
                .getOrNull()
        }
    }
}

data class ReleaseInfo(
    val version: AppVersion,
    val tag: String,
    val publishedAt: String,
    val notes: String,
    val page: String,
    val download: String?,
) {
    companion object {
        fun repository(value: String): String? {
            val raw =
                value.trim().removePrefix("https://github.com/").trimEnd('/').removeSuffix(".git")
            return raw.takeIf {
                Regex("[A-Za-z0-9][A-Za-z0-9-]{0,38}/[A-Za-z0-9_.-]{1,100}").matches(it) &&
                    it.substringAfter('/') !in setOf(".", "..")
            }
        }

        private fun validUrl(value: String, repository: String, prefix: String): Boolean =
            runCatching {
                val uri = URI(value)
                uri.scheme == "https" &&
                    uri.host == "github.com" &&
                    uri.userInfo == null &&
                    uri.port == -1 &&
                    uri.rawPath
                        .lowercase()
                        .startsWith("/${repository.lowercase()}/releases/$prefix")
            }
            .getOrDefault(false)

        fun parse(json: String, repository: String, abis: List<String>): ReleaseInfo {
            require(json.length <= 1024 * 1024)
            val obj = JsonParser.parseString(json).asJsonObject
            require(
                obj.get("draft")?.asBoolean == false && obj.get("prerelease")?.asBoolean == false,
            )
            val tag = obj.get("tag_name")?.asString.orEmpty()
            val version = AppVersion.parse(tag) ?: error("Invalid release version")
            val page = obj.get("html_url")?.asString.orEmpty()
            require(validUrl(page, repository, "tag/"))
            val candidates =
                obj.getAsJsonArray("assets")
                    ?.mapNotNull { element ->
                        val item = element.asJsonObject
                        val name = item.get("name")?.asString.orEmpty().lowercase()
                        val url = item.get("browser_download_url")?.asString.orEmpty()
                        if (
                            name.endsWith(".apk") &&
                                !name.contains("debug") &&
                                !name.contains("unsigned") &&
                                validUrl(url, repository, "download/")
                        )
                            name to url
                        else null
                    }
                    .orEmpty()
            val download =
                abis.firstNotNullOfOrNull { abi ->
                    val aliases =
                        when (abi) {
                            "arm64-v8a" -> listOf("arm64-v8a", "arm64", "aarch64")
                            "armeabi-v7a" -> listOf("armeabi-v7a", "armv7")
                            else -> listOf(abi)
                        }
                    candidates
                        .firstOrNull { (name, _) -> aliases.any { name.contains(it) } }
                        ?.second
                }
                    ?: candidates
                        .firstOrNull { (name, _) ->
                            name.contains("universal") ||
                                listOf("arm64", "aarch64", "armeabi", "armv7", "x86").none {
                                    name.contains(it)
                                }
                        }
                        ?.second
            return ReleaseInfo(
                version,
                tag,
                obj.get("published_at")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                obj.get("body")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                page,
                download,
            )
        }
    }
}
