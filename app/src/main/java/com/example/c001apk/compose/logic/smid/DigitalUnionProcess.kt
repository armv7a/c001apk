package com.example.c001apk.compose.logic.smid

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process

internal fun Context.isDigitalUnionProcess(): Boolean {
    val name = if (Build.VERSION.SDK_INT >= 28) Application.getProcessName() else {
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .runningAppProcesses?.firstOrNull { it.pid == Process.myPid() }?.processName
    }
    return name == "$packageName:digital_union"
}
