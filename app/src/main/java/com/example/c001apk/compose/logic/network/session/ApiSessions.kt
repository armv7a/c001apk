package com.example.c001apk.compose.logic.network.session

import android.content.Context
import com.example.c001apk.compose.util.CookieUtil
import com.google.gson.Gson

object ApiSessions {
    lateinit var store: ApiSessionStore
        private set

    fun initialize(context: Context) {
        val preferences = context.getSharedPreferences("coolapk_api_session", Context.MODE_PRIVATE)
        store =
            ApiSessionStore(
                read = { preferences.getString("state", null) },
                write = { preferences.edit().putString("state", it).commit() },
                active = {
                    val account = CookieUtil.accountSession
                    ApiSessionStore.Identity(
                        account.revision,
                        ApiSessionStore.digest(
                            Gson()
                                .toJson(
                                    listOf(
                                        CookieUtil.xAppDevice,
                                        account.isLogin,
                                        account.uid,
                                        account.token,
                                    ),
                                ),
                        ),
                    )
                },
            )
    }

    fun clear() {
        if (::store.isInitialized) store.clear()
    }
}
