package com.example.c001apk.compose.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import com.example.c001apk.compose.ui.component.currentWindowWidthSizeClass
import com.example.c001apk.compose.ui.search.SearchViewModel
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import com.example.c001apk.compose.util.CookieUtil.apiVersion
import com.example.c001apk.compose.util.CookieUtil.contentScale
import com.example.c001apk.compose.util.CookieUtil.customHaptics
import com.example.c001apk.compose.util.CookieUtil.fontScale
import com.example.c001apk.compose.util.CookieUtil.hapticFeedback
import com.example.c001apk.compose.util.CookieUtil.hapticStrength
import com.example.c001apk.compose.util.CookieUtil.imageFilter
import com.example.c001apk.compose.util.CookieUtil.imageQuality
import com.example.c001apk.compose.util.CookieUtil.isDarkMode
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.CookieUtil.materialYou
import com.example.c001apk.compose.util.CookieUtil.openInBrowser
import com.example.c001apk.compose.util.CookieUtil.paletteStyle
import com.example.c001apk.compose.util.CookieUtil.pureBlack
import com.example.c001apk.compose.util.CookieUtil.recordHistory
import com.example.c001apk.compose.util.CookieUtil.reverseHapticEffects
import com.example.c001apk.compose.util.CookieUtil.sdkInt
import com.example.c001apk.compose.util.CookieUtil.seedColor
import com.example.c001apk.compose.util.CookieUtil.showEmoji
import com.example.c001apk.compose.util.CookieUtil.showSquare
import com.example.c001apk.compose.util.CookieUtil.szlmId
import com.example.c001apk.compose.util.CookieUtil.themeType
import com.example.c001apk.compose.util.CookieUtil.token
import com.example.c001apk.compose.util.CookieUtil.uid
import com.example.c001apk.compose.util.CookieUtil.userAgent
import com.example.c001apk.compose.util.CookieUtil.username
import com.example.c001apk.compose.util.CookieUtil.versionCode
import com.example.c001apk.compose.util.CookieUtil.versionName
import com.example.c001apk.compose.util.CookieUtil.xAppDevice
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Created by bggRGjQaUbCoE on 2024/5/29 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var navController: NavHostController
    private val viewModel by viewModels<MainViewModel>()
    private val searchViewModel by viewModels<SearchViewModel>()
    private var stableStatusBarInset: Int? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!viewModel.preparingIdentity) handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.data?.let {
            navController.onOpenLink(
                context = this,
                url = it.toString(),
                title = null,
                needConvert = true,
                onSearch = { keyword ->
                    searchViewModel.saveHistory(keyword)
                },
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupStableStatusBarInsets()

        // Keep Android's animated launch icon while the initial device identity is prepared.
        // Subsequent launches reuse the cached identity; no extra Compose loading page is shown.
        val contentView = findViewById<View>(android.R.id.content)
        contentView.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (viewModel.preparingIdentity) return false
                    contentView.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )

        val initialPreferences = runBlocking {
            userPreferencesRepository.data.first()
        }

        setContent {
            navController = rememberNavController()

            val widthSizeClass = currentWindowWidthSizeClass()

            val userPreferences by
                userPreferencesRepository.data.collectAsStateWithLifecycle(
                    initialValue = initialPreferences,
                    lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current,
                )

            val preferences =
                if (userPreferences == null) {
                    return@setContent
                } else {
                    checkNotNull(userPreferences)
                }

            CompositionLocalProvider(
                LocalUserPreferences provides preferences,
            ) {
                LaunchedEffect(preferences.xAppDevice) {
                    if (preferences.xAppDevice.isEmpty()) viewModel.regenerateParams()
                }

                isLogin = preferences.isLogin
                szlmId = preferences.szlmId
                xAppDevice = preferences.xAppDevice
                uid = preferences.uid
                username = preferences.username
                token = preferences.token
                userAgent = preferences.userAgent
                sdkInt = preferences.sdkInt
                versionName = preferences.versionName
                versionCode = preferences.versionCode
                apiVersion = preferences.apiVersion
                imageQuality = preferences.imageQuality
                showEmoji = preferences.showEmoji
                showSquare = preferences.showSquare
                openInBrowser = preferences.openInBrowser
                imageFilter = preferences.imageFilter
                recordHistory = preferences.recordHistory
                materialYou = preferences.materialYou
                isDarkMode = preferences.isDarkMode()
                themeType = preferences.themeType
                paletteStyle = preferences.paletteStyle
                seedColor = preferences.seedColor
                pureBlack = preferences.pureBlack
                fontScale = preferences.fontScale
                contentScale = preferences.contentScale
                hapticFeedback = preferences.hapticFeedback
                hapticStrength = preferences.hapticStrength
                customHaptics = preferences.customHaptics
                reverseHapticEffects = preferences.reverseHapticEffects

                C001apkComposeTheme(
                    darkTheme = preferences.isDarkMode(),
                    themeType = preferences.themeType,
                    seedColor = preferences.seedColor,
                    materialYou = preferences.materialYou,
                    pureBlack = preferences.pureBlack,
                    paletteStyle = preferences.paletteStyle,
                    fontScale = preferences.fontScale,
                    contentScale = preferences.contentScale,
                ) {
                    Surface(
                        modifier =
                            Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background),
                    ) {
                        if (!viewModel.preparingIdentity) {
                            MainNavigation(
                                navController = navController,
                                badge = viewModel.badge,
                                resetBadge = viewModel::resetBadge,
                                widthSizeClass = widthSizeClass,
                            )
                        }
                    }
                }
            }

            LaunchedEffect(navController, viewModel.preparingIdentity) {
                if (!viewModel.preparingIdentity && savedInstanceState == null) {
                    handleIntent(intent)
                }
            }
        }
    }

    private fun setupStableStatusBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val currentTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val stableTop =
                stableStatusBarInset ?: currentTop.takeIf { it > 0 } ?: fallbackStatusBarHeight()
            if (stableStatusBarInset == null && stableTop > 0) {
                stableStatusBarInset = stableTop
            }
            if (stableTop == 0) {
                return@setOnApplyWindowInsetsListener insets
            }
            WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, stableTop, 0, 0))
                .build()
        }
    }

    private fun fallbackStatusBarHeight(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }
}
