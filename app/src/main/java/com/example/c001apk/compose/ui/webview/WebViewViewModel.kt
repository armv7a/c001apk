package com.example.c001apk.compose.ui.webview

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.accounts.AccountStore
import com.example.c001apk.compose.logic.accounts.SavedAccount
import com.example.c001apk.compose.util.CookieUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class WebViewViewModel @Inject constructor(private val accounts: AccountStore) : ViewModel() {
    private val revision = CookieUtil.accountSession.revision
    var error by mutableStateOf<String?>(null)
        private set

    fun setIsLogin(uid: String, username: String, token: String) {
        viewModelScope.launch {
            try {
                accounts.saveLogin(SavedAccount(uid, username, token), revision)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                error = "保存登录信息失败，请重新登录"
            }
        }
    }
}
