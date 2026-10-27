package com.example.c001apk.compose.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.logic.smid.DigitalUnionDebugEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in device smoke test; ordinary test runs do not contact the SDK service. */
class DigitalUnionNetworkTest {
    @Test
    fun nativeSdkGeneratesAndPersistsAnId() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("smidNetwork") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        // Keep the opt-in query visible so HyperOS does not freeze the SDK worker.
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "am start -W -n ${context.packageName}/androidx.activity.ComponentActivity",
        ).use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        }
        val repository =
            EntryPointAccessors.fromApplication(context, DigitalUnionDebugEntryPoint::class.java)
                .digitalUnion()
        val id = repository.ensureId(force = true).getOrThrow()
        assertTrue(id.length >= 16)
        val preferences =
            EntryPointAccessors.fromApplication(context, DigitalUnionDebugEntryPoint::class.java)
                .preferences()
        assertEquals(id, preferences.data.first().szlmId)
    }
}
