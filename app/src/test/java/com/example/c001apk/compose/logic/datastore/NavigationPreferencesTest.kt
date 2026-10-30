package com.example.c001apk.compose.logic.datastore

import com.example.c001apk.compose.UserPreferences
import androidx.datastore.core.DataStoreFactory
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPreferencesTest {
    @Test
    fun oldAndFreshPreferencesKeepPredictiveBackAndDefaultNavigationOrder() {
        val oldBytes = UserPreferences.newBuilder().setShowEmoji(false).build().toByteArray()
        val restored = UserPreferencesCompat(UserPreferences.parseFrom(oldBytes))
        assertTrue(restored.predictiveBack)
        assertEquals(listOf(0, 1, 2), restored.mainNavigationOrder)
        assertFalse(restored.showEmoji)
        assertTrue(UserPreferencesCompat.default().predictiveBack)
        assertEquals(listOf(0, 1, 2), UserPreferencesCompat.default().mainNavigationOrder)
    }

    @Test
    fun bothPredictiveBackChoicesAndEveryNavigationOrderSurviveUnrelatedChanges() {
        val orders = listOf(
            listOf(0, 1, 2), listOf(0, 2, 1), listOf(1, 0, 2),
            listOf(1, 2, 0), listOf(2, 0, 1), listOf(2, 1, 0),
        )
        for (enabled in listOf(false, true)) {
            for (order in orders) {
                val saved = UserPreferencesCompat.default().copy(
                    predictiveBack = enabled,
                    mainNavigationOrder = order,
                )
                val proto = saved.copy(fontScale = 1.2f, showEmoji = false).toProto()
                assertEquals(!enabled, proto.disablePredictiveBack)
                val restored = UserPreferencesCompat(UserPreferences.parseFrom(proto.toByteArray()))
                assertEquals(enabled, restored.predictiveBack)
                assertEquals(order, restored.mainNavigationOrder)
                assertEquals(1.2f, restored.fontScale)
                assertFalse(restored.showEmoji)
            }
        }
    }

    @Test
    fun repositoryWritesSurviveDiskReopenAndUnrelatedUpdates() = runBlocking {
        val directory = Files.createTempDirectory("navigation-preferences").toFile()
        val file = directory.resolve("preferences.pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = DataStoreFactory.create(UserPreferencesSerializer(), scope = firstScope, produceFile = { file })
            val repository = UserPreferencesRepository(UserPreferencesDataSource(store))
            assertTrue(repository.data.first().predictiveBack)
            repository.setPredictiveBack(false)
            repository.setMainNavigationOrder(listOf(2, 2, 0, 9))
            repository.setFontScale(1.1f)
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
            val reopened = DataStoreFactory.create(UserPreferencesSerializer(), scope = secondScope, produceFile = { file })
            val restored = reopened.data.first()
            assertFalse(restored.predictiveBack)
            assertEquals(listOf(2, 0, 1), restored.mainNavigationOrder)
            assertEquals(1.1f, restored.fontScale)
        } finally {
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
            secondScope.coroutineContext[Job]!!.cancelAndJoin()
            directory.deleteRecursively()
        }
    }

    @Test
    fun malformedStoredOrderDoesNotDropOrDuplicateMainPages() {
        val proto = UserPreferences.newBuilder()
            .addAllMainNavigationOrder(listOf(2, 2, -1, 99, 0))
            .build()
        assertEquals(listOf(2, 0, 1), UserPreferencesCompat(proto).mainNavigationOrder)
    }
}
