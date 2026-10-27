package com.example.c001apk.compose.ui

import android.content.Intent
import android.os.Process
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.logic.smid.DigitalUnionClient
import com.example.c001apk.compose.logic.smid.DigitalUnionFixtureService
import com.example.c001apk.compose.logic.smid.DigitalUnionService
import com.example.c001apk.compose.logic.smid.DIGITAL_UNION_FALLBACK_ENDPOINT
import com.example.c001apk.compose.logic.smid.queryDigitalUnionId
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DigitalUnionProcessTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val client = DigitalUnionClient(context)
    private fun request(behavior: String) =
        Intent(context, DigitalUnionFixtureService::class.java).putExtra("behavior", behavior)

    @Before
    fun keepTestProcessInForeground() {
        // HyperOS freezes background test processes even while instrumentation is running.
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "am start -W -n ${context.packageName}/androidx.activity.ComponentActivity",
        ).use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        }
    }

    @Test
    fun sdkServiceIsPrivateAndSuccessCrossesASeparateProcess() = runBlocking {
        val info = context.packageManager.getServiceInfo(
            android.content.ComponentName(context, DigitalUnionService::class.java), 0,
        )
        assertFalse(info.exported)
        assertEquals("${context.packageName}:digital_union", info.processName)
        val id = client.request(request("success"))
        assertTrue(id.startsWith("fixture-device-id-"))
        assertNotEquals(Process.myPid(), id.substringAfterLast('-').toInt())
    }

    @Test
    fun workerDeathReturnsFailureAndNextRequestCanSucceed() = runBlocking {
        val pid = Process.myPid()
        val failure = runCatching { client.request(request("die"), 5_000) }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertTrue(failure?.message.orEmpty().contains("异常退出"))
        assertEquals(pid, Process.myPid())
        assertTrue(client.request(request("success")).startsWith("fixture-device-id-"))
    }

    @Test
    fun hungWorkerTimesOutWithoutBlockingTheNextRequest() = runBlocking {
        val failure = runCatching { client.request(request("hang"), 1_000) }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertTrue(failure?.message.orEmpty().contains("超时"))
        assertTrue(client.request(request("success")).startsWith("fixture-device-id-"))
    }

    @Test
    fun cancelledRequestUnbindsAndAllowsAnotherQuery() = runBlocking {
        for (cancelAfter in listOf(25L, 500L)) {
            val pending = async { client.request(request("hang")) }
            delay(cancelAfter)
            pending.cancel()
            pending.join()
            assertTrue(pending.isCancelled)
            assertTrue(client.request(request("success")).startsWith("fixture-device-id-"))
        }
    }

    @Test
    fun primaryWorkerDeathAutomaticallyFallsBack() = runBlocking {
        val endpoints = mutableListOf<String?>()
        val id = queryDigitalUnionId { endpoint ->
            endpoints += endpoint
            client.request(request(if (endpoint == null) "die" else "success"), 5_000)
        }
        assertTrue(id.orEmpty().startsWith("fixture-device-id-"))
        assertEquals(listOf(null, DIGITAL_UNION_FALLBACK_ENDPOINT), endpoints)
    }

    @Test
    fun primaryTimeoutTearsDownBeforeFallbackStarts() = runBlocking {
        val endpoints = mutableListOf<String?>()
        val id = queryDigitalUnionId { endpoint ->
            endpoints += endpoint
            if (endpoint == null) client.request(request("hang"), 1_000)
            else client.request(request("success"), 5_000)
        }
        assertTrue(id.orEmpty().startsWith("fixture-device-id-"))
        assertEquals(listOf(null, DIGITAL_UNION_FALLBACK_ENDPOINT), endpoints)
    }
}
