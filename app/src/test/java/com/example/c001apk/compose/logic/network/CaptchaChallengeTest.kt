package com.example.c001apk.compose.logic.network

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class CaptchaChallengeTest {
    private fun challenge(extra: String) = CaptchaChallenge.from(JsonParser.parseString(
        """{"messageStatus":"err_request_captcha_v2","messageExtra":$extra}""").asJsonObject)!!
    @Test fun emptyFieldSelectsCookieAndMissingFieldKeepsFormProtocol() {
        assertEquals("", challenge("""{"captchaField":""}""").field)
        assertEquals("_v2_post_token", challenge("{}").field)
        assertEquals("_proof", challenge("""{"captchaField":"_proof"}""").field)
    }
    @Test fun proofsAreBoundToCaptchaPrefixAndRejectWhitespace() {
        val challenge = challenge("""{"captchaId":"1234567890abcdef"}""")
        assertEquals("NEC:12345678:synthetic", challenge.proof("synthetic"))
        assertNull(challenge.proof("has spaces"))
        assertNull(challenge.proof(""))
        assertFalse(CaptchaProof("synthetic;value").encoded.contains(';'))
    }
}
