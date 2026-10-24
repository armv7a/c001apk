package com.example.c001apk.compose.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.c001apk.compose.ThemeType
import com.example.c001apk.compose.constant.Constants.seedColors
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.util.CookieUtil
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/** Shared miuix palette for the Android Editable-based rich text editor. */
fun miuixViewColors(context: Context): Colors {
    val dark = CookieUtil.isDarkMode
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val accent =
        when {
            CookieUtil.materialYou ->
                Color(
                    context.getColor(
                        if (dark) android.R.color.system_accent1_200
                        else android.R.color.system_accent1_600,
                    ),
                )
            CookieUtil.themeType == ThemeType.Default -> base.primary
            else ->
                Color(
                    seedColors.getOrNull(ThemeType.entries.indexOf(CookieUtil.themeType))
                        ?: "FF${CookieUtil.seedColor}".toLongOrNull(16)
                        ?: seedColors[0],
                )
        }
    return base.copy(primary = accent)
}

/** Shared theme and feedback preferences for Compose content embedded in Android Views. */
@Composable
fun C001apkViewTheme(content: @Composable () -> Unit) {
    val preferences =
        UserPreferencesCompat.default()
            .copy(
                hapticFeedback = CookieUtil.hapticFeedback,
                hapticStrength = CookieUtil.hapticStrength,
                customHaptics = CookieUtil.customHaptics,
                reverseHapticEffects = CookieUtil.reverseHapticEffects,
            )
    CompositionLocalProvider(LocalUserPreferences provides preferences) {
        C001apkComposeTheme(
            darkTheme = CookieUtil.isDarkMode,
            materialYou = CookieUtil.materialYou,
            themeType = CookieUtil.themeType,
            seedColor = CookieUtil.seedColor,
            pureBlack = CookieUtil.pureBlack,
            paletteStyle = CookieUtil.paletteStyle,
            fontScale = CookieUtil.fontScale,
            contentScale = CookieUtil.contentScale,
            content = content,
        )
    }
}
