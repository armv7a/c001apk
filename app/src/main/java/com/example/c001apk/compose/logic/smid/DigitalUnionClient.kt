package com.example.c001apk.compose.logic.smid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** One bounded IPC request; native process death is a recoverable failure for the caller. */
class DigitalUnionClient @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun query(): String = request(Intent(context, DigitalUnionService::class.java))

    internal suspend fun request(intent: Intent, timeoutMillis: Long = 35_000): String =
        withContext(Dispatchers.Main.immediate) {
            var connection: ServiceConnection? = null
            var worker: IBinder? = null
            val connectionReady = CompletableDeferred<IBinder?>()
            var bindingAttempted = false
            try {
                withTimeoutOrNull(timeoutMillis) {
                    suspendCancellableCoroutine { continuation ->
                        fun fail(message: String) {
                            connectionReady.complete(null)
                            if (continuation.isActive) continuation.resumeWithException(IOException(message))
                        }
                        val reply = Messenger(Handler(Looper.getMainLooper()) { message ->
                            if (message.what == DigitalUnionService.RESULT && continuation.isActive) {
                                val id = message.data.getString(DigitalUnionService.ID)
                                    ?.trim()?.takeIf { it.length >= 16 && it != "null" }
                                if (id != null) continuation.resume(id)
                                else fail(message.data.getString(DigitalUnionService.ERROR)
                                    ?: "获取数盟 ID 失败，请稍后重试")
                            }
                            true
                        })
                        connection = object : ServiceConnection {
                            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                                worker = binder
                                connectionReady.complete(binder)
                                if (!continuation.isActive) return
                                try {
                                    Messenger(binder).send(Message.obtain(null, DigitalUnionService.QUERY).apply {
                                        replyTo = reply
                                    })
                                } catch (_: Exception) {
                                    fail("数盟组件连接失败，请稍后重试")
                                }
                            }

                            override fun onServiceDisconnected(name: ComponentName) {
                                fail("数盟组件异常退出，已保留原有 ID，可稍后重试或手动导入")
                            }

                            override fun onBindingDied(name: ComponentName) {
                                fail("数盟组件连接已中断，请重试")
                            }

                            override fun onNullBinding(name: ComponentName) {
                                fail("数盟组件无法启动，请重试")
                            }
                        }
                        try {
                            bindingAttempted = true
                            val bound = context.bindService(intent, checkNotNull(connection), Context.BIND_AUTO_CREATE)
                            if (!bound) fail("数盟组件无法启动，请重试")
                        } catch (_: Exception) {
                            fail("数盟组件无法启动，请重试")
                        }
                    }
                } ?: throw IOException("获取数盟 ID 超时，请检查网络后重试")
            } finally {
                if (bindingAttempted) {
                    withContext(NonCancellable) {
                        val stopped = CompletableDeferred<Unit>()
                        val death = IBinder.DeathRecipient { stopped.complete(Unit) }
                        // Cancellation can happen while Android is still creating the service.
                        // Let that binding settle before unbinding and allowing another query.
                        val binder = worker ?: withTimeoutOrNull(2_000) { connectionReady.await() }
                        val watching = try {
                            binder?.linkToDeath(death, 0)
                            binder != null
                        } catch (_: android.os.RemoteException) {
                            false
                        }
                        try {
                            // Android also requires cleanup when bindService returns false.
                            context.unbindService(checkNotNull(connection))
                        } catch (_: IllegalArgumentException) {
                            // A rejected binding may never have registered the connection.
                        }
                        // unbindService is asynchronous. Finish teardown before a caller can
                        // bind again, including when the original request was cancelled.
                        if (watching) {
                            withTimeoutOrNull(2_000) { stopped.await() }
                            binder?.unlinkToDeath(death, 0)
                        }
                    }
                }
            }
        }
}
