package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.LocalContentColor

val LocalDialogPrimaryAction = staticCompositionLocalOf { false }

@Composable
fun MiuixDialogButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colors =
        if (LocalDialogPrimaryAction.current) ButtonDefaults.buttonColorsPrimary()
        else ButtonDefaults.buttonColors()
    Button(onClick, modifier.fillMaxWidth(), enabled = enabled, colors = colors) {
        CompositionLocalProvider(LocalContentColor provides colors.contentColor) { content() }
    }
}
