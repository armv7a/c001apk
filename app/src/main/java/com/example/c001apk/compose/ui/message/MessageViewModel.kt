package com.example.c001apk.compose.ui.message

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.base.BaseViewModel
import com.example.c001apk.compose.util.CookieUtil.atcommentme
import com.example.c001apk.compose.util.CookieUtil.atme
import com.example.c001apk.compose.util.CookieUtil.contacts_follow
import com.example.c001apk.compose.util.CookieUtil.feedlike
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.CookieUtil.message
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.encodeCookieValue
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Created by bggRGjQaUbCoE on 2024/6/11 */
@HiltViewModel(assistedFactory = MessageViewModel.ViewModelFactory::class)
class MessageViewModel
@AssistedInject
constructor(
    @Assisted val url: String,
    networkRepo: NetworkRepo,
    blackListRepo: BlackListRepo,
    private val accounts: com.example.c001apk.compose.logic.accounts.AccountStore,
) : BaseViewModel(networkRepo, blackListRepo) {

    var fffList by mutableStateOf<List<String>>(emptyList())
        private set

    var badgeList by mutableStateOf(listOf(atme, atcommentme, feedlike, contacts_follow, message))
        private set

    init {
        loadingState = LoadingState.Success(emptyList())
    }

    @AssistedFactory
    interface ViewModelFactory {
        fun create(url: String): MessageViewModel
    }

    override suspend fun customFetchData() = networkRepo.getMessage(url, page, lastItem)

    private suspend fun fetchProfile(session: CookieUtil.AccountSession) {
        networkRepo.getProfile(session.uid).collect { result ->
            if (session.revision != CookieUtil.accountSession.revision) return@collect
            val data = result.getOrNull()
            if (data?.data != null) {
                fffList =
                    listOf(
                        data.data.feed?.id ?: "0",
                        data.data.follow ?: "0",
                        data.data.fans ?: "0",
                    )
                accounts.refreshProfile(
                    session.revision,
                    avatar = data.data.userAvatar,
                    username = encodeCookieValue(data.data.username),
                    level = data.data.level,
                    experience = data.data.experience,
                    nextLevelExperience = data.data.nextLevelExperience,
                )
            } else {
                result.exceptionOrNull()?.printStackTrace()
            }
        }
    }

    private suspend fun onCheckCount(session: CookieUtil.AccountSession) {
        networkRepo.checkCount().collect { result ->
            if (session.revision != CookieUtil.accountSession.revision) return@collect
            result.getOrNull()?.data?.let {
                CookieUtil.notification = it.notification
                badgeList =
                    listOf(
                        it.atme,
                        it.atcommentme,
                        it.feedlike,
                        it.contactsFollow,
                        it.message + it.notification,
                    )
            }
        }
    }

    private var refreshJob: Job? = null

    override fun refresh() = refresh(showIndicator = true)

    fun refreshSilently() = refresh(showIndicator = false)

    private fun refresh(showIndicator: Boolean) {
        if (!isLogin) return
        // A manual pull can show progress for an already running background refresh.
        if (showIndicator) isRefreshing = true
        if (refreshJob?.isActive == true) return
        val session = CookieUtil.accountSession
        refreshJob = viewModelScope.launch {
            try {
                coroutineScope {
                    launch { fetchProfile(session) }
                    launch { onCheckCount(session) }
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    fun clearBadge(index: Int) {
        badgeList = badgeList.mapIndexed { i, count -> if (index == i) null else count }
    }
}
