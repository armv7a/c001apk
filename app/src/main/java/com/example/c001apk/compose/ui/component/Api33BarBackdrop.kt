package com.example.c001apk.compose.ui.component

import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Loaded only after the runtime API check in rememberBarBackdrop. */
@RequiresApi(33)
@Composable
internal fun rememberApi33BarBackdrop(): BarBackdrop {
    val surface = MiuixTheme.colorScheme.surface
    val layer = rememberLayerBackdrop {
        drawRect(surface)
        drawContent()
    }
    return remember(layer) { Api33BarBackdrop(layer) }
}

@RequiresApi(33)
private class Api33BarBackdrop(private val layer: LayerBackdrop) : BarBackdrop {
    override fun recordModifier(): Modifier = Modifier.layerBackdrop(layer)

    @Composable
    override fun blurModifier(shape: Shape): Modifier =
        Modifier.textureBlur(
            backdrop = layer,
            shape = shape,
            blurRadius = 25f,
            colors =
                BlurDefaults.blurColors(
                    blendColors =
                        listOf(BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = 0.8f))),
                ),
        )
}
