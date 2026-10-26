package com.example.c001apk.compose.logic.datastore

import androidx.datastore.core.DataStoreFactory
import com.example.c001apk.compose.UserPreferences
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeLinkHintPreferencesTest {
    @Test
    fun hintStartsVisibleAndDismissalSurvivesDiskReopenAndOtherSettings() = runBlocking {
        assertFalse(UserPreferencesCompat(UserPreferences.getDefaultInstance()).hideTradeLinkHint)
        val directory = Files.createTempDirectory("trade-link-hint").toFile()
        val file = directory.resolve("preferences.pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = DataStoreFactory.create(UserPreferencesSerializer(), scope = firstScope, produceFile = { file })
            val repository = UserPreferencesRepository(UserPreferencesDataSource(store))
            assertFalse(repository.data.first().hideTradeLinkHint)
            repository.setHideTradeLinkHint(true)
            repository.setContentScale(0.95f)
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
            val reopened = DataStoreFactory.create(UserPreferencesSerializer(), scope = secondScope, produceFile = { file })
            assertTrue(reopened.data.first().hideTradeLinkHint)
        } finally {
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
            secondScope.coroutineContext[Job]!!.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}
