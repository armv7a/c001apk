package com.example.c001apk.compose.ui.topic

import com.example.c001apk.compose.logic.model.LikeResponse
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class TopicFollowResultTest {
    private fun acknowledged(json: String) = Gson().fromJson(json, LikeResponse::class.java).topicFollowAcknowledged()
    @Test fun messageOnlyFollowAndUnfollowAreAcknowledged() {
        assertTrue(acknowledged("""{"status":1,"messageStatus":1,"message":"关注成功"}"""))
        assertTrue(acknowledged("""{"status":1,"messageStatus":1,"message":"取消关注成功"}"""))
        assertTrue(acknowledged("""{"status":1}"""))
    }
    @Test fun failureAndCaptchaNeverChangeFollowState() {
        for (body in listOf(
            """{"status":-2,"messageStatus":-2,"message":"缺少参数"}""",
            """{"status":0,"message":"缺少参数"}""",
            """{"status":401,"message":"请先登录"}""",
            """{"status":1,"messageStatus":"err_request_captcha_v2","message":"需要验证码"}""",
            """{"message":"缺少参数"}""",
            """{"status":1,"error":1,"message":"关注成功"}""",
        )) assertFalse(body, acknowledged(body))
    }
}
