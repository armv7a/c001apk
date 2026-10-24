package com.example.c001apk.compose.util

import com.example.c001apk.compose.constant.Constants.APP_ID
import com.example.c001apk.compose.constant.Constants.REQUEST_WITH
import com.example.c001apk.compose.util.CookieUtil.SESSID
import com.example.c001apk.compose.util.CookieUtil.isGetCaptcha
import com.example.c001apk.compose.util.CookieUtil.isGetLoginParam
import com.example.c001apk.compose.util.CookieUtil.isGetSmsLoginParam
import com.example.c001apk.compose.util.CookieUtil.isGetSmsToken
import com.example.c001apk.compose.util.CookieUtil.isPreGetLoginParam
import com.example.c001apk.compose.util.CookieUtil.isTryLogin
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/** Created by bggRGjQaUbCoE on 2024/6/3 */
object LoginCookiesInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!CoolapkRequestPolicy.isAccount(chain.request().url)) {
            return chain.proceed(
                CoolapkRequestPolicy.withoutPrivateHeaders(chain.request()).build(),
            )
        }
        val builder: Request.Builder = chain.request().newBuilder()
        builder.apply {
            if (isGetLoginParam) {
                isGetLoginParam = false

                header(
                    "sec-ch-ua",
                    """"Android WebView";v="117", "Not;A=Brand";v="8", "Chromium";v="117"""",
                )
                header("sec-ch-ua-mobile", "?1")
                header("sec-ch-ua-platform", "Android")
                header("Upgrade-Insecure-Requests", "1")
                header("User-Agent", CookieUtil.userAgent)
                header(
                    "Accept",
                    """text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7""",
                )
                header("User-Agent", CookieUtil.userAgent)
                header("X-Requested-With", APP_ID)
                header("Cookie", SESSID)
            }
            if (isPreGetLoginParam) {
                isPreGetLoginParam = false

                header(
                    "sec-ch-ua",
                    """"Android WebView";v="117", "Not;A=Brand";v="8", "Chromium";v="117"""",
                )
                header("sec-ch-ua-mobile", "?1")
                header("sec-ch-ua-platform", "Android")
                header("Upgrade-Insecure-Requests", "1")
                header("User-Agent", CookieUtil.userAgent)
                header(
                    "Accept",
                    """text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7""",
                )
                header("X-Requested-With", APP_ID)
                /*header("Sec-Fetch-Site", "none")
                header("Sec-Fetch-Mode", "navigate")
                header("Sec-Fetch-User", "?1")
                header("Sec-Fetch-Dest", "document")
                header("Accept-Encoding", "gzip, deflate, br")
                header("Accept-Language", "zh-CM,zh;q=0.9,en-US;q=0.8,en;q=0.7")*/
            } else if (isTryLogin) {
                isTryLogin = false

                header("User-Agent", CookieUtil.userAgent)
                header("Cookie", "$SESSID; forward=https://www.coolapk.com")
                header("X-Requested-With", REQUEST_WITH)
                header("Content-Type", "application/x-www-form-urlencoded")
            } else if (isGetCaptcha) {
                isGetCaptcha = false

                header("User-Agent", CookieUtil.userAgent)
                header(
                    "sec-ch-ua",
                    """"Android WebView";v="117", "Not;A=Brand";v="8", "Chromium";v="117"""",
                )
                header("sec-ch-ua-mobile", "?1")
                header("User-Agent", CookieUtil.userAgent)
                header("sec-ch-ua-platform", "Android")
                header(
                    "Accept",
                    """image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8""",
                )
                header("X-Requested-With", APP_ID)
                header("Sec-Fetch-Site", "same-origin")
                header("Sec-Fetch-Mode", "no-cors")
                header("Sec-Fetch-Dest", "image")
                header("Referer", "https://account.coolapk.com/auth/loginByCoolapk")
                header("Cookie", "$SESSID; forward=https://www.coolapk.com")
            } else if (isGetSmsToken) {
                isGetSmsToken = false
                header(
                    "sec-ch-ua",
                    """"Android WebView";v="117", "Not;A=Brand";v="8", "Chromium";v="117"""",
                )
                header("Content-Type", "application/x-www-form-urlencoded")
                header("X-Requested-With", REQUEST_WITH)
                header("sec-ch-ua-mobile", "?1")
                header("User-Agent", CookieUtil.userAgent)
                header("sec-ch-ua-platform", "Android")
                header("Accept", "*/*")
                header("Origin", "https://account.coolapk.com")
                header("Sec-Fetch-Site", "same-origin")
                header("Sec-Fetch-Mode", "cors")
                header("Sec-Fetch-Dest", "empty")
                header("Referer", "https://account.coolapk.com/auth/login?type=mobile")
                header("Accept-Encoding", "gzip, deflate, br")
                header("Accept-Language", "zh-CM,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                header("Cookie", "$SESSID; forward=https://www.coolapk.com")
            } else if (isGetSmsLoginParam) {
                isGetSmsLoginParam = false
                header(
                    "sec-ch-ua",
                    """"Android WebView";v="117", "Not;A=Brand";v="8", "Chromium";v="117"""",
                )
                header("sec-ch-ua-mobile", "?1")
                header("sec-ch-ua-platform", "Android")
                header("Upgrade=Insecure-Requests", "1")
                header("User-Agent", CookieUtil.userAgent)
                header(
                    "Accept",
                    """text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7""",
                )
                header("X-Requested-With", APP_ID)
                header("Sec-Fetch-Site", "none")
                header("Sec-Fetch-Mode", "navigate")
                header("Sec-Fetch-User", "?1")
                header("Sec-Fetch-Dest", "document")
                header("Accept-Encoding", "gzip, deflate, br")
                header("Accept-Language", "zh-CM,zh;q=0.9,en-US;q=0.8,en;q=0.7")
            }
        }
        return chain.proceed(builder.build())
    }
}
