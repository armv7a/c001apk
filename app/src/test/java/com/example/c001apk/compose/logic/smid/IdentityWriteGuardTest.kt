package com.example.c001apk.compose.logic.smid

import androidx.datastore.core.DataStoreFactory
import com.example.c001apk.compose.logic.datastore.*
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class IdentityWriteGuardTest {
    @Test fun delayedGenerationCannotReplaceManualImportEvenWithSameValueOrAba() = runBlocking {
        val directory = Files.createTempDirectory("identity-race").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = DataStoreFactory.create(UserPreferencesSerializer(), scope = scope, produceFile = { directory.resolve("prefs.pb") })
            store.updateData { it.copy(manufacturer = "maker", brand = "brand", model = "model", buildNumber = "build", androidVersion = "16", sdkInt = "36") }
            val source = UserPreferencesDataSource(store)
            source.setSZLMId("original")
            val snapshot = store.data.first()
            val response = CompletableDeferred<String>()
            val lateWrite = async { source.saveGeneratedIdentity(response.await(), snapshot) }
            source.setSZLMId("manual")
            source.setSZLMId("original")
            response.complete("original")
            assertNull(lateWrite.await())
            assertEquals("original", store.data.first().szlmId)
            assertEquals(snapshot.deviceIdentityRevision + 2, store.data.first().deviceIdentityRevision)
            val current = store.data.first()
            store.updateData { it.copy(blurBars = true) }
            val saved = source.saveGeneratedIdentity("generated", current)!!
            assertEquals("generated", saved.szlmId)
            assertTrue(saved.blurBars)
            assertEquals(saved, store.data.first())
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}
