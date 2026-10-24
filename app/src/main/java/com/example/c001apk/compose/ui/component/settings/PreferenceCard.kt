package com.example.c001apk.compose.ui.component.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

private val LocalPreferenceInGroup = staticCompositionLocalOf { false }

/** One native card owns the shape and clipping for all of its preference rows. */
@Composable
fun PreferenceGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        title?.let { SmallTitle(text = it) }
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            CompositionLocalProvider(LocalPreferenceInGroup provides true) { content() }
        }
    }
}

@Composable
fun PreferenceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    if (LocalPreferenceInGroup.current) Column(modifier.fillMaxWidth(), content = content)
    else PreferenceGroup(modifier = modifier, content = content)
}
