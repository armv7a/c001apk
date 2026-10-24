package com.example.c001apk.compose.ui.component.settings

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.popup.WindowDropdownDialog

data class SelectionItem<T>(val label: String, val value: T)

@Composable
fun <T> DropdownListItem(
    value: T?,
    leadingImageVector: ImageVector? = null,
    headlineText: String,
    selections: List<SelectionItem<T>>,
    onValueChanged: (index: Int, value: T) -> Unit,
    enabled: Boolean = true,
) {
    var show by remember { mutableStateOf(false) }
    val currentOnChange by rememberUpdatedState(onValueChanged)
    val entry =
        remember(selections, value) {
            DropdownEntry(
                selections.mapIndexed { index, option ->
                    DropdownItem(
                        text = option.label,
                        selected = option.value == value,
                        onClick = { currentOnChange(index, option.value) },
                    )
                },
            )
        }
    BasicListItem(
        headlineText = headlineText,
        supportingText = selections.find { it.value == value }?.label.orEmpty(),
        leadingImageVector = leadingImageVector,
        enabled = enabled,
        holdDownState = show,
        onClick = { show = true },
    )
    WindowDropdownDialog(
        entry = entry,
        title = headlineText,
        dialogButtonString = "取消",
        show = show,
        onDismiss = { show = false },
        onDismissFinished = { show = false },
        dropdownColors = DropdownDefaults.dialogDropdownColors(),
    )
}
