package com.example.c001apk.compose.logic.filter

import androidx.datastore.core.DataStoreFactory
import com.example.c001apk.compose.logic.datastore.UserPreferencesSerializer
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class ContentFilterStoreTest {
    @Test fun concurrentRuleEditsRetainIdentityAndSurviveDiskReopen() = runBlocking {
        val directory = Files.createTempDirectory("content-rules").toFile()
        val file = directory.resolve("prefs.pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data = DataStoreFactory.create(UserPreferencesSerializer(), scope = firstScope, produceFile = { file })
            data.updateData { it.copy(szlmId = "manual", deviceIdentityRevision = 9) }
            val filters = ContentFilterStore(data)
            coroutineScope { (1..12).map { id -> async { filters.save(ContentRule("$id", "规则 $id", terms = listOf("词 $id"))) } }.awaitAll() }
            filters.update { it.copy(enabled = true, allowedContent = setOf("FEED:123")) }
            val saved = data.data.first()
            assertEquals("manual", saved.szlmId)
            assertEquals(9L, saved.deviceIdentityRevision)
            assertEquals(12, filters.data.first().rules.size)
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
            val reopened = DataStoreFactory.create(UserPreferencesSerializer(), scope = secondScope, produceFile = { file })
            assertEquals(saved, reopened.data.first())
            val restored = ContentFilterStore(reopened)
            restored.disable("1")
            assertFalse(restored.data.first().rules.first { it.id == "1" }.enabled)
            assertTrue("FEED:123" in restored.data.first().allowedContent)
        } finally {
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
            secondScope.coroutineContext[Job]!!.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}
