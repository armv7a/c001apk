package com.example.c001apk.compose.logic.network

import com.example.c001apk.compose.logic.model.LikeResponse
import com.google.gson.Gson
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LikeRequestTest {
    private val api =
        Retrofit.Builder()
            .baseUrl("https://api.coolapk.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

    @Test
    fun allLikeActionsUseTheRequiredQueryAndFormContract() {
        for (action in listOf("like", "unlike", "likeReply", "unLikeReply")) {
            // Build the real Retrofit request without contacting the service or using credentials.
            val request = api.postLike("/v6/feed/$action", "123456").request()
            assertEquals("POST", request.method)
            assertEquals("/v6/feed/$action", request.url.encodedPath)
            assertEquals("123456", request.url.queryParameter("id"))
            assertEquals("0", request.url.queryParameter("detail"))
            assertEquals(setOf("id", "detail"), request.url.queryParameterNames)
            val body = requireNotNull(request.body)
            assertEquals("application/x-www-form-urlencoded", body.contentType().toString())
            val wire = Buffer()
            body.writeTo(wire)
            assertEquals("trace=", wire.readUtf8())
        }
    }

    @Test
    fun responseParsingPreservesFeedReplyCountsAndServerErrors() {
        val gson = Gson()
        assertEquals(
            "12",
            gson.fromJson("""{"data":{"count":12}}""", LikeResponse::class.java).data?.count,
        )
        assertEquals("7", gson.fromJson("""{"data":7}""", LikeResponse::class.java).data?.count)
        val failed =
            gson.fromJson(
                """{"status":-1,"message":"需要验证","data":null}""",
                LikeResponse::class.java,
            )
        assertNull(failed.data)
        assertEquals("需要验证", failed.message)
    }
}
