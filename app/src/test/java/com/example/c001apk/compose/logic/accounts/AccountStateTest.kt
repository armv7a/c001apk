package com.example.c001apk.compose.logic.accounts

import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import org.junit.Assert.*
import org.junit.Test

class AccountStateTest {
    private val alice = SavedAccount("123", "Alice", "test-token-a")
    private val bob = SavedAccount("456", "Bob", "test-token-b")
    @Test fun migrationPreservesCurrentLoginAndLocalSettings() {
        val legacy = UserPreferencesCompat.default().copy(isLogin = true, uid = alice.uid,
            username = alice.username, token = alice.token, szlmId = "test-device", seedColor = "123456")
        val migrated = AccountState.migrate(legacy)
        assertEquals(listOf(alice), migrated.savedAccounts)
        assertEquals("test-device", migrated.szlmId)
        assertEquals("123456", migrated.seedColor)
        assertEquals(migrated, AccountState.migrate(migrated))
    }
    @Test fun guestAndSwitchKeepSavedCredentials() {
        val saved = AccountState.save(AccountState.save(AccountState.migrate(UserPreferencesCompat.default()), alice), bob)
        val guest = AccountState.activate(saved, null)
        assertFalse(guest.isLogin)
        assertEquals("", guest.token)
        assertEquals(2, guest.savedAccounts.size)
        val switched = AccountState.switch(guest, alice.uid)
        assertEquals(alice.token, switched.token)
        assertTrue(switched.accountRevision > guest.accountRevision)
    }
    @Test fun removingInactiveAccountKeepsSessionAndRemovingActiveUsesGuest() {
        val saved = AccountState.save(AccountState.save(AccountState.migrate(UserPreferencesCompat.default()), alice), bob)
        val one = AccountState.remove(saved, alice.uid)
        assertEquals(saved.accountRevision, one.accountRevision)
        assertEquals(bob.token, one.token)
        val guest = AccountState.remove(one, bob.uid)
        assertFalse(guest.isLogin)
        assertTrue(guest.savedAccounts.isEmpty())
    }
    @Test fun staleExpiryCannotLogOutNewAccount() {
        val first = AccountState.save(AccountState.migrate(UserPreferencesCompat.default()), alice)
        val second = AccountState.save(first, bob)
        assertEquals(second, AccountState.expire(second, first.accountRevision))
        val expired = AccountState.expire(second, second.accountRevision)
        assertFalse(expired.isLogin)
        assertTrue(expired.savedAccounts.single { it.uid == bob.uid }.needsLogin)
        assertEquals(alice.token, expired.savedAccounts.single { it.uid == alice.uid }.token)
    }
    @Test fun vaultAndActiveAccountRoundTripInSingleProto() {
        val prefs = AccountState.save(AccountState.migrate(UserPreferencesCompat.default()), alice)
        assertEquals(prefs, UserPreferencesCompat(prefs.toProto()))
        assertFalse(prefs.toString().contains(alice.token))
        assertFalse(alice.toString().contains(alice.token))
    }
}
