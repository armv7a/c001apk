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

/** Device-only IPC fixture. Never loads the SDK or contacts a server. */
class DigitalUnionFixtureService : Service() {
    override fun onBind(intent: Intent): IBinder = Messenger(Handler(Looper.getMainLooper()) { request ->
        when (intent.getStringExtra("behavior")) {
            "die" -> Process.killProcess(Process.myPid())
            "hang" -> Unit
            else -> request.replyTo.send(Message.obtain(null, DigitalUnionService.RESULT).apply {
                data = Bundle().apply {
                    putString(DigitalUnionService.ID, "fixture-device-id-${Process.myPid()}")
                }
            })
        }
        true
    }).binder

    override fun onDestroy() {
        super.onDestroy()
        if (isDigitalUnionProcess()) Process.killProcess(Process.myPid())
    }
}
