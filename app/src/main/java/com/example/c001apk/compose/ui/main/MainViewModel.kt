package com.example.c001apk.compose.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import com.example.c001apk.compose.logic.smid.DigitalUnionRepository
import com.example.c001apk.compose.ui.base.PrefsViewModel
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.encode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

/** Created by bggRGjQaUbCoE on 2024/6/3 */
@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val networkRepo: NetworkRepo,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val digitalUnion: DigitalUnionRepository,
    private val accounts: com.example.c001apk.compose.logic.accounts.AccountStore,
) : PrefsViewModel(userPreferencesRepository) {

    var preparingIdentity by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            try {
                accounts.initialize()
                digitalUnion.ensureId()
            } finally {
                preparingIdentity = false
            }
            getCheckLoginInfo()
        }
    }

    var badge by mutableIntStateOf(0)
        private set

    private fun getCheckLoginInfo() {
        viewModelScope.launch {
            accounts.data
                .distinctUntilChangedBy { it.accountRevision }
                .collectLatest { prefs ->
                    badge = 0
                    val revision = prefs.accountRevision
                    networkRepo.checkLoginInfo().collect { result ->
                        if (CookieUtil.accountSession.revision != revision) return@collect
                        val failure =
                            result.exceptionOrNull()
                                as? com.example.c001apk.compose.logic.network.ApiFailure
                        if (prefs.isLogin && failure?.expiredSession == true)
                            accounts.expire(revision)
                        val response = result.getOrNull() ?: return@collect
                        val login = response.body()?.data
                        // Anonymous or partial responses never create/replace a saved account.
                        if (prefs.isLogin && login?.uid == prefs.uid) {
                            badge = login.notifyCount.badge
                            CookieUtil.atme = login.notifyCount.atme
                            CookieUtil.atcommentme = login.notifyCount.atcommentme
                            CookieUtil.feedlike = login.notifyCount.feedlike
                            CookieUtil.contacts_follow = login.notifyCount.contactsFollow
                            CookieUtil.message = login.notifyCount.message
                            accounts.refreshProfile(
                                revision,
                                username = login.username.encode,
                                avatar = login.userAvatar,
                            )
                        }
                        if (prefs.isLogin && response.body()?.message == "登录信息有误")
                            accounts.expire(revision)
                    }
                }
        }
    }

    fun resetBadge() {
        badge = 0
    }
}
