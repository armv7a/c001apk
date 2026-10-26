package com.example.c001apk.compose.ui.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalBarColor = staticCompositionLocalOf { Color.Transparent }
internal val LocalBarBackdrop = staticCompositionLocalOf<BarBackdrop?> { null }

/** Does not expose any AGSL/RenderNode library types to older Android versions. */
internal interface BarBackdrop {
    fun recordModifier(): Modifier

    @Composable fun blurModifier(shape: Shape): Modifier
}

@Composable
internal fun rememberBarBackdrop(enabled: Boolean): BarBackdrop? =
    if (enabled && Build.VERSION.SDK_INT >= 33) rememberApi33BarBackdrop() else null

@Composable
internal fun BlurredBar(backdrop: BarBackdrop, content: @Composable () -> Unit) {
    Box(Modifier.barBlur(backdrop)) { content() }
}

@Composable
internal fun Modifier.barBlur(backdrop: BarBackdrop, shape: Shape = RectangleShape): Modifier =
    if (Build.VERSION.SDK_INT >= 33) then(backdrop.blurModifier(shape))
    else background(MiuixTheme.colorScheme.surface, shape)
