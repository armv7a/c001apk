package com.example.c001apk.compose.logic.network

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URLEncoder

class CaptchaProof(proof: String) {
    val encoded: String = URLEncoder.encode(proof, Charsets.UTF_8)

    override fun toString() = "CaptchaProof(redacted)"
}

data class CaptchaChallenge(val id: String, val field: String) {
    fun proof(validate: String): String? =
        validate
            .takeIf {
                it.isNotEmpty() &&
                    it.length <= 8192 &&
                    it.none { ch -> ch.isWhitespace() || ch.code < 32 }
            }
            ?.let { "NEC:${id.take(8)}:$it" }

    companion object {
        fun from(body: JsonObject?): CaptchaChallenge? {
            if (
                body?.get("messageStatus")?.takeIf { it.isJsonPrimitive }?.asString !=
                    "err_request_captcha_v2"
            )
                return null
            val raw = body.get("messageExtra") ?: body.get("extra")
            val extra = runCatching {
                when {
                    raw?.isJsonObject == true -> raw.asJsonObject
                    raw?.isJsonPrimitive == true ->
                        JsonParser.parseString(raw.asString).asJsonObject
                    else -> body
                }
            }
                .getOrDefault(body)
            val id = runCatching { extra.get("captchaId")?.asString }.getOrNull()
            val field = runCatching { extra.get("captchaField")?.asString }.getOrNull()
            return CaptchaChallenge(
                id?.takeIf { Regex("^[a-zA-Z0-9]{16,64}$").matches(it) }
                    ?: "414e5c9b866a03db03f860a1a9101672",
                field?.takeIf { it.isBlank() || Regex("^_[a-zA-Z0-9_]{1,63}$").matches(it) }?.trim()
                    ?: "_v2_post_token",
            )
        }
    }
}

class SkipCaptcha
