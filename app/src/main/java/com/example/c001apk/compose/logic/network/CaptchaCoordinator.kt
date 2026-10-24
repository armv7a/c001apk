package com.example.c001apk.compose.logic.network

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.c001apk.compose.ui.captcha.CaptchaActivity
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * User-driven verification only. Proofs never enter Intents, URLs, logs or saved instance state.
 */
object CaptchaCoordinator : Application.ActivityLifecycleCallbacks {
    class Pending(val challenge: CaptchaChallenge) {
        val id = UUID.randomUUID().toString()
        val result = CompletableFuture<String?>()
    }

    private var resumed = WeakReference<Activity>(null)
    private val current = AtomicReference<Pending?>()
    val epoch = AtomicLong()
    private val main = Handler(Looper.getMainLooper())

    fun pending(id: String?) = current.get()?.takeIf { it.id == id }

    fun complete(id: String, proof: String?) {
        current.get()?.takeIf { it.id == id }?.result?.complete(proof)
    }

    fun request(challenge: CaptchaChallenge, valid: () -> Boolean): String? {
        val pending = Pending(challenge)
        if (!current.compareAndSet(null, pending)) return null
        epoch.incrementAndGet()
        main.post {
            val activity = resumed.get()
            if (activity == null || activity.isFinishing || !valid()) pending.result.complete(null)
            else
                activity.startActivity(
                    Intent(activity, CaptchaActivity::class.java).putExtra("request", pending.id),
                )
        }
        return try {
            val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(5)
            while (valid() && System.nanoTime() < deadline) {
                try {
                    return pending.result.get(250, TimeUnit.MILLISECONDS)
                } catch (_: TimeoutException) {}
            }
            null
        } finally {
            pending.result.complete(null)
            current.compareAndSet(pending, null)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed.get() === activity) resumed.clear()
    }

    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
