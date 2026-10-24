package com.example.c001apk.compose.util

import com.example.c001apk.compose.constant.Constants.APP_ID
import com.example.c001apk.compose.constant.Constants.CHANNEL
import com.example.c001apk.compose.constant.Constants.DARK_MODE
import com.example.c001apk.compose.constant.Constants.LOCALE
import com.example.c001apk.compose.constant.Constants.MODE
import com.example.c001apk.compose.constant.Constants.REQUEST_WITH
import com.example.c001apk.compose.util.CookieUtil.SESSID
import com.example.c001apk.compose.util.TokenDeviceUtils.getTokenV2
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/** Created by bggRGjQaUbCoE on 2024/6/3 */
object AddCookiesInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = CoolapkRequestPolicy.withoutPrivateHeaders(request)
        val api = CoolapkRequestPolicy.isApi(request.url)
        if (!api && !CoolapkRequestPolicy.isPublicFeed(request)) {
            return chain.proceed(builder.build())
        }
        val deviceCode = CookieUtil.xAppDevice
        val token = deviceCode.getTokenV2()
        builder.apply {
            header("User-Agent", CookieUtil.userAgent)
            header("X-Requested-With", REQUEST_WITH)
            header("X-Sdk-Int", CookieUtil.sdkInt)
            header("X-Sdk-Locale", LOCALE)
            header("X-App-Id", APP_ID)
            header("X-App-Token", token)
            header("X-App-Version", CookieUtil.versionName)
            header("X-App-Code", CookieUtil.versionCode)
            header("X-Api-Version", CookieUtil.apiVersion)
            header("X-App-Device", deviceCode)
            header("X-Dark-Mode", DARK_MODE)
            header("X-App-Channel", CHANNEL)
            header("X-App-Mode", MODE)
            header("X-App-Supported", CookieUtil.versionCode)
            header("Content-Type", "application/x-www-form-urlencoded")
            if (api && CookieUtil.isLogin)
                header(
                    "Cookie",
                    "uid=${CookieUtil.uid}; username=${CookieUtil.username}; token=${CookieUtil.token}",
                )
            else if (api) header("Cookie", SESSID)
        }
        return chain.proceed(builder.build()).also { response ->
            if (
                com.example.c001apk.compose.BuildConfig.DEBUG &&
                    response.request.url.encodedPath.startsWith("/v6/")
            ) {
                android.util.Log.d(
                    "CoolApkApi",
                    "${response.request.url.encodedPath}: ${response.code}",
                )
            }
        }
    }
}
