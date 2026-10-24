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
import com.example.c001apk.compose.util.CookieUtil.uid
import com.example.c001apk.compose.util.encode
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
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

    private fun fetchProfile() =
        viewModelScope.launch(Dispatchers.IO) {
            val session = com.example.c001apk.compose.util.CookieUtil.accountSession
            networkRepo.getProfile(session.uid).collect { result ->
                if (
                    session.revision !=
                        com.example.c001apk.compose.util.CookieUtil.accountSession.revision
                )
                    return@collect
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
                        username = data.data.username.encode,
                        level = data.data.level,
                        experience = data.data.experience,
                        nextLevelExperience = data.data.nextLevelExperience,
                    )
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

    private fun onCheckCount() =
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.checkCount().collect { result ->
                result.getOrNull()?.data?.let {
                    com.example.c001apk.compose.util.CookieUtil.notification = it.notification
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

    override fun refresh() {
        if (!isLogin || isRefreshing) return
        isRefreshing = true
        viewModelScope.launch {
            try {
                joinAll(fetchProfile(), onCheckCount())
            } finally {
                isRefreshing = false
            }
        }
    }

    fun clearBadge(index: Int) {
        badgeList = badgeList.mapIndexed { i, count -> if (index == i) null else count }
    }
}
