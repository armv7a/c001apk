package com.example.c001apk.compose.ui.captcha

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.c001apk.compose.logic.network.CaptchaCoordinator
import com.example.c001apk.compose.ui.component.*
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import top.yukonga.miuix.kmp.basic.Text

class CaptchaActivity : ComponentActivity() {
    private var requestId: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestId = intent.getStringExtra("request")
        val pending =
            CaptchaCoordinator.pending(requestId)
                ?: run {
                    finish()
                    return
                }
        pending.result.whenComplete { _, _ -> runOnUiThread { if (!isDestroyed) finish() } }
        setContent {
            C001apkComposeTheme {
                var reload by remember { mutableIntStateOf(0) }
                MiuixScaffold(
                    topBar = {
                        MiuixTopBar(
                            title = "人机验证",
                            navigationIcon = { BackButton { finish() } },
                            actions = {
                                MiuixActionButton(onClick = { reload++ }) { Text("重新加载") }
                            },
                        )
                    },
                ) { padding ->
                    if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                        Text("请更新系统 WebView 后重试验证。", Modifier.padding(padding))
                    } else
                        key(reload) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                factory = { context ->
                                    WebView(context).apply {
                                        settings.javaScriptEnabled = true
                                        settings.allowFileAccess = false
                                        settings.allowContentAccess = false
                                        settings.javaScriptCanOpenWindowsAutomatically = false
                                        settings.mixedContentMode =
                                            WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                        webViewClient =
                                            object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(
                                                    view: WebView?,
                                                    request: WebResourceRequest?,
                                                ) = request?.isForMainFrame == true
                                            }
                                        WebViewCompat.addWebMessageListener(
                                            this,
                                            "CaptchaResult",
                                            setOf("https://captcha.c001apk.invalid"),
                                        ) { _, message, _, isMainFrame, _ ->
                                            if (isMainFrame)
                                                pending.challenge
                                                    .proof(message.data.orEmpty())
                                                    ?.let {
                                                        CaptchaCoordinator.complete(pending.id, it)
                                                    }
                                        }
                                        loadDataWithBaseURL(
                                            "https://captcha.c001apk.invalid/",
                                            html(pending.challenge.id),
                                            "text/html",
                                            "UTF-8",
                                            null,
                                        )
                                    }
                                },
                                onRelease = {
                                    it.stopLoading()
                                    it.destroy()
                                },
                            )
                        }
                }
            }
        }
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) requestId?.let { CaptchaCoordinator.complete(it, null) }
        super.onDestroy()
    }

    private fun html(id: String) =
        """
        <!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>:root{color-scheme:light dark}body{font:16px sans-serif;margin:24px;background:Canvas;color:CanvasText}p{line-height:1.6}</style>
        <p id="status">正在加载验证组件…</p><div id="captcha"></div><script>
        function statusText(t){document.getElementById('status').textContent=t;}
        var timeout=setTimeout(function(){statusText('加载超时，请点击重新加载');},15000);
        var script=document.createElement('script');script.src='https://cstaticdun.126.net/load.min.js';
        script.onerror=function(){clearTimeout(timeout);statusText('加载失败，请检查网络后重试');};
        script.onload=function(){try{initNECaptcha({captchaId:'$id',element:'#captcha',protocol:'https',mode:'embed',apiVersion:2,
        width:Math.min(320,window.innerWidth-48)+'px',lang:'zh-CN',onVerify:function(err,data){
        if(!err&&data&&data.validate){window.CaptchaResult.postMessage(data.validate);}else{statusText('验证未通过，请重试');}}},
        function(){clearTimeout(timeout);statusText('请按下方提示完成人机验证');},function(){clearTimeout(timeout);statusText('初始化失败，请重新加载');});
        }catch(e){clearTimeout(timeout);statusText('初始化失败，请重新加载');}};document.head.appendChild(script);
        </script></html>
    """
            .trimIndent()
}
