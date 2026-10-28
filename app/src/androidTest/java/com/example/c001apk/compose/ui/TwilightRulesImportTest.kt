package com.example.c001apk.compose.ui

import androidx.datastore.core.DataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.compose.logic.datastore.UserPreferencesSerializer
import com.example.c001apk.compose.logic.filter.ContentFilterStore
import com.example.c001apk.compose.logic.filter.ContentRule
import com.example.c001apk.compose.logic.filter.TwilightRulesClient
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

/** Opt-in live integration test; its temporary DataStore never changes the user's rules. */
class TwilightRulesImportTest {
    @Test fun downloadAndRefreshPublicRulesPreservePersonalChoices() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("remote-filter-test", ".pb", context.cacheDir)
        file.delete()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data = DataStoreFactory.create(UserPreferencesSerializer(), scope = scope, produceFile = { file })
            val store = ContentFilterStore(data)
            store.save(ContentRule("personal-test", "个人测试", terms = listOf("synthetic")))
            store.update { it.copy(allowedContent = setOf("FEED:test")) }
            val downloaded = TwilightRulesClient().fetch()
            assertTrue(downloaded.revision > 0)
            assertTrue(downloaded.rules.isNotEmpty())
            store.importTwilight(downloaded)
            store.disable(downloaded.rules.first().key)
            store.importTwilight(TwilightRulesClient().fetch())
            val result = store.data.first()
            assertFalse(result.enabled)
            assertEquals("personal-test", result.rules.single().id)
            assertEquals(setOf("FEED:test"), result.allowedContent)
            assertTrue(downloaded.rules.first().key in result.disabledTwilightRules)
            println("Downloaded Twilight revision ${result.twilight!!.revision}, ${result.twilight.rules.size} rules; personal choices retained")
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }
}
