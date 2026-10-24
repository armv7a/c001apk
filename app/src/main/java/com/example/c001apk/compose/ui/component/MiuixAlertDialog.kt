package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/** Native window presentation, with a bounded body and buttons kept outside the body. */
@Composable
fun MiuixAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    WindowDialog(show = true, modifier = modifier, onDismissRequest = onDismissRequest) {
        Column(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
            icon?.invoke()
            if (title != null) {
                MiuixTheme(
                    textStyles = MiuixTheme.textStyles.copy(main = MiuixTheme.textStyles.title3),
                ) {
                    title()
                }
                Spacer(Modifier.height(16.dp))
            }
            if (text != null) {
                Box(
                    Modifier.weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    text()
                }
                Spacer(Modifier.height(20.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (dismissButton != null) Box(Modifier.weight(1f)) { dismissButton() }
                Box(Modifier.weight(1f)) {
                    CompositionLocalProvider(LocalDialogPrimaryAction provides true) {
                        confirmButton()
                    }
                }
            }
        }
    }
}
