package com.example.c001apk.compose.util

import java.net.URLEncoder
import org.jsoup.Jsoup

object ShareContent {
    fun link(type: ShareType, id: String, mirror: Boolean = true): String {
        val route =
            when (type) {
                ShareType.FEED -> "feed"
                ShareType.APP -> "apk"
                ShareType.USER -> "u"
                ShareType.TOPIC -> "t"
                ShareType.PRODUCT -> "product"
            }
        val value = URLEncoder.encode(id, "UTF-8").replace("+", "%20")
        return "https://www.${if (mirror) "coolapk1s" else "coolapk"}.com/$route/$value"
    }

    fun text(type: ShareType, id: String, title: String? = null, message: String? = null): String {
        val heading =
            Jsoup.parseBodyFragment(title.orEmpty())
                .text()
                .ifBlank {
                    Jsoup.parseBodyFragment(message.orEmpty()).text().take(80)
                }
                .ifBlank {
                    when (type) {
                        ShareType.FEED -> "动态"
                        ShareType.APP -> "应用"
                        ShareType.USER -> "用户"
                        ShareType.TOPIC -> "话题"
                        ShareType.PRODUCT -> "数码"
                    }
                }
        return "$heading\n${link(type, id)}\n分享自c001apk"
    }
}
