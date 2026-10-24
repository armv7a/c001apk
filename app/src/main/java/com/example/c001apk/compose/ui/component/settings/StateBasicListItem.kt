package com.example.c001apk.compose.ui.component.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun StateBasicListItem(
    modifier: Modifier = Modifier,
    isEnable: Boolean = true,
    headlineText: String? = null,
    supportingText: String? = null,
    leadingImageVector: ImageVector? = null,
    leadingPainter: Painter? = null,
    leadingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) =
    BasicListItem(
        modifier,
        headlineText,
        supportingText,
        leadingImageVector,
        leadingPainter,
        leadingText,
        trailingContent,
        enabled = isEnable,
        onClick = onClick,
    )
