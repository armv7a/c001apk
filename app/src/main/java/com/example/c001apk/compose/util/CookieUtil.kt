package com.example.c001apk.compose.util

import com.example.c001apk.compose.ThemeType
import com.example.c001apk.compose.constant.Constants.API_VERSION
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.constant.Constants.VERSION_CODE
import com.example.c001apk.compose.constant.Constants.VERSION_NAME
import com.example.c001apk.compose.logic.model.HapticStrength

/** Created by bggRGjQaUbCoE on 2024/6/3 */
object CookieUtil {

    var SESSID = EMPTY_STRING
    var isPreGetLoginParam = false
    var isGetLoginParam = false
    var isTryLogin = false
    var isGetCaptcha = false
    var isGetSmsLoginParam = false
    var isGetSmsToken = false

    var atme: Int? = null
    var atcommentme: Int? = null
    var feedlike: Int? = null
    var contacts_follow: Int? = null
    var message: Int? = null
    var notification: Int = 0

    data class AccountSession(
        val revision: Long,
        val isLogin: Boolean,
        val uid: String,
        val username: String,
        val token: String,
    ) {
        override fun toString() = "AccountSession(credentials=redacted)"
    }

    @Volatile
    var accountSession = AccountSession(0, false, "", "", "")
        private set

    val isLogin
        get() = accountSession.isLogin

    val uid
        get() = accountSession.uid

    val username
        get() = accountSession.username

    val token
        get() = accountSession.token

    @Synchronized
    fun publishAccount(prefs: com.example.c001apk.compose.logic.datastore.UserPreferencesCompat) {
        if (prefs.accountRevision < accountSession.revision) return
        val changed = prefs.accountRevision != accountSession.revision
        accountSession =
            AccountSession(
                prefs.accountRevision,
                prefs.isLogin,
                prefs.uid,
                prefs.username,
                prefs.token,
            )
        if (changed) {
            SESSID = ""
            com.example.c001apk.compose.logic.network.session.ApiSessions.clear()
            atme = null
            atcommentme = null
            feedlike = null
            contacts_follow = null
            message = null
            notification = 0
            isPreGetLoginParam = false
            isGetLoginParam = false
            isTryLogin = false
            isGetCaptcha = false
            isGetSmsLoginParam = false
            isGetSmsToken = false
        }
    }

    var szlmId = EMPTY_STRING
    var xAppDevice = EMPTY_STRING
    var userAgent = EMPTY_STRING
    var sdkInt = EMPTY_STRING
    var versionName = VERSION_NAME
    var versionCode = VERSION_CODE
    var apiVersion = API_VERSION
    var imageQuality = 0
    var showEmoji = true
    var imageFilter = true
    var isDarkMode = false
    var showSquare = true
    var openInBrowser = false
    var recordHistory = true
    var materialYou = true
    var themeType: ThemeType = ThemeType.Default
    var paletteStyle: Int = 0
    var seedColor = EMPTY_STRING
    var pureBlack = false
    var fontScale = 1.0f
    var contentScale = 1.0f
    var hapticFeedback = true
    var hapticStrength = HapticStrength.Medium
    var customHaptics = false
    var reverseHapticEffects = false
}
