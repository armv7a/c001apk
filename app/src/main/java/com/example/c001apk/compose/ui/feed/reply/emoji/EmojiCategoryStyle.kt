package com.example.c001apk.compose.ui.feed.reply.emoji

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.toArgb
import com.example.c001apk.compose.R
import com.example.c001apk.compose.ui.theme.miuixViewColors

/** Keep the category foreground and selected fill in the same Miuix palette. */
internal fun TextView.updateEmojiCategoryStyle(selected: Boolean) {
    val colors = miuixViewColors(context)
    background = AppCompatResources.getDrawable(
        context,
        if (selected) R.drawable.selector_emoji_indicator_selected
        else R.drawable.selector_emoji_indicator,
    )?.mutate()
    if (selected) {
        val fill = (background as? RippleDrawable)
            ?.findDrawableByLayerId(android.R.id.background) as? GradientDrawable
        fill?.setColor(colors.secondaryVariant.toArgb())
    }
    setTextColor(
        (if (selected) colors.onSecondaryVariant else colors.onSurface).toArgb(),
    )
    isSelected = selected
}
