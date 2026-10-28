package com.example.c001apk.compose.ui.component.cards

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Retain only colors, not cover bitmaps, when cards leave the viewport.
private val coverTitleColors = LruCache<String, Int>(64)

@Composable
internal fun rememberCoverTitleColor(imageUrl: String, drawable: Drawable?): Color? {
    var color by remember(imageUrl) { mutableStateOf(coverTitleColors.get(imageUrl)) }

    LaunchedEffect(imageUrl, drawable) {
        if (color != null) return@LaunchedEffect
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
        if (bitmap.isRecycled) return@LaunchedEffect

        color = withContext(Dispatchers.Default) {
            Palette.from(bitmap)
                .resizeBitmapArea(4096)
                // Neutral covers should produce a neutral title strip too.
                .clearFilters()
                .generate()
                .dominantSwatch
                ?.rgb
                ?.let { dominant ->
                    // Keep the cover's hue while giving white titles at least 5:1 contrast.
                    ColorUtils.blendARGB(dominant, android.graphics.Color.BLACK, 0.58f)
                }
        }
        color?.let { coverTitleColors.put(imageUrl, it) }
    }

    return color?.let { Color(it) }
}
