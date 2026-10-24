package com.example.c001apk.compose.logic.accounts

import androidx.datastore.core.DataStoreFactory
import com.example.c001apk.compose.logic.datastore.UserPreferencesSerializer
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class AccountStoreIoTest {
    @Test
    fun accountSwitchSurvivesRealDiskReopenAndRejectsLateLogin() = runBlocking {
        val directory = Files.createTempDirectory("c001apk-account-test").toFile()
        val file = directory.resolve("preferences.pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data =
                DataStoreFactory.create(
                    UserPreferencesSerializer(),
                    scope = firstScope,
                    produceFile = { file },
                )
            val accounts = AccountStore(data)
            val initial = accounts.initialize()
            val alice =
                accounts.saveLogin(
                    SavedAccount("123", "Alice", "synthetic-token-a"),
                    initial.accountRevision,
                )
            val bob =
                accounts.saveLogin(
                    SavedAccount("456", "Bob", "synthetic-token-b"),
                    alice.accountRevision,
                )
            val guest = accounts.useGuest()
            assertTrue(
                runCatching {
                    accounts.saveLogin(
                        SavedAccount("789", "Late", "synthetic-late"),
                        bob.accountRevision,
                    )
                }
                    .isFailure,
            )
            assertEquals(guest, data.data.first())
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
            val reopened =
                DataStoreFactory.create(
                    UserPreferencesSerializer(),
                    scope = secondScope,
                    produceFile = { file },
                )
            val restored = AccountStore(reopened)
            val state = restored.initialize()
            assertFalse(state.isLogin)
            assertEquals(2, state.savedAccounts.size)
            assertEquals("synthetic-token-a", restored.switchTo("123").token)
            assertEquals("123", reopened.data.first().uid)
        } finally {
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
            secondScope.coroutineContext[Job]!!.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}
