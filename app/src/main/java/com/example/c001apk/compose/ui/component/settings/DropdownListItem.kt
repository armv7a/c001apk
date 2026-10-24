package com.example.c001apk.compose.ui.component.settings

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    PreferenceCard {
        WindowDropdownPreference(
            title = headlineText,
            items = selections.map { it.label },
            selectedIndex = selections.indexOfFirst { it.value == value },
            enabled = enabled,
            startAction =
                leadingImageVector?.let { icon ->
                    {
                        Icon(
                            icon,
                            null,
                            Modifier.size(24.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                },
            onSelectedIndexChange = { index ->
                selections.getOrNull(index)?.let { onValueChanged(index, it.value) }
            },
        )
    }
}
