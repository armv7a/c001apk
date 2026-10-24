package com.example.c001apk.compose.ui

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import cn.shuzilm.core.DUHelper
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import com.example.c001apk.compose.logic.smid.SmidConfig
import com.example.c001apk.compose.logic.smid.withDigitalUnionId
import org.junit.Assert.*
import org.junit.Test

class DigitalUnionIdentityTest {
    @Test
    fun sdkUsesOfficialIdentityWithoutChangingHostPackage() {
        val host = InstrumentationRegistry.getInstrumentation().targetContext
        val sdk = DUHelper.PackageLockedContext(host, SmidConfig.DIGITAL_UNION_PACKAGE_NAME)
        val before = host.packageManager.getPackageInfo(host.packageName, 0)
        val info =
            sdk.packageManager.getPackageInfo(
                sdk.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        assertEquals("com.coolapk.market", sdk.packageName)
        assertEquals("com.coolapk.market", info.packageName)
        assertEquals("16.1.3", info.versionName)
        assertEquals(2604022L, info.longVersionCode)
        assertEquals("com.example.c001apk.compose.miuix", host.packageName)
        val after = host.packageManager.getPackageInfo(host.packageName, 0)
        assertEquals(before.versionName, after.versionName)
        assertEquals(before.packageName, after.packageName)
    }

    @Test
    fun applyingIdMigratesProtocolDefaultsAndPreservesThemeAndAccount() {
        val original =
            UserPreferencesCompat.default()
                .copy(
                    versionName = "13.4.1",
                    versionCode = "2312121",
                    apiVersion = "13",
                    uid = "test-user",
                    token = "test-token",
                    pureBlack = true,
                )
        val updated = original.withDigitalUnionId("test-device-id-123456")
        assertEquals("test-device-id-123456", updated.szlmId)
        assertEquals("16.1.3", updated.versionName)
        assertEquals("2604022", updated.versionCode)
        assertTrue(updated.xAppDevice.isNotBlank())
        assertTrue(updated.userAgent.contains("CoolMarket/16.1.3-2604022"))
        assertEquals(original.uid, updated.uid)
        assertEquals(original.token, updated.token)
        assertTrue(updated.pureBlack)
    }
}
