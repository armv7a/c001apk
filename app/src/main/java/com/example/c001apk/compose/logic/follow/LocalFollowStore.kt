package com.example.c001apk.compose.logic.follow

import androidx.datastore.core.DataStore
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import javax.inject.Inject
import javax.inject.Singleton

data class LocalFollowUser(val uid: String, val name: String, val avatar: String)

@Singleton
class LocalFollowStore
@Inject
constructor(private val preferences: DataStore<UserPreferencesCompat>) {
    val data
        get() = preferences.data

    suspend fun toggle(user: LocalFollowUser) {
        require(user.uid.toLongOrNull()?.let { it > 0 } == true) { "用户 UID 无效" }
        preferences.updateData { prefs ->
            val remaining = prefs.localFollows.filterNot { it.uid == user.uid }
            prefs.copy(
                localFollows =
                    if (prefs.localFollows.any { it.uid == user.uid }) remaining
                    else listOf(user) + remaining,
            )
        }
    }

    suspend fun remove(uid: String) = preferences.updateData {
        it.copy(localFollows = it.localFollows.filterNot { user -> user.uid == uid })
    }

    suspend fun setQuiet(enabled: Boolean) = preferences.updateData {
        it.copy(quietFollow = enabled)
    }

    suspend fun select(value: Int) = preferences.updateData {
        it.copy(followSelection = value.coerceIn(0, 5))
    }
}
