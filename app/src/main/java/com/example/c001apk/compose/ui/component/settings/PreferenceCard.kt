package com.example.c001apk.compose.ui.component.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class PreferencePosition {
    Single,
    First,
    Middle,
    Last,
}

val LocalPreferencePosition = staticCompositionLocalOf { PreferencePosition.Single }

/** Each settings row remains a lazy item; only the outer rows carry rounded corners. */
@Composable
fun PreferenceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val position = LocalPreferencePosition.current
    val last = position == PreferencePosition.Last || position == PreferencePosition.Single
    val outer =
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = if (last) 12.dp else 0.dp)
    if (position == PreferencePosition.Single) {
        Card(modifier = outer, content = content)
    } else {
        val color = MiuixTheme.colorScheme.surfaceContainer
        val top = if (position == PreferencePosition.First) 16.dp else 0.dp
        val bottom = if (last) 16.dp else 0.dp
        Column(
            modifier =
                if (position == PreferencePosition.Middle) outer.background(color)
                else outer.squircleSurface(color, top, top, bottom, bottom),
            content = content,
        )
    }
}
