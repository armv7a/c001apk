package com.example.c001apk.compose.logic.accounts

import com.example.c001apk.compose.SavedAccountRecord
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat

/** Behavior ported from TG-Twilight/c001apk AccountStore (AGPL-3.0); see docs/TWILIGHT_PORT.md. */
data class SavedAccount(
    val uid: String,
    val username: String,
    val token: String,
    val avatar: String = "",
    val level: String = "",
    val experience: String = "",
    val nextLevelExperience: String = "",
) {
    val needsLogin
        get() = token.isBlank()

    override fun toString() = "SavedAccount(credentials=redacted)"

    fun toProto(): SavedAccountRecord =
        SavedAccountRecord.newBuilder()
            .setUid(uid)
            .setUsername(username)
            .setToken(token)
            .setAvatar(avatar)
            .setLevel(level)
            .setExperience(experience)
            .setNextLevelExperience(nextLevelExperience)
            .build()

    companion object {
        fun fromProto(value: SavedAccountRecord) =
            SavedAccount(
                value.uid,
                value.username,
                value.token,
                value.avatar,
                value.level,
                value.experience,
                value.nextLevelExperience,
            )

        fun fromPreferences(prefs: UserPreferencesCompat) =
            SavedAccount(
                prefs.uid,
                prefs.username,
                prefs.token,
                prefs.userAvatar,
                prefs.level,
                prefs.experience,
                prefs.nextLevelExperience,
            )

        fun validUid(uid: String) = Regex("^[0-9]{1,20}$").matches(uid)

        fun validToken(token: String) =
            token.isNotBlank() &&
                token.length <= 4096 &&
                token.none { it <= ' ' || it == ';' || it.code >= 127 }
    }
}

internal object AccountState {
    fun migrate(prefs: UserPreferencesCompat): UserPreferencesCompat {
        if (prefs.accountsVersion >= 1) return prefs
        val current = SavedAccount.fromPreferences(prefs)
        val valid =
            prefs.isLogin &&
                SavedAccount.validUid(current.uid) &&
                SavedAccount.validToken(current.token)
        return activate(
            prefs.copy(
                accountsVersion = 1,
                savedAccounts = if (valid) listOf(current) else emptyList(),
            ),
            if (valid) current else null,
            bumpRevision = false,
        )
    }

    fun activate(
        prefs: UserPreferencesCompat,
        account: SavedAccount?,
        bumpRevision: Boolean = true,
    ): UserPreferencesCompat =
        prefs.copy(
            isLogin = account != null,
            uid = account?.uid.orEmpty(),
            username = account?.username.orEmpty(),
            token = account?.token.orEmpty(),
            userAvatar = account?.avatar.orEmpty(),
            level = account?.level.orEmpty(),
            experience = account?.experience.orEmpty(),
            nextLevelExperience = account?.nextLevelExperience.orEmpty(),
            accountRevision = prefs.accountRevision + if (bumpRevision) 1 else 0,
        )

    fun save(prefs: UserPreferencesCompat, account: SavedAccount): UserPreferencesCompat {
        require(SavedAccount.validUid(account.uid) && SavedAccount.validToken(account.token)) {
            "登录信息不完整，请重新登录"
        }
        val old = prefs.savedAccounts.find { it.uid == account.uid }
        val merged =
            account.copy(
                avatar = account.avatar.ifEmpty { old?.avatar.orEmpty() },
                level = account.level.ifEmpty { old?.level.orEmpty() },
                experience = account.experience.ifEmpty { old?.experience.orEmpty() },
                nextLevelExperience =
                    account.nextLevelExperience.ifEmpty { old?.nextLevelExperience.orEmpty() },
            )
        return activate(
            prefs.copy(
                savedAccounts = prefs.savedAccounts.filterNot { it.uid == account.uid } + merged,
            ),
            merged,
        )
    }

    fun switch(prefs: UserPreferencesCompat, uid: String): UserPreferencesCompat {
        if (prefs.isLogin && prefs.uid == uid) return prefs
        val account =
            prefs.savedAccounts.find { it.uid == uid && !it.needsLogin } ?: error("账号已失效，请重新登录")
        return activate(prefs, account)
    }

    fun remove(prefs: UserPreferencesCompat, uid: String): UserPreferencesCompat {
        val updated = prefs.copy(savedAccounts = prefs.savedAccounts.filterNot { it.uid == uid })
        return if (prefs.uid == uid && prefs.isLogin) activate(updated, null) else updated
    }

    fun expire(prefs: UserPreferencesCompat, expectedRevision: Long): UserPreferencesCompat {
        if (!prefs.isLogin || prefs.accountRevision != expectedRevision) return prefs
        return activate(
            prefs.copy(
                savedAccounts =
                    prefs.savedAccounts.map {
                        if (it.uid == prefs.uid) it.copy(token = "") else it
                    },
            ),
            null,
        )
    }
}
