package com.example.c001apk.compose.logic.network.session

import com.example.c001apk.compose.util.CoolapkRequestPolicy
import com.google.gson.Gson
import java.security.MessageDigest
import java.util.UUID
import okhttp3.Cookie
import okhttp3.HttpUrl

/**
 * Scoped SESSID and accepted validation persistence; semantics ported from TG-Twilight (AGPL-3.0).
 */
class ApiSessionStore(
    read: () -> String?,
    private val write: (String?) -> Unit,
    private val active: () -> Identity,
    private val now: () -> Long = System::currentTimeMillis,
) {
    data class Identity(val generation: Long, val hash: String)

    data class Context(val identity: Identity, val revision: Long)

    private data class Session(
        val identity: String,
        val value: String,
        val domain: String,
        val hostOnly: Boolean,
        val path: String,
        val expires: Long,
    )

    private data class Validation(
        val identity: String,
        val session: String,
        val proof: String,
        val expires: Long,
        val process: String,
        val generation: Long,
    )

    private data class State(val session: Session? = null, val validation: Validation? = null)

    private val gson = Gson()
    private var state =
        runCatching { gson.fromJson(read(), State::class.java) }.getOrNull() ?: State()
    private var revision = 0L
    private val process = UUID.randomUUID().toString()

    private fun session() =
        state.session?.takeIf { it.identity == active().hash && it.expires > now() }

    @Synchronized fun capture() = Context(active(), revision)

    @Synchronized fun hash() = digest(session()?.value.orEmpty())

    @Synchronized
    fun cookieFor(url: HttpUrl): String {
        if (!CoolapkRequestPolicy.isApi(url)) return ""
        val record = session() ?: return ""
        if (record.hostOnly && record.domain != url.host) return ""
        if (record.domain != url.host && record.domain != "coolapk.com") return ""
        if (
            url.encodedPath != record.path &&
                !url.encodedPath.startsWith(record.path.trimEnd('/') + "/")
        )
            return ""
        return "SESSID=${record.value}"
    }

    @Synchronized
    fun validation(): String? =
        state.validation
            ?.takeIf {
                it.identity == active().hash &&
                    it.session == hash() &&
                    it.expires > now() &&
                    (it.process != process || it.generation == active().generation)
            }
            ?.proof

    @Synchronized
    fun save(url: HttpUrl, headers: List<String>, context: Context): String? {
        if (!CoolapkRequestPolicy.isApi(url) || context != capture()) return null
        for (header in headers) {
            val cookie = Cookie.parse(url, header) ?: continue
            if (
                cookie.name != "SESSID" ||
                    cookie.domain !in setOf(url.host, "coolapk.com") ||
                    cookie.path.length > 256
            )
                continue
            if (cookie.value.isEmpty() || cookie.expiresAt <= now()) {
                clear()
                return null
            }
            if (!Regex("^[A-Za-z0-9,._~%+/-]{1,512}$").matches(cookie.value)) continue
            val oldHash = hash()
            state =
                state.copy(
                    session =
                        Session(
                            context.identity.hash,
                            cookie.value,
                            cookie.domain,
                            cookie.hostOnly,
                            cookie.path,
                            minOf(cookie.expiresAt, now() + 7 * 86400000L),
                        ),
                )
            if (oldHash != hash()) state = state.copy(validation = null)
            revision++
            persist()
            return hash()
        }
        return hash()
    }

    @Synchronized
    fun acceptValidation(proof: String, context: Context, acceptedSession: String) {
        if (context.identity != active() || acceptedSession != hash()) return
        state =
            state.copy(
                validation =
                    Validation(
                        active().hash,
                        acceptedSession,
                        proof,
                        now() + 86400000L,
                        process,
                        active().generation,
                    ),
            )
        persist()
    }

    @Synchronized
    fun clearValidation() {
        state = state.copy(validation = null)
        persist()
    }

    @Synchronized
    fun clear() {
        revision++
        state = State()
        persist()
    }

    private fun persist() {
        runCatching { write(gson.toJson(state)) }
    }

    companion object {
        fun digest(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
