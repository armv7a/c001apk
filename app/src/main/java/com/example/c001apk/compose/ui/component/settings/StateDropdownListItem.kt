package com.example.c001apk.compose.ui.component.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun <T> StateDropdownListItem(
    isEnable: Boolean = true,
    value: T?,
    leadingImageVector: ImageVector? = null,
    headlineText: String,
    selections: List<SelectionItem<T>>,
    onValueChanged: (index: Int, value: T) -> Unit,
) =
    DropdownListItem(
        value,
        leadingImageVector,
        headlineText,
        selections,
        onValueChanged,
        enabled = isEnable,
    )
