package com.example.c001apk.compose.logic.smid

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.os.RemoteException
import cn.shuzilm.core.Main as Shuzilm
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/** The supplied libdu binary can abort its process; it is never loaded by the UI. */
class DigitalUnionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var requested = false
    private val messenger = Messenger(Handler(Looper.getMainLooper()) { message ->
        val reply = message.replyTo
        if (message.what == QUERY && reply != null && !requested) {
            requested = true
            scope.launch {
                val result = Bundle()
                try {
                    // PhoneStateListener must be initialized on a Looper thread.
                    Shuzilm.init(this@DigitalUnionService, SmidConfig.DIGITAL_UNION_API_KEY, false)
                    listOf("pkglist", "operation", "cdlmt").forEach {
                        Shuzilm.setConfig(it, SmidConfig.DIGITAL_UNION_DISABLE)
                    }
                    val id = queryDigitalUnionId { url ->
                        suspendCancellableCoroutine { continuation ->
                            Shuzilm.getQueryID(this@DigitalUnionService, null, "", false, url) {
                                if (continuation.isActive) continuation.resume(it)
                            }
                        }
                    }
                    if (id != null) result.putString(ID, id)
                    else result.putString(ERROR, "暂时无法获取数盟 ID，请检查网络后重试")
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: LinkageError) {
                    result.putString(ERROR, "数盟组件加载失败")
                } catch (_: Exception) {
                    result.putString(ERROR, "获取数盟 ID 失败，请稍后重试")
                }
                requested = false
                try {
                    reply.send(Message.obtain(null, RESULT).apply { data = result })
                } catch (_: RemoteException) {
                    // The caller has left; onDestroy tears down native background threads.
                }
            }
        }
        true
    })

    override fun onBind(intent: Intent): IBinder = messenger.binder

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
        // Unbinding after success, cancellation or timeout also stops native work that cannot
        // be cancelled from Kotlin. Only this service's private process may be terminated.
        if (isDigitalUnionProcess()) Process.killProcess(Process.myPid())
    }

    internal companion object {
        const val QUERY = 1
        const val RESULT = 2
        const val ID = "id"
        const val ERROR = "error"
    }
}
