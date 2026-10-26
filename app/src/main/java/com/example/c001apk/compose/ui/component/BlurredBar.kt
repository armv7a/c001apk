package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalBarColor = staticCompositionLocalOf { Color.Transparent }
internal val LocalBarBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

@Composable
fun rememberBarBackdrop(): LayerBackdrop {
    val surface = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surface)
        drawContent()
    }
}

/** Uses the official miuix AGSL pipeline; Android 13 is the minimum supported version. */
@Composable
fun BlurredBar(backdrop: LayerBackdrop, content: @Composable () -> Unit) {
    Box(Modifier.barBlur(backdrop)) { content() }
}

@Composable
internal fun Modifier.barBlur(backdrop: LayerBackdrop, shape: Shape = RectangleShape): Modifier =
    textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadius = 25f,
        colors =
            BlurDefaults.blurColors(
                blendColors =
                    listOf(BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = 0.8f))),
            ),
    )
