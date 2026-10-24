package com.example.c001apk.compose.logic.accounts

import androidx.datastore.core.DataStore
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import com.example.c001apk.compose.util.CookieUtil
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The vault and the active account are written in the same DataStore transaction. */
@Singleton
class AccountStore @Inject constructor(private val preferences: DataStore<UserPreferencesCompat>) {
    private val mutex = Mutex()
    val data
        get() = preferences.data

    private suspend fun change(block: (UserPreferencesCompat) -> UserPreferencesCompat) =
        mutex.withLock {
            val result = preferences.updateData { block(AccountState.migrate(it)) }
            CookieUtil.publishAccount(result)
            result
        }

    suspend fun initialize() = change { it }

    suspend fun saveLogin(account: SavedAccount, expectedRevision: Long) = change {
        check(it.accountRevision == expectedRevision) { "账号已切换，请重新登录" }
        AccountState.save(it, account)
    }

    suspend fun switchTo(uid: String) = change { AccountState.switch(it, uid) }

    suspend fun useGuest() = change { if (it.isLogin) AccountState.activate(it, null) else it }

    suspend fun remove(uid: String) = change { AccountState.remove(it, uid) }

    suspend fun expire(expectedRevision: Long) = change {
        AccountState.expire(it, expectedRevision)
    }

    suspend fun refreshProfile(
        expectedRevision: Long,
        username: String? = null,
        avatar: String? = null,
        level: String? = null,
        experience: String? = null,
        nextLevelExperience: String? = null,
    ) = change { prefs ->
        if (!prefs.isLogin || prefs.accountRevision != expectedRevision) prefs
        else {
            val account =
                SavedAccount.fromPreferences(prefs)
                    .copy(
                        username = username ?: prefs.username,
                        avatar = avatar ?: prefs.userAvatar,
                        level = level ?: prefs.level,
                        experience = experience ?: prefs.experience,
                        nextLevelExperience = nextLevelExperience ?: prefs.nextLevelExperience,
                    )
            AccountState.activate(
                prefs.copy(
                    savedAccounts = prefs.savedAccounts.filterNot { it.uid == prefs.uid } + account,
                ),
                account,
                bumpRevision = false,
            )
        }
    }
}
