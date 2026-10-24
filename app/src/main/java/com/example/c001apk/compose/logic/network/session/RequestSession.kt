package com.example.c001apk.compose.logic.network.session

import com.example.c001apk.compose.util.CookieUtil
import java.io.IOException
import okhttp3.Interceptor

class RequestSession(val accountRevision: Long, val device: String) {
    fun isCurrent() =
        accountRevision == CookieUtil.accountSession.revision && device == CookieUtil.xAppDevice

    override fun toString() = "RequestSession(identity=redacted)"

    companion object {
        val capture = Interceptor { chain ->
            val session = RequestSession(CookieUtil.accountSession.revision, CookieUtil.xAppDevice)
            val response =
                chain.proceed(
                    chain.request().newBuilder().tag(RequestSession::class.java, session).build(),
                )
            if (!session.isCurrent()) {
                response.close()
                throw IOException("账号或设备已变化，请重新加载")
            }
            response
        }
    }
}

class AcceptedApiSession(val hash: String?)
